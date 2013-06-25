package com.blacksmithlabs.networkrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;
import com.blacksmithlabs.networkrecorder.helpers.SysUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

	public static NetworkRecorderService instance;
	public static Handler handler;

	private static String logFile = null;
	private static Notification notification;

	private boolean hasRoot = false;
	private boolean hasBinaries = false;

	protected ArrayList<Messenger> clients = new ArrayList<Messenger>();
	final private Messenger messenger = new Messenger(new IncomingHandler(this));


	@Override
	public void onCreate() {
		Log.d("NetworkRecorder", "[service] onCreate");

		if (!hasRoot()) {
			MessageBox.alert(this, getString(R.string.error_noroot), getString(R.string.error_title));
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
		final String ports = extras.getString(EXTRA_PORTS, null);

		if (uid == -1 || ports == null || ports.isEmpty()) {
			Log.e("NetworkRecorder", "[service] Invalid arguments. No app or ports specified.");
			return Service.START_NOT_STICKY;
		}

		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
		final String date = df.format(new Date());
		logFile =  date + ".pk.log";

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

	protected boolean startRecording(final int uid, final String ports) {
		final Intent logViewIntent = new Intent(this, LogView.class);
		logViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final Intent stopLogIntent = new Intent(this, LogView.class);
		stopLogIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		stopLogIntent.putExtra("KillLog", true);

		notification = new Notification.Builder(this)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.notification_text))
				.setTicker(getString(R.string.notification_ticker))
				.setSmallIcon(R.drawable.arrow_page)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(PendingIntent.getActivity(this, 0, logViewIntent, 0))
				.addAction(android.R.drawable.ic_delete, "Kill Log", PendingIntent.getActivity(this, 0, stopLogIntent, 0))
				.setOngoing(true)
				.build();

		startForeground(NOTIFICATION_ID, notification);

		// TODO finish implementing

		return true;
	}

	protected void stopRecording() {
		// TODO implement
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
