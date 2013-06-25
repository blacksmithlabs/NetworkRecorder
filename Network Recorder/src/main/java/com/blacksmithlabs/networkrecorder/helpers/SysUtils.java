package com.blacksmithlabs.networkrecorder.helpers;

import android.content.Context;
import android.util.Log;

import java.io.*;

/**
 * Created by brian on 6/24/13.
 */
public class SysUtils {
	public static final String SCRIPT = "networkrecorder.sh";

	public static boolean hasRoot(Context context) {
		final String error = executeScript(context, "checkRoot", "exit 0", true, true);
		if (error != null) {
			Log.d("NetworkRecorder", "Failed check root: " + error);
			return false;
		} else {
			Log.d("NetworkRecorder", "Check root passed");
			return true;
		}
	}

	public static boolean installBinaries(Context context) {
		// TODO implement
		return true;
	}

	public static String executeScript(Context context, String tag, String script, boolean waitForExit, boolean asRoot) {
		synchronized (SCRIPT) {
			final File f = new File(context.getFilesDir().getAbsolutePath(), SCRIPT);
			final String scriptPath = f.getAbsolutePath();

			try {
				final PrintWriter scriptWriter = new PrintWriter(new BufferedWriter(new FileWriter(f)));
				scriptWriter.println(script);
				scriptWriter.flush();
				scriptWriter.close();
			} catch (IOException ex) {
				Log.e("NetworkRecorder", "Script '" + tag + "' creation error", ex);
			}

			final String[] command;
			if (asRoot) {
				command = new String[] {"su", "-c", "sh " + scriptPath};
			} else {
				command = new String[] {"sh " + scriptPath};
			}

			return new ShellCommand(command, tag).start(waitForExit);
		}
	}
}
