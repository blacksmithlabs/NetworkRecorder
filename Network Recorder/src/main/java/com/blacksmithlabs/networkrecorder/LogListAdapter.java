package com.blacksmithlabs.networkrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.blacksmithlabs.networkrecorder.helpers.LogHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brian on 6/24/13.
 */
public class LogListAdapter extends BaseExpandableListAdapter {
	private Context context;
	private LogHelper.LogGroup[] groups;

	public LogListAdapter(Context context, LogHelper.LogGroup[] logGroups) {
		this.context = context;
		this.groups = logGroups;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public int getGroupCount() {
		return groups.length;
	}

	@Override
	public int getChildrenCount(int groupIndex) {
		return groups[groupIndex].logFiles.length;
	}

	@Override
	public Object getGroup(int groupIndex) {
		return groups[groupIndex];
	}

	@Override
	public Object getChild(int groupIndex, int childIndex) {
		return groups[groupIndex].logFiles[childIndex];
	}

	@Override
	public long getGroupId(int groupIndex) {
		return groupIndex;
	}

	@Override
	public long getChildId(int groupIndex, int childIndex) {
		return childIndex;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupIndex, boolean isExpanded, View convertView, ViewGroup parent) {
		LogHelper.LogGroup group = (LogHelper.LogGroup)getGroup(groupIndex);

		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.log_list_group, null);
		}

		TextView groupName = (TextView)convertView.findViewById(R.id.log_group_text);
		groupName.setText(group.app == null ? Integer.toString(group.uid) : group.app.toString());

		ImageView groupIcon = (ImageView)convertView.findViewById(R.id.log_group_icon);
		group.app.loadIcon(context, groupIcon);

		convertView.setTag(group);

		return convertView;
	}

	@Override
	public View getChildView(int groupIndex, int childIndex, boolean isLastChild, View convertView, ViewGroup parent) {
		LogHelper.LogFile logFile = (LogHelper.LogFile)getChild(groupIndex, childIndex);

		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.log_list_item, null);
		}

		TextView itemText = (TextView)convertView.findViewById(R.id.log_item_text);
		itemText.setText(logFile.creationDate);

		convertView.setTag(logFile);

		return convertView;
	}

	@Override
	public boolean isChildSelectable(int i, int i2) {
		return true;
	}
}
