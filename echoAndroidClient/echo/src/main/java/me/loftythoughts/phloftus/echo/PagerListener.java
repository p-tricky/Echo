package me.loftythoughts.phloftus.echo;

/**
 * Created by Patrick on 2/14/2016.
 */
public interface PagerListener {
    void enablePager();
    void disablePager();
    int getPage();
    void setPage(int position);
    void refreshMap();
}
