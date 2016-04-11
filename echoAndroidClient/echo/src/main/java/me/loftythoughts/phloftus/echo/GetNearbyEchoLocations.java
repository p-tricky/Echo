package me.loftythoughts.phloftus.echo;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Patrick on 2/12/2016.
 *
 * This class encapsulates all of the logic required to get the Echoes in the
 * current location.
 *
 * It retrieves local echoes as a json array and passes them to the callback method,
 * which it posts to the main UI thread.
 */
public class GetNearbyEchoLocations extends AsyncTask<Void, Void, JsonArray> {
    private Location mLocation;
    private EchoLocationsReceivedCallback mCallback;

    public void setCallback(EchoLocationsReceivedCallback callback) {
        mCallback = callback;
    }

    public void setLocation(Location curLoc) {
        mLocation = curLoc;
    }

    @Override
    protected JsonArray doInBackground(Void... inputs) {

        // declarations
        boolean statusOk = true;
        InputStream echoServerInStream = null;
        HttpURLConnection connectionToEchoServer = null;
        BufferedReader echoInStreamReader = null;
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        JsonParser jParse = new JsonParser();
        JsonArray echoResponse = null;

        // query echo server for nearby echos and save response as json array
        try {
            // query echo server for echos near you
            // the echo server should respond with a json object containing the
            // echos within range of you
            URL url = new URL(
                    Constants.BACKEND_URL +
                            "getPicsTakenNearMe" +
                            "?lat=" + mLocation.getLatitude() + "&lon=" + mLocation.getLongitude());
            connectionToEchoServer = (HttpURLConnection) url.openConnection();
            if (connectionToEchoServer.getResponseCode() != 200) statusOk = false;

            // read the response and convert it to a Json Array
            echoServerInStream = connectionToEchoServer.getInputStream();
            echoInStreamReader = new BufferedReader(new InputStreamReader(echoServerInStream));
            while ((line = echoInStreamReader.readLine()) != null) responseBuilder.append(line);
            echoResponse = jParse.parse(responseBuilder.toString()).getAsJsonArray();
            Log.i(Constants.LOG_TAG, "EchoResponseAsJson: " + String.valueOf(echoResponse));
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
            statusOk = false;
        } finally {
            // free resources
            try {
                if (connectionToEchoServer != null)
                    connectionToEchoServer.disconnect();
                if (echoServerInStream != null)
                    echoServerInStream.close();
                if (echoInStreamReader != null)
                    echoInStreamReader.close();
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
                statusOk = false;
            }
        }

        // if everything went well, store LatLng markers for echos taken near us
        // then pass the markers to the main UI thread through onPostExecute
        if (statusOk) return echoResponse;
        return null;
    }

    @Override
    protected void onPostExecute(JsonArray result) {
        if (mCallback != null) mCallback.onLocationsReceived(result);
    }
}
