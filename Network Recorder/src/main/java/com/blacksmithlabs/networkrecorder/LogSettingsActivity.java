package com.blacksmithlabs.networkrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by brian on 6/25/13.
 */
public class LogSettingsActivity extends FragmentActivity {
	public static final String EXTRA_APP = "LogSettings.app";

	protected static final String PORT_FRAGMENT_TAG = "LogSettings.PortFragment";
	protected static final String PORT_FRAGMENT_COUNT = "LogSettings.PortCount";

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

		titleFragment = (AppTitleFragment)fragmentManager.findFragmentById(R.id.settings_app_fragment);

		// If we are not resuming, populate the fragments
		if (savedInstanceState == null) {
			_addPortFragment(null);
		} else {
			final int portCount = savedInstanceState.getInt(PORT_FRAGMENT_COUNT, 0);
			if (portCount <= 0) {
				_addPortFragment(null);
			} else {
				for (int i=0; i<portCount; i++) {
					portSelectors.add((PortSelectorFragment)fragmentManager.findFragmentByTag(PORT_FRAGMENT_TAG + i));
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
		final ToggleButton toggle = (ToggleButton)view;
		if (toggle.isChecked()) {
			HashSet<Integer> ports = new HashSet<Integer>(portSelectors.size());
			for (PortSelectorFragment portFrag : portSelectors) {
				int port = portFrag.getPort();
				if (port >= 0) {
					ports.add(port);
				}
			}

			// No valid ports found
			if (ports.isEmpty()) {
				MessageBox.alert(this, getString(R.string.settings_no_ports));
				toggle.setChecked(false);
				return;
			}

			final Intent logIntent = new Intent(this, LogViewActivity.class);
			logIntent.putExtra(LogViewActivity.EXTRA_START, true);
			logIntent.putExtra(LogViewActivity.EXTRA_APP, app);
			logIntent.putExtra(LogViewActivity.EXTRA_PORTS, new ArrayList<Integer>(ports));
			logIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(logIntent);
			finish();
		}
	}

	public void _addPortFragment(String port) {
		final Bundle args = new Bundle();
		args.putString("Port", port);

		final PortSelectorFragment fragment = (PortSelectorFragment)Fragment.instantiate(this, PortSelectorFragment.class.getName(), args);
		portSelectors.add(fragment);

		final String fragmentTag = PORT_FRAGMENT_TAG + portSelectors.size();

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
		protected String defaultPort = "";

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			defaultPort = getArguments() != null ? getArguments().getString("Port", "") : "";
		}

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

			final EditText text = (EditText)view.findViewById(R.id.port);
			// If we're creating it new, request focus
			if (savedInstanceState == null) {
				text.requestFocus(TextView.FOCUS_DOWN);
			}
			if (!defaultPort.isEmpty()) {
				text.setText(defaultPort);
			}
			// Force input to only valid ranges
			List<InputFilter> filters = new ArrayList<InputFilter>(Arrays.asList(text.getFilters()));
			filters.add(new InputFilter() {
				@Override
				public CharSequence filter(CharSequence charSequence, int start, int end, Spanned spanned, int dstart, int dend) {
					try {
						final int input = Integer.parseInt(charSequence.toString() + spanned.toString());
						// Unsigned short, max value of 2^16
						if (input >= 0 && input <= 65535) {
							return null;
						}
					} catch (NumberFormatException ex) {
						// That's awkward. Android broke...
					}

					return "";
				}
			});
			text.setFilters(filters.toArray(new InputFilter[]{}));

			return view;
		}

		/**
		 * Get the selected port. -1 for invalid/empty ports
		 * @return
		 */
		public int getPort() {
			final EditText portText = (EditText)getView().findViewById(R.id.port);
			final String value = portText.getText().toString();
			if (value != null && !value.isEmpty()) {
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException ex) {
					// That's awkward. Android broke...
				}
			}

			return -1;
		}
	}
}
