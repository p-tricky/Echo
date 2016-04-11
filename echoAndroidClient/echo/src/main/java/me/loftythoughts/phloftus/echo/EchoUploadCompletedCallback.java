package me.loftythoughts.phloftus.echo;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Handler;

/**
 * Created by Patrick on 2/27/2016.
 */
public abstract class EchoUploadCompletedCallback {
    private Handler mResponseHandler;
    private String awsKey;
    private Bitmap mEcho;
    private Location mLocaton;

    public EchoUploadCompletedCallback(Handler handle, String key, Bitmap echo, Location takenAt) {
        mResponseHandler = handle;
        awsKey = key;
        mEcho = echo;
        mLocaton = takenAt;

    }

    public Handler getHandle() {
        return mResponseHandler;
    }

    public String getKey() {
        return awsKey;
    }

    public Bitmap getEcho() {
        return mEcho;
    }

    public Location getLocation() {
        return mLocaton;
    }

    abstract void onUploadCompleted(boolean success);
}
