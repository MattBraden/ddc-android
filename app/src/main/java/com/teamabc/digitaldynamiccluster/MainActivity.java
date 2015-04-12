package com.teamabc.digitaldynamiccluster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.app.AlertDialog;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.codeandmagic.android.gauge.GaugeView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.teamabc.digitaldynamiccluster.MESSAGE";
    private static final String TAG = "MainActivity";
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActivityTitle = "Closed!";
    final GaugeData gaugeData = new GaugeData(this);
    private ViewGroup rootView;
    private View editView;
    private ViewGroup focusedGauge = null;
    private static UsbSerialPort sPort = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        View rootView = findViewById(R.id.root_view);
        rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                enableEdit();
                Log.d(TAG, "enable edit");
                return true;
            }
        });
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableEdit();
                Log.d(TAG, "disable edit");
            }
        });

        // Splash Screen Warning
        new AlertDialog.Builder(this)
                .setTitle("WARNING")
                .setMessage("Do not operate this Digital Display while under driving conditions. " +
                        "By clicking OK, you agree to this bullshit yada yada yada you're going to do it anyway.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setIcon(R.drawable.info).show();


        mDrawerList = (ListView) findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Floating Action Button
        ImageView imageView = new ImageView(this); // Create an icon
        imageView.setImageResource(R.drawable.target);

        ImageView iconAddGauge = new ImageView(this);
        iconAddGauge.setImageResource(R.drawable.plus);

        ImageView iconDeleteAllGauges = new ImageView(this);
        iconDeleteAllGauges.setImageResource(R.drawable.delete);

        ImageView iconSettings = new ImageView(this);
        iconSettings.setImageResource(R.drawable.process);

        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(imageView)
                .setBackgroundDrawable(R.drawable.delete)
                .build();

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

        SubActionButton buttonAddGauge = itemBuilder.setContentView(iconAddGauge).build();
        SubActionButton buttonDeleteAllGauges = itemBuilder.setContentView(iconDeleteAllGauges).build();
        SubActionButton buttonSettings = itemBuilder.setContentView(iconSettings).build();

        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonAddGauge)
                .addSubActionView(buttonDeleteAllGauges)
                .addSubActionView(buttonSettings)
                .attachTo(actionButton)
                .build();
    }

    private void addDrawerItems() {
        String[] osArray = {"Digital Dynamic Cluster", "Settings", "About"};
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Time for an upgrade!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle("Navigation!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_connect) {
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    public void addGauge(View view) {
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
                ViewGroup newGaugeView = (ViewGroup) LayoutInflater.from(getBaseContext()).inflate(R.layout.gauge_layout, null);

                GaugeView gauge = (GaugeView) newGaugeView.getChildAt(0);

                // Add new gauge to root layer
                rootView = (ViewGroup) findViewById(R.id.root_view);
                rootView.addView(newGaugeView);
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
                        if (hasFocus) {
                            enableEdit();
                            focusedGauge = (ViewGroup) v;
                            v.setBackgroundResource(R.drawable.border_background);
                            v.setOnTouchListener(new ViewMove());
                            ((ViewGroup) v).getChildAt(1).setVisibility(View.VISIBLE);
                        } else {
                            focusedGauge = null;
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
    */

    /*
    public void removeGauge(View v) {
        // Remove focused gauge, dont care about view that was clicked
        rootView.removeView(focusedGauge);
    }
    */

    // TODO: Implement saveView functionality
    public void saveView(View view) {

    }

    // TODO: Implement editGauge functionality
    public void editGauge(View view) {

    }

    private void enableEdit() {

        editView.setVisibility(View.VISIBLE);
    }

    private void disableEdit() {
        // TODO: Fix bug when you initially click before adding any gauges
        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        editView.setVisibility(View.INVISIBLE);
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
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            //mTvSerial.append("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                //mTvSerial.append("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                //mTvSerial.append("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            //mTvSerial.append("Serial device: " + sPort.getClass().getSimpleName());
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

    // TODO: Find a different way to do this...
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
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
                focusedGauge = (ViewGroup) v.getParent();

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
                focusedGauge = null;


                Log.d(TAG, "Resize Done!");
            }
            return true;
        }
    }

    // TODO: This function is buggy when long pressing on gauge
    public class ViewMove implements View.OnTouchListener {
        private View rootView = findViewById(R.id.root_view);
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
}
