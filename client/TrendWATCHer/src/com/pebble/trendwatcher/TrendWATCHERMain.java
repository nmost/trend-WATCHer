package com.pebble.trendwatcher;

import static com.pebble.trendwatcher.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.pebble.trendwatcher.CommonUtilities.SENDER_ID;
import static com.pebble.trendwatcher.CommonUtilities.SERVER_URL;

import java.util.ArrayList;
import java.util.HashMap;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.pebble.Web.AsyncHttpGet;
import com.pebble.Web.AsyncHttpPost;

public class TrendWATCHERMain extends Activity implements OnClickListener {

	AsyncTask<Void, Void, Void> mRegisterTask;
	SharedPreferences prefs;
	String genId;
	Context c;
	private String picked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		genId = prefs.getString("genId", "");
		c = this;

		setContentView(R.layout.activity_trend_watchermain);

		((Button) findViewById(R.id.main_button_start))
				.setOnClickListener(this);

		checkNotNull(SERVER_URL, "SERVER_URL");
		checkNotNull(SENDER_ID, "SENDER_ID");
		GCMRegistrar.checkDevice(this);

		GCMRegistrar.checkManifest(this);
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

			} else {

				final Context context = this;
				mRegisterTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						boolean registered = ServerUtilities.register(context,
								regId);

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

		if (!genId.equals("")) {
			getTrends();
		}

		final ImageView myImageView = (ImageView) findViewById(R.id.imageView1);
		myImageView.setVisibility(View.VISIBLE);
		Animation myFadeInAnimation = AnimationUtils.loadAnimation(this,
				R.anim.fadeout);
		myImageView.startAnimation(myFadeInAnimation); // Set animation to your
														// ImageView
		myFadeInAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation arg0) {
				myImageView.setVisibility(View.GONE);
			}
		});

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

				ListView lv = (ListView) findViewById(R.id.main_trend_list);
				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						this, R.layout.list_item, trendList);

				lv.setAdapter(arrayAdapter);

				OnItemClickListener listener = new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int pos, long id) {
						Toast.makeText(
								c,
								"Going to watch: "
										+ ((TextView) parent.getChildAt(pos))
												.getText().toString(),
								Toast.LENGTH_SHORT).show();

						for (int a = 0; a < parent.getChildCount(); a++) {
							parent.getChildAt(a).setBackgroundColor(
									c.getResources()
											.getColor(R.color.backBlack));
						}

						view.setBackgroundColor(c.getResources().getColor(
								R.color.orange));

						picked = ((TextView) parent.getChildAt(pos)).getText()
								.toString();

					}

				};

				lv.setOnItemClickListener(listener);

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

	public void sendTweetsToPebble(String tweets) {

		if (tweets == null)
			return;

		// So to send to the pebble watch we're taking a JSONArray and tossing
		// it into a string.

		// You guys can do the rest....

		final Intent i = new Intent("com.getpebble.action.SEND_DATA");
		i.putExtra("sender", "TrendWATCHer");
		i.putExtra("tweet_array", tweets);
		Log.d("trending", "Sending the tweets to pebble: " + tweets);
		sendBroadcast(i);
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == R.id.main_button_start) {
			if (picked == "" || picked == null) {
				Toast.makeText(c, "Please pick a trend", Toast.LENGTH_SHORT)
						.show();
			} else {
				Toast.makeText(c, "Lets get WATCHing", Toast.LENGTH_SHORT)
						.show();

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("_id", genId);
				data.put("is_watching", "true");
				data.put("trend", picked);

				AsyncHttpPost asyncHttpPost = new AsyncHttpPost(data) {
					@Override
					protected void onPostExecute(String result) {
						Toast.makeText(c, result, Toast.LENGTH_LONG).show();
					};
				};
				asyncHttpPost
						.execute("http://trend-watcher.herokuapp.com/settrend");

			}
		}
	}

}
