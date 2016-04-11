package me.loftythoughts.phloftus.echo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Patrick on 2/25/2016.
 */
public class CustomPagerAdapter extends FragmentPagerAdapter {

    public CustomPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return HostFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return Constants.TOTAL_PAGES;
    }

}
