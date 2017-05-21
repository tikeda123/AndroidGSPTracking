package com.example.tikeda.gpstracking.file;

/**
 * Created by tikeda on 2017/01/01.
 */

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Created by tikeda on 2017/01/01.
 */
/******************************************************************
 LocationRepository Class : write Tracking Data to file in android wear.
 ******************************************************************/
public class LocationRepository
{
    private static final String TAG = "LocationRepository";

    private static File         m_Locfile   = null;
    private static FileWriter   m_FileWrite = null;
    private static String       m_serialNumber = null;

    // ファイル名フォーマット prefix-yyyy-mm-dd-HH-MM-SS.csv
    private static final String LOCATION_FILE_FORMAT = "%1$s-%2$tF-%2$tH-%2$tM-%2$tS.json";

    private LocationRepository() {}

    /******************************************************************
     createFile : Create CSV File for Logging..
     :Parameters:
     :String sn: Serial Number of this Device..
     :Context context : Context data from this Activity...
     :Returns Type: void
     ******************************************************************/
    public static void createFile
        (
                String sn,
                Context context
        )
    {
        m_serialNumber = sn;
        // 出力先ディレクトリを取得
        File outputDir = getOutputDir(context);

        if ( outputDir == null )
        {
            // 何らかの原因でディレクトリが見つからなかった
            return;
        }

        getFileName(outputDir);
    }
    /******************************************************************
     getOutputDir : Create Directory for CSV File
     :Parameters:
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

        Calendar now = Calendar.getInstance();

        String fileName = String.format( LOCATION_FILE_FORMAT, m_serialNumber, now );

        m_Locfile = new File(outputDir, fileName);
    }
    /******************************************************************
     writeToFile : Write JSON Data to File in Android wear
     :Parameters:
     :String jsondata : String JSONDATA
     :Returns Type: boolea : true / false
     ******************************************************************/
    public static boolean writeToFile
                    (
                            String jsondata
                    )
    {
        try
        {
            m_FileWrite = new FileWriter( m_Locfile, Boolean.TRUE);
            m_FileWrite.write( jsondata );
            m_FileWrite.flush();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return false;

        }finally {
            if ( m_FileWrite != null)
            {
                try
                {
                    m_FileWrite.close();
                    return Boolean.TRUE;
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return Boolean.FALSE;
    }
    /******************************************************************
     FileClose : File close and delete o byte file
     :Parameters:
     :Returns void
     ******************************************************************/
    public static void FileClose()
    {
        if( m_Locfile != null )
        {

            Log.d(TAG, String.valueOf(m_Locfile.length()));

            if (m_Locfile.length() == 0) // 0 Byte data file should be delete.
            {
                m_Locfile.deleteOnExit();
            }
        }

    }

}

