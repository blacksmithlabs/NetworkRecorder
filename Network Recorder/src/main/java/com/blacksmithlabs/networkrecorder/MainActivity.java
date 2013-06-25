package com.blacksmithlabs.networkrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

	MainPageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    List<Fragment> fragments = getFragments();
	    pageAdapter = new MainPageAdapter(getSupportFragmentManager(), fragments);
	    ViewPager pager = (ViewPager)findViewById(R.id.main_pager);
	    pager.setAdapter(pageAdapter);

	    PagerTabStrip tabs = (PagerTabStrip)findViewById(R.id.main_pager_title);
	    tabs.setTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
    }

	private List<Fragment> getFragments() {
		final List<Fragment> fragments = new ArrayList<Fragment>();

		final ApplicationListFragment appListFragment = new ApplicationListFragment();
		appListFragment.setAppClickListener(new ApplicationListFragment.OnAppClickListener() {
			@Override
			public void onAppClick(ApplicationHelper.DroidApp app) {
				// TODO Bring up the options dialog
				Log.d("networkrecorder", "Selected app: " + app);
				// Temporary test of the service - no way to kill it...
				final Intent startService = new Intent(MainActivity.this, NetworkRecorderService.class);
				startService.putExtra(NetworkRecorderService.EXTRA_APP_UID, app.uid);
				startService.putExtra(NetworkRecorderService.EXTRA_PORTS, "80|8080");
				startService(startService);
			}

			@Override
			public boolean onAppLongClick(ApplicationHelper.DroidApp app) {
				return false;
			}
		});
		fragments.add(appListFragment);

		final LogListFragment logListFragment = new LogListFragment();
		// TODO click handler? Or do we want that in the fragment?
		fragments.add(logListFragment);

		return fragments;
	}

	private class MainPageAdapter extends FragmentPagerAdapter {
		final private List<Fragment> fragments;

		public MainPageAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch(position) {
				case 0:
					return getString(R.string.title_applications);
				case 1:
					return getString(R.string.title_logs);
			}
			return null;
		}
	}
}
