package com.blacksmithlabs.networkrecorder.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.blacksmithlabs.networkrecorder.R;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;

/**
 * Created by brian on 7/1/13.
 */
public class AppActionHeaderFragment extends Fragment {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private boolean isAttached = false;
	private CharSequence textOn = null;
	private CharSequence textOff = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view =  inflater.inflate(R.layout.app_action_header_fragment, container, false);

		final ToggleButton toggle = (ToggleButton)view.findViewById(R.id.toggle_action);
		if (textOn != null && textOn.length() > 0) {
			toggle.setTextOn(textOn);
		}
		if (textOff != null && textOff.length() > 0) {
			toggle.setTextOff(textOff);
		}
		// TODO we don't know if this is checked or not. Make that an attribute
		// TODO make sure the view properly invalidates with the new text items
		toggle.setChecked(true);

		return view;
	}

	@Override
	public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
		super.onInflate(activity, attrs, savedInstanceState);

		textOn = activity.getString(attrs.getAttributeResourceValue(androidns, "textOn", 0));
		textOff = activity.getString(attrs.getAttributeResourceValue(androidns, "textOff", 0));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		isAttached = true;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		isAttached = false;
	}

	public void populateAppData(ApplicationHelper.DroidApp app) {
		final TextView text = (TextView)getView().findViewById(R.id.app_info_text);
		final ImageView icon = (ImageView)getView().findViewById(R.id.app_info_icon);

		text.setText(app.toString());
		if (isAttached) {
			app.loadIcon(getActivity(), icon);
		}
	}
}
