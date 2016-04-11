package me.loftythoughts.phloftus.echo;


import com.google.gson.JsonArray;

/**
 * Created by Patrick on 2/12/2016.
 */
public abstract class EchoLocationsReceivedCallback {
    abstract void onLocationsReceived(JsonArray echoData);
}
