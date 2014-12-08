package com.agalod.coachguide.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

class StableArrayAdapter extends ArrayAdapter<String>
{

	HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

	public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects)
	{
		super(context, textViewResourceId, objects);
		for (int i = 0; i < objects.size(); ++i)
		{
			mIdMap.put(objects.get(i), i);
		}
		// this.getView(position, convertView, parent)
	}

	@Override
	public long getItemId(int position)
	{
		String item = getItem(position);
		return mIdMap.get(item);
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	@Override
	public void notifyDataSetChanged()
	{
		Log.i("ArrayAdapter-notifyDataSetChanged", "hello");
	}

}

public class MainActivity extends Activity implements TextToSpeech.OnInitListener
{

	public static Integer m_Hello;
	public static FOPPlaceEngine m_PlaceEngine;
	public static TextView m_Info;
	public static EditText m_Edit_Min_Distance_GPS;
	public static EditText m_Edit_Min_Distance_Network;
	public static EditText m_Edit_Min_Time;
	public static TextToSpeech m_Talker;
	public static AQuery m_AJAX;
	public static String m_PlaceSource_CurrentQueryString;
	public static SharedPreferences m_Preferences;
	public static ListView m_PlaceList;

// // Declare the UI components
// private ListView songsListView;
//
// // Declare an array to store data to fill the list
// private String[] songsArray;
//
// // Declare an ArrayAdapter that we use to join the data set and the ListView
// // is the way of type safe, means you only can pass Strings to this array
// // Anyway ArrayAdapter supports only TextView
// private ArrayAdapter arrayAdapter;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{

		case R.id.action_settings:
			Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivityForResult(intent, 111);
			break;

		}

		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		m_Info = (TextView) findViewById(R.id.VenueName);
		m_Info.setTextColor(Color.WHITE);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
		layout.setBackgroundColor(Color.BLACK);

// initialize buttons
		Button buttonDescription = (Button) findViewById(R.id.Button_Description);
		buttonDescription.setOnClickListener(onDescription);

		Button buttonNext = (Button) findViewById(R.id.Button_Next);
		buttonNext.setOnClickListener(onNext);

		Button buttonOpenUntil = (Button) findViewById(R.id.Button_OpenUntil);
		buttonOpenUntil.setOnClickListener(onOpenUntil);

		Button buttonStopTalking = (Button) findViewById(R.id.Button_StopTalking);
		buttonStopTalking.setOnClickListener(onStopTalking);

		Button buttonUpdateDB = (Button) findViewById(R.id.Button_UpdateDB);
		buttonUpdateDB.setOnClickListener(onUpdateDB);

// initialize text to speech
		if (Build.VERSION.SDK_INT < 16) // SDK 16 = Android 4.1
		{
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, 222);
		}
		else
			m_Talker = new TextToSpeech(this, this);

		m_PlaceList = (ListView) findViewById(R.id.listview);

		m_PlaceList.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
			{
				String item = (String) parent.getItemAtPosition(position);
				Log.i("ListView - setOnItemClickListener", item);
				FOPPlace place = m_PlaceEngine.m_CurrentClosestPlaces.get(position);
// Intent navigation =
// new Intent(Intent.ACTION_VIEW, Uri
// .parse("http://maps.google.com/maps?saddr=" + place.lat + ","
// + place.lng));
				Uri GMapsUri =
// Uri.parse("geo:q=" + String.valueOf(place.lat) + ","
						Uri.parse("google.navigation:q=" + String.valueOf(place.lat) + ","
								+ String.valueOf(place.lng));// + ";" +
// String.valueOf(place.lat) + ","
				// + String.valueOf(place.lng));
				Intent navigation = new Intent(Intent.ACTION_VIEW, GMapsUri);

				startActivity(navigation);
			}
		});

// setup location engine
		m_AJAX = new AQuery(this);
		m_PlaceEngine = new FOPPlaceEngine(this);
		m_PlaceEngine.m_LocationManager =
				(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		m_PlaceSource_CurrentQueryString =
				"http://friendsofplaces.com/placespeaker/NearbyPlacesAll.php";

		if (m_PlaceEngine.m_DataBaseCreator.isEmpty())
			m_PlaceEngine.updateDatabaseFromInternet();

		m_PlaceEngine.initLocationUpdates();
		m_PlaceEngine.m_DataBaseCreator.resetVisited();

	}

	// only for Text-to-Speech
	@Override
	public void onInit(int status)
	{

		if (status == TextToSpeech.SUCCESS)
		{
			int result = MainActivity.m_Talker.setLanguage(Locale.GERMAN);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED)
			{
				m_Talker.speak("missing data or unsupported", TextToSpeech.QUEUE_FLUSH, null);
			}
			else
			{
				m_Info.setText("Lokalisiere ihr Geraet ...");
				m_Talker.speak("Lokalisiere ihr Geraet.", TextToSpeech.QUEUE_FLUSH, null);
				Toast.makeText(this, "TextToSpeech activated.", Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			Toast.makeText(this, "No text to speech, sorry.", Toast.LENGTH_LONG).show();
		}
	}

// // Define a listener that responds to location updates
// public static LocationListener onLocationChanged = new LocationListener()
// {
// @Override
// public void onLocationChanged(Location location)
// {
// if (location.getLatitude() < 1 ||
// location.equals(m_PlaceEngine.m_Location_Current))
// return;
//
// FOPPlace closestplace = m_PlaceEngine.getClosestPlace(location);
// if (closestplace == null)
// closestplace = new FOPPlace();
//
// MainActivity.m_Info.setText(closestplace.title + "\n" + closestplace.distance
// + " km");
// String textToSpeak =
// "In " + closestplace.distance + " Kilomentern: " + closestplace.title + ". "
// + closestplace.description;
// MainActivity.m_Talker.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
// }
//
// @Override
// public void onStatusChanged(String provider, int status, Bundle extras)
// {
// }
//
// @Override
// public void onProviderEnabled(String provider)
// {
// }
//
// @Override
// public void onProviderDisabled(String provider)
// {
// }
// };

	private final OnClickListener onStopTalking = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			m_Talker.speak("", TextToSpeech.QUEUE_FLUSH, null);
		}

	};

	private final OnClickListener onOpenUntil = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			m_PlaceEngine.placesOpenUntil();
		}

	};

	private final OnClickListener onNext = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			m_PlaceEngine.placesNext();
		}

	};

	private final OnClickListener onDescription = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(intent);
		}

	};

	private final OnClickListener onUpdateDB = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			m_PlaceEngine.updateDatabaseFromInternet();
		}

	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 111)
		{
			m_PlaceEngine.updateDatabaseFromInternet();
			m_PlaceEngine.initLocationUpdates();
			if (m_PlaceEngine.m_Location_Current.getLatitude() != 0.0)
				m_PlaceEngine.getClosestPlace(m_PlaceEngine.m_Location_Current);
		}
		else if (requestCode == 222)
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				m_Talker = new TextToSpeech(this, this);
				m_Talker.setOnUtteranceProgressListener(m_PlaceEngine.m_TTSProgressListener);

			}
			else
			{
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}

	}

	public void setToast(String toasttext)
	{
		Toast.makeText(this, toasttext, Toast.LENGTH_LONG).show();

	}

	// @Override
	// protected void onResume() {
	// m_PlaceProvider.m_DataBase =
	// m_PlaceProvider.m_DataBaseCreator.getWritableDatabase();
	// super.onResume();
	// }
	//
	// @Override
	// protected void onPause() {
	// m_PlaceProvider.m_DataBase.close();
	// super.onPause();
	// }

}
