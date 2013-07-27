package com.blacksmithlabs.networkrecorder.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.blacksmithlabs.networkrecorder.R;
import com.blacksmithlabs.networkrecorder.helpers.LogHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by brian on 6/23/13.
 */
public class LogListFragment extends Fragment
		implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener,
				LogHelper.LogHandler {

	private ExpandableListView logListView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.log_list_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (logListView == null) {
			logListView = (ExpandableListView)getView().findViewById(R.id.log_list);
			logListView.setOnGroupClickListener(this);
			logListView.setOnChildClickListener(this);
			logListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
					Log.d("networkrecorder", "Long click: " + view.getTag());
					return false;
				}
			});
		}

		LogHelper.loadLogs(getActivity(), this, true);
	}

	@Override
	public void onPause() {
		super.onPause();
	}


	@Override
	public void onLogsLoaded(LogHelper.LogGroup[] logs) {
		Arrays.sort(logs, new Comparator<LogHelper.LogGroup>() {
			@Override
			public int compare(LogHelper.LogGroup lg1, LogHelper.LogGroup lg2) {
				// Sort alphabetically by app name, if we have it, or uid otherwise
				if (lg1.app != null && lg2.app != null) {
					return String.CASE_INSENSITIVE_ORDER.compare(lg1.app.names[0], lg2.app.names[0]);
				} else {
					return (lg1.uid < lg2.uid) ? -1 : 1;
				}
			}
		});

		logListView.setAdapter(new LogListAdapter(getActivity(), logs));
	}

	@Override
	public void onLogsLoadError(String error) {
		final Resources resources = getActivity().getResources();

		final String errorMessage;
		if (error == null || error.isEmpty()) {
			errorMessage = resources.getString(R.string.reading_logs_error);
		} else {
			errorMessage = resources.getString(R.string.reading_logs_error_message, error);
		}

		MessageBox.alert(getActivity(), errorMessage);
	}

	@Override
	public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
		Toast.makeText(getActivity(), "Group clicked", Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
		Toast.makeText(getActivity(), "Child clicked", Toast.LENGTH_SHORT).show();
		return false;
	}
}