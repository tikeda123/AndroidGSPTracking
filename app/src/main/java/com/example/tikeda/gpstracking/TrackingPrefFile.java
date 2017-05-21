package com.example.tikeda.gpstracking;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by tikeda on 2017/01/14.
 */

public class TrackingPrefFile
{
    private static final String TAG = "TrackingPrefFile";

    private static final String TRACKINGPRE_FILE_FORMAT     = "%1$s-TrackingPreFile.json";

    private static File         m_Locfile                   = null;
    private static FileWriter   m_FileWriter                = null;
    private static FileReader   m_FileReader                = null;
    private static String       m_serialNumber              = null;

    private static final String HTTP_FLAG_NAME              = "http_flag";
    private static boolean      m_HTTP_FLAG                 = false;

    private static final String GPS_MIN_TIME_NAME           = "gps_min_time";
    private static long         m_GPS_MIN_TIME              = 2000;     // Minmum Interval Time 1000m Sec

    private static final String GPS_MIN_DISTANCE_NAME       = "gps_min_distance";
    private static       float  m_GPS_MIN_DISTANCE          = 3;        // Minmum Interval Distance  3m

    private static final String URL_CLOUDSERVER_NAME        = "url_cloudserver";
    private static String       m_URL_CLOUDSERVER           = "http://202.235.116.150:9200/triathlon_index/triathlon_map";

    private static final String STEP_COUNTER_DELAY_NAME     = "step_counter_delay";
    private static int   m_STEP_COUNTER_DELAY               = SensorManager.SENSOR_DELAY_NORMAL;

    private static final String STEP_DIRECTOR_DELAY_NAME    = "step_director_delay";
    private static int   m_STEP_DIRECTOR_DELAY              = SensorManager.SENSOR_DELAY_NORMAL;




    private static TrackingPrefFile m_TrackingPrefFile      = new TrackingPrefFile();

    private TrackingPrefFile(){};

    public static TrackingPrefFile GetTrackingPrefFile()
    {
        return m_TrackingPrefFile;
    }

    /******************************************************************
     ReadTrackingPreFile : Read Tracking Pre File..
     :Parameters:
     :String sn: Serial Number of this Device..
     :Context context : Context data from this Activity...
     :Returns Type: void
     ******************************************************************/
    public void ReadTrackingPreFile
            (
                    String sn,
                    Context context
            )
    {
        m_serialNumber  = sn;
        // 出力先ディレクトリを取得
        File outputDir = getOutputDir( context );

        if ( outputDir == null )
        {
            // 何らかの原因でディレクトリが見つからなかった
            Log.d(TAG, "not found  Directory");
            return;// 何らかの原因でディレクトリが見つからなかった
        }
        getFileName( outputDir );
    }

    /******************************************************************
     getOutputDir : Get OutputDir
     :Parameters:
     :String sn: Serial Number of this Device..
     :Context context : Context data from this Activity...
     :Returns Type: File  File Open Data
     ******************************************************************/
    private static File getOutputDir
            (
                    Context context
            )
    {

        File outputDir;

        if ( Build.VERSION.SDK_INT >= 19 )
        {
            outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }
        else
        {
            outputDir = new File(context.getExternalFilesDir(null), "Documents");
        }

        Log.d(TAG, outputDir.getAbsolutePath());

        if (outputDir == null)
        {
            // 外部ストレージがマウントされていない等の場合
            return null;
        }

        boolean isExist = true;

        if ( !outputDir.exists()
                || !outputDir.isDirectory())
        {
            isExist = outputDir.mkdirs();
        }

        if ( isExist )
        {
            return outputDir;
        }
        else
        {
            // ディレクトリの作成に失敗した場合
            return null;
        }
    }

