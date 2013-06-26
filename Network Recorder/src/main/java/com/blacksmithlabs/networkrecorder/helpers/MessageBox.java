package com.blacksmithlabs.networkrecorder.helpers;

import android.app.AlertDialog;
import android.content.Context;
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
		alert(ctx, msg, null);
	}
	public static void alert(Context ctx, CharSequence msg, CharSequence title) {
		if (ctx != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setNeutralButton(android.R.string.ok, null)
				.setMessage(msg);

			if (title != null) {
				builder.setTitle(title);
			}

			builder.show();
		}
	}

	public static void error(Context ctx, CharSequence msg) {
		alert(ctx, msg, ctx.getString(R.string.error_title));
	}
}
