package com.teamabc.digitaldynamiccluster;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class GaugeData extends Observable {
    private static final String TAG = "GaugeData";
    private ArrayList<Observer> gauges = new ArrayList<Observer>();
    private Context mContext;
    private byte[] data = new byte[4096];

    public GaugeData(Context context) {
        this.mContext = context;
    }

    public boolean setupUSB() {
        return false;
    }

    public void sendData(String message) {

    }

    public void readData() {

    }

    @Override
    public void notifyObservers() {
        int duration = Toast.LENGTH_SHORT;

        CharSequence text = "Update Observers!";
        Toast toast = Toast.makeText(mContext, text, duration);
        toast.show();
        for(Observer gauge: gauges) {
            gauge.update(this, data);
        }
    }
}
