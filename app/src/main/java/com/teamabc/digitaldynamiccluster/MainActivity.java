package com.teamabc.digitaldynamiccluster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // This is the first run
        SP.edit().putBoolean("firstrun", true).commit();

        // Hide the top action bar
        //getSupportActionBar().hide();

        // Display the warning only the first time MainActivity runs
        if (SP.getBoolean("firstrun", true)) {
            new AlertDialog.Builder(this)
                    .setTitle("WARNING")
                    .setMessage("The Digital Dynamic Cluster application is not permitted to be operated while a vehicle is in operation.. " +
                            "By clicking Ok, you agree to not operate this Digital Display while operating a vehicle.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setIcon(R.drawable.info).show();

            new AlertDialog.Builder(this)
                    .setTitle("Preferenecs")
                    .setMessage("Cluster Configuration: " + clusterConfigName + "\n" + "Cluster Background: " + clusterBackground)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setIcon(R.drawable.info).show();
        }

        // Long click on main view enables edit mode
        ViewGroup mContentView = (ViewGroup) findViewById(R.id.content_frame);
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                enableEdit();
                Log.d(TAG, "enable edit");
                return true;
            }
        });
        // Click on the main view disables edit mode
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableEdit();
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


        // Navigation Drawer
        // TODO: Reimplement this code.  Sorry Jacob I was trying some stuff out.
        /*
        listView = (ListView) findViewById(R.id.drawerList);
        drawerLayout = (DrawerLayout) findViewById(R.id.mainActivityLayout);
        myAdapter = new MyAdapter(this);
        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(this);
        */

        SP.edit().putBoolean("firstrun", false).commit();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void connectToDevice() {
        // Find all available drivers from attached devices.
        Toast.makeText(this, "Connecting", Toast.LENGTH_SHORT).show();

        List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        if (drivers.isEmpty()) {
            Toast.makeText(this, "No drivers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = drivers.get(0);
        UsbDeviceConnection connection = mUsbManager.openDevice(driver.getDevice());
        if (connection == null) {
            Toast.makeText(this, "Can't Connect", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read some data! Most have just one port (port 0).
        sPort = driver.getPorts().get(0);
        try {
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
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
                //intent = new Intent(this, ConnectActivity.class);
                //startActivity(intent);
                connectToDevice();
                // TODO: Implement connect code, this should not call an activity but rather a dialog box to select how to connect
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
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                // Create new gauge view
                Gauge newGauge = new Gauge();
                // TODO: Get the type of gauge
                
                newGauge.setType("RPM");

                // Attach observer
                gaugeData.attach(newGauge);
                ViewGroup newGaugeView = (ViewGroup) LayoutInflater.from(getBaseContext()).inflate(R.layout.gauge_layout, null);

                GaugeView gauge = (GaugeView) newGaugeView.getChildAt(0);
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

        if (sPort == null) {
            Toast.makeText(this, "No serial device", Toast.LENGTH_SHORT).show();
        } else {
            UsbDeviceConnection connection = mUsbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                Toast.makeText(this, "Opening device failed", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                Toast.makeText(this, "Error setting up device", Toast.LENGTH_SHORT);
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            Toast.makeText(this, "Serial device: " + sPort.getClass().getSimpleName(), Toast.LENGTH_SHORT);
        }
        onDeviceStateChange();
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
        private View resizeView;

        public ViewResize(View resizeView) {
            this.resizeView = resizeView;
        }

        float centerX, centerY, startR, startScale, startX, startY;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            View dragHandle = v;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                mFocusedGauge = (ViewGroup) v.getParent();

                // calculate center of image
                centerX = (resizeView.getLeft() + resizeView.getRight()) / 2f;
                centerY = (resizeView.getTop() + resizeView.getBottom()) / 2f;

                // recalculate coordinates of starting point
                startX = e.getRawX() - dragHandle.getX() + centerX;
                startY = e.getRawY() - dragHandle.getY() + centerY;

                // get starting distance and scale
                startR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);
                startScale = resizeView.getScaleX();

            } else if (e.getAction() == MotionEvent.ACTION_MOVE) {

                // calculate new distance
                float newR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);

                // set new scale
                float newScale = newR / startR * startScale;
                resizeView.setScaleX(newScale);
                resizeView.setScaleY(newScale);

            } else if (e.getAction() == MotionEvent.ACTION_UP) {

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
}
