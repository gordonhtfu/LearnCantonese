
package com.blackberry.common.ui.drawer;

import com.blackberry.common.ui.R;
import com.blackberry.common.utils.LogTag;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

public abstract class AbstractDrawerActivity extends Activity {

    private static final String LOG_TAG = LogTag.getLogTag();

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    private boolean mUserLearnedDrawer;
    private boolean mFromSavedInstanceState;
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private RelativeLayout mDrawerContainer;
    private Fragment mMainContent;
    private Fragment mDrawerHeader;
    private Fragment mDrawerFooter;
    private Fragment mDrawerBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            // do stuff
            mFromSavedInstanceState = true;
        }

        // setup the layout
        setContentView(R.layout.drawer_activity);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerContainer = (RelativeLayout) findViewById(R.id.drawer_container);

        if (savedInstanceState == null) {
            // the previously active list of fragments will
            // be regenerated, and states restored, so don't
            // re-setup the drawer contents if we're in restore state.
            setupDrawerContents();
        }

        setupDrawerBehaviors();
    }

    private void setupDrawerBehaviors() {
        // setup drawer decoration
        DrawerConfig config = getDrawerConfig();
        mDrawerLayout.setDrawerShadow(config.mDrawerShadowResId, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                config.mDrawerIconResId, /* nav drawer image to replace 'Up' caret */
                config.mDrawerOpenStringResId, /* "open drawer" description for accessibility */
                config.mDrawerCloseStringResId /* "close drawer" description for accessibility */
                ) {
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        AbstractDrawerActivity.this.onDrawerClosed(drawerView);
                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        super.onDrawerOpened(drawerView);
                        AbstractDrawerActivity.this.onDrawerOpened(drawerView);
                    }
                };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mDrawerContainer);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void setupDrawerContents() {
        // setup the drawer contents
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        String tag = null;

        if (hasContentFragment(DrawerAnchor.Header)) {
            mDrawerHeader = newContentFragment(DrawerAnchor.Header);
            tag = getFragmentTag(DrawerAnchor.Header);
            if (tag.isEmpty()) {
                Log.i(LOG_TAG, "Missing tag for DrawerAnchor.Header");
            }
            ft.add(R.id.header_container, mDrawerHeader, tag);
        }

        if (hasContentFragment(DrawerAnchor.Body)) {
            mDrawerBody = newContentFragment(DrawerAnchor.Body);
            tag = getFragmentTag(DrawerAnchor.Body);
            if (tag.isEmpty()) {
                Log.i(LOG_TAG, "Missing tag for DrawerAnchor.Body");
            }
            ft.add(R.id.body_container, mDrawerBody, tag);
        }

        if (hasContentFragment(DrawerAnchor.Footer)) {
            mDrawerFooter = newContentFragment(DrawerAnchor.Footer);
            tag = getFragmentTag(DrawerAnchor.Footer);
            if (tag.isEmpty()) {
                Log.i(LOG_TAG, "Missing tag for DrawerAnchor.Footer");
            }
            ft.add(R.id.footer_container, mDrawerFooter, tag);
        }

        if (hasContentFragment(DrawerAnchor.MainContent)) {
            mMainContent = newContentFragment(DrawerAnchor.MainContent);
            tag = getFragmentTag(DrawerAnchor.MainContent);
            if (tag.isEmpty()) {
                Log.i(LOG_TAG, "Missing tag for DrawerAnchor.MainContent");
            }
            ft.replace(R.id.main_container, mMainContent, tag);
        } else {
            Log.e(LOG_TAG, "Drawer doesn't have main content!");
        }

        ft.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Provide the fragment to be inserted in the given anchoring position.
     * 
     * @see DrawerAnchor
     * @param anchor
     * @return the fragment to be inserted.
     */
    protected abstract Fragment newContentFragment(DrawerAnchor anchor);

    /**
     * Provide the tag for the fragment in the given anchor position. This will be useful for config
     * changes, where the activity is recreated, and access to the active fragment is needed. On
     * restore state processing, call getFragmentManager().findFragmentByTag() to get the fragment.
     * 
     * @param anchor
     * @return the tag for the fragment
     */
    protected abstract String getFragmentTag(DrawerAnchor anchor);

    /**
     * Convenience method to check if the activity will have a fragment inserted in the given
     * anchoring position.
     * 
     * @see DrawerAnchor
     * @param anchor
     * @return true if there will be fragment, or false otherwise
     */
    protected abstract boolean hasContentFragment(DrawerAnchor anchor);

    protected Fragment getContentFragment(DrawerAnchor anchor) {
        Fragment fragment = null;
        switch (anchor) {
            case MainContent:
                fragment = mMainContent;
                break;
            case Header:
                fragment = mDrawerHeader;
                break;
            case Body:
                fragment = mDrawerBody;
                break;
            case Footer:
                fragment = mDrawerFooter;
                break;
            default:
                Log.e(LOG_TAG, "Invalid anchor");
        }
        return fragment;
    }

    protected void onDrawerClosed(View drawerView) {
        invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
    }

    /**
     * Default behavior on drawer opened is to handle the user-learning pattern, and invalidate the
     * options menu. If overriding this method, please chain-up first before performing further
     * work.
     * 
     * @param drawerView
     */
    protected void onDrawerOpened(View drawerView) {
        if (!mUserLearnedDrawer) {
            // The user manually opened the drawer; store this flag to prevent auto-showing
            // the navigation drawer automatically in the future.
            mUserLearnedDrawer = true;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(AbstractDrawerActivity.this);
            sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
        }

        invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
    }

    protected void setDrawerOpened(boolean open) {
        if (open) {
            mDrawerLayout.openDrawer(mDrawerContainer);
        } else {
            mDrawerLayout.closeDrawer(mDrawerContainer);
        }
    }

    /**
     * Provide the drawable and string resources used to decorate the Drawer. Subclass who overrides
     * this method, should obtain the base config object, and update any resources need to be
     * changed.
     * 
     * @return the drawer configuration
     */
    protected DrawerConfig getDrawerConfig() {
        DrawerConfig dc = new DrawerConfig(R.drawable.drawer_shadow,
                R.drawable.ic_drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        return dc;
    }

    /**
     * The Anchor used to position fragments within this drawer activity. Note: Header, Body and
     * Footer are anchoring position within the activity's drawer. MainContent is the anchoring
     * position for the main content fragment (usually takes up the entire screen).
     */
    protected enum DrawerAnchor {
        MainContent,
        Header,
        Body,
        Footer
    }

    /**
     * Drawer Configuration This class will be used to decorate the drawer, which includes:
     * shadow_resource, icon_resource, the strings used to open and close the drawer.
     */
    protected class DrawerConfig {
        int mDrawerShadowResId;
        int mDrawerIconResId;
        int mDrawerOpenStringResId;
        int mDrawerCloseStringResId;

        DrawerConfig(int shadowId, int iconId, int openId, int closeId) {
            this.mDrawerShadowResId = shadowId;
            this.mDrawerIconResId = iconId;
            this.mDrawerOpenStringResId = openId;
            this.mDrawerCloseStringResId = closeId;
        }
    }
}
