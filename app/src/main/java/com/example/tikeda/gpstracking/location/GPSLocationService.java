package com.example.tikeda.gpstracking.location;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.location.LocationListener;
import android.location.LocationProvider;


import com.example.tikeda.gpstracking.MainActivity;
import com.example.tikeda.gpstracking.TrackingPrefFile;


/**
 * Created by tikeda on 2017/01/08.
 */
/******************************************************************
 GPSLocationService Class : GPS location service class
 ******************************************************************/
public class GPSLocationService extends Service implements LocationListener
{
    private static final String TAG = "GPSLocationService";
    private static final int REQUEST_PERMISSION = 1000;
    public static final int GPS_MESSAGE_ID = 1;

    private LocationManager m_LocationManager = null;
    private Messenger m_Messenger= null;;
    public static Messenger m_ReplyMessenger= null;;

    /******************************************************************
     * onStartCommand
     ******************************************************************/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    /******************************************************************
     * onCreate : callback onCreate..
     * :Parameters:
     * :Returns Type: void
     ******************************************************************/
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");
        m_Messenger = new Messenger(new ServiceHandler());

        m_LocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        final boolean gpsEnabled
                = m_LocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled )
        {// GPSを設定するように促す
            enableLocationSettings();
        }

        if (m_LocationManager != null) {
            // バックグラウンドから戻ってしまうと例外が発生する場合がある
            try {
                // minTime = 10msec, minDistance = 1m
                if (ActivityCompat.checkSelfPermission
                        (
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission
                                (
                                        this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                        ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    // Prepare File Write for location info
                    return;
                }
                m_LocationManager.requestLocationUpdates
                        (
                                LocationManager.GPS_PROVIDER,
                                TrackingPrefFile.GetTrackingPrefFile().getGPS_MIN_TIME(),
                                TrackingPrefFile.GetTrackingPrefFile().getGPS_MIN_DISTANCE(),
                                this
                        );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
     /******************************************************************
     * onLocationChanged : get new location info from GPS system
     * :Parameters:
     * :Location location: location Info..
     * :Returns:
     * :Returns Type: void
     ******************************************************************/
    @Override
    public void onLocationChanged
    (
            Location location
    )
    {
        SendMessage(location);
    }

    /******************************************************************
     * onStatusChanged : callback change status of gps device
     * :Parameters:
     * :Location location: location Info..
     * :Returns:
     * :Returns Type: void
     ******************************************************************/
    @Override
    public void onStatusChanged
    (
            String provider,
            int status,
            Bundle extras
    ) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                break;

            case LocationProvider.OUT_OF_SERVICE:
                break;

            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                break;
        }
    }

    /******************************************************************
     * enableLocationSettings : callback change status of gps device
     * :Returns Type: void
     ******************************************************************/
    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }
    /******************************************************************
     TestHandler : Handle Message
     ******************************************************************/
    static class ServiceHandler extends Handler
    {
        public ServiceHandler() {};

        /******************************************************************
         * handleMessage : Handlng Message from Service..
         * :Parameters:
         * :Message msg: Message with Data from Service
         * :Returns Type: void
         ******************************************************************/
        @Override
        public void handleMessage
            (
                Message msg
            )
        {
            switch ( msg.what )
            {
                case GPSLocationService.GPS_MESSAGE_ID:
                    Bundle arg = msg.getData();
                    GPSLocationMsg gps_message = (GPSLocationMsg) arg.getSerializable( "GPSLocationMsg" );
                    Log.i(TAG,"Received Message.from GPSLocationService");
                    break;
                case MainActivity.MA_MESSAGE_ID:
                    m_ReplyMessenger = msg.replyTo;
                    Log.i(TAG,"Received Message.from MainActivity");
                    break;
                default:
                    Log.i(TAG,"Received Message.from OtherService");
            }
            super.handleMessage(msg);
        }
    }


    /******************************************************************
     * SendMessage : Send Message to MainActivity
     * :Parameters:
     * :Location location: location Info..
     * :Returns Type: void
     ******************************************************************/
    protected void SendMessage
        (
            Location loc
        )
    {
        if( m_ReplyMessenger!=null )
        {
            try
            {
                GPSLocationMsg gps_message = new GPSLocationMsg();
                gps_message.setM_Time(loc.getTime());
                gps_message.setM_Latitude(loc.getLatitude());
                gps_message.setM_Longitude(loc.getLongitude());
                gps_message.setM_Accuracy(loc.getAccuracy());
                gps_message.setM_Bearing(loc.getBearing());
                gps_message.setM_Speed(loc.getSpeed());
                gps_message.setM_Altitude(loc.getAltitude());
                Bundle arg = new Bundle();
                arg.putSerializable( GPSLocationMsg.class.getName(), gps_message );
                Message msg = Message.obtain(null, GPS_MESSAGE_ID);
                msg.setData(arg);
                m_ReplyMessenger.send(msg);
                Log.d(TAG, "Send message to MainActivity");
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
                Log.e(TAG, "failed send message");
            }
        }
    }
    /******************************************************************
     * IBinder onBind : Bind Connection for MainActivity
     * :Parameters:
     * :Intent i: Intent info
     * :Returns Type: IBinder
     ******************************************************************/
    @Override
    public IBinder onBind
        (
                Intent i
        )
    {
        Log.d(TAG,"Make Bind");
        return m_Messenger.getBinder();
    }
    /******************************************************************
     * onDestroy : end of life cycle
     * :Parameters:
     * void
     ******************************************************************/
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (m_LocationManager != null) {
            // バックグラウンドから戻ってしまうと例外が発生する場合がある
            try {
                // minTime = 10msec, minDistance = 1m
                if (ActivityCompat.checkSelfPermission
                        (
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission
                                (
                                        this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                        ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    // Prepare File Write for location info
                    return;
                }
                m_LocationManager.removeUpdates(this);
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

    }

}
