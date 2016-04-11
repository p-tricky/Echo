package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.GsonBuilder;

/**
 * TLDR - This class looks pointless, but it's not.
 *
 * When I started writing Echo, I wanted to learn more about fragments.
 * For this reason, I wanted Echo to be a single Activity application.
 *
 * The single activity design created a few complications.
 * One of the primary difficulties was performing fragment transactions
 * within the FragmentPagerAdapter.
 *
 * When the App is launched the FragmentPagerAdapter needs to know which fragment to
 * put in each page {@link CustomPagerAdapter#getItem(int)}.  However, once a
 * fragment has been assigned to a page in the adapter, replacing said fragment
 * results in weird behavior.  This is problematic because Echo needs to be able to
 * transition from the CameraFragment to the UploaderFragment and from the MapFragment
 * to the ViewerFragment and vice-versa.
 *
 * To get around this, I created the HostFragment.  The HostFragment is an empty fragment.
 * Its job is to host whatever guest fragment needs to be in the page.
 * The HostFragment never transitions out of its page in the adapter, so weird behavior
 * is avoided. However, the guest fragments can be swapped in and out
 * so we get the desired behaviour
 *
 */
public class HostFragment extends Fragment {

	private Fragment mGuestFragment;
	private static final String TAG = "HostFragment";
	private int GuestId;  // GuestId determines the initial host fragment's type


	public HostFragment() {
	}

	public static HostFragment newInstance(int id) {
		HostFragment frag = new HostFragment();
		Bundle args = new Bundle();
		args.putInt(TAG, id);
		frag.setArguments(args);
		return frag;
	}



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GuestId = getArguments().getInt(TAG);
		mGuestFragment =  getChildFragmentManager().findFragmentByTag(Integer.toString(GuestId));
		if (mGuestFragment == null) {
			switch (GuestId) {
				case Constants.MAP_FRAGMENT:
                    // If we have saved the user's location from the last time the application
                    // was opened then pass the saved location to the MapHolder
					SharedPreferences prefs = getActivity().getSharedPreferences(
							Constants.ECHO_PREFS,
							Context.MODE_PRIVATE);
					String jsonLoc = prefs.getString(Constants.LAST_LOCATION, "");
					if (!jsonLoc.isEmpty()) {
						Location lastLoc = new GsonBuilder().create().fromJson(
								jsonLoc, Location.class);
						mGuestFragment = MapHolderFragment.newInstance(lastLoc);
					} else {
						mGuestFragment = new MapHolderFragment();
					}
					break;
				case Constants.CAMERA_FRAGMENT:
					mGuestFragment = new EchoCameraFragment();
					break;
				default:
					break;
			}
		}
        else
            Log.i(Constants.LOG_TAG, "OMG! Fragment found!");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the empty host layout
		View view = inflater.inflate(R.layout.fragment_host, container, false);

		FragmentTransaction transaction = getChildFragmentManager()
				.beginTransaction();

        // Replace empty guest spot with the guest fragment
		transaction.replace(R.id.guest_spot, mGuestFragment, Integer.toString(GuestId));

		transaction.commit();

		return view;
	}

	public Fragment getHostedFragment() {
		return mGuestFragment;
	}

}
