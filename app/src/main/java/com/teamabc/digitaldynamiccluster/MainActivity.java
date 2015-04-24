package com.teamabc.digitaldynamiccluster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.teamabc.customviews.GaugeView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Shared Preferences
    private static SharedPreferences SP;

    // Gauge Stuff
    final GaugeData gaugeData = new GaugeData(this);
    private ViewGroup mContentView;
    private View mEditView;
    private ViewGroup mFocusedGauge = null;

    private UsbSerialPort sPort = null;
    private UsbManager mUsbManager;

    // Navigation Drawer
    private DrawerLayout drawerLayout;
    private ListView listView;
    private ActionBarDrawerToggle drawerListener;
    private MyAdapter myAdapter;
    String[] drawerOptions;

    // Navigation Drawer
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String mTitle;

    // Floating Action Button
    FloatingActionsMenu mFloatingActionsMenu;

    // Bluetooth
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    BluetoothSPP bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mEditView = findViewById(R.id.edit_actions_menu);
        mFloatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.edit_actions_menu);

        // TODO: Move Preferences stuff somewhere else
        // Get Shared Preferences
        SP = getSharedPreferences("com.teamabc.digitaldynamiccluster", MODE_PRIVATE);

        // Get shared preferences
        SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Register shared preferences changed listener
        SP.registerOnSharedPreferenceChangeListener(spChanged);

        String clusterConfigName = SP.getString("username", "NA");
        boolean landscapeLock = SP.getBoolean("landscapeLock", false);
        String clusterBackground = SP.getString("clusterBackground", "Black");

        // Display the warning only the first time MainActivity runs
        if (SP.getBoolean("firstrun", false)) {
            new AlertDialog.Builder(this)
                    .setTitle("WARNING")
                    .setMessage("The Digital Dynamic Cluster application is not permitted to be operated while a vehicle is in operation.. " +
                            "By clicking Ok, you agree to not operate this Digital Display while operating a vehicle.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setIcon(R.drawable.info).show();
        }


        ViewGroup mContentView = (ViewGroup) findViewById(R.id.content_frame);
        // Click on the main view enables/disables edit mode
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditView.getVisibility() == View.VISIBLE) {
                    disableEdit();
                }
                else {
                    enableEdit();
                }
                Log.d(TAG, "disable edit");
            }
        });


        mDrawerOptions = getResources().getStringArray(R.array.drawer_options);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.drawer_list);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerOptions));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        bt = new BluetoothSPP(this);
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                 Toast.makeText(getBaseContext(), "Data", Toast.LENGTH_SHORT).show();
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getBaseContext(), "Bluetooth Connected", Toast.LENGTH_SHORT);
            }

            public void onDeviceDisconnected() {
                Toast.makeText(getBaseContext(), "Bluetooth Disconnected", Toast.LENGTH_SHORT);
            }

            public void onDeviceConnectionFailed() {
                Toast.makeText(getBaseContext(), "Bluetooth Failed", Toast.LENGTH_SHORT);
            }
        });


        SP.edit().putBoolean("firstrun", false).commit();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }

    /*
    public void setup() {
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d(TAG, "Data received");
            }
        });
    }
    */

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if(!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void connectToBluetoothDevice() {
        if(!bt.isBluetoothAvailable()) {
            // any command for bluetooth is not available
            Log.d(TAG, "Bluetooth unavailable");
        }

        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

        //bt.autoConnect("DigitalDynamicCluster");

        /*
        try {

            findBT();
            openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private void connectToUSBDevice() {
        // Find all available drivers from attached devices.
        List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        if (drivers.isEmpty()) {
            //Toast.makeText(this, "Can't Connect", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = drivers.get(0);
        UsbDeviceConnection connection = mUsbManager.openDevice(driver.getDevice());
        if (connection == null) {
            //Toast.makeText(this, "Can't Connect", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read some data! Most have just one port (port 0).
        sPort = driver.getPorts().get(0);
        try {
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT);
            e.printStackTrace();
        }
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT);
        startIoManager();
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Depending on the selection open up a fragment or activity
        Intent intent;
        switch(position) {
            case 0: // Connect
                connectToBluetoothDevice();
                //connectToUSBDevice();
                break;

            case 1: // Layouts
                intent = new Intent(this, LayoutsActivity.class);
                startActivity(intent);
                break;

            case 2: // About
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;

            case 3: // Settings
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }

        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawers();
    }

    // Full Screen
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }


    public void addGauge(View view) {
        // Collapse floating action menu
        mFloatingActionsMenu.collapse();
        LayoutInflater li = LayoutInflater.from(this);
        View addGaugeView = li.inflate(R.layout.add_gauge, null);

        // Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_gauge);

        // Set custom view
        builder.setView(addGaugeView);

        //final Spinner gaugeStyleSelect = (Spinner) addGaugeView.findViewById(R.id.gauge_style_select);
        final Spinner gaugeTypeSelect = (Spinner) addGaugeView.findViewById(R.id.gauge_type_select);
        //final Spinner gaugeTextColorSelect = (Spinner) addGaugeView.findViewById(R.id.text_color_select);
        //final Spinner gaugeColorSelect = (Spinner) addGaugeView.findViewById(R.id.gauge_face_color_select);
        final EditText gaugeMinValueSelect = (EditText) addGaugeView.findViewById(R.id.gauge_min_value_select);
        final EditText gaugeMaxValueSelect = (EditText) addGaugeView.findViewById(R.id.gauge_max_value_select);
        final EditText gaugeGaugeNicksSelect = (EditText) addGaugeView.findViewById(R.id.gauge_ticks_select);
        final String[] gaugeTypeSelectValues = getResources().getStringArray(R.array.gauge_type_values);
        final String[] gaugeUnitValues = getResources().getStringArray(R.array.gauge_units);
        final int[] colorValues = getResources().getIntArray(R.array.colors);

        /*
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                //Get the selected value
                int selectedValue = selectedValues.getInt(position, -1);
                Log.d("demo", "selectedValues = " + selectedValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });
        */
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                // Create new gauge or text view
                Gauge newGauge = new Gauge();

                newGauge.setType(gaugeTypeSelectValues[gaugeTypeSelect.getSelectedItemPosition()]);

                // Attach observer
                gaugeData.attach(newGauge);
                ViewGroup newGaugeView = (ViewGroup) LayoutInflater.from(getBaseContext()).inflate(R.layout.gauge_layout, null);


                GaugeView gauge = (GaugeView) newGaugeView.getChildAt(0);
                gauge.setLabelText(gaugeTypeSelectValues[gaugeTypeSelect.getSelectedItemPosition()]);
                gauge.setUnitText(gaugeUnitValues[gaugeTypeSelect.getSelectedItemPosition()]);
                if (gaugeMinValueSelect.getText().toString() != null) {
                    gauge.setScaleMinNumber(Integer.parseInt(gaugeMinValueSelect.getText().toString()));
                }
                if (gaugeMaxValueSelect.getText().toString() != null) {
                    gauge.setScaleMaxNumber(Integer.parseInt(gaugeMaxValueSelect.getText().toString()));
                }


                if (gaugeGaugeNicksSelect.getText().toString() != null) {
                    int ticks = Integer.parseInt(gaugeGaugeNicksSelect.getText().toString());
                    if (ticks <= 0) {
                        ticks = 1;
                    }
                    else {
                        ticks += 1;
                    }
                    gauge.setScaleTotalNicks(ticks);
                }


                //gauge.setGaugeFaceColor(colorValues[gaugeColorSelect.getSelectedItemPosition()]);
                //gauge.setScaleColor(colorValues[gaugeColorSelect.getSelectedItemPosition()]);
                // TODO: Modify gauge settings here

                // Add new gauge to root layer
                mContentView = (ViewGroup) findViewById(R.id.content_frame);
                mContentView.addView(newGaugeView);
                newGaugeView.bringToFront();

                // Set up listeners
                // TODO: Not sure to comment this out or use it, causes bug but has better functionality...
                newGaugeView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        // After a long click the gauge is focused
                        //enableEdit();
                        view.requestFocus();

                        return true;
                    }
                });

                newGaugeView.setFocusable(true);
                newGaugeView.setFocusableInTouchMode(true);
                newGaugeView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if(hasFocus){
                            enableEdit();
                            mFocusedGauge = (ViewGroup) v;
                            v.setBackgroundResource(R.drawable.border_background);
                            v.setOnTouchListener(new ViewMove());
                            ((ViewGroup) v).getChildAt(1).setVisibility(View.VISIBLE);
                        }
                        else {
                            mFocusedGauge = null;
                            v.setBackground(null);
                            v.setOnTouchListener(null);
                            ((ViewGroup) v).getChildAt(1).setVisibility(View.INVISIBLE);
                        }
                    }
                });

                // Set listener for resize
                newGaugeView.getChildAt(1).setOnTouchListener(new ViewResize(newGaugeView));

                // Set initial size and position
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) newGaugeView.getLayoutParams();
                layoutParams.leftMargin = 0;
                layoutParams.topMargin = 0;
                layoutParams.width = 300;
                layoutParams.height = 300;
                newGaugeView.setLayoutParams(layoutParams);

                newGauge.setView(newGaugeView);
                gaugeData.addObserver(newGauge);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void bringToFront(View v) {
        // Collapse floating action menu
        mFloatingActionsMenu.collapse();
        if (mFocusedGauge == null) {
            Toast.makeText(this, "Please select a gauge first", Toast.LENGTH_SHORT).show();
        }
        mFocusedGauge.bringToFront();
    }

    // Remove focused gauge
    public void removeGauge(View v) {
        // Collapse floating action menu
        mFloatingActionsMenu.collapse();
        if (mFocusedGauge == null) {
            Toast.makeText(this, "Please select a gauge first", Toast.LENGTH_SHORT).show();
        }
        mContentView.removeView(mFocusedGauge);
    }

    // TODO: Implement editGauge functionality
    public void editGauge(View view) {
        // Collapse floating action menu
        mFloatingActionsMenu.collapse();
        if (mFocusedGauge == null) {
            Toast.makeText(this, "Please select a gauge first", Toast.LENGTH_SHORT).show();
        }
    }

    // TODO: Implement saveView functionality
    public void saveView(View view) {

    }

    private void enableEdit() {
        mEditView.setVisibility(View.VISIBLE);
    }

    private void disableEdit() {
        // Collapse floating action menu
        mFloatingActionsMenu.collapse();
        // TODO: Fix bug when you initially click before adding any gauges
        mContentView.setFocusable(true);
        mContentView.setFocusableInTouchMode(true);
        mContentView.requestFocus();
        mEditView.setVisibility(View.INVISIBLE);
    }

    // ------------ Serial Data functions ------------------
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gaugeData.updateData(data);
                            //HexDump.dumpHexString(data)
                        }
                    });
                }
            };

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        //finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: check if bluetooth or usb
        //connectToUSBDevice();
        //connectToBluetoothDevice();
        //onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        //mTvSerial.append(message);
    }

    // TODO: Scale the gauge nicely
    public class ViewResize implements View.OnTouchListener {
        private ViewGroup resizeView;

        public ViewResize(ViewGroup resizeView) {
            this.resizeView = resizeView;
        }

        float centerX, centerY, startR, startScale, startX, startY, newScale, newR;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            View dragHandle = v;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                mFocusedGauge = resizeView;

                // calculate center of image
                centerX = (resizeView.getLeft() + resizeView.getRight()) / 2f;
                centerY = (resizeView.getTop() + resizeView.getBottom()) / 2f;

                // recalculate coordinates of starting point
                startX = dragHandle.getX();
                startY = dragHandle.getY();

                // get starting distance and scale
                startR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);
                startScale = resizeView.getScaleX();

            } else if (e.getAction() == MotionEvent.ACTION_MOVE) {

                // calculate new distance
                newR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);

                // set new scale
                newScale = newR / startR * startScale;
                resizeView.setScaleX(newScale);
                resizeView.setScaleY(newScale);

            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                ViewGroup.LayoutParams lp = resizeView.getLayoutParams();
                lp.height = (int) (resizeView.getHeight() * resizeView.getScaleX());
                lp.width = (int) (resizeView.getWidth() * resizeView.getScaleY());
                resizeView.setLayoutParams(lp);
                resizeView.setScaleX(1);
                resizeView.setScaleY(1);
                Log.d(TAG, "Resize Done!");
            }
            return true;
        }
    }

    // TODO: This function is buggy when long pressing on gauge
    public class ViewMove implements View.OnTouchListener {
        private int _xDelta;
        private int _yDelta;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    _xDelta = X - lParams.leftMargin;
                    _yDelta = Y - lParams.topMargin;
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    layoutParams.leftMargin = X - _xDelta;
                    layoutParams.topMargin = Y - _yDelta;
                    layoutParams.rightMargin = -250;
                    layoutParams.bottomMargin = -250;
                    view.setLayoutParams(layoutParams);
                    break;
            }
            view.invalidate();
            return true;
        }
    }

    class MyAdapter extends BaseAdapter {

        private Context context;

        // Arrays used to hold items to be put in drawer rows
        int[] images = {R.drawable.heartrate, R.drawable.layers, R.drawable.nut4, R.drawable.questionmarkicon};

        public MyAdapter(Context context) {
            this.context = context;
            //drawerOptions = context.getResources().getStringArray(R.array.drawer);
        }

        @Override
        public int getCount() {
            return drawerOptions.length;
        }

        @Override
        public Object getItem(int position) {
            return drawerOptions[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = null;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.custom_row, parent, false);
            } else {
                row = convertView;
            }

            TextView titleTextView = (TextView) row.findViewById((R.id.textView1));
            ImageView titleImageView = (ImageView) row.findViewById(R.id.imageView1);

            titleTextView.setText(drawerOptions[position]);
            titleImageView.setImageResource(images[position]);

            return row;
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.contentEquals("clusterBackground")) {
                Toast.makeText(getApplicationContext(), "Background has been changed! Key: " + key,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("DigitalDynamicCluster")) {
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            //Toast.makeText(getBaseContext(), data, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
}
