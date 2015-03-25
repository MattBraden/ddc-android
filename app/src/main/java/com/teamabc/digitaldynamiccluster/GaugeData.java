package com.teamabc.digitaldynamiccluster;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.Observable;

/**
 * Created by MattBeast on 3/25/2015.
 */
public class GaugeData extends Observable {
    private static final String TAG = "GaugeData";
    private Context mContext;
    private UsbSerialPort port;

    public GaugeData(Context context) {
        this.mContext = context;
    }

    public void setupUSB() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No drivers!");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            Log.d(TAG, "No connection!");
            return;
        }

        // Read some data! Most have just one port (port 0).
        port = driver.getPorts().get(0);

        try {
            port.open(connection);
            port.setParameters(115200, 8, 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendData(String message) {
        while (1<2) {
            try {
                byte buffer[] = message.getBytes();
            /*
            int numBytesRead = port.read(buffer, 1000);
            Log.d(TAG, "Read " + numBytesRead + " bytes.");
            */
                port.write(buffer, 1000);
                //port.close();
            } catch (IOException e) {
                Log.d(TAG, "IOEception orrcured");
            } finally {
                Log.d(TAG, "Done.");
            }
        }
    }




}
