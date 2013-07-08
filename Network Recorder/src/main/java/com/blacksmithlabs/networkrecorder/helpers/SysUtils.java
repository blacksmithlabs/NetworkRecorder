package com.blacksmithlabs.networkrecorder.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import com.blacksmithlabs.networkrecorder.R;

import java.io.*;
import java.util.zip.ZipInputStream;

/**
 * Created by brian on 6/24/13.
 */
public class SysUtils {
	public static final String SCRIPT = "networkrecorder.sh";

	protected static String iptablesBin;
	protected static int iptablesSize;
	protected static int iptablesResId;
	protected static String tcproxyBin;
	protected static int tcproxySize;
	protected static int tcproxyResId;

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

	protected static boolean identifyBinaries() {
		String cpu_abi = Build.CPU_ABI.toLowerCase();

		if (cpu_abi.contains("armeabi-v7")) {
			iptablesBin = "iptables_armv7";
			iptablesSize = 192261;
			iptablesResId = R.raw.iptables_armv7;
			tcproxyBin = "tcproxy_armv7";
			tcproxySize = 13596;
			tcproxyResId = R.raw.tcproxy_armv7;
		} else if (cpu_abi.contains("armeabi")) {
			iptablesBin = "iptables_armv5";
			iptablesSize = 196304;
			iptablesResId = R.raw.iptables_armv5;
			tcproxyBin = "tcproxy_armv5";
			tcproxySize = 17688;
			tcproxyResId = R.raw.tcproxy_armv5;
		} else if (cpu_abi.contains("x86")) {
			iptablesBin = "iptables_x86";
			iptablesSize = 295916;
			iptablesResId = R.raw.iptables_x86;
			tcproxyBin = "tcproxy_x86";
			tcproxySize = 9536;
			tcproxyResId = R.raw.tcproxy_x86;
		} else if (cpu_abi.contains("mips")) {
			iptablesBin = "iptables_mips";
			iptablesSize = 361264;
			iptablesResId = R.raw.iptables_mips;
			tcproxyBin = "tcproxy_mips";
			tcproxySize = 71072;
			tcproxyResId = R.raw.tcproxy_mips;
		} else {
			iptablesBin = null;
			tcproxyBin = null;
			return false;
		}
		return true;
	}

	public static String getIptablesBin() {
		if (iptablesBin == null) {
			identifyBinaries();
		}
		return iptablesBin;
	}

	public static String getTcproxyBin() {
		if (tcproxyBin == null) {
			identifyBinaries();
		}
		return tcproxyBin;
	}

	protected static boolean installBinary(Context context, String binary, int size, int resId, File file) {
		// Need to update/install the file
		if (!file.exists() || file.length() != size) {
			final Resources res = context.getResources();

			try {
				final ZipInputStream in = new ZipInputStream(res.openRawResource(resId));
				in.getNextEntry();

				final FileOutputStream out = new FileOutputStream(file);

				int len;
				byte buf[] = new byte[1024];
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				out.close();
				in.close();

				Runtime.getRuntime().exec("chmod 755 " + file.getAbsolutePath()).waitFor();
			} catch (Exception ex) {
				MessageBox.error(context, res.getString(R.string.error_installing_binary, binary, ex.getMessage()));
				return false;
			}
		}

		return true;
	}

	public static boolean installBinaries(Context context) {
		if (!identifyBinaries()) {
			final Resources res = context.getResources();
			MessageBox.alert(context, res.getString(R.string.error_unsupported_system_text, Build.CPU_ABI), res.getString(R.string.error_unsupported_system_title));
			return false;
		}

		final File binDir = context.getDir("bin", context.MODE_PRIVATE);

		final File iptables = new File(binDir, iptablesBin);
		if (!installBinary(context, iptablesBin, iptablesSize, iptablesResId, iptables)) {
			return false;
		}

		final File tcproxy = new File(binDir, tcproxyBin);
		if (!installBinary(context, tcproxyBin, tcproxySize, tcproxyResId, tcproxy)) {
			return false;
		}

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

	public static boolean isServiceRunning(Context context, String serviceName) {
		final ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceName.equals(service.service.getClassName())) {
				return true;
			}
		}

		return false;
	}
}
