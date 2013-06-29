package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;
import com.blacksmithlabs.networkrecorder.helpers.SysUtils;

import java.util.ArrayList;

/**
 * Created by brian on 6/24/13.
 */
public class LogViewActivity extends Activity {
	public static final String EXTRA_APP = "LogView.app";
	public static final String EXTRA_PORTS = "LogView.ports";
	public static final String EXTRA_START = "LogView.start";
	public static final String EXTRA_LOG_FILE = "LogView.logFile";

	public static boolean isBound = false;

	protected ApplicationHelper.DroidApp app;
	protected ArrayList<Integer> ports;
	protected String logFile;
	protected boolean newLogRequest = false;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_view);

		if (savedInstanceState != null) {
			app = savedInstanceState.getParcelable(EXTRA_APP);
			ports = savedInstanceState.getIntegerArrayList(EXTRA_PORTS);
			logFile = savedInstanceState.getString(EXTRA_LOG_FILE);
		} else {
			final Intent intent = getIntent();
			if (intent != null) {
				newLogRequest = intent.getBooleanExtra(EXTRA_START, false);
				if (newLogRequest) {
					app = intent.getParcelableExtra(EXTRA_APP);
					if (app == null) {
						MessageBox.error(this, getString(R.string.error_settings_noapp), new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialogInterface) {
								LogViewActivity.this.finish();
							}
						});
					}

					ports = intent.getIntegerArrayListExtra(EXTRA_PORTS);
					if (ports == null || ports.isEmpty()) {
						MessageBox.error(this, getString(R.string.settings_no_ports), new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialogInterface) {
								LogViewActivity.this.finish();
							}
						});
					}

					// Our new log file
					logFile = app.appinfo.packageName + "." + System.currentTimeMillis() + ".log";
				} else {
					logFile = intent.getStringExtra(EXTRA_LOG_FILE);
				}
			}
		}

		// Service interactions...
		if (SysUtils.isServiceRunning(this, NetworkRecorderService.class.getName())) {
			// If we are resuming, rebind to the service
			if (!newLogRequest) {
				bindService();
			} else {
				unbindService();
			}
		} else if (newLogRequest) {
			startService();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_APP, app);
		outState.putIntegerArrayList(EXTRA_PORTS, ports);
		outState.putString(EXTRA_LOG_FILE, logFile);
	}

	protected void bindService() {
		// TODO implement
	}

	protected void unbindService() {
		// TODO implement
	}

	protected void startService() {
		final Intent startService = new Intent(this, NetworkRecorderService.class);
		startService.putExtra(NetworkRecorderService.EXTRA_APP_UID, app.uid);
		startService.putExtra(NetworkRecorderService.EXTRA_PORTS, ports);
		startService.putExtra(NetworkRecorderService.EXTRA_LOG_FILE, logFile);
		startService(startService);

		bindService();
	}

	protected void stopService() {
		unbindService();
		stopService(new Intent(this, NetworkRecorderService.class));
	}
}