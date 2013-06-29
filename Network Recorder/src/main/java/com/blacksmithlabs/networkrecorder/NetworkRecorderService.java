package com.blacksmithlabs.networkrecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;
import com.blacksmithlabs.networkrecorder.helpers.SysUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by brian on 6/24/13.
 */
public class NetworkRecorderService extends Service {
	public static final int NOTIFICATION_ID = "Network Recorder Service".hashCode();
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_BROADCAST_PACKET = 3;

	public static final String EXTRA_APP_UID = "NetworkRecorderService.uid";
	public static final String EXTRA_PORTS = "NetworkRecorderService.ports";
	public static final String EXTRA_LOG_FILE = "NetworkRecorderService.logFile";

	public static final String BROADCAST_KILL_SERVICE = "NetworkRecorderService.stop";
	public static final String BROADCAST_EXTRA_VIEW_LOG = "NetworkRecorderService.viewLog";

	public static NetworkRecorderService instance;
	public static Handler handler;

	private static String logFile = null;
	private static Notification notification;

	private boolean hasRoot = false;
	private boolean hasBinaries = false;

	protected ArrayList<Messenger> clients = new ArrayList<Messenger>();
	final private Messenger messenger = new Messenger(new IncomingHandler(this));

	final private KillServiceReceiver killReceiver = new KillServiceReceiver();


	@Override
	public void onCreate() {
		Log.d("NetworkRecorder", "[service] onCreate");

		if (!hasRoot()) {
			MessageBox.error(this, getString(R.string.error_noroot));
			hasRoot = false;
			stopSelf();
			return;
		} else {
			hasRoot = true;
		}

		if (!SysUtils.installBinaries(this)) {
			hasBinaries = false;
			stopSelf();
			return;
		} else {
			hasBinaries = true;
		}

		if (instance != null) {
			Log.w("NetworkRecorder", "[service] Last instance destroyed unexpectedly");
		}

		instance = this;
		handler = new Handler();

		registerReceiver(killReceiver, new IntentFilter(BROADCAST_KILL_SERVICE));
	}

	public IBinder onBind(Intent intent) {
		if (!hasRoot || !hasBinaries) {
			return null;
		} else {
			return messenger.getBinder();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("NetworkRecorder", "[service] onStartCommand");

		if (!hasRoot || !hasBinaries || intent == null) {
			return Service.START_NOT_STICKY;
		}

		final Bundle extras = intent.getExtras();

		final int uid = extras.getInt(EXTRA_APP_UID, -1);
		final ArrayList<Integer> ports = extras.getIntegerArrayList(EXTRA_PORTS);

		if (uid == -1 || ports == null || ports.isEmpty()) {
			Log.e("NetworkRecorder", "[service] Invalid arguments. No app or ports specified.");
			return Service.START_NOT_STICKY;
		}


		logFile = extras.getString(EXTRA_LOG_FILE);
		if (logFile == null || logFile.isEmpty()) {
			final String date = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
			logFile =  date + ".log";
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d("NetworkRecorder", "[service] Starting: " + logFile);

				if (!startRecording(uid, ports)) {
					Log.d("NetworkRecorder", "[service] start recording error, aborting");
					handler.post(new Runnable() {
						@Override
						public void run() {
							stopSelf();
						}
					});
				}
			}
		}).start();

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("NetworkRecorder", "[service] onDestroy");

		unregisterReceiver(killReceiver);
		stopForeground(true);
		instance = null;
		handler = null;

		if (hasRoot && hasBinaries) {
			stopRecording();
		}
	}

	private boolean hasRoot() {
		return SysUtils.hasRoot(this);
	}

	protected Notification createNotification() {
		final Intent logViewIntent = new Intent(this, LogViewActivity.class);
		logViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		logViewIntent.putExtra(LogViewActivity.EXTRA_LOG_FILE, logFile);

		final Intent stopLogIntent = new Intent(BROADCAST_KILL_SERVICE);
		stopLogIntent.putExtra(BROADCAST_EXTRA_VIEW_LOG, true);

		return new Notification.Builder(this)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.notification_text))
				.setTicker(getString(R.string.notification_ticker))
				.setSmallIcon(R.drawable.arrow_page)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(PendingIntent.getActivity(this, 0, logViewIntent, PendingIntent.FLAG_UPDATE_CURRENT))
				.addAction(android.R.drawable.ic_delete, getString(R.string.recording_stop), PendingIntent.getBroadcast(this, 0, stopLogIntent, 0))
				.setOngoing(true)
				.build();
	}

	protected boolean startRecording(final int uid, final List<Integer> ports) {
		notification = createNotification();

		startForeground(NOTIFICATION_ID, notification);

		// TODO finish implementing

		return true;
	}

	protected void stopRecording() {
		// TODO implement
	}

	private class KillServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(BROADCAST_EXTRA_VIEW_LOG, false)) {
				final Intent viewIntent = new Intent(NetworkRecorderService.this, LogViewActivity.class);
				viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				viewIntent.putExtra(LogViewActivity.EXTRA_LOG_FILE, logFile);
				getApplication().startActivity(viewIntent);
			}

			NetworkRecorderService.this.stopSelf();
		}
	}

	private class IncomingHandler extends Handler {
		final private Context context;

		public IncomingHandler(Context context) {
			this.context = context;
		}

		@Override
		public void handleMessage(Message msg) {
			Log.d("NetworkRecorder", "[service] get message: " + msg);

			switch (msg.what) {
				case MSG_REGISTER_CLIENT:
					Log.d("NetworkRecorder", "[service] registering client " + msg.replyTo);
					clients.add(msg.replyTo);
					break;

				case MSG_UNREGISTER_CLIENT:
					Log.d("NetworkRecorder", "[service] unregistering client " + msg.replyTo);
					clients.remove(msg.replyTo);
					break;

				case MSG_BROADCAST_PACKET:
					Log.d("NetworkRecorder", "[service] got MSG_BROADCAST_PACKET unexpectedly");
					break;

				default:
					Log.d("NetworkRecorder", "[service] unhandled message");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
