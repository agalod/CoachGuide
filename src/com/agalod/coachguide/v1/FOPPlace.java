package com.agalod.coachguide.v1;

import java.net.URLDecoder;

import android.database.Cursor;

public class FOPPlace
{
	public float lat;
	public float lng;
	public float distance;

	public int id;
	public String description;
	public int visited;
	public String title;

	public FOPPlace()
	{
		description = title = "";
	}

	public static FOPPlace cursorToPlace(Cursor cursor)
	{
		FOPPlace place = new FOPPlace();
		place.title = URLDecoder.decode(cursor.getString(cursor.getColumnIndex("FOP_Place_Title")));
		place.description =
				URLDecoder.decode(cursor.getString(cursor.getColumnIndex("FOP_Place_Description")));
		place.lat = cursor.getFloat(cursor.getColumnIndex("FOP_Place_Lat"));
		place.lng = cursor.getFloat(cursor.getColumnIndex("FOP_Place_Lng"));
		place.distance = cursor.getFloat(cursor.getColumnIndex("FOP_Place_Distance"));
		place.id = cursor.getInt(cursor.getColumnIndex("FOP_Place_ID"));
		place.visited = cursor.getInt(cursor.getColumnIndex("FOP_Place_Visited"));
		return place;
	}

}
