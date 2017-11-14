/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import butter.droid.BuildConfig;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.beaming.BeamPlayerNotificationService;
import butter.droid.base.beaming.server.BeamServerService;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.PrefUtils;
import butter.droid.fragments.MediaContainerFragment;
import butter.droid.fragments.NavigationDrawerFragment;
import butter.droid.utils.ToolbarUtils;
import butter.droid.widget.ScrimInsetsFrameLayout;
import butterknife.Bind;

/**
 * The main activity that houses the navigation drawer, and controls navigation between fragments
 */
public class MainActivity extends ButterBaseActivity implements NavigationDrawerFragment.Callbacks {

    private static final int PERMISSIONS_REQUEST = 123;
    private Fragment mCurrentFragment;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.navigation_drawer_container)
    ScrimInsetsFrameLayout mNavigationDrawerContainer;
    @Nullable
    @Bind(R.id.tabs)
    TabLayout mTabs;
    NavigationDrawerFragment mNavigationDrawerFragment;

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_main);

        if (!PrefUtils.contains(this, TermsActivity.TERMS_ACCEPTED)) {
            startActivity(new Intent(this, TermsActivity.class));
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }

        {
            String action = getIntent().getAction();
            Uri data = getIntent().getData();
            if (action != null && action.equals(Intent.ACTION_VIEW) && data != null) {
                String streamUrl = data.toString();
                try {
                    streamUrl = URLDecoder.decode(streamUrl, "utf-8");
                    StreamLoadingActivity.startActivity(this, new StreamInfo(streamUrl));
                    finish();
                    return;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        FragmentManager.enableDebugLogging(BuildConfig.DEBUG);

        setSupportActionBar(mToolbar);
        setShowCasting(true);

        ToolbarUtils.updateToolbarHeight(this, mToolbar);

        // Set up the drawer.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer_fragment);

        mNavigationDrawerFragment.initialise(mNavigationDrawerContainer, drawerLayout);

        if (savedInstanceState != null) return;
        int providerId = PrefUtils.get(this, Prefs.DEFAULT_VIEW, 0);
        mNavigationDrawerFragment.selectItem(providerId);

        createSyncAccount(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String title = mNavigationDrawerFragment.getCurrentItem().getTitle();
        setTitle(title != null ? title : getString(R.string.app_name));
        supportInvalidateOptionsMenu();
        if (mNavigationDrawerFragment.getCurrentItem() != null && mNavigationDrawerFragment.getCurrentItem().getTitle() != null) {
            setTitle(mNavigationDrawerFragment.getCurrentItem().getTitle());
        }

        mNavigationDrawerFragment.initItems();

        if(BeamServerService.getServer() != null)
            BeamServerService.getServer().stop();

        BeamPlayerNotificationService.cancelNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_overview, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                /* Override default {@link pct.droid.activities.BaseActivity } behaviour */
                return false;
            case R.id.action_search:
                //start the search activity
                SearchActivity.startActivity(this, mNavigationDrawerFragment.getCurrentItem().getMediaProvider());
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onNavigationDrawerItemSelected(NavigationDrawerFragment.NavDrawerItem item, String title) {
        setTitle(title != null ? title : getString(R.string.app_name));
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        String tag = title + "_tag";
        // Fragment fragment = mFragmentCache.get(position);
        mCurrentFragment = fragmentManager.findFragmentByTag(tag);
        if (mCurrentFragment == null && item.hasProvider()) {
            mCurrentFragment = MediaContainerFragment.newInstance(item.getMediaProvider());
        }

        if(mTabs.getTabCount() > 0)
            mTabs.getTabAt(0).select();

        fragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment, tag).commit();

        if(mCurrentFragment instanceof MediaContainerFragment) {
            updateTabs((MediaContainerFragment) mCurrentFragment, ((MediaContainerFragment) mCurrentFragment).getCurrentSelection());
        }
    }

    public void updateTabs(MediaContainerFragment containerFragment, final int position) {
        if (mTabs == null)
            return;

        if (containerFragment != null) {
            ViewPager viewPager = containerFragment.getViewPager();
            if (viewPager == null)
                return;

            mTabs.setupWithViewPager(viewPager);
            mTabs.setTabGravity(TabLayout.GRAVITY_CENTER);
            mTabs.setTabMode(TabLayout.MODE_SCROLLABLE);
            mTabs.setVisibility(View.VISIBLE);

            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));
            mTabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

            if (mTabs.getTabCount() > 0) {
                mTabs.getTabAt(0).select();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mTabs.getTabCount() > position)
                            mTabs.getTabAt(position).select();
                    }
                }, 10);
            }

        } else {
            mTabs.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length < 1 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    finish();
                }
            }
        }
    }

    public void createSyncAccount(Context context) {
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.app_name_authority));
        AccountManager accountManager =(AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(newAccount, null, null);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.app_name_authority), true);
    }
}