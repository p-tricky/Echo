package me.loftythoughts.phloftus.echo;

import android.graphics.Bitmap;
import android.os.Handler;

/**
 * Created by Patrick on 2/9/2016.
 */
public abstract class EchoReceivedCallback {
    private Handler mResponseHandler;
    private String awsKey;
    private int mScreenX;
    private int mScreenY;

    public EchoReceivedCallback(Handler handle, String key, int screenX, int screenY) {
        mResponseHandler = handle;
        awsKey = key;
        mScreenX = screenX;
        mScreenY = screenY;
    }

    public Handler getHandle() {
        return mResponseHandler;
    }

    public String getKey() {
        return awsKey;
    }

    public int getScreenX() {
        return mScreenX;
    }

    public int getScreenY() {
        return mScreenY;
    }

    abstract void onEchoReceived(Bitmap echo);
}
