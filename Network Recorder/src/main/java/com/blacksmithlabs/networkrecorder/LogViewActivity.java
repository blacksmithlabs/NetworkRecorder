package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by brian on 6/24/13.
 */
public class LogViewActivity extends Activity {
	public static final String EXTRA_APP = "LogView.app";
	public static final String EXTRA_PORTS = "LogView.ports";


	public static final String EXTRA_LOG_INFO = "LogViewActivity.info";
	public static final String EXTRA_KILL_SERVICE = "LogViewActivity.killService";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_view);

		// Service interactions...?
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if (intent != null) {
			Log.e("NetworkRecorder", "KillService Intent: " + intent.getBooleanExtra("KillService", false));
			Log.e("NetworkRecorder", "App Intent: " + intent.getParcelableExtra(EXTRA_APP));
			Log.e("NetworkRecorder", "App Ports: " + intent.getIntegerArrayListExtra(EXTRA_PORTS));
		}

		// TODO remove this and implement this activity
		Toast.makeText(this, "Killing service", Toast.LENGTH_SHORT).show();
		final Intent serviceIntent = new Intent(this, NetworkRecorderService.class);
		stopService(serviceIntent);
	}

	protected void startService() {
		/*
		final Intent startService = new Intent(this, NetworkRecorderService.class);
		startService.putExtra(NetworkRecorderService.EXTRA_APP_UID, app.uid);
		startService.putExtra(NetworkRecorderService.EXTRA_PORTS, "80|8080");
		startService(startService);
		*/

		// TODO bind to service
	}

	protected void stopService() {
		// TODO unbind from service
		stopService(new Intent(this, NetworkRecorderService.class));
	}
}