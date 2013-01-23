package com.pebble.trendwatcher;

import static com.pebble.trendwatcher.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.pebble.trendwatcher.CommonUtilities.SENDER_ID;
import static com.pebble.trendwatcher.CommonUtilities.SERVER_URL;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.pebble.Web.AsyncHttpGet;

public class TrendWATCHERMain extends Activity {

	TextView mDisplay;
	AsyncTask<Void, Void, Void> mRegisterTask;
	SharedPreferences prefs;
	String genId;
	Context c;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		genId = prefs.getString("genId", "");
		c = this;

		setContentView(R.layout.activity_trend_watchermain);
		checkNotNull(SERVER_URL, "SERVER_URL");
		checkNotNull(SENDER_ID, "SENDER_ID");
		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(this);
		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		GCMRegistrar.checkManifest(this);
		// mDisplay = (TextView) findViewById(R.id.display);
		registerReceiver(mHandleMessageReceiver, new IntentFilter(
				DISPLAY_MESSAGE_ACTION));
		final String regId = GCMRegistrar.getRegistrationId(this);
		if (regId.equals("")) {
			// Automatically registers application on startup.
			GCMRegistrar.register(this, SENDER_ID);
		} else {
			// Device is already registered on GCM, check server.
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skips registration.
				// mDisplay.append(getString(R.string.already_registered) +
				// "\n");
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = this;
				mRegisterTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						boolean registered = ServerUtilities.register(context,
								regId);
						// At this point all attempts to register with the app
						// server failed, so we need to unregister the device
						// from GCM - the app will try to register again when
						// it is restarted. Note that GCM will send an
						// unregistered callback upon completion, but
						// GCMIntentService.onUnregistered() will ignore it.
						if (!registered) {
							GCMRegistrar.unregister(context);
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mRegisterTask = null;
					}

				};
				mRegisterTask.execute(null, null, null);
			}
		}

		if (!genId.contains("")) {
			getTrends();
		}

	}

	public void registered(String newgenId) {
		SharedPreferences.Editor editor = prefs.edit();
		genId = newgenId;
		editor.putString("genId", genId);
		editor.commit();

		getTrends();

	}

	private void getTrends() {
		AsyncHttpGet asyncHttpGet = new AsyncHttpGet() {
			@Override
			protected void onPostExecute(String result) {
				gotTrends(result);

			};
		};
		asyncHttpGet
				.execute("http://trend-watcher.herokuapp.com/currenttrends");
	}

	public void gotTrends(String result) {

		try {
			JSONArray toptrends = new JSONArray(result);

			ArrayList<String> trendList = new ArrayList<String>();
			JSONObject trend = new JSONObject();

			for (int i = 0; i < toptrends.length(); i++) {

				trend = toptrends.getJSONObject(i);
				try {
					trendList.add(trend.getString("trend_name"));
				} catch (Exception e) {
					//
				}
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	protected void onDestroy() {
		if (mRegisterTask != null) {
			mRegisterTask.cancel(true);
		}
		unregisterReceiver(mHandleMessageReceiver);
		GCMRegistrar.onDestroy(this);
		super.onDestroy();
	}

	private void checkNotNull(Object reference, String name) {
		if (reference == null) {
			throw new NullPointerException("error thing" + name);
		}
	}

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String newMessage = intent.getExtras().getString("message");
			Toast.makeText(context, newMessage, Toast.LENGTH_LONG).show();
			if (newMessage.contains("update")) {
			} else if (!newMessage.contains("nothing")) {
				registered(newMessage);

			}
			// mDisplay.append(newMessage + "\n");
		}

	};

}
