package com.blacksmithlabs.networkrecorder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.blacksmithlabs.networkrecorder.fragments.AppActionHeaderFragment;
import com.blacksmithlabs.networkrecorder.helpers.ApplicationHelper;
import com.blacksmithlabs.networkrecorder.helpers.LogHelper;
import com.blacksmithlabs.networkrecorder.helpers.MessageBox;

import java.io.BufferedReader;
import java.io.IOException;

public class ViewLogActivity extends Activity
		implements ApplicationHelper.ApplicationHandler {
	public static final String LOG_INFO = "ViewLog.info";

	protected ProgressDialog loadingProgress;
	protected LogHelper.LogFile logInfo;
	protected AppActionHeaderFragment titleFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_log);

	    if (savedInstanceState != null) {
		    logInfo = savedInstanceState.getParcelable(LOG_INFO);
	    } else {
			String errorMessage = getString(R.string.error_viewlog_nolog);

		    final Intent intent = getIntent();
		    if (intent != null) {
				logInfo = intent.getParcelableExtra(LOG_INFO);
			    if (logInfo != null) {
				    errorMessage = null;
			    }
		    }

		    // Initialization error
		    if (errorMessage != null) {
			    MessageBox.error(this, errorMessage, new DialogInterface.OnDismissListener() {
				    @Override
				    public void onDismiss(DialogInterface dialogInterface) {
					    ViewLogActivity.this.finish();
				    }
			    });
			    return;
		    }
	    }

	    // Spawn off our start up in a thread
	    ApplicationHelper.loadApps(this, this, false);
	    loadingProgress = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.log_initializing), true, false);
	    new Handler() {
		    @Override
		    public void handleMessage(Message msg) {
			    final TextView logText = (TextView)findViewById(R.id.log_view_text);
			    final BufferedReader reader = logInfo.getContents();

			    // Populate the text box
			    String line;
			    try {
				    while ((line = reader.readLine()) != null) {
					    logText.append(line + "\n");
				    }
			    } catch (IOException ex) {
				    MessageBox.error(ViewLogActivity.this, ex.getMessage(), new DialogInterface.OnDismissListener() {
					    @Override
					    public void onDismiss(DialogInterface dialogInterface) {
						    ViewLogActivity.this.finish();
					    }
				    });
			    }

			    try {
				    loadingProgress.dismiss();
			    } catch (Exception ex) {
				    // ...
			    }
		    }
	    }.sendEmptyMessageDelayed(0, 100);
    }

	/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_log, menu);
        return true;
    }
    */

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(LOG_INFO, logInfo);
	}

	@Override
	public void onAppsLoaded(ApplicationHelper.DroidApp[] applications) {
		final ApplicationHelper.DroidApp app = ApplicationHelper.getAppInfo(logInfo.appUID);

		final TextView text = (TextView)findViewById(R.id.app_info_text);
		final ImageView icon = (ImageView)findViewById(R.id.app_info_icon);

		text.setText(app.toString());
		app.loadIcon(this, icon);
	}

	@Override
	public void onAppsLoadError(String error) {
		// Just hide the top layout elements and move on with life
		final LinearLayout topBar = (LinearLayout)findViewById(R.id.app_info_container);
		if (topBar != null) {
			((LinearLayout)topBar.getParent()).removeView(topBar);
		}
	}
}
