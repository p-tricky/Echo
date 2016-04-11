package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Patrick on 2/14/2016.
 *
 * Pretty standard view pager, except that the swipe-navigation can be turned on and off.
 * Disabling navigation is useful for certain fragments like the UploaderFragment, where
 * drawing motions could easily be misinterpreted as swipes.
 */
public class CustomViewPager extends ViewPager {
    private boolean swipeEnabled;

    public CustomViewPager(Context context) {
        super(context);
        swipeEnabled = true;
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        swipeEnabled = true;
    }

    // Some fragments need to disable the swipe view feature.
    // This method does that.
    public void setSwipeEnabled(boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (swipeEnabled)	{
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (swipeEnabled)	{
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

}
