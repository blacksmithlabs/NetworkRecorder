package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by brian on 6/24/13.
 */
public class LogView extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Service interactions...
	}

	@Override
	protected void onResume() {
		super.onResume();

		// TODO remove this and implement this activity
		Toast.makeText(this, "Killing service", Toast.LENGTH_SHORT).show();
		final Intent serviceIntent = new Intent(this, NetworkRecorderService.class);
		stopService(serviceIntent);
	}
}