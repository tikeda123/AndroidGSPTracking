package com.example.tikeda.gpstracking.steps;

import com.example.tikeda.gpstracking.*;
import com.example.tikeda.gpstracking.MainActivity;
import com.example.tikeda.gpstracking.location.GPSLocationService;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * Created by tikeda on 2017/01/07.
 */
/******************************************************************
 StepSensorService Class
 ******************************************************************/
public class StepSensorService extends Service
{
    private static final String TAG = "StepSensorService";
    public static final int     STEPS_MESSAGE_ID = 3;

    private SensorManager   m_SensorManager;
    private Sensor          m_StepCounterSensor;
    private Sensor          m_StepDirectorSensor;

    private Messenger       m_Messenger= null;;
    public static Messenger m_ReplyMessenger= null;;

    private static int m_LastStepCount = 0;
    private static int m_StepsAcy = 0;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand()");
        m_LastStepCount = 0;
        return START_STICKY;
    }

    /******************************************************************
     onCreate : callback onCreate..
     :Parameters:
     :Returns Type: void
     ******************************************************************/
    @Override
    public void onCreate()
    {
        super.onCreate();

        TrackingPrefFile tp = TrackingPrefFile.GetTrackingPrefFile();
        m_SensorManager     = (SensorManager) getSystemService(SENSOR_SERVICE);
        m_StepCounterSensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        m_StepDirectorSensor= m_SensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        m_Messenger         = new Messenger(new ServiceHandler());

        // Register for sensor events in batch mode, allowing up to 5 seconds delay before events
        // get reported. We don't care about the delay *too* much, but 5 seconds seems about right,
        // and some devices seem more inclined to follow this suggestion than others.
        m_SensorManager.registerListener
                (
                        stepSensorEventListener,
                        m_StepCounterSensor,
                        TrackingPrefFile.GetTrackingPrefFile().getSTEP_COUNTER_DELAY(),
                        (int) TimeUnit.SECONDS.toMicros(5)
                );

        m_SensorManager.registerListener
                (
                        stepSensorEventListener,
                        m_StepDirectorSensor,
                        TrackingPrefFile.GetTrackingPrefFile().getSTEP_DIRECTOR_DELAY(),
                        (int) TimeUnit.SECONDS.toMicros(5)
                );
    }

    /**
     * This is our implementation of {@link android.hardware.SensorEventListener} which listens for
     * step counter sensor events.
     */
    /***************************************************************************
     SensorEventListener : his is our implementation of which listens for step counter sensor events.
     :Parameters:
     :Returns Type: void
     ***************************************************************************/
    private final SensorEventListener stepSensorEventListener  = new SensorEventListener()
    {
        @Override
        public void onSensorChanged( SensorEvent event )
        {
            int count       = (int) event.values[0];
            Sensor sensor   = event.sensor;
            int stepsThisEvent = 0;
            long timestamp = new Date().getTime();

            if(sensor.getType() == Sensor.TYPE_STEP_COUNTER )
            {
                String disp = String.format( "lastStepCount:%d,count:%d", m_LastStepCount, count);
                Log.i(TAG, disp);

                if (m_LastStepCount == 0)
                {
                    m_LastStepCount = count;
                }

                stepsThisEvent  = count - m_LastStepCount;
                SendMessage( stepsThisEvent, timestamp, m_StepsAcy );
            }
        }

        /***************************************************************************
         onAccuracyChanged : Ignored, there's no accuracy for the step counter
        :Returns Type: void
         ***************************************************************************/
        @Override
        public void onAccuracyChanged
            (
                Sensor sensor,
                int accuracy
            )
        {

            if( sensor.getType() == Sensor.TYPE_STEP_DETECTOR )
            {
                String disp = String.format("Step Accuracy :%d", accuracy );
                Log.i(TAG, disp );
                m_StepsAcy = accuracy;
            }
        }
    };

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
                    Log.i(TAG,"Received Message.from GPSLocationService");
                    break;
                case StepSensorService.STEPS_MESSAGE_ID:
                    Bundle arg = msg.getData();
                    StepSensorMsg step_message = (StepSensorMsg) arg.getSerializable( StepSensorMsg.class.getName() );
                    Log.i(TAG,"Received Message.from StepSensorService");
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
            long    steps,
            long    timestamp,
            int     stepsacy
        )
    {
        if( m_ReplyMessenger!=null )
        {
            try
            {
                StepSensorMsg step_message  = new StepSensorMsg();
                step_message.setM_Steps( steps );
                step_message.setM_TimeStamp( timestamp );
                step_message.setM_StepsAccuracy(stepsacy);
                Bundle arg = new Bundle();
                arg.putSerializable( StepSensorMsg.class.getName(), step_message );
                Message msg = Message.obtain(null, STEPS_MESSAGE_ID);
                msg.setData( arg );
                m_ReplyMessenger.send( msg );
                Log.d(TAG, "Send message to MainActivity");
            }
            catch ( RemoteException e )
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
        m_SensorManager.unregisterListener(stepSensorEventListener);
    }
}
