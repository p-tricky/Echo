package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Patrick on 2/7/2016.
 *
 * The EchoHandler processes Echo upload, download, and delete requests.
 *
 * The EchoHandler thread is tied to the application's main UI thread.
 * The UI thread puts messages on the EchoHandler's queue.  The message
 * will provide all the info that the EchoHandler needs to fulfill the
 * upload, download, or delete request.
 */
public class EchoHandler extends HandlerThread {
    private Handler mHandler;  // Handles the messages on our queue
    private Context mContext;  // Needed for Amazon s3 uploads/downloads


    // Constructor
    public EchoHandler(Context c) {
        super("EchoHandler");
        mContext = c; // needed for uploading and downloading from s3
    }

    // The following 3 methods are used by the UI thread.
    // They all methods pass in unique callbacks that provide useful info
    // to the EchoHandler as well as callbacks to run once the EchoHandler
    // completes and returns control to the UI thread
    public void queueEchoDownload(EchoReceivedCallback callback) {
        mHandler.obtainMessage(Constants.ECHO_DOWNLOAD, callback).sendToTarget();
    }

    public void queueEchoUpload(EchoUploadCompletedCallback callback) {
        mHandler.obtainMessage(Constants.ECHO_UPLOAD, callback).sendToTarget();
    }

    public void queueEchoDelete(EchoDeleteCallback callback) {
        mHandler.obtainMessage(Constants.ECHO_DELETE, callback).sendToTarget();
    }

