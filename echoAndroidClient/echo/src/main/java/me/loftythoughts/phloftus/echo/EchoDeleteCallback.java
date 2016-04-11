package me.loftythoughts.phloftus.echo;

import android.os.Handler;

/**
 * Created by Patrick on 3/11/2016.
 */
public abstract class EchoDeleteCallback {
    private Handler mResponseHandler;
    private String awsKey;

    public EchoDeleteCallback(Handler handle, String key) {
        mResponseHandler = handle;
        awsKey = key;

    }

    public Handler getHandle() {
        return mResponseHandler;
    }

    public String getKey() {
        return awsKey;
    }

    abstract void onDeleted(boolean success);
}
