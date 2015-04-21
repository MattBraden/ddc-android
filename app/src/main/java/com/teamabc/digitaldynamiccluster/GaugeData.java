package com.teamabc.digitaldynamiccluster;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class GaugeData extends Observable {
    private static final String TAG = "GaugeData";
    private ArrayList<Observer> observers = new ArrayList<Observer>();
    private Context mContext;

    private String newData;

    public GaugeData(Context context) {
        this.mContext = context;
    }

    public void updateData(byte[] data) {
        String decoded = null;
        try {
            newData = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        notifyObservers();
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this, newData);
        }
    }

    public void attach(Observer observer){
        observers.add(observer);
    }
}
