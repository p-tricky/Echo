package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

// This fragment simply holds and displays the echo jpg.
// It disables swipe gestures when it comes into focus and
// re-enables it when it leaves.
public class EchoViewerFragment extends Fragment {

    private PagerListener mPagerListener;

    public EchoViewerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mPagerListener.enablePager();
        mPagerListener = null;
    }

}
