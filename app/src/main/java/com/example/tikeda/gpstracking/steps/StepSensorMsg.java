package com.example.tikeda.gpstracking.steps;

import java.io.Serializable;

/**
 * Created by tikeda on 2017/01/09.
 */

public class StepSensorMsg implements Serializable
{
    private static final long serialVersionUID = -8730383208417253446L;

    private long    m_Steps;
    private long    m_TimeStamp;
    private int     m_StepsAccuracy;

    public long getM_Steps() {
        return m_Steps;
    }

    public void setM_Steps(long m_Steps) {
        this.m_Steps = m_Steps;
    }

    public long getM_TimeStamp() {
        return m_TimeStamp;
    }

    public void setM_TimeStamp(long m_TimeStamp) {
        this.m_TimeStamp = m_TimeStamp;
    }

    public int getM_StepsAccuracy() {
        return m_StepsAccuracy;
    }

    public void setM_StepsAccuracy(int m_StepsAccuracy) {
        this.m_StepsAccuracy = m_StepsAccuracy;
    }

}
