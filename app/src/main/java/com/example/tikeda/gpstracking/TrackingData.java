package com.example.tikeda.gpstracking;

import android.location.Location;
import android.util.Log;

import com.example.tikeda.gpstracking.location.GPSLocationMsg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
/**
 * Created by tikeda on 2017/01/04.
 */
/******************************************************************
 TrackingData Class : Convert Location to JSON Format to ElasticSearch
 ******************************************************************/
public class TrackingData
{
    private final String TAG = "TrackingData";

    private static final String JSON_DATEFORMAT  = "yyyy-MM-dd kk:mm:ssZ";
    private static final String JSON_LFCODE      = "\n";

    private String  m_SerialNum;
    private long    m_Time;
    private double  m_Latitude;
    private double  m_Longitude;
    private float   m_Speed;
    private float   m_Accuracy;
    private float   m_Bearing;
    private double  m_Altitude;
    private long    m_Steps;
    private int     m_StepsAcy;

    /******************************************************************
     TrackingData :  Convert Location to JSON Format to ElasticSearch
     :Parameters:
     :steps     : number of steps
     :timestamp : time stamp at recording steps
     :Returns Type: void
     ******************************************************************/
    public TrackingData
            (
                    String  serial,
                    long    steps,
                    int     stepsacy,
                    Location loc
            )
    {
        m_SerialNum = serial;
        m_Steps     = steps;
        m_StepsAcy  = stepsacy;
        m_Time      = loc.getTime();
        m_Latitude  = loc.getLatitude();
        m_Longitude = loc.getLongitude();
        m_Speed     = loc.getSpeed();
        m_Accuracy  = loc.getAccuracy();
        m_Bearing   = loc.getBearing();
        m_Altitude  = loc.getAltitude();
    }
    /******************************************************************
     TrackingData :  Convert Location to JSON Format to ElasticSearch
     :Parameters:
     :steps     : number of steps
     :timestamp : time stamp at recording steps
     :Returns Type: void
     ******************************************************************/
    public TrackingData
        (
            String  serial,
            long    steps,
            int     stepsacy,
            GPSLocationMsg loc
        )
    {
        m_SerialNum = serial;
        m_Steps     = steps;
        m_StepsAcy  = stepsacy;
        m_Time      = loc.getM_Time();
        m_Latitude  = loc.getM_Latitude();
        m_Longitude = loc.getM_Longitude();
        m_Speed     = loc.getM_Speed();
        m_Accuracy  = loc.getM_Accuracy();
        m_Bearing   = loc.getM_Bearing();
        m_Altitude  = loc.getM_Altitude();
    }

    String GetJSON()
    {
        SimpleDateFormat df = new SimpleDateFormat(JSON_DATEFORMAT);
        JSONObject json = new JSONObject();

        try
        {
            json.accumulate("SerialNum", m_SerialNum);
            json.accumulate( "Rundate", df.format(m_Time));

            JSONArray ja = new JSONArray();
            ja.put( 0,  m_Longitude );
            ja.put( 1,  m_Latitude );

            json.accumulate("Location", ja);
            json.accumulate("Speed",    m_Speed );
            json.accumulate("Accuracy", m_Accuracy);
            json.accumulate("Bearing",  m_Bearing);
            json.accumulate("Altitude", m_Altitude);
            json.accumulate("Steps",  m_Steps);
            json.accumulate("StepsAcy", m_StepsAcy);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        Log.i(TAG,json.toString());
        return json.toString() + JSON_LFCODE;
    }

    public float getSpeedAsKmh()
    {
        return ( (m_Speed*60*60) / 1000 );
    }

    public float getSpeed()
    {
        return (m_Speed);
    }

    public float    GetAccuracy()
    {
        return m_Accuracy;
    }
    public long     GetSteps()       {return m_Steps;}

}
