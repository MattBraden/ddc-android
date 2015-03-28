package com.teamabc.digitaldynamiccluster;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class GaugeData extends Observable {
    private static final String TAG = "GaugeData";
    private ArrayList<Observer> gauges = new ArrayList<Observer>();
    private Context mContext;
    private UsbSerialPort port;
    private byte[] data = new byte[1000];

    public GaugeData(Context context) {
        this.mContext = context;
    }

    public boolean setupUSB() {
        // Find all available drivers from attached devices.
        int duration = Toast.LENGTH_SHORT;


        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No drivers!");

            CharSequence text = "No Drivers!";
            Toast toast = Toast.makeText(mContext, text, duration);
            toast.show();
            return false;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            Log.d(TAG, "No connection!");
            CharSequence text = "No Connection!";
            Toast toast = Toast.makeText(mContext, text, duration);
            toast.show();
            return false;
        }

        // Read some data! Most have just one port (port 0).
        port = driver.getPorts().get(0);

        try {
            port.open(connection);
            port.setParameters(115200, 8, 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CharSequence text = "Connected!";
        Toast toast = Toast.makeText(mContext, text, duration);
        toast.show();

        return true;
    }

    public void sendData(String message) {

        try {
            byte buffer[] = message.getBytes();
            /*
            int numBytesRead = port.read(buffer, 1000);
            Log.d(TAG, "Read " + numBytesRead + " bytes.");
            */
            port.write(buffer, 1000);
        } catch (IOException e) {
            Log.d(TAG, "IOEception orrcured");
        } finally {
            Log.d(TAG, "Done.");
        }
    }

    public void readData() {
        Log.d(TAG, "Reading new data");
        byte buffer[] = new byte[1000];
        try {
            int numBytesRead = port.read(buffer, 1000);
            Log.d(TAG, "Read " + numBytesRead + " bytes.");

            port.read(buffer, 1000);
        } catch (IOException e) {
            Log.d(TAG, "IOEception orrcured");
        }


        data = buffer;
        notifyObservers();
    }

    @Override
    public void notifyObservers() {
        Log.d(TAG, "Updating observers");
        for(Observer gauge: gauges) {
            gauge.update(this, data);
        }
    }
}
