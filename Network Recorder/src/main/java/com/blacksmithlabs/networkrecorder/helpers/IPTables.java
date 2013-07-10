package com.blacksmithlabs.networkrecorder.helpers;

import android.content.Context;
import android.util.Log;
import com.blacksmithlabs.networkrecorder.NetworkRecorderService;
import com.blacksmithlabs.networkrecorder.R;

import java.io.*;
import java.util.List;

/**
 * Created by brian on 7/9/13.
 */
public class IPTables {
	public static final String SCRIPT = "iptables.sh";

	private static String scriptHeader(Context context) {
		final File dir = SysUtils.getBinDir(context);
		final String myiptables = (new File(dir, SysUtils.getIptablesBin())).getAbsolutePath();
		final String mygrep = (new File(dir, SysUtils.getGrepBin())).getAbsolutePath();

		return  "IPTABLES=iptables\n" +
				"GREP=grep\n" +
				"# Find our grep\n" +
				"if echo 1 | " + mygrep + " -q 1 >/dev/null 2>/dev/null; then\n" +
				"    GREP=" + mygrep + "\n" +
				"elif ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null; then\n" +
				"    echo The grep command is required. NetworkRecorder will not work.\n" +
				"    exit 1\n" +
				"fi\n" +
				"# Find our iptables\n" +
				"if " + myiptables + " --version >/dev/null 2>/dev/null; then\n" +
				"    IPTABLES=" + myiptables + "\n" +
				"fi\n" +
				"\n";
	}

	public static boolean applyRules(Context context, int uid, List<Integer> ports, boolean showErrors) {
		if (context == null) {
			return false;
		}

		final StringBuilder script = new StringBuilder();
		// Set up iptables
		script.append(scriptHeader(context))
				.append("$IPTABLES --version || exit 1\n")
				.append("# Create our chain if necessary\n")
				.append("$IPTABLES -t nat -L netrec >/dev/null 2>/dev/null || $IPTABLES -t nat --new netrec || exit 2\n")
				.append("# Add our chain to the output chain\n")
				.append("$IPTABLES -t nat -L OUTPUT | $GREP -q netrec || $IPTABLES -t nat -A OUTPUT -j netrec || exit 3\n")
				.append("\n# Flush existing rules\n\n")
				.append("$IPTABLES -t nat -F netrec || exit 4\n")
				.append("\n# Enable port forwarding\n\n")
				.append("echo 1 > /proc/sys/net/ipv4/ip_forward\n");

		// Filter the desired ports to tcproxy
		script.append("\n# Filtering rules\n\n");
		for (int port : ports) {
			script.append("$IPTABLES -t nat -A netrec -p tcp -j REDIRECT")
				.append(" -m owner --uid-owner ").append(uid)
				.append(" --dport ").append(port)
				.append(" --to-ports ").append(NetworkRecorderService.LISTEN_PORT)
				.append(" || exit 5\n");
		}

		synchronized (SCRIPT) {
			try {
				final File f = new File(context.getFilesDir().getAbsolutePath(), SCRIPT);
				final String scriptPath = f.getAbsolutePath();
				final String scriptTag = "iptables-rules";

				final PrintWriter scriptWriter = new PrintWriter(new BufferedWriter(new FileWriter(f)));
				scriptWriter.println(script);
				scriptWriter.flush();
				scriptWriter.close();

				final String[] command = new String[] {"su", "-c", "sh " + scriptPath};

				final ShellCommand cmd = new ShellCommand(command, scriptTag);
				cmd.start(false);

				final StringBuilder result = new StringBuilder();
				while (!cmd.checkForExit()) {
					final String line = cmd.readStdout(true);
					if (line == null)
						continue;

					result.append(line);
				}

				if (cmd.getExitStatus() != 0) {
					String msg = result.toString();
					String[] unhelfupMsgs = new String[] {
						"\nTry `iptables -h' or 'iptables --help' for more information.",
						"\nprotoent* getprotobyname(char const*)(3) is not implemented on Android",
					};
					for (String unhelpful : unhelfupMsgs) {
						if (msg.indexOf(unhelpful) != -1) {
							msg = msg.replace(unhelpful, "");
						}
					}

					final String errorString = context.getResources().getString(R.string.error_applying_iptables_rules, cmd.getExitStatus(), msg);
					Log.e("NetworkRecorder", errorString);
					if (showErrors) {
						MessageBox.error(context, errorString);
					}
				} else {
					return true;
				}
			} catch (Exception e) {
				final String errorString = context.getResources().getString(R.string.error_general, e.getMessage());
				Log.e("NetworkRecorder", errorString);
				if (showErrors) {
					MessageBox.error(context, errorString);
				}
			}

			return false;
		}
	}

	public static boolean removeRules(Context context) {
		if (context == null) {
			return false;
		}

		final StringBuilder script = new StringBuilder();
		script.append(scriptHeader(context))
				.append("$IPTABLES --version || exit 1\n")
				.append("\n#Flush the rules, if they exist\n\n")
				.append("$IPTABLES -t nat -L netrec >/dev/null 2>/dev/null && ($IPTABLES -t nat -F netrec || exit 2)\n");

		SysUtils.executeScript(context, "iptables-rules", script.toString(), true, true);
		return true;
	}
}
