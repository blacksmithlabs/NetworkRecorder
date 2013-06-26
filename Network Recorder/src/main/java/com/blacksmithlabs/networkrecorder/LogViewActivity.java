package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by brian on 6/24/13.
 */
public class LogViewActivity extends Activity {
	public static final String EXTRA_LOG_INFO = "LogViewActivity.info";
	public static final String EXTRA_KILL_SERVICE = "LogViewActivity.killService";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_view);

		// Service interactions...
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if (intent != null) {
			Log.d("NetworkRecorder", "KillService Intent: " + intent.getBooleanExtra("KillService", false));
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

	public static final class LogSettings {
		public int uid;
		public String ports;
	}
}