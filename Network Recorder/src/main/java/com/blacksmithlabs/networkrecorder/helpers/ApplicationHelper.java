package com.blacksmithlabs.networkrecorder.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.util.Log;
import android.widget.ImageView;
import com.blacksmithlabs.networkrecorder.Constants;
import com.blacksmithlabs.networkrecorder.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by brian on 6/22/13.
 */
public class ApplicationHelper {

	// Cached applications
	protected static Map<Integer, DroidApp> applicationLookup = null;
	protected static DroidApp applications[] = null;

	/**
	 * Get specific information about an app. Not - only works after loadApps has been called
	 * @depends loadApps
	 * @param uid the UID of the app
	 * @return the App information
	 */
	public static DroidApp getAppInfo(int uid) {
		if (applicationLookup == null)
			return null;

		return applicationLookup.get(uid);
	}

	/**
	 * Get all the installed network applications
	 * @param ctx application context (mandatory)
	 * @param handler the handler for errors and on load complete
	 * @param showProgress whether to show a progress spinner while loading or not
	 * @return a list of applications
	 */
	public static void loadApps(final Context ctx, final ApplicationHandler handler, boolean showProgress) {
		// return cached instance if we have it
		if (applications != null) {
			onAppsLoaded(handler);
			return;
		}

		final ProgressDialog progress;
		if (showProgress) {
			final Resources resources = ctx.getResources();
			progress = ProgressDialog.show(ctx, resources.getString(R.string.working), resources.getString(R.string.reading_apps), true);
		} else {
			progress = null;
		}

		new Handler() {
			@Override
			public void handleMessage(Message msg) {
				try
				{
					readApps(ctx);
				}
				catch (Exception ex)
				{
					onAppLoadError(handler, ex.getMessage());
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

				onAppsLoaded(handler);
			}
		}.sendEmptyMessageDelayed(0, 100);
	}

	/**
	 * Do the heavy lifting of loading the applications
	 * @param ctx application context (mandatory)
	 * @throws Exception
	 */
	private static void readApps(Context ctx) throws Exception {
		final SharedPreferences preferences = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
		// Allowed application names separated by pipe '|' (persisted)
		final String savedUid = preferences.getString(Constants.PREF_SELECTED_UID, "");
		final HashSet<Integer> selected = new HashSet<Integer>();
		if (!savedUid.isEmpty()) {
			try {
				selected.add(Integer.parseInt(savedUid));
			} catch (Exception ex) {
				// That didn't work...
			}
		}

		final PackageManager pkgManager = ctx.getPackageManager();
		final List<ApplicationInfo> installed = pkgManager.getInstalledApplications(0);
		final SharedPreferences.Editor edit = preferences.edit();

		applicationLookup = new HashMap<Integer, DroidApp>();

		boolean changed = false;
		String name = null;
		String cachekey = null;
		DroidApp app = null;

		for (final ApplicationInfo apInfo : installed) {
			boolean firstseen = false;
			app = applicationLookup.get(apInfo.uid);
			// Filter applications which are not allowed to access the Internet
			if (app == null && PackageManager.PERMISSION_GRANTED != pkgManager.checkPermission(android.Manifest.permission.INTERNET, apInfo.packageName)) {
				continue;
			}
			// We're not going to allow the infinite loop of filtering our own traffic
			if (apInfo.packageName.equals(ctx.getPackageName())) {
				continue;
			}

			// Try to get the application label from our cache since getApplicationLable is horribly slow
			cachekey = "cache.label."+apInfo.packageName;
			name = preferences.getString(cachekey, "");
			if (name.isEmpty()) {
				// Get label and put in cache
				name = pkgManager.getApplicationLabel(apInfo).toString();
				edit.putString(cachekey, name);
				changed = true;
				firstseen = true;
			}

			if (app == null) {
				app = new DroidApp();
				app.uid = apInfo.uid;
				app.names = new String[] { name };
				app.appinfo = apInfo;
				applicationLookup.put(apInfo.uid, app);
			} else {
				final String newnames[] = new String[app.names.length + 1];
				System.arraycopy(app.names, 0, newnames, 0, app.names.length);
				newnames[app.names.length] = name;
				app.names = newnames;
			}
			app.firstseen = firstseen;

			// Check if this application is selected
			if (!app.selected && selected.contains(app.uid)) {
				app.selected = true;
			}
		}

		if (changed) {
			edit.commit();
		}

		// Add special applications to the list
		final DroidApp special[] = {
				// TODO add in kernel and other apps when we support them
		};
		for (DroidApp dapp : special) {
			if (dapp.uid != -1 && !applicationLookup.containsKey(dapp.uid)) {
				// Is it selected?
				if (selected.contains(dapp.uid)) {
					dapp.selected = true;
				}
				applicationLookup.put(dapp.uid, dapp);
			}
		}

		// Convert the map into an array
		applications = applicationLookup.values().toArray(new DroidApp[applicationLookup.size()]);
	}

	private static void onAppsLoaded(ApplicationHandler handler) {
		if (handler != null) {
			handler.onAppsLoaded(applications);
		}
	}

	private static void onAppLoadError(ApplicationHandler handler, String error) {
		if (handler != null) {
			handler.onAppsLoadError(error);
		}
	}

	public static interface ApplicationHandler {
		public void onAppsLoaded(DroidApp[] applications);
		public void onAppsLoadError(String error);
	}

	/**
	 * Small structure to hold an application info
	 */
	public static final class DroidApp implements Parcelable {
		/** linux user id */
		public int uid;
		/** application names belonging to this user id */
		public String names[];
		/** whether we are monitoring this app or not */
		public boolean selected;
		/** application info */
		public ApplicationInfo appinfo;
		/** first time seen? */
		public boolean firstseen;

		/** toString cache */
		private String tostr;
		/** cached application icon */
		private Drawable cached_icon;
		/** indicates if the icon has been loaded already */
		private boolean icon_loaded;

		public DroidApp() {
		}
		public DroidApp(int uid, String name, boolean selected) {
			this.uid = uid;
			this.names = new String[] {name};
			this.selected = selected;
		}

		/**
		 * Unparcel an instance of this class
		 * @param parcel
		 */
		private DroidApp(Parcel parcel) {
			uid = parcel.readInt();

			int namesLen = parcel.readInt();
			names = new String[namesLen];
			parcel.readStringArray(names);

			selected = parcel.readInt() == 1;
			appinfo = parcel.readParcelable(ApplicationInfo.class.getClassLoader());
		}
		/**
		 * Screen representation of this application
		 */
		@Override
		public String toString() {
			if (tostr == null) {
				final StringBuilder s = new StringBuilder();
				if (uid > 0) s.append(uid).append(": ");
				for (int i=0; i<names.length; i++) {
					if (i != 0) s.append(", ");
					s.append(names[i]);
				}
				s.append("\n");
				tostr = s.toString();
			}
			return tostr;
		}

		public void loadIcon(final Context ctx, final ImageView view) {
			view.setImageDrawable(cached_icon);
			if (!icon_loaded && appinfo != null) {
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... args) {
						try {
							if (!icon_loaded) {
								cached_icon = ctx.getPackageManager().getApplicationIcon(appinfo);
								icon_loaded = true;
							}
						} catch (Exception ex) {
							Log.e("networkrecorder", "Error loading icon", ex);
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void arg) {
						try {
							view.setImageDrawable(cached_icon);
						} catch (Exception ex) {
							Log.e("networkrecorder", "Error showing icon", ex);
						}
					}
				}.execute();
			}
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeInt(uid);
			parcel.writeInt(names.length);
			parcel.writeStringArray(names);
			parcel.writeInt(selected ? 1 : 0);
			parcel.writeParcelable(appinfo, flags);
		}

		public static final Creator<DroidApp> CREATOR = new Creator<DroidApp>() {
			@Override
			public DroidApp createFromParcel(Parcel parcel) {
				return new DroidApp(parcel);
			}

			@Override
			public DroidApp[] newArray(int size) {
				return new DroidApp[size];
			}
		};
	}
}
