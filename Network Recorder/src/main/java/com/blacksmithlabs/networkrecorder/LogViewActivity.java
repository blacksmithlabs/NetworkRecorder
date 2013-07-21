package com.blacksmithlabs.networkrecorder;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.*;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.blacksmithlabs.networkrecorder.fragments.AppActionHeaderFragment;
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

	public static final int MAX_LINES = 200;
	public static boolean trimLines = false;

	public static Messenger service = null;
	public static Messenger messenger = null;
	public static boolean isBound = false;
	public static RecorderServiceConnection connection = new RecorderServiceConnection();

	protected ApplicationHelper.DroidApp app;
	protected ArrayList<Integer> ports;
	protected String logFile;
	protected boolean newLogRequest = false;
	protected AppActionHeaderFragment titleFragment;
	protected TextView logViewText;

	final private KillServiceReceiver killReceiver = new KillServiceReceiver();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_view);

		if (savedInstanceState != null) {
			app = savedInstanceState.getParcelable(EXTRA_APP);
			ports = savedInstanceState.getIntegerArrayList(EXTRA_PORTS);
			logFile = savedInstanceState.getString(EXTRA_LOG_FILE);
		} else {
			String errorMessage = null;

			final Intent intent = getIntent();
			if (intent != null) {
				app = intent.getParcelableExtra(EXTRA_APP);

				newLogRequest = intent.getBooleanExtra(EXTRA_START, false);
				if (newLogRequest) {
					ports = intent.getIntegerArrayListExtra(EXTRA_PORTS);

					if (app == null) {
						errorMessage = getString(R.string.error_settings_noapp);
					} else {
						// Our new log file
						logFile = app.appinfo.packageName + "." + System.currentTimeMillis() + ".log";
					}

					if (ports == null || ports.isEmpty()) {
						errorMessage = getString(R.string.settings_no_ports);
					}
				} else {
					logFile = intent.getStringExtra(EXTRA_LOG_FILE);
				}
			} else {
				errorMessage = getString(R.string.error_settings_noapp);
			}

			// Initialization error
			if (errorMessage != null) {
				MessageBox.error(this, errorMessage, new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						LogViewActivity.this.finish();
					}
				});
				return;
			}
		}

		titleFragment = (AppActionHeaderFragment)getSupportFragmentManager().findFragmentById(R.id.app_header_fragment);

		registerReceiver(killReceiver, new IntentFilter(NetworkRecorderService.BROADCAST_KILL_SERVICE));

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
	protected void onResume() {
		super.onResume();

		if (app != null) {
			titleFragment.populateAppData(app);
		}

		logViewText = (TextView)findViewById(R.id.log_view_text);
		logViewText.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(killReceiver);
		unbindService();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_APP, app);
		outState.putIntegerArrayList(EXTRA_PORTS, ports);
		outState.putString(EXTRA_LOG_FILE, logFile);
	}

	public void onAppHeaderToggleClicked(View view) {
		final ToggleButton toggle = (ToggleButton)view;
		if (toggle.isChecked()) {
			new Handler() {
				@Override
				public void handleMessage(Message msg) {
					startService();
				}
			}.sendEmptyMessageDelayed(0, 100);
		} else {
			promptStopService(toggle);
		}
	}

	protected void promptStopService(final ToggleButton toggle) {
		final AlertDialog.Builder prompt = new AlertDialog.Builder(this);

		prompt.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					stopService();
					toggle.setChecked(false);
				}
			})
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					toggle.setChecked(true);
				}
			})
			.setTitle(R.string.prompt_stop_recording_title)
			.setMessage(R.string.prompt_stop_recording_text)
			.show();
	}

	protected void toggleHeaderButton(boolean checked) {
		final View toggleView = findViewById(R.id.toggle_action);
		if (toggleView != null) {
			((ToggleButton)toggleView).setChecked(checked);
		}
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
		if (isBound) {
			if (service != null) {
				try {
					final Message msg = Message.obtain(null, NetworkRecorderService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = messenger;
					service.send(msg);
				} catch (RemoteException ex) {
					Log.e("NetworkRecorder", "RemoteException when unregistering with service", ex);
				}
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
		toggleHeaderButton(true);
	}

	protected void stopService() {
		toggleHeaderButton(false);
		unbindService();

		stopService(new Intent(this, NetworkRecorderService.class));
	}

	protected void appendText(String toAppend, boolean withNewline) {
		if (withNewline && !toAppend.endsWith("\n")) {
			toAppend += "\n";
		}

		if (logViewText != null) {
			logViewText.append(toAppend);
			// Remove excessive lines
			if (trimLines) {
				int excessLines = logViewText.getLineCount() - MAX_LINES;
				if (excessLines > 0) {
					int eolIndex = logViewText.getLayout().getLineEnd(excessLines);
					CharSequence newText = logViewText.getText().subSequence(eolIndex, logViewText.length());
					logViewText.setText(newText);
				}
			}
		}
	}

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case NetworkRecorderService.MSG_BROADCAST_LOG_LINE:
					appendText((String)msg.obj, true);
					break;

				case NetworkRecorderService.MSG_BROADCAST_LOG_EXIT:
					if (isBound) {
						appendText(getString(R.string.log_closed), true);
					}
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

	private class KillServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			toggleHeaderButton(false);
		}
	}
}