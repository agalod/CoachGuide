package com.agalod.coachguide.v1;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class FOPPlaceEngine
{
	public Location m_Location_Current;
	public static LocationManager m_LocationManager;
	public static LocationManager m_LocationManager_GPS;
	public static LocationListener m_LocationListener;

	public JSONObject m_Places_Current;
	public Integer m_Places_Current_Index;
	final String[] m_DaysOfWeek =
		{ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	public FOPPlaceDataBase m_DataBaseCreator;
	private final Context m_ApplicationContext;
	private final SharedPreferences m_SharedPreferences;
	public ArrayList<FOPPlace> m_CurrentClosestPlaces = new ArrayList<FOPPlace>();
	private int m_CurrentClosestPlaces_Index;
	public UtteranceProgressListener m_TTSProgressListener = new UtteranceProgressListener()
	{
		@Override
		public void onDone(String utteranceId)
		{
			if (m_CurrentClosestPlaces_Index >= m_CurrentClosestPlaces.size())
				return;

			FOPPlace closestplace = null;
			while (m_CurrentClosestPlaces_Index < m_CurrentClosestPlaces.size())
			{
				closestplace = m_CurrentClosestPlaces.get(m_CurrentClosestPlaces_Index);
				if (closestplace.visited == 0)
					break;
				closestplace = m_CurrentClosestPlaces.get(m_CurrentClosestPlaces_Index++);
			}
			if (m_CurrentClosestPlaces_Index == m_CurrentClosestPlaces.size())
				return;

// MainActivity.m_Info.setText(closestplace.title + "\n");
// closestplace.distance + " km");
			String textToSpeak =
					"In " + Float.toString(closestplace.distance).substring(0, 3)
							+ " Kilomentern: " + closestplace.title;
// + ". " + closestplace.description;
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, closestplace.title);
			MainActivity.m_Talker.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, map);

			ContentValues args = new ContentValues();
			args.put("FOP_Place_Visited", 1);
			m_DataBaseCreator.m_DataBase_Write.update("places", args, "FOP_Place_ID = "
					+ closestplace.id, null);

			m_CurrentClosestPlaces_Index++;

		}

		@Override
		public void onError(String utteranceId)
		{
		}

		@Override
		public void onStart(String utteranceId)
		{
			int h = 0;
		}
	};

	public FOPPlaceEngine(Context context)
	{
		// TODO Auto-generated constructor stub
		m_CurrentClosestPlaces_Index = 0;
		m_Location_Current = new Location("0");
		m_Places_Current_Index = 0;
		m_DataBaseCreator = new FOPPlaceDataBase(context);

		m_ApplicationContext = context;
		m_SharedPreferences = PreferenceManager.getDefaultSharedPreferences(m_ApplicationContext);
	}

	public void initLocationUpdates()
	{

		if (m_LocationListener != null)
			m_LocationManager.removeUpdates(m_LocationListener);
		m_LocationListener = new LocationListener()
		{
			@Override
			public void onLocationChanged(Location location)
			{
				if (location.getLatitude() < 1 || location.equals(m_Location_Current))
					return;

				// --------------------------------------------------------
				// main retrieval of current places
				// --------------------------------------------------------
				m_CurrentClosestPlaces = getClosestPlace(location);
				if (m_CurrentClosestPlaces == null || m_CurrentClosestPlaces.size() < 1)
				{
					String textToSpeak = "Keine Orte gefunden.";
					MainActivity.m_Info.setText(textToSpeak);

					return;
				}
				// --------------------------------------------------------

				int i = 0;
				int numplacesnew = 0;
				while (i < m_CurrentClosestPlaces.size())
				{
					if (m_CurrentClosestPlaces.get(i).visited == 0)
						numplacesnew++;
					i++;
				}

// if (numplacesnew == 0)
// return;

				HashMap<String, String> map = new HashMap<String, String>();
				map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Neue Orte gefunden.");
				String textToSpeak =
						numplacesnew + " neue Orte im Umkreis von "
								+ m_SharedPreferences.getString("Radius", "2") + "km:";
				MainActivity.m_Info.setText(textToSpeak);
				if (numplacesnew > 0)
					MainActivity.m_Talker.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, map);
				m_CurrentClosestPlaces_Index = 0;
				// MainActivity.m_Talker.setOnUtteranceProgressListener(m_TTSProgressListener);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras)
			{
				Toast.makeText(MainActivity.m_AJAX.getContext(),
						"LocationListener-StatusChanged: " + provider + " " + status,
						Toast.LENGTH_LONG).show();
			}

			@Override
			public void onProviderEnabled(String provider)
			{
				Toast.makeText(MainActivity.m_AJAX.getContext(),
						"LocationListener-onProviderEnabled: " + provider, Toast.LENGTH_LONG)
						.show();
			}

			@Override
			public void onProviderDisabled(String provider)
			{
				Toast.makeText(MainActivity.m_AJAX.getContext(),
						"LocationListener-onProviderDisabled: ", Toast.LENGTH_LONG).show();
			}
		};

		int mintime = Integer.parseInt(m_SharedPreferences.getString("MinTime", "240000"));
		int mindistancegps =
				Integer.parseInt(m_SharedPreferences.getString("MinDistance_GPS", "1000"));
		int mindistancenetwork =
				Integer.parseInt(m_SharedPreferences.getString("MinDistance_Network", "1000"));

		if (m_LocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			m_LocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					mintime /* 1
							 * minute */, mindistancegps, m_LocationListener);
		else
			m_LocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					mintime /* 1
							 * minute */, mindistancenetwork, m_LocationListener);

		MainActivity.m_Talker.setOnUtteranceProgressListener(m_TTSProgressListener);

	}

	public void placesNext()
	{
		// TODO Auto-generated method stub
		try
		{
			if (m_Places_Current == null)
				return;
			m_Places_Current_Index++;
			String nearestVenueName =
					URLDecoder.decode(m_Places_Current.getJSONArray("places")
							.getJSONObject(m_Places_Current_Index).getString("name"));
			String distance =
					m_Places_Current.getJSONArray("places").getJSONObject(m_Places_Current_Index)
							.getString("distance");
			String openuntil =
					m_Places_Current.getJSONArray("places").getJSONObject(m_Places_Current_Index)
							.getString("openuntil");
			String textToSpeak =
					"\nIn "
							+ distance
							+ " Kilomentern: "
							+ nearestVenueName
							+ ". "
							+ URLDecoder
									.decode(m_Places_Current.getJSONArray("places")
											.getJSONObject(m_Places_Current_Index)
											.getString("description"));

			MainActivity.m_Info.setText(nearestVenueName + "\n" + distance + " km");
			MainActivity.m_Talker.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	public void placesOpenUntil()
	{
		try
		{
			if (m_Places_Current == null)
				return;

			String openuntil =
					m_Places_Current.getJSONArray("places").getJSONObject(m_Places_Current_Index)
							.getString("openuntil");

			String textToSpeak = "Geoeffnet bis " + openuntil.substring(0, 2) + " Uhr";

			MainActivity.m_Talker.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void placesDescription()
	{
		try
		{
			if (m_Places_Current == null)
				return;
			String venueDescription =
					m_Places_Current.getJSONArray("places").getJSONObject(m_Places_Current_Index)
							.getString("description");
			venueDescription = URLDecoder.decode(URLDecoder.decode(venueDescription));

			if (venueDescription.length() == 0)
				venueDescription = "Es gibt hierfuer keine Beschreibung.";

			MainActivity.m_Talker.speak(venueDescription, TextToSpeech.QUEUE_FLUSH, null);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateDatabaseFromInternet()
	{
		MainActivity.m_Talker.speak("Updating place data base", TextToSpeech.QUEUE_FLUSH, null);

		Toast.makeText(MainActivity.m_AJAX.getContext(), "Updating place data base",
				Toast.LENGTH_LONG).show();

		Map<String, Object> params = new HashMap<String, Object>();
		Time today = new Time(Time.getCurrentTimezone());
		today.setToNow();

// String url = MainActivity.m_PlaceSource_CurrentQueryString; //
// "http://alsotoday.com/android/NearbyPlaces.php";
		String url =
				m_SharedPreferences.getString("sources_list",
						"http://friendsofplaces.com/placespeaker/NearbyPlacesAll.php");
		url += "?hour=" + today.hour;
		url += "&dayofweek=" + m_DaysOfWeek[today.weekDay];
		params.put("hour", today.hour);
		params.put("dayofweek", m_DaysOfWeek[today.weekDay]);

		MainActivity.m_AJAX.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>()
		{
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status)
			{
				if (json != null)
				{
					try
					{
						m_DataBaseCreator.emptyDB();

						JSONArray places = json.getJSONArray("places");
						for (int i = 0; i < places.length(); i++)
						{
							JSONObject c = places.getJSONObject(i);
							String name = c.getString("name");
							String descr = c.getString("description");
							float lat = Float.valueOf(c.getString("lat"));
							float lng = Float.valueOf(c.getString("lng"));
							int id = Integer.valueOf(c.getString("id"));

							ContentValues row = new ContentValues();
							row.put("FOP_Place_Title", name);
							row.put("FOP_Place_Description", descr);
							row.put("FOP_Place_Lat", lat);
							row.put("FOP_Place_Lng", lng);
							row.put("FOP_Place_ID", id);
							long insertId =
									m_DataBaseCreator.m_DataBase_Write.insert(
											FOPPlaceDataBase.FOP_TABLE, null, row);
						}

						Cursor cursor =
								m_DataBaseCreator.m_DataBase_Read.rawQuery("SELECT * from "
										+ m_DataBaseCreator.FOP_TABLE + ";", null);
						int dbnumrows = cursor.getCount();
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

				}
				else
				{

					Toast.makeText(MainActivity.m_AJAX.getContext(), "Error:" + status.getCode(),
							Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	/* retrieves the current closest place to display and speak */
	public ArrayList<FOPPlace> getClosestPlace(Location location)
	{
// init function
		if (m_DataBaseCreator.m_DataBase_Read == null)
			return null;

		Log.i("FOPDataBase->retrieveClosestNonVisitedPlace()",
				"Location update to " + location.getLatitude() + ", " + location.getLongitude());

// calculate distance to each place and update each row with the distance
// parameter
		Cursor cursor =
				m_DataBaseCreator.m_DataBase_Read.rawQuery("SELECT * from "
						+ m_DataBaseCreator.FOP_TABLE + ";", null);
		if (cursor.getCount() < 1)
		{
			Toast.makeText(m_ApplicationContext, "Erro while in retrieveClosestNonVisitedPlace",
					Toast.LENGTH_LONG).show();
			return null;
		}
		cursor.moveToFirst();
		float lat = 0, lng = 0;
		int id = 0;
		double distance = 0;
		while (!cursor.isAfterLast())
		{
			FOPPlace place = FOPPlace.cursorToPlace(cursor);
			lat = cursor.getFloat(cursor.getColumnIndex("FOP_Place_Lat"));
			lng = cursor.getFloat(cursor.getColumnIndex("FOP_Place_Lng"));
			id = cursor.getInt(cursor.getColumnIndex("FOP_Place_ID"));
			distance =
					((Math.acos(Math.sin((lat * Math.PI / 180))
							* Math.sin((location.getLatitude() * Math.PI / 180))
							+ Math.cos((lat * Math.PI / 180))
							* Math.cos((location.getLatitude() * Math.PI / 180))
							* Math.cos(((lng - location.getLongitude()) * Math.PI / 180)))) * 180 / Math.PI) * 60 * 1.1515 * 1.609344;

			ContentValues args = new ContentValues();
			args.put("FOP_Place_Distance", distance);
			m_DataBaseCreator.m_DataBase_Write.update("places", args, "FOP_Place_ID = " + id, null);
			cursor.moveToNext();
		}

// retrieve the closest place from the updated data base
		cursor =
				m_DataBaseCreator.m_DataBase_Write.rawQuery("SELECT * from "
						+ m_DataBaseCreator.FOP_TABLE + " WHERE FOP_Place_Distance < "
						+ m_SharedPreferences.getString("Radius", "2")
						+ " ORDER BY FOP_Place_Distance ASC;", null);
		if (cursor.getCount() < 1)
		{
			Toast.makeText(m_ApplicationContext, "No places found", Toast.LENGTH_LONG).show();
			MainActivity.m_PlaceList.setAdapter(null);
			return null;
		}

		ArrayList<FOPPlace> placeList = new ArrayList<FOPPlace>();

		final ArrayList<String> list = new ArrayList<String>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			FOPPlace place = FOPPlace.cursorToPlace(cursor);
			placeList.add(place);
			cursor.moveToNext();
			list.add(Float.toString(place.distance).substring(0, 3) + " km: " + place.title);
			Log.i("FOPPlaceEngine - getClosestPlace()", place.distance + " km: " + place.title);
		}

// return closest non visited place

		final StableArrayAdapter adapter =
				new StableArrayAdapter(MainActivity.m_AJAX.getContext(),
						android.R.layout.simple_list_item_1, list);
		MainActivity.m_PlaceList.setAdapter(adapter);

		return placeList;

	}
}
