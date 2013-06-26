package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;

import java.util.ArrayList;

/**
 * Created by brian on 6/25/13.
 */
public class LogSettingsActivity extends FragmentActivity {
	final public static String EXTRA_APP = "LogSettings.app";

	protected ApplicationHelper.DroidApp app;

	protected AppTitleFragment titleFragment;
	protected ArrayList<PortSelectorFragment> portSelectors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_settings);

		if (savedInstanceState == null) {
			app = getIntent().getParcelableExtra(EXTRA_APP);
		} else {
			app = savedInstanceState.getParcelable(EXTRA_APP);
		}

		if (app == null) {
			MessageBox.error(this, getString(R.string.error_settings_noapp));

			Intent main = new Intent(this, MainActivity.class);
			main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(main);

			finish();
		}

		if (portSelectors == null) {
			portSelectors = new ArrayList<PortSelectorFragment>();
		}

		// If we are not resuming, populate the fragments
		if (savedInstanceState == null) {
			titleFragment = new AppTitleFragment();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.settings_app_container, titleFragment)
					.commit();

			addPortFragment(null);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_APP, app);
	}

	@Override
	protected void onResume() {
		super.onResume();

		titleFragment.populateData(app);
	}

	public void onRecordToggleClicked(View view) {
		// TODO load the log activity and give it all the settings so it can start the service and such
	}

	protected void addPortFragment(String port) {
		PortSelectorFragment fragment = new PortSelectorFragment();
		portSelectors.add(fragment);

		getSupportFragmentManager().beginTransaction()
				.add(R.id.settings_ports_container, fragment)
				.commit();
	}

	protected void removePortFragment(Fragment fragment) {
		int location = portSelectors.indexOf(fragment);
		if (location >= 0) {
			portSelectors.remove(location);

			getSupportFragmentManager().beginTransaction()
					.remove(fragment)
					.commit();
		}

		// Don't let it become empty
		if (portSelectors.isEmpty()) {
			addPortFragment(null);
		}
	}

	public class AppTitleFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			return inflater.inflate(R.layout.app_list_item, container, false);
		}

		public void populateData(ApplicationHelper.DroidApp app) {
			TextView text = (TextView)getView().findViewById(R.id.app_item_text);
			ImageView icon = (ImageView)getView().findViewById(R.id.app_item_icon);

			text.setText(app.toString());
			app.loadIcon(getActivity(), icon);
		}
	}

	public class PortSelectorFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			View view = inflater.inflate(R.layout.settings_port_fragment, container, false);

			ImageView addButton = (ImageView)view.findViewById(R.id.add_port);
			addButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_add));
			addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					LogSettingsActivity.this.addPortFragment(null);
				}
			});

			ImageView removeButton = (ImageView)view.findViewById(R.id.remove_port);
			removeButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));
			removeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					LogSettingsActivity.this.removePortFragment(PortSelectorFragment.this);
				}
			});

			TextView text = (TextView)view.findViewById(R.id.port);
			text.requestFocus(TextView.FOCUS_DOWN);

			return view;
		}
	}
}
