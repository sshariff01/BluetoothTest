package com.capstone.solemate.solemate;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.math.BigDecimal;


public class StatisticsActivity extends Activity {
    private StepCountThread mStepCountThread;
    protected static TextView stepCountText, stepFrequencyText;
    protected static TextView heelValText, leftValText, rightValText, toeValText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Up navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_statistics);

        Drawable statisticsRLayout = findViewById(R.id.statisticsRLayout).getBackground();
        statisticsRLayout.setAlpha(70);

        stepCountText = (TextView) findViewById(R.id.stepsTaken);
        stepFrequencyText = (TextView) findViewById(R.id.stepsFrequency);

        heelValText = (TextView) findViewById(R.id.heelVal);
        leftValText = (TextView) findViewById(R.id.leftVal);
        rightValText = (TextView) findViewById(R.id.rightVal);
        toeValText = (TextView) findViewById(R.id.toeVal);

        // Create thread to update step count
        mStepCountThread = new StepCountThread();
        mStepCountThread.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * ASYNC TASKS AND OTHER THREADS
     */
    private class StepCountThread extends Thread {
        public StepCountThread() { }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stepCountText.setText(String.valueOf(FeedbackActivity.numSteps));
                        stepFrequencyText.setText(String.valueOf(round(FeedbackActivity.stepFreq, 3)));

                        float heelPressure = FeedbackActivity.heelVal / (float) FeedbackActivity.MAX_PRESSURE_VAL;
                        float leftPressure = FeedbackActivity.leftVal / (float) FeedbackActivity.MAX_PRESSURE_VAL;
                        float rightPressure = FeedbackActivity.rightVal / (float) FeedbackActivity.MAX_PRESSURE_VAL;
                        float toePressure = FeedbackActivity.toeVal / (float) FeedbackActivity.MAX_PRESSURE_VAL;

                        heelValText.setText(String.valueOf(round(heelPressure, 1)) + " %");
                        leftValText.setText(String.valueOf(round(leftPressure, 1)) + " %");
                        rightValText.setText(String.valueOf(round(rightPressure, 1)) + " %");
                        toeValText.setText(String.valueOf(round(toePressure, 1)) + " %");
                    }
                });

            }
        }
    }

    /*
     * HELPER METHODS
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}
