package com.blacksmithlabs.networkrecorder.helpers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Created by brian on 6/24/13.
 */
public class ShellCommand {
	final protected Runtime rt;
	final protected String[] command;
	final protected String tag;

	protected Process process;
	protected BufferedReader stdout;
	protected int exitStatus;

	public ShellCommand(String[] command, String tag) {
		this.command = command;
		this.tag = (tag == null || tag.isEmpty()) ? null : tag;
		this.rt = Runtime.getRuntime();
	}

	public ShellCommand(String[] command) {
		this(command, null);
	}

	public String start(boolean waitForExit) {
		if (process == null) {
			Log.d("NetworkRecorder", "ShellCommand: starting [" + tag + "] " + Arrays.toString(command));

			try {
				exitStatus = -1;

				process = new ProcessBuilder()
						.command(command)
						.redirectErrorStream(true)
						.start();

				stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			} catch (Exception ex) {
				Log.e("NetworkRecorder", "Failure starting shell command [" + tag + "]", ex);
				return ex.getCause().getMessage();
			}
		}

		if (waitForExit) {
			waitForExit();
		}

		return null;
	}

	public int getExitStatus() {
		return exitStatus;
	}

	public void waitForExit() {
		while (checkForExit() == false) {
			if (stdoutAvailable()) {
				// Discarding the read
			} else {
				try {
					Thread.sleep(100);
				} catch (Exception ex) {
					Log.d("NetworkRecorder", "waitForExit", ex);
				}
			}
		}
	}

	public void finish() {
		if (process == null) {
			return;
		}

		Log.d("NetworkRecorder", "ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));

		try {
			if (stdout != null) {
				stdout.close();
			}
		} catch (Exception ex) {
			Log.e("NetworkRecorder", "ShellCommand: Exception finishing [" + tag + "]", ex);
		}

		process.destroy();
		process = null;
	}

	public boolean checkForExit() {
		if (process == null) {
			return true;
		}

		try {
			exitStatus = process.exitValue();
			Log.d("NetworkRecorder", "ShellCommand: exited [" + tag + "] with " + exitStatus);
		} catch (Exception ex) {
			return false;
		}

		finish();
		return true;
	}

	public boolean stdoutAvailable() {
		if (stdout == null) {
			return false;
		}

		try {
			return stdout.ready();
		} catch (IOException ex) {
			Log.e("NetworkRecorder", "ShellCommand: stdoutAvailable [" + tag + "] error", ex);
			return false;
		}
	}

	public String readStdout(boolean block) {
		final String line;

		if (stdout == null) {
			return null;
		}

		try {
			// If we are going to block on read or we have input ready
			if (block || stdout.ready()) {
				line = stdout.readLine();
				if (line == null) {
					return null;
				} else {
					return line + "\n";
				}
			} else {
				// Non-blocking and nothing to read
				return "";
			}
		} catch (Exception ex) {
			Log.e("NetworkRecorder", "ShellCommand: readStdout(" + block + ") [" + tag + "] error", ex);
			return null;
		}
	}
}
