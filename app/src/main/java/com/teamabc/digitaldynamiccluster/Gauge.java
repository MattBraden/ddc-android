package com.teamabc.digitaldynamiccluster;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.teamabc.customviews.GaugeView;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class Gauge implements Observer {
    private static final String TAG = "Gauge";
    private String type;
    private ViewGroup view;
    private final Random RAND = new Random();

    public Gauge() {
    }

    @Override
    public void update(Observable observable, Object data) {
        // Check if gauge cares about new data
        Pattern typePattern = Pattern.compile(getType() + ":(\\d+)");
        Matcher m = typePattern.matcher((String)data);
        if (m.find()) {
            ((GaugeView) view.getChildAt(0)).setValue(Float.parseFloat(m.group(1)));
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public View getView() {
        return view;
    }

    public void setView(ViewGroup view) {
        this.view = view;
    }

}
