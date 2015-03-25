package com.teamabc.digitaldynamiccluster;

import android.view.View;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class Gauge implements Observer {
    private View view;

    @Override
    public void update(Observable observable, Object data) {

    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }
}
