package com.blacksmithlabs.networkrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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

	final protected static String TITLE_FRAGMENT_TAG = "LogSettings.TitleFragment";
	final protected static String PORT_FRAGMNET_TAG = "LogSettings.PortFragment";
	final protected static String PORT_FRAGMENT_COUNT = "LoggetSettings.PortCount";

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

			final Intent main = new Intent(this, MainActivity.class);
			main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(main);

			finish();
		}

		if (portSelectors == null) {
			portSelectors = new ArrayList<PortSelectorFragment>();
		}

		final FragmentManager fragmentManager = getSupportFragmentManager();

		// If we are not resuming, populate the fragments
		if (savedInstanceState == null) {
			titleFragment = new AppTitleFragment();
			fragmentManager.beginTransaction()
					.add(R.id.settings_app_container, titleFragment, TITLE_FRAGMENT_TAG)
					.commit();

			_addPortFragment(null);
		} else {
			titleFragment = (AppTitleFragment)fragmentManager.findFragmentByTag(TITLE_FRAGMENT_TAG);

			final int portCount = savedInstanceState.getInt(PORT_FRAGMENT_COUNT, 0);
			if (portCount <= 0) {
				_addPortFragment(null);
			} else {
				for (int i=0; i<portCount; i++) {
					portSelectors.add((PortSelectorFragment)fragmentManager.findFragmentByTag(PORT_FRAGMNET_TAG + i));
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_APP, app);
		outState.putInt(PORT_FRAGMENT_COUNT, portSelectors.size());
	}

	@Override
	protected void onResume() {
		super.onResume();

		titleFragment.populateData(app);
	}

	public void onRecordToggleClicked(View view) {
		// TODO load the log activity and give it all the settings so it can start the service and such
	}

	public void _addPortFragment(String port) {
		final PortSelectorFragment fragment = new PortSelectorFragment();
		portSelectors.add(fragment);

		final String fragmentTag = PORT_FRAGMNET_TAG + portSelectors.size();

		getSupportFragmentManager().beginTransaction()
				.add(R.id.settings_ports_container, fragment, fragmentTag)
				.commit();
	}

	public void _removePortFragment(Fragment fragment) {
		final int location = portSelectors.indexOf(fragment);
		if (location >= 0) {
			portSelectors.remove(location);

			getSupportFragmentManager().beginTransaction()
					.remove(fragment)
					.commit();
		}

		// Don't let it become empty
		if (portSelectors.isEmpty()) {
			_addPortFragment(null);
		}
	}

	public static class AppTitleFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			return inflater.inflate(R.layout.app_list_item, container, false);
		}

		public void populateData(ApplicationHelper.DroidApp app) {
			final TextView text = (TextView)getView().findViewById(R.id.app_item_text);
			final ImageView icon = (ImageView)getView().findViewById(R.id.app_item_icon);

			text.setText(app.toString());
			app.loadIcon(getActivity(), icon);
		}
	}

	public static class PortSelectorFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			final View view = inflater.inflate(R.layout.settings_port_fragment, container, false);

			final ImageView addButton = (ImageView)view.findViewById(R.id.add_port);
			addButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_add));
			addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					((LogSettingsActivity)getActivity())._addPortFragment(null);
				}
			});

			final ImageView removeButton = (ImageView)view.findViewById(R.id.remove_port);
			removeButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));
			removeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					((LogSettingsActivity)getActivity())._removePortFragment(PortSelectorFragment.this);
				}
			});

			if (savedInstanceState == null) {
				final TextView text = (TextView)view.findViewById(R.id.port);
				text.requestFocus(TextView.FOCUS_DOWN);
			}

			return view;
		}
	}
}
