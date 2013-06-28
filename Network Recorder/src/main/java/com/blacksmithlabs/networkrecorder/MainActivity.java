package com.blacksmithlabs.networkrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

	MainPageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    // If we are not resuming, populate our fragments
	    List<Fragment> fragments = getFragments();
	    pageAdapter = new MainPageAdapter(getSupportFragmentManager(), fragments);
	    ViewPager pager = (ViewPager)findViewById(R.id.main_pager);
	    pager.setAdapter(pageAdapter);

	    PagerTabStrip tabs = (PagerTabStrip)findViewById(R.id.main_pager_title);
	    tabs.setTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
    }

	@Override
	protected void onResume() {
		super.onResume();

		ApplicationListFragment.addClickListener(MainActivity.class.getName(), new ApplicationListFragment.OnAppClickListener() {
			@Override
			public void onAppClick(ApplicationHelper.DroidApp app) {
				Intent settings = new Intent(MainActivity.this, LogSettingsActivity.class);
				settings.putExtra(LogSettingsActivity.EXTRA_APP, app);
				startActivity(settings);
			}

			@Override
			public boolean onAppLongClick(ApplicationHelper.DroidApp app) {
				return false;
			}
		});

		// TODO click handler for logListFragment? Or do we want that in the fragment?
	}

	@Override
	protected void onPause() {
		super.onPause();
		ApplicationListFragment.removeClickListener(MainActivity.class.getName());
	}

	private List<Fragment> getFragments() {
		final List<Fragment> fragments = new ArrayList<Fragment>();
		fragments.add(new ApplicationListFragment());
		fragments.add(new LogListFragment());

		return fragments;
	}

	private class MainPageAdapter extends FragmentStatePagerAdapter {
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
