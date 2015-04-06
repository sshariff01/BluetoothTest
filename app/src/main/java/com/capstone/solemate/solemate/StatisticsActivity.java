package com.capstone.solemate.solemate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
            case R.id.view_trends:
                // Launch trends activity
                Intent myIntent = new Intent(StatisticsActivity.this, TrendsActivity.class);
                StatisticsActivity.this.startActivity(myIntent);
                return true;
            case R.id.reset_step_count:
                FeedbackActivity.numSteps = 0;
                FeedbackActivity.numStepsInterval = 0;
                FeedbackActivity.stepFreq = 0;
                FeedbackActivity.numStepsPeriod = 0;
                FeedbackActivity.stepsIntervalIndex = 0;
                FeedbackActivity.numStepsIntervalArray = new int[FeedbackActivity.PERIOD_SIZE];
                FeedbackActivity.baseTime = System.currentTimeMillis();
                stepCountText.setText(String.valueOf(FeedbackActivity.numSteps));
                stepFrequencyText.setText(String.valueOf(round(FeedbackActivity.stepFreq, 3)));
                return true;
//            case R.id.action_settings:
//                return true;
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

                        float totalPressure = FeedbackActivity.heelVal
                                + FeedbackActivity.leftVal
                                + FeedbackActivity.rightVal
                                + FeedbackActivity.toeVal;
                        float heelPressure = 0;
                        float leftPressure = 0;
                        float rightPressure = 0;
                        float toePressure = 0;

                        if (totalPressure > 0) {
                            heelPressure = (FeedbackActivity.heelVal / totalPressure) * 100;
                            leftPressure = (FeedbackActivity.leftVal / totalPressure) * 100;
                            rightPressure = (FeedbackActivity.rightVal / totalPressure) * 100;
                            toePressure = (FeedbackActivity.toeVal / totalPressure) * 100;
                        }

                        heelValText.setText(String.valueOf(round(heelPressure, 1)) + " %");
                        leftValText.setText(String.valueOf(round(leftPressure, 1)) + " %");
                        rightValText.setText(String.valueOf(round(rightPressure, 1)) + " %");
                        toeValText.setText(String.valueOf(round(toePressure, 1)) + " %");

                        if (FeedbackActivity.idleCount == 99) {
                            FeedbackActivity.idleCount = 0;
                            if(!(isFinishing())) {
                                new AlertDialog.Builder(StatisticsActivity.this)
                                        .setTitle("Let's Go for a Walk")
                                        .setMessage("You've been stationary for a while. It's important to keep your" +
                                                "blood circulating to stay healthy.")
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                FeedbackActivity.idleCount = 0;
                                            }
                                        })
                                        .show();
                            }
                        }

//                        heelValText.setText(String.valueOf(round(FeedbackActivity.heelVal, 2)));
//                        leftValText.setText(String.valueOf(round(FeedbackActivity.leftVal, 2)));
//                        rightValText.setText(String.valueOf(round(FeedbackActivity.rightVal, 2)));
//                        toeValText.setText(String.valueOf(round(FeedbackActivity.toeVal, 2)));

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
