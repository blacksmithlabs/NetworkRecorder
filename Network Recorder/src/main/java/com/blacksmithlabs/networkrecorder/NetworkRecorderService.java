package com.blacksmithlabs.networkrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;
import com.blacksmithlabs.networkrecorder.helpers.SysUtils;

import java.util.ArrayList;

/**
 * Created by brian on 6/24/13.
 */
public class NetworkRecorderService extends Service {
	public static final int NOTIFICATION_ID = "Network Recorder Service".hashCode();
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_BROADCAST_PACKET = 3;

	private boolean hasRoot = false;
	private boolean hasBinaries = false;

	final private Messenger messenger = new Messenger(new IncomingHandler(this));


	protected ArrayList<Messenger> clients = new ArrayList<Messenger>();


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

		// TODO install binaries
	}

	public IBinder onBind(Intent intent) {
		if (!hasRoot || !hasBinaries) {
			return null;
		} else {
			return messenger.getBinder();
		}
	}


	private boolean hasRoot() {
		return SysUtils.hasRoot(this);
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
