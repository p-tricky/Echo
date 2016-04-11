package me.loftythoughts.phloftus.echo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick on 2/11/2016.
 */
public class EchoCameraFragment extends Fragment
{
    private final int POSITION_IN_PAGER = 1;

    private Camera mCamera; // Provides access to android camera.
    private View mProgressContainer;  // Spinning loading gif that overlays camera briefly
                                      // after picture is taken.
    private SurfaceView mSurfaceView; // Blank view that previews the camera
    private ImageButton mTakePictureButton; // Click this button to take a picture.
    private Boolean mSelfieMode;            // If true, then use front-facing camera.
    private UploaderFragment mUploaderFragment; // The fragment that will host the Echo,
                                                // after it is captured, but before it is
                                                // uploaded.

    // Constructor
    public EchoCameraFragment() {
    }

    // Slightly different static constructor that takes a boolean
    // that defines which type of camera should be used.
    public static EchoCameraFragment newInstance(boolean selfieMode) {
        EchoCameraFragment fragment = new EchoCameraFragment();
        Bundle args = new Bundle();
        args.putBoolean(Constants.CAMERA_IN_SELFIE_MODE, selfieMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) { // If argument passed to constructor, then use
                                      // it to determine which camera to use.
            mSelfieMode = getArguments().getBoolean(Constants.CAMERA_IN_SELFIE_MODE);
        }
        else { // No constructor inputs, so use the front-facing camera by default.
            mSelfieMode = true;
        }
    }

    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {

        public void onShutter() {
            // Display the progress indicator
            mProgressContainer.setVisibility(View.VISIBLE);
        }
    };

    private PictureCallback mJpegCallback = new PictureCallback() {
        // Callback for when the picture is taken.
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap newEcho = BitmapFactory.decodeByteArray(data, 0, data.length); // Create a bitmap from the pixel array.

            // There are a few discrepancies between the Camera preview that is displayed in the surface holder,
            // and the pictures that are taken by the camera.
            // To get the preview and the pictures to match, we make a few adjustments through matrix operations.
            Matrix m = new Matrix();
            m.setRotate(90); // Rotate the picture 90 degrees.
            if (mSelfieMode)
                m.preScale(-1, 1);  // If taking selfies, then mirror the image (should see the same image you would see in a mirror)
            newEcho = Bitmap.createBitmap(newEcho, 0, 0, newEcho.getWidth(), newEcho.getHeight(), m, false);

            // Create and display the UploaderFragment.
            if (mUploaderFragment == null)
                mUploaderFragment = new UploaderFragment();
            mUploaderFragment.setEcho(newEcho);
            mUploaderFragment.setLocation(EchoIndex.getIndex(getContext()).getLastLocation());
            getFragmentManager().beginTransaction().replace(R.id.guest_spot, mUploaderFragment).addToBackStack(null).commit();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_echo_camera, parent, false);

        // Progress container is invisible, until photo is taken
        mProgressContainer = v.findViewById(R.id.echo_camera_progress_container);
        mProgressContainer.setVisibility(View.INVISIBLE);

