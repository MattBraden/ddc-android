package com.teamabc.digitaldynamiccluster;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.codeandmagic.android.gauge.GaugeView;

import java.util.Random;

public class MainActivity extends Activity {
    public final static String EXTRA_MESSAGE = "com.teamabc.digitaldynamiccluster.MESSAGE";
    private ViewGroup rootLayout;
    private ViewGroup gaugeViewLayout;
    private GaugeView gaugeView;
    private ImageView imageView;
    private ViewGroup focusedGauge = null;
    private final Random RAND = new Random();
    private static final String TAG = "SendMessageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Setup default layout
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

    /*
    public void sendMessage(View view) {
        Intent intent = new Intent(this, SendMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
    */

    public void addGauge(View view) {
        ViewGroup newGauge = (ViewGroup) LayoutInflater.from(getBaseContext()).inflate(R.layout.gauge_layout, null);

        // Add new gauge to root layer
        rootLayout = (ViewGroup) findViewById(R.id.root_view);
        rootLayout.addView(newGauge);
        newGauge.bringToFront();

        // Set up listeners
        newGauge.setOnTouchListener(new ViewMove());
        newGauge.getChildAt(1).setOnTouchListener(new ViewResize(newGauge));

        // Set initial size and position
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) newGauge.getLayoutParams();
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = 0;
        layoutParams.width = 300;
        layoutParams.height = 300;
        newGauge.setLayoutParams(layoutParams);
    }

    public void removeGauge(View view) {

    }

    public void editGauge(View view) {

    }

    public class ViewResize implements View.OnTouchListener {
        private View resizeView;
        public ViewResize (View resizeView) {
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

               // TODO: Scale the gauge nicely

                // move handler image
                //dragHandle.setX(centerX + resizeView.getWidth()/2f * newScale);
                //dragHandle.setY(centerY + resizeView.getHeight()/2f * newScale);

            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                focusedGauge = null;
            }
            return true;
        }
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
                    layoutParams.rightMargin = -2500;
                    layoutParams.bottomMargin = -2500;
                    view.setLayoutParams(layoutParams);
                    break;
            }
            view.invalidate();
            return true;
        }
    }
}
