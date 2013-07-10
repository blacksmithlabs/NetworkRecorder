package com.blacksmithlabs.networkrecorder.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
	protected static String grepBin;
	protected static int grepSize;
	protected static int grepResId;

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
			tcproxySize = 13648;
			tcproxyResId = R.raw.tcproxy_armv7;
			grepBin = "grep_armv7";
			grepSize = 17692;
			grepResId = R.raw.grep_armv7;
		} else if (cpu_abi.contains("armeabi")) {
			iptablesBin = "iptables_armv5";
			iptablesSize = 196304;
			iptablesResId = R.raw.iptables_armv5;
			tcproxyBin = "tcproxy_armv5";
			tcproxySize = 17736;
			tcproxyResId = R.raw.tcproxy_armv5;
			grepBin = "grep_armv5";
			grepSize = 17688;
			grepResId = R.raw.grep_armv5;
		} else if (cpu_abi.contains("x86")) {
			iptablesBin = "iptables_x86";
			iptablesSize = 295916;
			iptablesResId = R.raw.iptables_x86;
			tcproxyBin = "tcproxy_x86";
			tcproxySize = 9588;
			tcproxyResId = R.raw.tcproxy_x86;
			grepBin = "grep_x86";
			grepSize = 17712;
			grepResId = R.raw.grep_x86;
		} else if (cpu_abi.contains("mips")) {
			iptablesBin = "iptables_mips";
			iptablesSize = 361264;
			iptablesResId = R.raw.iptables_mips;
			tcproxyBin = "tcproxy_mips";
			tcproxySize = 71088;
			tcproxyResId = R.raw.tcproxy_mips;
			grepBin = "grep_mips";
			grepSize = 71128;
			grepResId = R.raw.grep_mips;
		} else {
			iptablesBin = null;
			tcproxyBin = null;
			grepBin = null;
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

	public static String getGrepBin() {
		if (grepBin == null) {
			identifyBinaries();
		}
		return grepBin;
	}

	public static File getBinDir(Context context) {
		return context.getDir("bin", Context.MODE_PRIVATE);
	}

	protected static boolean installBinary(Context context, String binary, int size, int resId, boolean showErrors) {
		final File file = new File(getBinDir(context), binary);
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
				Log.e("NetworkRecorder", res.getString(R.string.error_installing_binary, binary, ex.getMessage()));
				if (showErrors) {
					MessageBox.error(context, res.getString(R.string.error_installing_binary, binary, ex.getMessage()));
				}
				return false;
			}
		}

		return true;
	}

	public static boolean installBinaries(Context context, boolean showErrors) {
		if (!identifyBinaries()) {
			final Resources res = context.getResources();
			MessageBox.alert(context, res.getString(R.string.error_unsupported_system_text, Build.CPU_ABI), res.getString(R.string.error_unsupported_system_title));
			return false;
		}

		if (!installBinary(context, iptablesBin, iptablesSize, iptablesResId, showErrors)) {
			return false;
		}

		if (!installBinary(context, tcproxyBin, tcproxySize, tcproxyResId, showErrors)) {
			return false;
		}

		if (!installBinary(context, grepBin, grepSize, grepResId, showErrors)) {
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

	/**
	 * The application UID cache.
	 * @use getApplicationUID
	 */
	private static Integer applicationUID = null;
	/**
	 * Get the UID of the application related to the context
	 * Be smart about using this, because it caches the value
	 * @param ctx
	 * @return
	 */
	public static Integer getApplicationUID(Context ctx) {
		if (applicationUID == null) {
			final PackageManager pm = ctx.getPackageManager();
			try {
				final ApplicationInfo ai = pm.getApplicationInfo(ctx.getPackageName(), 0);
				applicationUID = ai.uid;
			} catch (final PackageManager.NameNotFoundException e) {
				// Drat
			}
		}

		return applicationUID;
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
