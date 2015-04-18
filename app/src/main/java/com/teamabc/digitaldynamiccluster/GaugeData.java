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

    public GaugeData(Context context) {
        this.mContext = context;
    }

    public void updateData(byte[] data) {
        int duration = Toast.LENGTH_SHORT;

        CharSequence text = "Updated data!";
        Toast toast = Toast.makeText(mContext, text, duration);
        toast.show();
    }

    @Override
    public void notifyObservers() {

        for (Observer gauge : gauges) {
            //gauge.update(this, data);
        }
    }
}
