package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Patrick on 1/2/2016.
 *
 * The MyMapFragment class displays Echoes on the map, and opens Echoes
 * when clicked, if they are within a set distance of the user.
 */
public class MyMapFragment extends SupportMapFragment implements
        GoogleMap.OnMarkerClickListener, OnMapReadyCallback, MyLocationListener {
    static final int POSITION_IN_PAGER = 0;

    private GoogleMap mMap;
    private PagerListener mPagerListener;

    private boolean refreshNeeded = true;
    private boolean refreshing = false;

    // after viewing an echo, the map is repopulated
    // when repopulating it is possible that the echo has not yet been
    // deleted on the server
    // if so, we need to remember it, so that we don't put it back on the map

    private Set<String> mDeletesInProgress = new HashSet<>();

    private Location mLastLocation;

    // used for map camera
    private LatLng mCenter = null;
    private float mZoom = 20;
    private float mTilt = 0;
    private float mBearing = 0;

    private UUID mID = UUID.randomUUID();

    public MyMapFragment() {}

    public static MyMapFragment newInstance(Location loc) {
        Bundle args = new Bundle();
        args.putParcelable(Constants.LAST_LOCATION, loc);
        MyMapFragment fragment = new MyMapFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (getArguments() != null) {
            mLastLocation = getArguments().getParcelable(Constants.LAST_LOCATION);
            Log.i(Constants.LOG_TAG, "Map's Location: " + mLastLocation);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PagerListener) {
            mPagerListener = (PagerListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PagerListener interfaces");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EchoIndex.getIndex(getContext()).addLocationListener(this);
        if (mMap == null)
            getMapAsync(this);
        refreshNeeded = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        EchoIndex.getIndex(getContext()).removeLocationListener(this);
        EchoIndex.getIndex(getContext()).clearIndex();
        if (mMap != null)
            mMap.clear();
        mCenter = null;
        Log.i(Constants.LOG_TAG, "map on pause!");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setScrollGesturesEnabled(false);   // Since the app allows swipe
                                                                // navigation, we should disable
                                                                // the scroll gesture
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setOnMarkerClickListener(this);
        Log.i(Constants.LOG_TAG, "Getting Map!");
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {}
        if (mLastLocation != null) centerMap(mLastLocation);
    }

    // Pings the server in search of Echoes in the user's current area.
    // If Echoes are found, markers will be added to the map, and the
    // index will create geofences for the Echoes
    public void getAndDisplayNearbyEchos(Location myLocation) {
        Log.i(Constants.LOG_TAG, "Getting echoes!");
        if (mMap != null && myLocation != null) {
            GetNearbyEchoLocations getNearbyEchoLocationsTask = new GetNearbyEchoLocations();
            getNearbyEchoLocationsTask.setLocation(myLocation);
            EchoLocationsReceivedCallback callback = new EchoLocationsReceivedCallback() {
                @Override
                void onLocationsReceived(JsonArray result) {
                    if (mMap != null) {
                        EchoIndex.getIndex(getContext()).clearIndex();
                        mMap.clear();
                        if (result == null) {
                            Toast.makeText(getActivity(), "Error connecting to echo server!",
                                    Toast.LENGTH_SHORT).show();
                            refreshing = false;
                        } else {
                            ArrayList<Geofence> geofences = new ArrayList<>();
                            for (JsonElement nearbyEcho : result) {
                                String key = nearbyEcho.getAsJsonObject().get("aws_key").getAsString();
                                Log.i(Constants.LOG_TAG, "Creating marker and geofence for " + key);
                                if (!mDeletesInProgress.contains(key)) {
                                    String[] coords = nearbyEcho.getAsJsonObject().get("location").getAsString().split(",");
                                    LatLng markerLocation = new LatLng(Double.parseDouble(coords[0]),
                                            Double.parseDouble(coords[1]));
                                    Bitmap iconBmp = BitmapFactory.decodeResource(getResources(), R.drawable.echo_icon_red);
                                    EchoIndex.getIndex(getContext()).addKeyMarkerPairToLocalIndex(key, mMap.addMarker(new MarkerOptions().position(markerLocation)
                                            .icon(BitmapDescriptorFactory.fromBitmap(iconBmp))));
                                    geofences.add(new Geofence.Builder()
                                            .setRequestId(key)
                                            .setCircularRegion(
                                                    Double.parseDouble(coords[0]), // lat
                                                    Double.parseDouble(coords[1]), // lon
                                                    Constants.GEOFENCE_RADIUS_IN_METERS
                                            )
                                            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_TIME)
                                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                            .build()
                                    );
                                }
                            }
                            EchoIndex.getIndex(getContext()).addGeofencesToGoogleClient(geofences);
                        }
                        refreshNeeded = false;
                        refreshing = false;
                    }
                }
            };
            getNearbyEchoLocationsTask.setCallback(callback);
            getNearbyEchoLocationsTask.execute();
        } else {
            refreshing = false;
        }
    }


    // When a marker is clicked, you should check to see if the user is within the clicked
    // Echo's geofence.  If not within the geofence, the user doesn't have access to view that
    // echo so just return.  If within the geofence, then show the echo and delete it from
    // server and s3 bucket
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(Constants.LOG_TAG, "Clicked: " + marker.getId());
        Log.i(Constants.LOG_TAG, EchoIndex.getIndex(getContext()).toString());
        final String key = EchoIndex.getIndex(getContext()).getKeyFromMarker(marker);
        if (EchoIndex.getIndex(getContext()).getStatusFromKey(key) != Constants.AVAILABLE) {
            return true;  // you're not in range so return
        }

        // we will resize the downloaded echo so that it is the same size as the screen
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);

        // Download the echo from s3
        EchoReceivedCallback echoReceivedCallback = new EchoReceivedCallback(
                new Handler(), key, size.x, size.y) {
            @Override
            public void onEchoReceived(final Bitmap echo) {
                final EchoViewerFragment frag = new EchoViewerFragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
                        View v = inflater.inflate(R.layout.fragment_echo_viewer, container, false);
                        ImageView display = (ImageView) v.findViewById(R.id.echo_viewer);
                        display.setImageBitmap(echo);
                        return v;
                    }
                };

                // now we have the requested echo, and we are going to show it
                if (!isVisible()) {   // if map holder fragment is no longer visible, then the
                                      // user must have navigated away, so no need to show
                                      // requested echo
                    return;
                }

                // mapfragment still visible so show echo
                FragmentTransaction trans = getFragmentManager().beginTransaction();
                trans.replace(R.id.guest_spot, frag);
                trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                trans.addToBackStack(null);
                trans.commit();


                // Echoes are only viewed once, so now that it has been opened, delete it
                // from server
                mDeletesInProgress.add(key);
                EchoDeleteCallback deleteCallback = new EchoDeleteCallback(new Handler(), key) {
                    @Override
                    void onDeleted(boolean success) {
                        mDeletesInProgress.remove(key);
                        Marker deletedMarker = EchoIndex.getIndex(getContext()).getMarkerFromKey(key);
                        if (deletedMarker != null && mMap != null) {
                            deletedMarker.remove();
                        }
                        EchoIndex.getIndex(getContext()).removeEchoFromLocalIndex(key);
                        if (success) {
                            Log.i(Constants.LOG_TAG, "Echo successfully deleted!");
                        }
                    }
                };
                EchoIndex.getIndex(getContext()).deleteEcho(deleteCallback);

                // Only display the echo for 7 seconds
                // Then delete the echo from the server permanently
                new CountDownTimer(7000, 1000) {
                    boolean backEnabled = true;
                    public void onTick(long ms) {
                        Log.i(Constants.LOG_TAG, "Tick: " + ms);
                    }
                    public void onFinish() {
                        if (frag.isVisible())
                            getFragmentManager().popBackStack();
                    }
                }.start();
            };
        };
        EchoIndex.getIndex(getContext()).getBitmap(echoReceivedCallback);
        return true;
    }

    // Centers the map's camera on the user's location
    public void centerMap(Location lastLoc) {
        if (lastLoc == null || mPagerListener == null) return;  //check that the new location is
                                                                // valid  and that MapFragment
                                                                // has been attached to the activity
        if (mPagerListener.getPage() == POSITION_IN_PAGER && isVisible()) { // if the user is
                                                                            // looking at the map
            if (mMap != null) {  // and the map is setup
                LatLng newCenter = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
                if (mCenter != null) {  // The camera has moved, so it's possible that the zoom and
                                        // tilt have been adjusted by the user.  We should use user-
                                        // defined values.

                    if (Math.abs( mCenter.latitude - mMap.getCameraPosition().target.latitude) +
                            Math.abs(mCenter.longitude - mMap.getCameraPosition().target.longitude)
                            > .00005) { //the camera is currently moving so don't reanimate
                        mCenter = mMap.getCameraPosition().target;
                        return;
                    }

                    mZoom = Math.max(mMap.getCameraPosition().zoom, 15);
                    mTilt = mMap.getCameraPosition().tilt;    // angle of camera in XZ plane 0-90 (0 is overhead view)
                    mBearing = mMap.getCameraPosition().bearing; // angle of camera in YZ plane (0=North, 90=East, ...)

                    mMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition(newCenter, mZoom, mTilt, mBearing))
                    );
                } else { // the camera hasn't moved yet, so move it to the starting position
                    mMap.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition(newCenter, mZoom, mTilt, mBearing))
                    );
                }
                mCenter = newCenter;    //Keep track of camera center so can tell when camera
                                        // is moving due to user input
            }
        }
    }

    // Map camera should follow user.
    @Override
    public void onLocationUpdated(Location current) {
        if (current != null) mLastLocation = current;
        centerMap(current);
        if (refreshNeeded && !refreshing) {
            refreshing = true;
            getAndDisplayNearbyEchos(current);
        }
    }

    public void setRefreshNeeded(boolean isNeeded) {
        Log.i(Constants.LOG_TAG, "Refresh needed!");
        refreshNeeded = isNeeded;
    }

}
