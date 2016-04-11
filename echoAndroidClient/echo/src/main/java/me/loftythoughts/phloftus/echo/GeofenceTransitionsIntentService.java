package me.loftythoughts.phloftus.echo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick on 1/3/2016.
 *
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIntentService";

    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

     // This Intent is provided to Location
     // Services (inside a PendingIntent) when addGeofences() is called.
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(Constants.LOG_TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
                Log.i(Constants.LOG_TAG, "Geofence entered!");
            else
                Log.i(Constants.LOG_TAG, "Geofence exited!");

            Boolean available = (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER);

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            ArrayList<String> keys = new ArrayList<>();
            int status = available ? Constants.AVAILABLE : Constants.UNAVAILABLE;
            for (Geofence gf : triggeringGeofences) {
                String key = gf.getRequestId();
                keys.add(key);
            }

            // broadcast the triggered geofences
            Intent in = new Intent(Constants.ACTION);
            in.putStringArrayListExtra(Constants.TRIGGERED_ECHO_KEYS, keys);
            in.putExtra(Constants.ECHO_STATUS, status);
            LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        }
        else
        {
            // Log the error.
            Log.e(Constants.LOG_TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }


}