        // Button for taking pictures
        mTakePictureButton = (ImageButton)v.
                findViewById(R.id.echo_camera_take_picture_button);
        mTakePictureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.takePicture(mShutterCallback, null, mJpegCallback);
                }
            }
        });

        // Button that reverses camera (i.e., switches from selfie-style, front-facing camera
        // to normal rear-facing camera).
        ImageButton toggleSelfieModeButton = (ImageButton)v.
                findViewById(R.id.echo_camera_toggle_selfie);
        toggleSelfieModeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSelfieMode = !mSelfieMode;  // When button is clicked, switch the mode.
                if (mCamera != null) {  // Release the current camera.
                    mCamera.release();
                    mCamera = null;
                }
                getCamera();             // Get the new camera (should face the opposite
                                         // direction of the old camera).
                refreshSurfaceView();    // Set surfaceView to preview the new camera
            }
        });

        mSurfaceView = (SurfaceView) v.findViewById(R.id.echo_camera_surfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();

        // Add appropriate callbacks to the SurfaceView holder
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCamera != null) {
                    mCamera.stopPreview();  // Don't need to preview
                                            // if there is nothing to display it on
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Tell the camera to use this surface as its preview area
                try {
                    if (mCamera != null) {
                        mCamera.setPreviewDisplay(holder);  // Start previewing once there is
                                                            // a surface to display it
                    }
                } catch (IOException exception) {
                    Log.e(Constants.LOG_TAG, "Error setting up preview display", exception);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
                if (mCamera == null) {
                    return;
                }
                // The surface has changed size; update the camera preview size.
                Camera.Parameters parameters = mCamera.getParameters();
                ArrayList<Camera.Size> s = getBestSupportedSize(  // Algorithm to get largest matching
                        parameters.getSupportedPreviewSizes(),    // preview and picture sizes.  They need
                        parameters.getSupportedPictureSizes(),    // to match, because we want the picture
                        width, height);                           // to look exactly as is in the preview.
                parameters.setPreviewSize(s.get(0).width, s.get(0).height);
                parameters.setPictureSize(s.get(1).width, s.get(1).height);

                // Need to rotate camera orientation so it doesn't look weird in the preview.
                mCamera.setDisplayOrientation(90);
                mCamera.setParameters(parameters);
                try {
                    mCamera.startPreview();
                } catch (Exception e) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getCamera();           // Get camera.
        refreshSurfaceView();  // Connect camera to preview display.
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // Display the camera preview
    private void refreshSurfaceView() {
        if (mCamera != null) {
            try {
                // Get the best sizes for the preview and the picture that is about to be taken.
                int width = mSurfaceView.getWidth();
                int height = mSurfaceView.getHeight();
                Camera.Parameters parameters = mCamera.getParameters();
                ArrayList<Camera.Size> s = getBestSupportedSize(parameters.getSupportedPreviewSizes(),
                        parameters.getSupportedPictureSizes(), width, height);
                parameters.setPreviewSize(s.get(0).width, s.get(0).height);
                parameters.setPictureSize(s.get(1).width, s.get(1).height);

                // Need to rotate camera orientation so it's not weird in the preview.
                mCamera.setDisplayOrientation(90);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, Log.getStackTraceString(e));
            }
        }
    }

    // Get access to the phone's camera
    public void getCamera() {
        if (mCamera == null)
            try {
                if (mSelfieMode != null && mSelfieMode) { //get front facing camera
                    int cameraCount = 0;
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    cameraCount = Camera.getNumberOfCameras();
                    // Iterate over all cameras, and find front-facing ones.
                    for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
                        Camera.getCameraInfo( camIdx, cameraInfo );
                        if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT  ) {
                            mCamera = Camera.open( camIdx );
                        }
                    }
                }
                else { //get normal camera
                    mCamera = Camera.open();
                }
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, "Error getting camera", e);
            }
    }

    // Gets the largest pair of preview and picture sizes, respectively.
    //
    // The preview size determines what the camera capture looks like to the
    // user before the picture is taken.
    // The picture size determines what the actual photograph looks like.
    // These two sizes should be the same, so that the preview that the user sees appears
    // exactly as the picture that is taken.
    //
    // The 4K picture sizes made the UI slow and lag-prone on my camera, so I made 1920*1080
    // the larges possible size.
    private ArrayList<Camera.Size> getBestSupportedSize(List<Camera.Size> prevSizes,
                                                        List<Camera.Size> picSizes, int width, int height) {
        int maxArea = 1920*1080;
        Camera.Size bestPicSize = picSizes.get(0);
        Camera.Size bestPrevSize = prevSizes.get(0);
        float smallestMissalignmentScore =
                Math.abs(bestPicSize.width - bestPrevSize.width)/(float)((bestPicSize.width+bestPrevSize.width)/2.0)
                + Math.abs(bestPicSize.height - bestPrevSize.height)/(float)((bestPicSize.height+bestPrevSize.height)/2);
        for (Camera.Size prevSz : prevSizes) {
            for (Camera.Size picSz : picSizes) {
                if (picSz.height*picSz.width > maxArea) continue;
                float missalignmentScore = Math.abs(picSz.width - prevSz.width) / (float) ((picSz.width + prevSz.width) / 2.0)
                        + Math.abs(picSz.height - prevSz.height) / (float) ((picSz.height + prevSz.height) / 2);
                if (missalignmentScore < smallestMissalignmentScore) {
                    bestPicSize = picSz;
                    bestPrevSize = prevSz;
                    smallestMissalignmentScore = missalignmentScore;
                }
            }
        }

        // Return the best sizes in an array
        ArrayList<Camera.Size> best = new ArrayList<>();
        best.add(bestPrevSize);
        best.add(bestPicSize);
        return best;
    }

}
