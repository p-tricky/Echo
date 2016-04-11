package me.loftythoughts.phloftus.echo;

import com.google.android.gms.location.Geofence;

/**
 * Created by Patrick on 1/2/2016.
 */
public class Constants {
    public static final String LOG_TAG = "PUT_LOG_TAG_HERE";
    public static final String BACKEND_URL = "PUT_SERVER_URL_HERE";

    // Pager stuff
    public static final int TOTAL_PAGES = 2;
    public static final int MAP_FRAGMENT = 0;
    public static final int CAMERA_FRAGMENT = 1;

    // google api client stuff
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final long CONNECTION_TIME_OUT_MS = 100;
    public static final long GEOFENCE_EXPIRATION_TIME = Geofence.NEVER_EXPIRE;
    public static final float GEOFENCE_RADIUS_IN_METERS = 75;
    public static final long SECONDS_BETWEEN_LOCATION_UPDATES = 3;  // google api claims that the interval is in milliseconds
                                                                    // but log cat tells a different story

    protected static final String ACTION = "GeofenceTransitionsIntentService";

    // Communication between IntentService and BroadcastReceiver
    protected static final String ECHO_STATUS = "status";
    protected static final int UNAVAILABLE = 0;
    protected static final int FETCHING = 1;
    protected static final int AVAILABLE = 2;
    protected static final int ERROR_STATUS = -1;
    protected static final String TRIGGERED_ECHO_KEYS = "triggered_echo_keys";

    protected static final String COGNITO_POOL_ID =  "******";
    protected static final String BUCKET_NAME =  "******";

    // messages
    protected static final int ECHO_DOWNLOAD = 0;
    protected static final int ECHO_UPLOAD = 1;
    protected static final int ECHO_DELETE = 2;

    public static final String AWS_KEY_EXTRA = "aws_key_extra";

    // fragment transaction tags
    public static final String MAP_FRAG_TRANS = "map_frag_trans";

    public static final String ECHO_UPLOAD_IN_PROGRESS = "upload_in_progress";
    public static final String CAMERA_IN_SELFIE_MODE = "selfie_mode";

    // shared preference key
    public static final String ECHO_PREFS = "echo_shared_preferences";
    public static final String LAST_LOCATION = "last_location";
}
