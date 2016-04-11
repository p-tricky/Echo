package me.loftythoughts.phloftus.echo;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity
        implements PagerListener,
        ActivityCompat.OnRequestPermissionsResultCallback
{

    // dealing with location permissions
    private static final int PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionsDenied = false;
    private boolean mRefreshNeeded = false;


    // Called when a user swipes from one page to another.
    // The purpose of this listener is to refresh the map  when we swipe to it.
    // The map won't always need refreshing, but sometimes it will.
    // If the user uploaded echoes, then he/she swipes to the map, we should refresh
    // the map so that the new echoes appear.
    private ViewPager.SimpleOnPageChangeListener mPageListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            if (position == MyMapFragment.POSITION_IN_PAGER && mRefreshNeeded) {
                // if map needs refreshing, then refresh it when switching to it
                HostFragment host = (HostFragment) getSupportFragmentManager().findFragmentByTag(
                        "android:switcher:" + R.id.container + ":" + MyMapFragment.POSITION_IN_PAGER);
                if (host != null) {
                    Fragment hosted = host.getHostedFragment();
                    if (hosted != null && hosted instanceof MapHolderFragment)
                        ((MapHolderFragment) hosted).setRefreshNeeded();
                }
            }
        }
    };

    // the following classes enable the swipe gesture navigation
    private CustomPagerAdapter mCustomPagerAdapter;  // pretty standard FragmentPagerAdapter
    private CustomViewPager mViewPager;              // pager w/option to disable swipe navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mCustomPagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mCustomPagerAdapter);
            mViewPager.addOnPageChangeListener(mPageListener);

            // required to avoid a black flash when the map is loaded
            // the flash is due to the map's underlying surface view
            mViewPager.requestTransparentRegion(mViewPager);
        }

        enableLocationAndCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start the index
        EchoIndex.getIndex(getApplicationContext()).start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Store the last location, so that map will likely display a location
        // at least somewhat close to the user's current location when it's
        // initialized
        Location lastLoc = EchoIndex.getIndex(getApplicationContext()).getLastLocation();
        if (lastLoc != null) {
            SharedPreferences prefs = getSharedPreferences(Constants.ECHO_PREFS,
                    Context.MODE_PRIVATE);
            prefs.edit().putString(
                    Constants.LAST_LOCATION, new GsonBuilder().create().toJson(lastLoc)
            ).apply();
        }

        // Stop the index
        EchoIndex.getIndex(getApplicationContext()).stop();
    }

    // If the back button is pressed from the UploaderFragment it should return to the CameraFragment.
    // If pressed from the ViewerFragment, return to the MapFragment.
    // If pressed from camera or map, then stop the activity.
    @Override
    public void onBackPressed() {
        Fragment curFrag = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        if (curFrag == null || curFrag.getChildFragmentManager().getBackStackEntryCount() == 0)
            super.onBackPressed();
        else
            curFrag.getChildFragmentManager().popBackStack();
    }

    // The next 5 methods are PagerListener methods.
    public void enablePager() {
        mViewPager.setSwipeEnabled(true);
    }

    public void disablePager() {
        mViewPager.setSwipeEnabled(false);
    }

    public void setPage(int position) {
        mViewPager.setCurrentItem(position);
    }

    // When mRefreshNeeded is true the pager listener will tell the map to
    // refresh when the map is navigated to
    public void refreshMap() {
        mRefreshNeeded = true;
    }

    public int getPage() {
        return mViewPager.getCurrentItem();
    }


    // Permissions Stuff //
    private void enableLocationAndCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(Constants.LOG_TAG, "Could not get permission!");
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, PERMISSION_REQUEST_CODE,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CAMERA}, true);
        } else {
            mPermissionsDenied = false;
        }
    }

    // The results of our permission request are in, so check to see if we have them
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) &&
                PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.CAMERA)) {
            // Enable the my location layer if the permission has been granted.
            EchoIndex.getIndex(getApplicationContext()).onPermissionEnabled();
            Log.i(Constants.LOG_TAG, "Permission granted!");
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionsDenied = true;
            Log.i(Constants.LOG_TAG, "Permission denied!");
        }
    }

    // If the user didn't give us permission, then we should show the permission denied dialog,
    // which will explain how to grant the required permissions and then it will close the app
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionsDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
        }
    }

    // Displays a dialog with error message explaining that the location permission is missing.
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

}
