package com.teamabc.customviews;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;


public class MainActivity extends ActionBarActivity {
    private GaugeView gauge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

        return super.onOptionsItemSelected(item);
    }

    public void test(View view) {
        gauge = (GaugeView) findViewById(R.id.gauge);
        Random rand = new Random();

        /*
        ViewGroup.LayoutParams lp = gauge.getLayoutParams();
        lp.height = rand.nextInt((500 - 400) +1) + 400;
        lp.width = lp.height;
        gauge.setLayoutParams(lp);
        */
        int randomInteger = rand.nextInt(10000);
        gauge.setValue(randomInteger);
        gauge.setLabelText("RPM");
    }

}