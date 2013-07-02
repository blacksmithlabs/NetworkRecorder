package com.blacksmithlabs.networkrecorder.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.blacksmithlabs.networkrecorder.R;

/**
 * Created by brian on 6/22/13.
 */
public class MessageBox {
	/**
	 * Displays a simple alert box
	 * @param ctx context
	 * @param msg message
	 */
	public static void alert(Context ctx, CharSequence msg) {
		alert(ctx, msg, null, null);
	}
	public static void alert(Context ctx, CharSequence msg, CharSequence title) {
		alert(ctx, msg, title, null);
	}
	public static void alert(Context ctx, CharSequence msg, CharSequence title, DialogInterface.OnDismissListener onDismiss) {
		if (ctx != null) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setNeutralButton(android.R.string.ok, null)
				.setMessage(msg);

			if (title != null) {
				builder.setTitle(title);
			}

			if (onDismiss != null) {
				builder.setOnDismissListener(onDismiss);
			}

			builder.show();
		}
	}

	public static void error(Context ctx, CharSequence msg) {
		error(ctx, msg, null);
	}

	public static void error(Context ctx, CharSequence msg, DialogInterface.OnDismissListener onDismiss) {
		alert(ctx, msg, ctx.getString(R.string.error_title), onDismiss);
	}
}
