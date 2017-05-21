package com.example.tikeda.gpstracking;

import com.example.tikeda.gpstracking.http.*;
import com.example.tikeda.gpstracking.file.*;
import com.example.tikeda.gpstracking.location.GPSLocationMsg;
import com.example.tikeda.gpstracking.location.GPSLocationService;
import com.example.tikeda.gpstracking.steps.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.View.OnClickListener;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.Manifest;

import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.location.Location;


/******************************************************************
 MainActivity Class
 ******************************************************************/
public class MainActivity extends WearableActivity
        implements OnClickListener,ServiceConnection
{
    private static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSION = 1000;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;
    public  static final int MA_MESSAGE_ID = 2;

    private Messenger m_Messenger;
    private Messenger m_ReplyMessenger_GPS;
    private Messenger m_ReplyMessenger_STP;

    private TextView        m_SpeedTextview;
    private boolean         m_IsRunning = false;
    private View            m_BlinkingGpsStatusDotView;
    private ImageButton     m_SettingBnt;
    private TextView        m_AccuracyTextview;
    private TextView        m_StepsTextview;

    private Handler         m_Handler   = new Handler();
    private TrackingData    m_TrackingData;
    private AsyncHttpPost   m_AsyncHTTP;

    // mesure number of Steps from steps sencer
    private static long     m_Steps;
    private static long     m_StepsTimestamp;
    private static int      m_StepsAcy;

    /******************************************************************
     onCreate : Android create Activity..
     :Parameters:
     :Bundle savedInstanceState
     :Returns:
     :Returns Type: void
     ******************************************************************/
    @Override
    protected void onCreate
        (
                Bundle savedInstanceState
        )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        setAmbientEnabled();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener
        (
                new WatchViewStub.OnLayoutInflatedListener()
                {
                    @Override
                    public void onLayoutInflated(WatchViewStub stub)
                    {
                        m_SpeedTextview     = (TextView) stub.findViewById(R.id.speed_textview);
                        m_AccuracyTextview  = (TextView) stub.findViewById(R.id.accuracy_textview);
                        m_StepsTextview     = (TextView) stub.findViewById(R.id.steps_textView);
                        m_BlinkingGpsStatusDotView = (View) stub.findViewById(R.id.gps_status);
                        m_BlinkingGpsStatusDotView.setVisibility(View.INVISIBLE);
                        m_SettingBnt        = (ImageButton) stub.findViewById(R.id.config_setting);

                        Button bt=(Button)findViewById(R.id.btn_start);
                        bt.setOnClickListener(MainActivity.this);
                    }
                }
        );

        TrackingPrefFile.GetTrackingPrefFile().ReadTrackingPreFile( GetSerialNumber(), this );
        m_Messenger = new Messenger(new MAHandler(this));

        // More than Android 6, API 23, need to permission
        if(Build.VERSION.SDK_INT >= 23)
        {
            checkPermission();
        }

    }

    /******************************************************************
     onClick : Click Start Button, pop up Dialog for comfirm stop or cancel
     :Parameters:
     :View view
     :Returns:
     :Returns Type: void
     ******************************************************************/
    public  void onSettingClick
        (
            View view
        )
    {
        if ( !m_IsRunning )
        {
            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            builder.setTitle(R.string.dialog_confsetting_title);
            builder.setMessage(R.string.dialog_confsetting_message);

            builder.setPositiveButton // Pressed OK Button
                    (
                            "OK",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    setTitle("OK");
                                }
                            }
                    );

            builder.setNegativeButton // Sressed Cancel Button
                    (
                            "CANCEL",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    setTitle("CANCEL");
                                }
                            }
                    );

            m_BlinkingGpsStatusDotView.setVisibility( View.INVISIBLE ) ;// turn off green dot.
            AlertDialog dialog  = builder.create();
            dialog.show();
        }
    }

    /******************************************************************
     onClick : Click Start Button, pop up Dialog for comfirm stop or cancel
     :Parameters:
     :View view
     :Returns:
     :Returns Type: void
     ******************************************************************/
    public void onClick
        (
                View view
        )
    {
        if ( m_IsRunning )
        {
            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            builder.setTitle(R.string.dialog_confirm_startstop_title);
            builder.setMessage(R.string.dialog_confirm_startstop_message);

            builder.setPositiveButton // Pressed OK Button
                    (
                            "OK",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    setTitle("OK");
                                    // Stop StepSensorService service
                                    StopAllService();
                                    LocationRepository.FileClose();
                                    finish();
                                    System.exit(0);
                                }
                            }
                    );

            builder.setNegativeButton // Sressed Cancel Button
                    (
                            "CANCEL",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    setTitle("CANCEL");
                                }
                            }
                    );

            m_BlinkingGpsStatusDotView.setVisibility( View.INVISIBLE );// turn off green dot.
            AlertDialog dialog  = builder.create();
            dialog.show();
        }
        else
        {
            m_SettingBnt.setVisibility( View.INVISIBLE );// Start GPS Running !!!!
            ( ( Button )view ).setText( "Stop" );// change Start Button to Stop Button.
            m_IsRunning = true;// Change Runing Status Flag
            LocationRepository.createFile(GetSerialNumber(),this );
            StartAllService();// Start all services(GPSLocation,StepSensor)
            connect();// Conect GPS
        }
    }

    /******************************************************************
     checkPermission : Confirm Permission of Accessing Location Info
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    // Confirm Permission of Accessing Location Info
    public void checkPermission()
    {
        // Already Get Permission
        if ( ActivityCompat.checkSelfPermission
                (
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                )
        {

        }
        else
        {
            requestLocationPermission();
        }
    }
    /******************************************************************
     requestLocationPermission : request Location Permission
     :Returns:
     :Returns Type: void
     ******************************************************************/
    // Require of Permission
    private void requestLocationPermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale
                (
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ))
        {
            ActivityCompat.requestPermissions
                    (
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSION
                    );

        }
        else
        {
            Toast toast = Toast.makeText(this, R.string.RequestPermissions_message, Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions
                    (
                            this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,},
                            REQUEST_PERMISSION
                    );

        }
    }
    /******************************************************************
     onRequestPermissionsResult : Call back mathod..
     :Returns:
     :Returns Type: void
     ******************************************************************/
    // Accepet Result
    @Override
    public void onRequestPermissionsResult
        (
                int         requestCode,
                String[]    permissions,
                int[]       grantResults
        )
    {
        if (requestCode == REQUEST_PERMISSION)
        {
            // 使用が許可された
            if ( grantResults[0] == PackageManager.PERMISSION_GRANTED )
            {
                return;
            }
            else
            {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this,R.string.PermissionsResult_message, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
    /******************************************************************
     StartAllService : Start All Services
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    protected void StartAllService()
    {
        super.onResume();
        try {
            // Start GPS Location Service
            Intent startServiceIntent = new Intent( this, GPSLocationService.class );
            this.startService( startServiceIntent );

            // Start Step Sencsor Service
            //startServiceIntent = new Intent( this, StepSensorService.class );
            //this.startService( startServiceIntent );
            AlarmReceiver.schedule( this );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
     }
    /******************************************************************
     StopAllService : Stop All Services
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    private void StopAllService()
    {
        Intent stopServiceIntent    = new Intent( MainActivity.this, StepSensorService.class );
        stopService( stopServiceIntent );

        stopServiceIntent           = new Intent( MainActivity.this, GPSLocationService.class );
        stopService( stopServiceIntent );
    }

    /******************************************************************
     onResume : callback onResume event..
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    @Override
    public void onResume()
    {
        super.onResume();
    }

    /******************************************************************
     onPause : callback onPause event..
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    @Override
    protected void onPause()
    {
      super.onPause();
    }
    /******************************************************************
     onLocationChanged : get new location info from GPS system
     :Parameters:
     :Location location: location Info..
     :Returns:
     :Returns Type: void
     ******************************************************************/
    public  void ChangeStep
        (
                StepSensorMsg msg
        )
    {
        m_Steps             = msg.getM_Steps();
        m_StepsTimestamp    = msg.getM_TimeStamp();
        m_StepsAcy          = msg.getM_StepsAccuracy();
        m_StepsTextview.setText
                (
                        String.format("%d",msg.getM_Steps() )
                );
    }

    /******************************************************************
     onLocationChanged : get new location info from GPS system
     :Parameters:
     :Location location: location Info..
     :Returns:
     :Returns Type: void
     ******************************************************************/
    public  void ChangeLocation
        (
                GPSLocationMsg loc
        )
    {

        m_TrackingData = new TrackingData
                    (
                            GetSerialNumber(),
                            m_Steps,
                            m_StepsAcy,
                            loc
                    );


        m_AccuracyTextview.setText
                (
                        String.format("%f",m_TrackingData.GetAccuracy())+"m"
                );
        m_SpeedTextview.setText
                (
                        String.format( "%05.2f",m_TrackingData.getSpeedAsKmh() )
                );

        m_StepsTextview.setText
                (
                        String.format("%d",m_TrackingData.GetSteps() )
                );

        // write location info to file.
        LocationRepository.writeToFile( m_TrackingData.GetJSON() );

        m_Handler.post // set callback func for dot green
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                m_BlinkingGpsStatusDotView.setVisibility(View.VISIBLE);
                            }
                        }
                );

        m_BlinkingGpsStatusDotView.setVisibility( View.VISIBLE );

        m_Handler.postDelayed
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                m_BlinkingGpsStatusDotView.setVisibility(View.INVISIBLE);
                            }
                        },
                        INDICATOR_DOT_FADE_AWAY_MS
                );

        // Create Thread for Send HTTPPost to Elastic Server..
        if ( TrackingPrefFile.GetTrackingPrefFile().getHTTP_FLAG() == true )
        {
            m_AsyncHTTP = new AsyncHttpPost(TrackingPrefFile.GetTrackingPrefFile().getURL_CLOUDSERVER());
            m_AsyncHTTP.setPostData(m_TrackingData.GetJSON());
            m_AsyncHTTP.execute();
        }
     }
    /******************************************************************
     GetSerialNumber : Get Serial Number from Android wear device(SWR50)
     :Parameters:
     :Returns:
     :Returns Type: String Serial Number
     ******************************************************************/
    private String GetSerialNumber()
    {
        // get android wear ( sony SWR 50) serial number
        String SerialNumber = Build.SERIAL != Build.UNKNOWN ? Build.SERIAL : Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        return SerialNumber;
    }
    /******************************************************************
     TestHandler : Handle Message
     ******************************************************************/
    static class MAHandler extends Handler
    {
        private MainActivity m_MA;

        public MAHandler(MainActivity ma) {
            m_MA = ma;
        }

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
                    Bundle arga = msg.getData();
                    GPSLocationMsg gps_message = (GPSLocationMsg) arga.getSerializable( GPSLocationMsg.class.getName());
                    if( gps_message != null) {
                        m_MA.ChangeLocation(gps_message);
                        super.handleMessage(msg);
                        Log.i(TAG,"Received Message.from GPSLocationService");
                    }
                    else{
                        Log.i(TAG,"Received NULL Message.from GPSLocationService");
                    }
                    break;
                case StepSensorService.STEPS_MESSAGE_ID:
                    Bundle argb = msg.getData();
                    StepSensorMsg stp_message = (StepSensorMsg) argb.getSerializable( StepSensorMsg.class.getName());
                    if( stp_message != null) {
                        m_MA.ChangeStep(stp_message);
                        super.handleMessage(msg);
                        Log.i(TAG,"Received Message.from StepSensorService");
                    }
                    else{
                        Log.i(TAG,"Received NULL Message.from StepSensorService");
                    }
                    break;

                default:
                    Log.i(TAG,"Received Message.from OtherService");
            }
            super.handleMessage(msg);
        }
    }
    /******************************************************************
     onServiceConnected :
     :Parameters:
     :Returns:
     :Returns Type: String Serial Number
     ******************************************************************/
    @Override
    public void onServiceConnected
        (
                ComponentName name,
                IBinder service
        )
    {
        String className = name.getClassName();
        Log.i(TAG,"Service Connected class name:"+className);

        if ( className.equals( GPSLocationService.class.getName() ) )
        {
            m_ReplyMessenger_GPS = new Messenger( service );
            ExchangeReplyMessage( m_ReplyMessenger_GPS );
            Log.i(TAG,"send reply message to GPS:" + GPSLocationService.class.getName());
        }
        else if ( className.equals( StepSensorService.class.getName() ) )
        {
            m_ReplyMessenger_STP = new Messenger( service );
            ExchangeReplyMessage( m_ReplyMessenger_STP );
            Log.i(TAG,"send reply message to STP:" + StepSensorService.class.getName());
        }else{
            Log.i(TAG,"send reply message to Others:" +name.getClassName());
        }

    }
    /******************************************************************
     onServiceDisconnected :
     :Parameters:
     :Returns:
     :Returns Type: String Serial Number
     ******************************************************************/
    @Override
    public void onServiceDisconnected
        (
                ComponentName name
        )
    {
        Log.i(TAG,"Service Disconnected class name:"+name.getClassName());
     }
    /******************************************************************
     oconnect : connect to service..
     :Parameters:
     :Returns:
     :Returns Type: String Serial Number
     ******************************************************************/
    public void connect()
    {
        Intent ingps = new Intent(this,GPSLocationService.class);
        ingps.setPackage("com.example.tikeda.gpstracking.location");
        bindService(ingps, this, Context.BIND_AUTO_CREATE);
        Log.d(TAG,"connect to GPSService");

        Intent instp = new Intent(this,StepSensorService.class);
        instp.setPackage("com.example.tikeda.gpstracking.steps");
        bindService(instp, this, Context.BIND_AUTO_CREATE);

        Log.d(TAG,"connect to STEPSService");
    }
    /******************************************************************
     * disconnect  : Bind Connection for MainActivity
     * :Parameters:
     * :Intent i: Intent info
     * :Returns Type: IBinder
     ******************************************************************/
    public void disconnect()
    {
        unbindService( this );
        m_Messenger = null;
    }
    /******************************************************************
     * ExchangeReplyMessage  : Bind Connection for MainActivity
     * :Parameters:
     * :Intent i: Intent info
     * :Returns Type: IBinder
     ******************************************************************/
    public void ExchangeReplyMessage
        (
                Messenger replay_msg
        )
    {
        try {
            Message msg = Message.obtain(null,MA_MESSAGE_ID,"Hello");
            msg.replyTo = m_Messenger;
            replay_msg.send(msg);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}