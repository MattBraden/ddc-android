package com.teamabc.digitaldynamiccluster;


import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.codeandmagic.android.gauge.GaugeView;

import java.util.Random;


public class MainActivity extends ActionBarActivity {
    public final static String EXTRA_MESSAGE = "com.teamabc.digitaldynamiccluster.MESSAGE";
    private ViewGroup rootLayout;
    private GaugeView gaugeView;
    private ImageView imageView;
    private int _xDelta;
    private int _yDelta;
    private final Random RAND = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up listener for long click
        imageView = (ImageView) findViewById(R.id.image_view);
        rootLayout = (ViewGroup) findViewById(R.id.root_view);
        gaugeView = (GaugeView) findViewById(R.id.gauge_view1);
        mTimer.start();
        gaugeView.setOnTouchListener(new ViewMove());
        imageView.setOnTouchListener(new ViewMove());
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
            return true;
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (hasFocus) {
                getWindow().getDecorView()
                        .setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }
        }
    }
    */

    private final CountDownTimer mTimer = new CountDownTimer(30000, 1000) {

        @Override
        public void onTick(final long millisUntilFinished) {
            gaugeView.setTargetValue(RAND.nextInt(101));
        }

        @Override
        public void onFinish() {}
    };

    public void sendMessage(View view) {
        Intent intent = new Intent(this, SendMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void addGauge(View view) {
        GaugeView newGauge = new GaugeView(this);

        // Add new gauge to root layer
        rootLayout = (ViewGroup) findViewById(R.id.root_view);
        rootLayout.addView(newGauge);
        newGauge.bringToFront();

        // Set initial size and position
        newGauge.setOnTouchListener(new ViewMove());
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) newGauge.getLayoutParams();
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = 0;
        layoutParams.width = 200;
        layoutParams.height = 200;
        newGauge.setLayoutParams(layoutParams);
    }

    public class ViewMove implements View.OnTouchListener {
        private ViewGroup rootLayout = (ViewGroup) findViewById(R.id.root_view);
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
            rootLayout.invalidate();
            return true;
        }
    }
}
