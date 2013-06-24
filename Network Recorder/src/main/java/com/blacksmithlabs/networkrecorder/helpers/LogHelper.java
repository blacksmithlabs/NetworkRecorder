package com.blacksmithlabs.networkrecorder.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import com.blacksmithlabs.networkrecorder.R;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by brian on 6/23/13.
 */
public class LogHelper {

	protected static LogGroup logs[] = null;

	public static void loadLogs(final Context ctx, final LogHandler handler, boolean showProgress) {
		if (logs != null) {
			onLogsLoaded(handler);
			return;
		}

		final ProgressDialog progress;
		if (showProgress) {
			final Resources resources = ctx.getResources();
			progress = ProgressDialog.show(ctx, resources.getString(R.string.working), resources.getString(R.string.reading_logs), true);
		} else {
			progress = null;
		}

		final Handler loadLogsHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				try
				{
					readLogs(ctx);
				}
				catch (Exception ex)
				{
					onLogsLoadError(handler, ex.getMessage());
					return;
				}
				finally
				{
					if (progress != null) {
						try {
							progress.dismiss();
						} catch (Exception ex) {
							// Oh well, we tried
						}
					}
				}

				onLogsLoaded(handler);
			}
		};


		// Make sure we have the apps loaded (so we can get their icons), then load our log files
		ApplicationHelper.loadApps(ctx, new ApplicationHelper.ApplicationHandler() {
			@Override
			public void onAppsLoaded(ApplicationHelper.DroidApp[] applications) {
				loadLogsHandler.sendEmptyMessage(0);
			}
			@Override
			public void onAppsLoadError(String error) {
				// I guess we won't have icons :'(
				loadLogsHandler.sendEmptyMessage(0);
			}
		}, false);
	}

	/**
	 * Do the heavy lifting for loading the log files
	 * @param ctx application context (mandatory)
	 * @throws Exception
	 */
	private static void readLogs(Context ctx) throws Exception {
		HashMap<Integer, ArrayList<LogFile>> logGroups = new HashMap<Integer, ArrayList<LogFile>>();

		// TODO actually implement
		ArrayList<LogFile> tmpFiles = new ArrayList<LogFile>();
		tmpFiles.add(new LogFile("tmpFile1", "2013-06-24 00:34:00"));
		tmpFiles.add(new LogFile("tmpFile2", "2013-06-24 00:00:00"));
		logGroups.put(10079, tmpFiles);
		logGroups.put(10099, tmpFiles);
		logGroups.put(10088, tmpFiles);
		logGroups.put(10107, tmpFiles);


		// Convert to our log group array
		LogGroup[] groups = new LogGroup[logGroups.size()];

		int i = 0;
		for (HashMap.Entry<Integer, ArrayList<LogFile>> entry : logGroups.entrySet()) {
			LogGroup group = new LogGroup();
			group.uid = entry.getKey();

			ArrayList<LogFile> logFiles = entry.getValue();
			group.logFiles = logFiles.toArray(new LogFile[logFiles.size()]);

			// Get the application information if we can
			group.app = ApplicationHelper.getAppInfo(group.uid);

			// Add it to the list
			groups[i++] = group;
		}

		// To make it thread-safe, we put it all in a temporary variable, then assign everything in one operation
		logs = groups;
	}

	private static void onLogsLoaded(LogHandler handler) {
		if (handler != null) {
			handler.onLogsLoaded(logs);
		}
	}

	private static void onLogsLoadError(LogHandler handler, String error) {
		if (handler != null) {
			handler.onLogsLoadError(error);
		}
	}

	public static interface LogHandler {
		public void onLogsLoaded(LogGroup[] logs);
		public void onLogsLoadError(String error);
	}

	/**
	 * Small structure to hold a group of logs associated with a given application
	 */
	public static final class LogGroup {
		/** linux user id of the application */
		public int uid;
		/** application associate with the log file (optional) */
		public ApplicationHelper.DroidApp app;
		/** log files for this group */
		public LogFile[] logFiles;
	}

	/**
	 * Small class to hold log file information
	 */
	public static final class LogFile {
		public String fileName;
		public String creationDate;

		public LogFile() {
		}
		public LogFile(String fileName, String creationDate) {
			this.fileName = fileName;
			this.creationDate = creationDate;
		}

		public BufferedReader getContents() {
			// TODO implement
			return null;
		}
	}
}
