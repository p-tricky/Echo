package me.loftythoughts.phloftus.echo;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

// The MapHolderFragment contains the SupportMapFragment.
// By having a holder/container for the map fragment,
// I can add buttons that overlay the map fragment, such
// as the refresh button
public class MapHolderFragment extends Fragment implements MyLocationListener {

    private Location mLastLocation;
    private MyMapFragment mMapFrag;

    public MapHolderFragment() {
    }

    public static MapHolderFragment newInstance(Location lastLoc) {
        MapHolderFragment fragment = new MapHolderFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.LAST_LOCATION, lastLoc);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) { // If argument passed to constructor, then use
                                      // it as last location.
            mLastLocation = getArguments().getParcelable(Constants.LAST_LOCATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        if (mLastLocation == null) {
            mMapFrag = new MyMapFragment();
        } else {
            mMapFrag = MyMapFragment.newInstance(mLastLocation);
        }
        ImageButton refresh = (ImageButton) v.findViewById(R.id.refresh_button);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapFrag.setRefreshNeeded(true);
            }
        });
        getFragmentManager().beginTransaction().replace(R.id.map, mMapFrag).commit();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EchoIndex.getIndex(getContext()).addLocationListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EchoIndex.getIndex(getContext()).removeLocationListener(this);
    }

    public void setRefreshNeeded() {
        mMapFrag.setRefreshNeeded(true);
    }

    @Override
    public void onLocationUpdated(Location updatedLocation) {
        if (updatedLocation != null)
            mLastLocation = updatedLocation;
    }
}
