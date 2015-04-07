package com.teamabc.digitaldynamiccluster;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.codeandmagic.android.gauge.GaugeView;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class Gauge implements Observer {
    private static final String TAG = "Gauge";
    private ViewGroup view;
    private final Random RAND = new Random();

    public Gauge() {
    }

    @Override
    public void update(Observable observable, Object data) {
        // update gauge
        ((GaugeView) view.getChildAt(0)).setTargetValue(RAND.nextInt(101));
    }

    public View getView() {
        return view;
    }

    public void setView(ViewGroup view) {
        this.view = view;
    }
}
