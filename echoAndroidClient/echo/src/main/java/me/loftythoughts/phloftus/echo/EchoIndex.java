package me.loftythoughts.phloftus.echo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Patrick on 1/8/2016.
 *
 * The index is a singleton class that keeps track of all relevant Echo data.
 *
 * Does everything from responding to location updates to  providing
 * asynchronous methods for connecting to Echo's backend.
 */
public class EchoIndex implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status>,
        PermissionEnabledListener
{

    private static EchoIndex sEchoIndex;           //private reference to the global index.
                                                   //other classes don't instantiate the index,
                                                   //they just request access.
                                                   //when they do, give them the private static index
    private Context mAppContext;
    private BiMap<String, Marker> keysAndMarkers;  //s3 keys and their corresponding map markers
    private Map<String, JsonObject> echoMetaData;  //misc echo data. currently only stores the
                                                   //state whether or not echo is in range
    private EchoHandler mEchoFetchThread;          //subclass of handler thread used for connecting
                                                   //to backend
    // the following locks ensure that the main UI thread
    // and the mEchoFetchThread play nice together
    private ReentrantReadWriteLock keyMarkerLock;
    private ReentrantReadWriteLock metaDataLock;

    private GoogleApiClient mGoogleApiClient;  //geofences and location updates are google services
                                               //we need a client to access them

    private PendingIntent mGeofenceTransitionService;       //used by google client when a geofence
    private BroadcastReceiver mGeofencTransitionsResponder; //is entered or exited
    private ArrayList<Geofence> mEchoGeofences;

    private Location mLastLocation;
    private LocationListener mLocListener;
    private ArrayList<MyLocationListener> mLocationListeners; //if another class needs location updates
                                                              //it should add itself to this list

    // Classes that need access to the index should call this method,
    // not the constructor.
    public static EchoIndex getIndex(Context c) {
        if (sEchoIndex == null) {
            sEchoIndex = new EchoIndex(c.getApplicationContext());
        }
        return sEchoIndex;
    }

    // constructor
    private EchoIndex(Context appContext) {
        keyMarkerLock = new ReentrantReadWriteLock();
        metaDataLock = new ReentrantReadWriteLock();
        keysAndMarkers = HashBiMap.create();
        echoMetaData = new HashMap<>();
        mAppContext = appContext;
        mLocListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) return;
                mLastLocation = location;
                for (MyLocationListener listener : mLocationListeners) {
                    listener.onLocationUpdated(location);
                }
            }
        };
        mLocationListeners = new ArrayList<>();
        mGoogleApiClient = new GoogleApiClient.Builder(mAppContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mEchoGeofences = new ArrayList<>();
    }

    public void start() {
        startThread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mAppContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)
                mGoogleApiClient.connect();
        } else {
            mGoogleApiClient.connect();
        }
        // register the receiver that will respond to geofence events
        IntentFilter filter = new IntentFilter(Constants.ACTION);
        LocalBroadcastManager.getInstance(mAppContext).registerReceiver(getGeofenceEventResponder(), filter);
    }

    public void stop() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocListener);
            LocalBroadcastManager.getInstance(mAppContext).unregisterReceiver(getGeofenceEventResponder());
            mGoogleApiClient.disconnect();
            mGeofenceTransitionService.cancel();
            mGeofenceTransitionService = null;
            clearIndex();
        }
        stopThread();

    }

    public void clearIndex() {
        keyMarkerLock.writeLock().lock();
        keysAndMarkers.clear();
        keyMarkerLock.writeLock().unlock();
        metaDataLock.writeLock().lock();
        echoMetaData.clear();
        metaDataLock.writeLock().unlock();
        removeGeofencesFromGoogleClient();
        mEchoGeofences.clear();
    }

    private void startThread() {
        mEchoFetchThread = new EchoHandler(mAppContext);
        mEchoFetchThread.start();
        mEchoFetchThread.getLooper();
    }

    private void stopThread() {
        if (mEchoFetchThread.isAlive())
            mEchoFetchThread.quit();
    }

    // After a geofence transition, we should update the status of any triggered Echoes.
    // If the user entered an Echo's geofence, then set the status of the echo to available.
    // Available means that the echo will open when clicked.  Also, change the color of the
    // marker to green.  When the user exits the geofence undo these changes
    public void updateIndex(ArrayList<String> keys, int status) {
        Bitmap iconBmp = null;
        switch (status) {
            // We have entered the echos' radii so fetch them
            case Constants.AVAILABLE:
                iconBmp = BitmapFactory.decodeResource(mAppContext.getResources(), R.drawable.echo_icon);
                break;
            case Constants.UNAVAILABLE:
                iconBmp = BitmapFactory.decodeResource(mAppContext.getResources(), R.drawable.echo_icon_red);
                break;
        }
        keyMarkerLock.writeLock().lock();
        for (String key : keys) {
            if (keysAndMarkers.containsKey(key)) {
                setStatus(key, status);
                Marker marker = getMarkerFromKey(key);
                if (marker != null)
                    try {
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(iconBmp));
                    } catch (IllegalStateException e) {
                        Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
                    }
            }
        }
        keyMarkerLock.writeLock().unlock();
    }

    // have worker thread download echo from aws
    public void getBitmap(EchoReceivedCallback callbackObj) {
        mEchoFetchThread.queueEchoDownload(callbackObj);
    }

    // have worker thread upload echo to aws and the server
    public void uploadEcho(EchoUploadCompletedCallback callback) {
        mEchoFetchThread.queueEchoUpload(callback);
    }

    // have worker thread upload echo to aws and the server
    public void deleteEcho(EchoDeleteCallback callback) {
        mEchoFetchThread.queueEchoDelete(callback);
    }

    public void removeEchoFromLocalIndex(String key) {
        keyMarkerLock.writeLock().lock();
        keysAndMarkers.remove(key);
        keyMarkerLock.writeLock().unlock();
        metaDataLock.writeLock().lock();
        echoMetaData.remove(key);
        metaDataLock.writeLock().unlock();
    }

    public void addKeyMarkerPairToLocalIndex(String key, Marker mapMark) {
        JsonObject metaData = new JsonObject();
        metaData.addProperty(Constants.ECHO_STATUS, Constants.UNAVAILABLE);
        keyMarkerLock.writeLock().lock();
        keysAndMarkers.put(key, mapMark);
        keyMarkerLock.writeLock().unlock();
        metaDataLock.writeLock().lock();
        echoMetaData.put(key, metaData);
        metaDataLock.writeLock().unlock();
    }

    public Marker getMarkerFromKey(String key) {
        Marker mark;
        keyMarkerLock.readLock().lock();
        mark = keysAndMarkers.get(key);
        keyMarkerLock.readLock().unlock();
        return mark;
    }

    public String getKeyFromMarker(Marker mark) {
        String key;
        keyMarkerLock.readLock().lock();
        key = keysAndMarkers.inverse().get(mark);
        keyMarkerLock.readLock().unlock();
        return key;
    }

    public int getStatusFromKey(String key) {
        int status = Constants.ERROR_STATUS;
        metaDataLock.readLock().lock();
        JsonObject data = echoMetaData.get(key);
        if (data != null && data.has(Constants.ECHO_STATUS)) {
            status = data.get(Constants.ECHO_STATUS).getAsInt();
        }
        metaDataLock.readLock().unlock();
        return status;
    }

    public void setStatus(String key, int status) {
        metaDataLock.writeLock().lock();
        if (echoMetaData.containsKey(key))
            echoMetaData.get(key).addProperty(Constants.ECHO_STATUS, status);
        metaDataLock.writeLock().unlock();
    }

    public Location getLastLocation() {
        return mLastLocation;
    }

    public void addLocationListener(MyLocationListener listener) {
       mLocationListeners.add(listener);
    }

    public void removeLocationListener(MyLocationListener listener) {
        mLocationListeners.remove(listener);
    }

    public String toString() {
        String ret = "Keys and Marekers: \n";
        for (Map.Entry<String, Marker> pair : keysAndMarkers.entrySet())
            ret += pair.getKey() + ": " + pair.getValue().getId() + "\n";
        ret += "MetaData: \n";
        for (Map.Entry<String, JsonObject> pair : echoMetaData.entrySet())
            ret += pair.getKey() + ": " + pair.getValue() + "\n";
        return ret;
    }


    ////////// GOOGLE API STUFF //////////

    /// Google API Client Callbacks ///
    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(Constants.SECONDS_BETWEEN_LOCATION_UPDATES);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, mLocListener);
        } catch (SecurityException e) {}
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(Constants.LOG_TAG, "Connection suspended!!");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(Constants.LOG_TAG, "Connection failed!!");
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess())
            Log.i(Constants.LOG_TAG, "Geofence success!");
        else
            Log.e(Constants.LOG_TAG, "Geofence failure!");

    }

    // We need to publish geofences with google
    // Once published, google will tell us when we enter/exit geofences
    public void addGeofencesToGoogleClient(ArrayList<Geofence> geofences) {
        if (geofences.isEmpty()) return;
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mAppContext, "Connection to gAPI failed!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (!mEchoGeofences.isEmpty())
                removeGeofencesFromGoogleClient();
            mEchoGeofences = geofences;
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    // remove fences from google client (doesn't touch mEchoGeofences)
    public void removeGeofencesFromGoogleClient() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mAppContext, "Connection to gAPI failed!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mEchoGeofences);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofenceTransitionService == null) {
            Intent intent = new Intent(mAppContext, GeofenceTransitionsIntentService.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            mGeofenceTransitionService = PendingIntent.getService(mAppContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofenceTransitionService;
    }

    // Returns the broadcast receiver that communicates with the intent service
    private BroadcastReceiver getGeofenceEventResponder() {
        if (mGeofencTransitionsResponder != null) {
            return mGeofencTransitionsResponder;
        }
        mGeofencTransitionsResponder = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ArrayList<String> keys = intent.getStringArrayListExtra(Constants.TRIGGERED_ECHO_KEYS);
                int status = intent.getIntExtra(Constants.ECHO_STATUS, Constants.UNAVAILABLE);
                updateIndex(keys, status);
            }
        };
        return mGeofencTransitionsResponder;
    }


    private void logSecurityException(SecurityException securityException) {
        Log.e(Constants.LOG_TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    // It's possible that the index tried to connect to google location services before
    // location permissions were enabled, in this case the connection fails.  For this
    // reason we need to reconnect after permissions are enabled.
    @Override
    public void onPermissionEnabled() {
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }
}
