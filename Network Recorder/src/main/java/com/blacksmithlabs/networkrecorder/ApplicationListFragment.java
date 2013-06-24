package com.blacksmithlabs.networkrecorder;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by brian on 6/22/13.
 */
public class ApplicationListFragment extends Fragment
		implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
				ApplicationHelper.ApplicationHandler {

	private ListView appListView;

	private OnAppClickListener clickListener = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.app_list_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (appListView == null) {
			appListView = (ListView)getView().findViewById(R.id.application_list);
			appListView.setOnItemClickListener(this);
		}

		ApplicationHelper.loadApps(getActivity(), this, true);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (appListView != null) {
			appListView.setAdapter(null);
		}
	}

	@Override
	public void onAppsLoaded(final ApplicationHelper.DroidApp[] applications) {
		// Sort applications, selected first then alphabetically
		Arrays.sort(applications, new Comparator<ApplicationHelper.DroidApp>() {
			@Override
			public int compare(ApplicationHelper.DroidApp app1, ApplicationHelper.DroidApp app2) {
				if (app1.firstseen != app2.firstseen) {
					return (app1.firstseen ? -1 : 1);
				}
				if (app1.selected == app2.selected) {
					return String.CASE_INSENSITIVE_ORDER.compare(app1.names[0], app2.names[0]);
				}
				if (app1.selected)
					return -1;
				return 1;
			}
		});

		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final ListAdapter adapter = new ArrayAdapter<ApplicationHelper.DroidApp>(
				getActivity(), R.layout.app_list_fragment, R.id.application_list, applications) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ListEntry entry;
				if (convertView == null || (entry = (ListEntry)convertView.getTag()) == null) {
					convertView = inflater.inflate(R.layout.app_list_item, parent, false);

					entry = new ListEntry();
					entry.text = (TextView)convertView.findViewById(R.id.app_item_text);
					entry.icon = (ImageView)convertView.findViewById(R.id.app_item_icon);
					convertView.setTag(entry);
				}

				final ApplicationHelper.DroidApp app = applications[position];
				entry.app = app;
				entry.text.setText(app.toString());
				app.loadIcon(getActivity(), entry.icon);

				convertView.setSelected(app.selected);

				return convertView;
			}
		};
		appListView.setAdapter(adapter);
	}

	@Override
	public void onAppsLoadError(String error) {
		final Resources resources = getActivity().getResources();

		final String errorMessage;
		if (error == null || error.isEmpty()) {
			errorMessage = resources.getString(R.string.reading_apps_error);
		} else {
			errorMessage = resources.getString(R.string.reading_apps_error_message, error);
		}

		MessageBox.alert(getActivity(), errorMessage);
	}

	public void setAppClickListener(OnAppClickListener listener) {
		clickListener = listener;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		view.setSelected(true);

		ListEntry entry = (ListEntry)view.getTag();
		entry.app.selected = true;

		if (clickListener != null) {
			clickListener.onAppClick(entry.app);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
		if (clickListener != null) {
			ListEntry entry = (ListEntry)view.getTag();
			if (clickListener.onAppLongClick(entry.app)) {
				entry.app.selected = true;
				return true;
			}
		}
		return false;
	}

	public static interface OnAppClickListener {
		public void onAppClick(ApplicationHelper.DroidApp app);
		public boolean onAppLongClick(ApplicationHelper.DroidApp app);
	}

	private static class ListEntry {
		private TextView text;
		private ImageView icon;
		private ApplicationHelper.DroidApp app;
	}
}
