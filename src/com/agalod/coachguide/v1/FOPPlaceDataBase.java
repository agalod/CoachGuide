package com.agalod.coachguide.v1;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class FOPPlaceDataBase extends SQLiteOpenHelper
{

	public SQLiteDatabase m_DataBase_Write;
	public SQLiteDatabase m_DataBase_Read;
	
	public static final String FOP_TABLE = "places";	
	private static final String DATABASE_NAME = "fop.places.db";
	private static final int DATABASE_VERSION = 3;
	private Context m_ApplicationContext;
	
	 // Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
	  + FOP_TABLE + "( " 
	  + "FOP_Place_ID integer primary key autoincrement, "
	  + "FOP_Place_Title TEXT not null, "
	  + "FOP_Place_Description TEXT, "
	  + "FOP_Place_Lat REAL not null, "
	  + "FOP_Place_Lng REAL not null,"
	  + "FOP_Place_Visited Boolean INTEGER DEFAULT 0,"
	  + "FOP_Place_Distance REAL DEFAULT 0"
	  + " );";
	
	  public FOPPlaceDataBase(Context context) 
	  {
		  super(context, DATABASE_NAME, null, DATABASE_VERSION);
		  m_ApplicationContext = context;
		  m_DataBase_Write = getWritableDatabase();
		  m_DataBase_Read 	= getReadableDatabase();
	  }
	
	  @Override
	  public void onCreate(SQLiteDatabase database) 
	  {
	    database.execSQL(DATABASE_CREATE);
	  }
	
	  @Override
	  public void onUpgrade(SQLiteDatabase freshdb, int oldVersion, int newVersion) 
	  {
		  String UpdateMessage = "Upgrading database from version " + oldVersion + " to "
	        + newVersion + ", which will destroy all old data";
	    Log.w(FOPPlaceDataBase.class.getName(), UpdateMessage );
		Toast.makeText( m_ApplicationContext, UpdateMessage, Toast.LENGTH_LONG).show();
		freshdb.execSQL("DROP TABLE IF EXISTS " + FOP_TABLE);
	    onCreate(freshdb);		  
	  }
	  
	  public void emptyDB()
	  {
		  if(m_DataBase_Write == null)
			  return;
		  m_DataBase_Write.execSQL("DROP TABLE IF EXISTS " + FOP_TABLE);
		  onCreate(m_DataBase_Write);		  
	  }
	  
	  public boolean isEmpty()
	  {
//		  List<Comment> comments = new ArrayList<Comment>();
//		  private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
//			      MySQLiteHelper.COLUMN_COMMENT };
		  
		  Cursor cursor = m_DataBase_Write.query(FOP_TABLE,
		            null, null, null, null, null, null);
		  int numrows = cursor.getCount();
		  return cursor.getCount() == 0;
		  
		  
//		  long dbsize = m_DataBase_Write.getPageSize();
//		  return m_DataBase_Write.getPageSize() == 0.0;
	  }
	  
	  public void resetVisited()
	  {
          ContentValues args = new ContentValues();
          args.put("FOP_Place_Visited", 0);
          m_DataBase_Write.update("places", args, null, null);		  
//          m_DataBase_Write.rawQuery("UPDATE places SET FOP_Place_Visited = 0;", null);
	  }	

}