    /******************************************************************
     getOutputDir : Obtain Output File
     :Parameters:
     :File outputDir : File Pointer for created Directory
     :Returns Type: void
     ******************************************************************/
    private static void getFileName
        (
            File outputDir
        )
    {
        String fileName = String.format( TRACKINGPRE_FILE_FORMAT, m_serialNumber );
        m_Locfile       = new File( outputDir, fileName );

        if ( m_Locfile == null )
        {
            Log.d(TAG, " File new create file.");
            return;
        }

        if ( true == m_Locfile.isFile() )
        {
            String json = readToFile();
            jsonToRepository( json );
        }
        else
        {
            String json = RepositoryTojson();
            writeToFile( json );
        }
    }
    /******************************************************************
     readToFile : Read JSON Data to File in Android wear
     :Parameters:
     :Returns Type: boolea : true / false
     ******************************************************************/
    public static void writeToFile
        (
                String json
        )
    {

        try
        {
            m_FileWriter = new FileWriter( m_Locfile, Boolean.TRUE);
            m_FileWriter.write( json );
            m_FileWriter.flush();
        }
        catch ( IOException e )
        {
            e.printStackTrace();

        }finally {
            if ( m_FileWriter != null)
            {
                try
                {
                    m_FileWriter.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }


    /******************************************************************
     readToFile : Read JSON Data to File in Android wear
     :Parameters:
     :Returns Type: boolea : true / false
     ******************************************************************/
    public static String readToFile()
    {
        String jsondata = null;
        String str      = null;

        try
        {
            m_FileReader        = new FileReader( m_Locfile );
            BufferedReader br   = new BufferedReader (m_FileReader) ;

            jsondata = br.readLine();

            while ( (str = br.readLine() ) != null )
            {
                jsondata  = jsondata + str;
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }finally {
            if ( m_FileReader != null)
            {
                try
                {
                    m_FileReader.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return jsondata;
    }

    /******************************************************************
     readToFile : Read JSON Data to File in Android wear
     :Parameters:
     :Returns Type: boolea : true / false
     ******************************************************************/
    public static void jsonToRepository
        (
                String jdata
        )
    {
        try
        {
            JSONObject json = new JSONObject( jdata );

            m_HTTP_FLAG         = json.getBoolean( HTTP_FLAG_NAME );
            m_GPS_MIN_TIME      = json.getLong( GPS_MIN_TIME_NAME );
            m_GPS_MIN_DISTANCE  = ( long )json.getLong( GPS_MIN_DISTANCE_NAME );
            m_URL_CLOUDSERVER   = json.getString( URL_CLOUDSERVER_NAME );
            m_STEP_COUNTER_DELAY= json.getInt( STEP_COUNTER_DELAY_NAME );
            m_STEP_DIRECTOR_DELAY=json.getInt( STEP_DIRECTOR_DELAY_NAME );
            Log.i(TAG,"Read file to:" + json.toString());
       }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }
    }
    /******************************************************************
     readToFile : Read JSON Data to File in Android wear
     :Parameters:
     :Returns Type: boolea : true / false
     ******************************************************************/
    public static String RepositoryTojson()
    {
        try
        {
            JSONObject json = new JSONObject();

            json.accumulate( HTTP_FLAG_NAME, m_HTTP_FLAG );
            json.accumulate( GPS_MIN_TIME_NAME, m_GPS_MIN_TIME );
            json.accumulate( GPS_MIN_DISTANCE_NAME, m_GPS_MIN_DISTANCE);
            json.accumulate( URL_CLOUDSERVER_NAME,  m_URL_CLOUDSERVER );
            json.accumulate( STEP_COUNTER_DELAY_NAME, m_STEP_COUNTER_DELAY );
            json.accumulate( STEP_DIRECTOR_DELAY_NAME, m_STEP_DIRECTOR_DELAY );
            Log.i(TAG,json.toString());
            return json.toString();
       }
        catch ( JSONException e )
        {
            e.printStackTrace();
            return null;
        }
    }

    public  boolean getHTTP_FLAG()
    {
        return m_HTTP_FLAG;
    }
    public  long getGPS_MIN_TIME()
    {
        return m_GPS_MIN_TIME;
    }
    public  float getGPS_MIN_DISTANCE()
    {
        return m_GPS_MIN_DISTANCE;
    }
    public  String getURL_CLOUDSERVER()
    {
        return m_URL_CLOUDSERVER;
    }
    public  int getSTEP_COUNTER_DELAY()
    {
        return m_STEP_COUNTER_DELAY;
    }
    public  int getSTEP_DIRECTOR_DELAY()
    {
        return m_STEP_DIRECTOR_DELAY;
    }
    public  void getHTTP_FLAG(boolean flag)
    {
        m_HTTP_FLAG = flag;
    }
    public  void setGPS_MIN_TIME(long min_time)
    {
        m_GPS_MIN_TIME = min_time;
    }
    public  void setGPS_MIN_DISTANCE(float min_distance)
    {
        m_GPS_MIN_DISTANCE = min_distance;
    }
    public  void setURL_CLOUDSERVER (String url)
    {
        m_URL_CLOUDSERVER = url;
    }
    public  void getSTEP_COUNTER_DELAY(int delay)
    {
        m_STEP_COUNTER_DELAY = delay;
    }
    public  void getSTEP_DIRECTOR_DELAY(int delay)
    {
        m_STEP_DIRECTOR_DELAY = delay;
    }
}