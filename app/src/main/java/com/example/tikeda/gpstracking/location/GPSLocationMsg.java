package com.example.tikeda.gpstracking.location;

import android.location.Location;

import java.io.Serializable;

/**
 * Created by tikeda on 2017/01/08.
 */

public class GPSLocationMsg implements Serializable
{
    private static final long serialVersionUID = -8710373208447953446L;

    private long    m_Time;
    private double  m_Latitude;
    private double  m_Longitude;
    private float   m_Speed;
    private float   m_Accuracy;
    private float   m_Bearing;
    private double  m_Altitude;

    public long getM_Time() {
        return m_Time;
    }

    public void setM_Time(long m_Time) {
        this.m_Time = m_Time;
    }

    public double getM_Latitude() {
        return m_Latitude;
    }

    public void setM_Latitude(double m_Latitude) {
        this.m_Latitude = m_Latitude;
    }

    public double getM_Longitude() {
        return m_Longitude;
    }

    public void setM_Longitude(double m_Longitude) {
        this.m_Longitude = m_Longitude;
    }

    public float getM_Speed() {
        return m_Speed;
    }

    public void setM_Speed(float m_Speed) {
        this.m_Speed = m_Speed;
    }

    public float getM_Accuracy() {
        return m_Accuracy;
    }

    public void setM_Accuracy(float m_Accuracy) {
        this.m_Accuracy = m_Accuracy;
    }

    public float getM_Bearing() {
        return m_Bearing;
    }

    public void setM_Bearing(float m_Bearing) {
        this.m_Bearing = m_Bearing;
    }

    public double getM_Altitude() {
        return m_Altitude;
    }

    public void setM_Altitude(double m_Altitude) {
        this.m_Altitude = m_Altitude;
    }
 }
