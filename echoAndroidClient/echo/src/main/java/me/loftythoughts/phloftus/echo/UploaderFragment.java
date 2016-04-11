package me.loftythoughts.phloftus.echo;


import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.UUID;


public class UploaderFragment extends Fragment implements EchoEditListener {

    private PagerListener mPagerListener;  // Interface implemented by the main activity that
                                           // allows the UploaderFragment to disable, the swipe
                                           // paging.
    private Bitmap mEcho;                  // Echo captured by EchoCameraFragment
    private DrawingView mDrawingView;      // The view used to doodle on the Echo photo.
    private ImageButton mUploaderButton;   // Button for uploading to remote server.
    private boolean mUploadInProgress = false;
    private Location mEchoLocation;


    public UploaderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_uploader, container, false);

        mDrawingView = (DrawingView) v.findViewById(R.id.upload_view);
        if (mEcho != null)
            mDrawingView.setEcho(mEcho);

        // Icons for editing doodles
        EchoEditIcons editIcons = (EchoEditIcons) v.findViewById(R.id.color_selector);
        editIcons.setEchoEditListener(this);  // This view needs to respond to things like
                                              // color changes and undo actions from the
                                              // from the edit icons

        mUploaderButton = (ImageButton) v.findViewById(R.id.upload_button);
        if (mUploadInProgress) {  // If we are still trying to upload the previous echo, then
                                  // let's not provide the option to start a new upload
            mUploaderButton.setVisibility(View.INVISIBLE);
        }
        mUploaderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View uploadButton) {
                if (!mUploadInProgress) {
                    mUploadInProgress = true;  // Pause upload capabilities
                    EchoIndex.getIndex(getContext()).uploadEcho(
                            new EchoUploadCompletedCallback(
                                    new Handler(), // Handle to current thread, which will run callback
                                    UUID.randomUUID().toString(), // Unique aws key
                                    mDrawingView.getEcho(), // Echo pic bitmap
                                    mEchoLocation
                            ) {
                                @Override
                                void onUploadCompleted(boolean success) {
                                    mUploadInProgress = false;  // Unpause upload capabilities
                                    mUploaderButton.setVisibility(View.VISIBLE);
                                    if (success) {
                                        mPagerListener.refreshMap();  // New echo in the area, so
                                                                      // refresh the google map
                                                                      // displays
                                    }
                                }
                            });
                    getFragmentManager().popBackStack();
                }
            }
        });
        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PagerListener) {
            mPagerListener = (PagerListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PagerListener interface");
        }
        mPagerListener.disablePager();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPagerListener.enablePager();  // Re-enable swipe navigation of fragments.
    }

    // Used by camera fragment to provide photo to this Fragment.
    //
    // Sometimes Echo photos are too big to be put in a bundle, so we can't just pass the Bitmaps
    // into a static constructor as we do in other parts of the app.
    public void setEcho(Bitmap echo) {
        mEcho = echo;
    }

    // Used by camera fragment to tell this fragment where the photo was taken.
    public void setLocation(Location location) {
        mEchoLocation = location;
    }


        // The following interface methods mostly allow for communication between the edit icons and
    // the drawing view.
    @Override
    public void onColorChanged(int color) {
        mDrawingView.onColorChanged(color);
    }

    @Override
    public int onUndo() {
        return mDrawingView.onUndo();
    }

    @Override
    public void editing(boolean isEditing) {
        if (isEditing) {
            // Hide the upload button when editing to give user a better view of the entire photo
            mUploaderButton.setVisibility(View.INVISIBLE);
        }
        else {
            mUploaderButton.setVisibility(View.VISIBLE);
        }
    }
}
