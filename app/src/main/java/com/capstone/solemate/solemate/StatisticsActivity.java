package com.capstone.solemate.solemate;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class StatisticsActivity extends Activity {
    private StepCountThread mStepCountThread;
    protected static TextView stepCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Up navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_statistics);

        stepCountText = (TextView) findViewById(R.id.stepsTaken);

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
                    }
                });
            }
        }
    }
}