    // The looper is ready to start processing messages from the UI thread.
    // We need to tell it how to handle the messages.  We do so with the handler.
    @Override
        protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == Constants.ECHO_DOWNLOAD) {
                    Log.i(Constants.LOG_TAG, "Echo download requested!");
                    EchoReceivedCallback callback = (EchoReceivedCallback) msg.obj;
                    handleDownloadRequest(callback);
                }
                else if (msg.what == Constants.ECHO_UPLOAD) {
                    Log.i(Constants.LOG_TAG, "Echo upload requested!");
                    EchoUploadCompletedCallback callback = (EchoUploadCompletedCallback) msg.obj;
                    handleUploadRequest(callback);
                }
                else if (msg.what == Constants.ECHO_DELETE) {
                    Log.i(Constants.LOG_TAG, "Echo delete requested!");
                    EchoDeleteCallback callback = (EchoDeleteCallback) msg.obj;
                    handleDeleteRequest(callback);
                }
            }
        };

    }

    // Handling upload/download/deletes is a bit wonky.
    //
    // All the Echos are stored as jpg files in an s3 bucket.
    // There is a minimal backend that acts as a datastore, relating
    // an Echo's aws key to its gps coordinates.
    //
    // Normally all upload/download/deletes would go through the backend.
    // The android client would send a request to the backend server
    // and the server would fetch/delete/store the Echo jpg in S3.
    // However, the server I am using is super cheap and it's not hosted by aws.
    // For me, the app is much more responsive when it uploads/downloads directly from the
    // s3 bucket.  Thus, the app's network requests look a bit odd because they are split into
    // 2 steps.  One step uses an s3client to work with jpg files, and the other uses traditional
    // JSON http requests to manipulate the key-location pairs in the db server
    //
    private void handleDownloadRequest(final EchoReceivedCallback callback) {
        try {
            String aws_key = callback.getKey(); // get an aws_key to download
            Log.i(Constants.LOG_TAG, "Downloading Echo jpg for " + aws_key);

            // download the echo jpg file as an s3 object
            AmazonS3 s3Client = S3Util.getS3Client(mContext);
            S3Object echoS3 = s3Client.getObject(Constants.BUCKET_NAME, aws_key);
            Bitmap echoBig = BitmapFactory.decodeStream(echoS3.getObjectContent());

            //adjust uncompressed image for screen size
            final Bitmap echo = Bitmap.createScaledBitmap(echoBig, callback.getScreenX(),
                    callback.getScreenY(), false);
            callback.getHandle().post(new Runnable() { // Get a handle to the main UI thread from the
                                                       // callback post the callback method.
                @Override
                public void run() {
                    callback.onEchoReceived(echo);
                }
            });
        } catch (AmazonS3Exception s3e) {
            if (s3e.getErrorCode().equals("NoSuchKey")) {
                try { // Key isn't in s3 bucket, so try to delete it from the database.
                    URL deleteURL = new URL(Constants.BACKEND_URL + "pics/" + callback.getKey());
                    HttpURLConnection echoServerDeleteConn = (HttpURLConnection) deleteURL.openConnection();
                    echoServerDeleteConn.setRequestMethod("DELETE");
                    Log.i(Constants.LOG_TAG, "Attempting to delete missing echo from server.  Response code: "
                            + echoServerDeleteConn.getResponseCode());
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
                }
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Error downloading Echo", e);
        }
    }

    public void handleUploadRequest(final EchoUploadCompletedCallback callback) {
        boolean successStatus = true;
        try {
            String aws_key = callback.getKey(); // get aws_key from callback
            Bitmap echo = callback.getEcho();   // get Echo from callback so we can upload it

            // compress the Echo and send it to byte array stream
            AmazonS3 s3Client = S3Util.getS3Client(mContext);
            ByteArrayOutputStream echoOutStr = new ByteArrayOutputStream();
            echo.compress(Bitmap.CompressFormat.JPEG, 50, echoOutStr);
            byte[] echoData = echoOutStr.toByteArray();
            InputStream is = new ByteArrayInputStream(echoData);

            // fill out the meta data for aws s3
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(echoData.length);
            meta.setContentType("image/jpeg");
            PutObjectResult result = s3Client.putObject(Constants.BUCKET_NAME, aws_key, is, meta);

            // We uploaded the Echo jpg to s3, so now we need a record of it in our echo DB server
            Location location = callback.getLocation();
            JSONObject postObj = new JSONObject();
            postObj.put("lat", location.getLatitude());
            postObj.put("lon", location.getLongitude());
            postObj.put("aws_key", aws_key);
            URL postURL = new URL(Constants.BACKEND_URL + "pics");

            HttpURLConnection echoServerPostConn = (HttpURLConnection)
                    postURL.openConnection();    // Open connection to server

            // Fill out request headers
            echoServerPostConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            echoServerPostConn.setRequestProperty("Accept", "application/json");
            echoServerPostConn.setRequestMethod("POST");

            echoServerPostConn.setDoInput(true); // We will be writing to server we connected with
            echoServerPostConn.setDoOutput(true);// We expect the server to write back
            OutputStreamWriter echoServerWriter = new // create stream for server to write to
                    OutputStreamWriter(echoServerPostConn.getOutputStream());

            // Send post request to server and close connection.
            echoServerWriter.write(postObj.toString());
            echoServerWriter.flush();
            echoServerWriter.close();

            Log.i(Constants.LOG_TAG, "EchoPostResponse: " + echoServerPostConn.getResponseMessage());
            if (echoServerPostConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                successStatus = false;
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Error uploading Echo", e);
            successStatus = false;
        } finally {
            final boolean finalStatus = successStatus;
            callback.getHandle().post(new Runnable() {
                @Override
                public void run() {
                    callback.onUploadCompleted(finalStatus);
                }
            });
        }

    }

    public void handleDeleteRequest(final EchoDeleteCallback callback) {
        boolean status = true;
        try {
            // Use delete API to delete Echo's record
            URL deleteURL = new URL(Constants.BACKEND_URL + "pics/" + callback.getKey());
            HttpURLConnection echoServerDeleteConn = (HttpURLConnection)deleteURL.openConnection();
            echoServerDeleteConn.setRequestMethod("DELETE");
            Log.i(Constants.LOG_TAG, "EchoDeleteResponse: " + echoServerDeleteConn.getResponseCode());
            status = (echoServerDeleteConn.getResponseCode() == HttpURLConnection.HTTP_OK);

            // if we successfully deleted the Echo from the db server, then delete it from S3 too
            if (status) {
                AmazonS3 s3Client = S3Util.getS3Client(mContext);
                s3Client.deleteObject(Constants.BUCKET_NAME, callback.getKey());
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
            status = false;
        } finally {
            final boolean finalStatus = status;
            callback.getHandle().post(new Runnable() {
                @Override
                public void run() {
                    callback.onDeleted(finalStatus);
                }
            });

        }
    }
}
