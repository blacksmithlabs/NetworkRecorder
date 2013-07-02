package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.*;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;
import com.blacksmithlabs.networkrecorder.helpers.SysUtils;

import java.util.ArrayList;

/**
 * Created by brian on 6/24/13.
 */
public class LogViewActivity extends FragmentActivity {
	public static final String EXTRA_APP = "LogView.app";
	public static final String EXTRA_PORTS = "LogView.ports";
	public static final String EXTRA_START = "LogView.start";
	public static final String EXTRA_LOG_FILE = "LogView.logFile";

	public static Messenger service = null;
	public static Messenger messenger = null;
	public static boolean isBound = false;
	public static RecorderServiceConnection connection = new RecorderServiceConnection();

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
					ports = intent.getIntegerArrayListExtra(EXTRA_PORTS);

					String errorMessage = null;
					if (app == null) {
						errorMessage = getString(R.string.error_settings_noapp);
					} else if (ports == null || ports.isEmpty()) {
						errorMessage = getString(R.string.settings_no_ports);
					}

					if (errorMessage != null) {
						MessageBox.error(this, errorMessage, new DialogInterface.OnDismissListener() {
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

		// Spawn off our start up in a thread
		final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.log_initializing), true, true);
		new Handler() {
			@Override
			public void handleMessage(Message msg) {
				boolean serviceRunning = SysUtils.isServiceRunning(LogViewActivity.this, NetworkRecorderService.class.getName());

				try {
					progress.dismiss();
				} catch (Exception ex) {
					// ...
				}

				// Service interactions...
				if (serviceRunning) {
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
		}.sendEmptyMessageDelayed(0, 100);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_APP, app);
		outState.putIntegerArrayList(EXTRA_PORTS, ports);
		outState.putString(EXTRA_LOG_FILE, logFile);
	}

	protected void bindService() {
		if (isBound) {
			Log.d("NetworkRecorder", "Already bound to service. Unbinding...");
			unbindService();
		}

		messenger = new Messenger(new IncomingHandler());
		bindService(new Intent(this, NetworkRecorderService.class), connection, 0);
	}

	protected void unbindService() {
		if (isBound && service != null) {
			try {
				final Message msg = Message.obtain(null, NetworkRecorderService.MSG_UNREGISTER_CLIENT);
				msg.replyTo = messenger;
				service.send(msg);
			} catch (RemoteException ex) {
				Log.e("NetworkRecorder", "RemoteException when unregistering with service", ex);
			}

			try {
				unbindService(connection);
			} catch (Exception ex) {
				// Ignore...
			} finally {
				isBound = false;
			}
		}
	}

	protected void startService() {
		final Intent startService = new Intent(this, NetworkRecorderService.class);
		startService.putExtra(NetworkRecorderService.EXTRA_APP_UID, app.uid);
		startService.putExtra(NetworkRecorderService.EXTRA_PORTS, ports);
		startService.putExtra(NetworkRecorderService.EXTRA_LOG_FILE, logFile);
		startService(startService);

		bindService();

		// TODO subscribe to the service exit broadcast so we can handle the interface accordingly
		// TODO make sure the header button is toggled on
	}

	protected void stopService() {
		unbindService();

		stopService(new Intent(this, NetworkRecorderService.class));

		// TODO make sure the header button is toggled off
	}

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("NetworkRecorder", "[client] Received message: " + msg);

			switch (msg.what) {
				case NetworkRecorderService.MSG_BROADCAST_PACKET:
					// TODO handle the packet
					break;

				default:
					super.handleMessage(msg);
					break;
			}
		}
	}

	private static class RecorderServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = new Messenger(iBinder);
			isBound = true;

			// Register with the service
			try {
				final Message msg = Message.obtain(null, NetworkRecorderService.MSG_REGISTER_CLIENT);
				msg.replyTo = messenger;
				service.send(msg);
			} catch (RemoteException ex) {
				Log.e("NetworkRecorder", "RemoteException when registering with service", ex);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			service = null;
			isBound = false;
		}
	}
}