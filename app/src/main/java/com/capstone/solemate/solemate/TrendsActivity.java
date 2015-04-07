package com.capstone.solemate.solemate;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;


public class TrendsActivity extends Activity {
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private LineGraphSeries<DataPoint> mSeries1, mSeries2;
    private double graphLastXValue = 0;
    private static final int NUM_DATA_POINTS = 75;
    private static GraphView graph;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Up navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_trends);

        mSeries1 = new LineGraphSeries<DataPoint>();
        mSeries1.setTitle("Heel");
        mSeries1.setColor(Color.BLUE);
        mSeries2 = new LineGraphSeries<DataPoint>();
        mSeries2.setTitle("Toe");
        mSeries2.setColor(Color.GREEN);

        graph = (GraphView) findViewById(R.id.graph);
        graph.setTitle("Pressure Distribution Over Past 15s");
        graph.addSeries(mSeries1);
        graph.addSeries(mSeries2);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(NUM_DATA_POINTS);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(64);
        graph.getViewport().setScrollable(false);
        graph.getViewport().setScalable(false);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time(s)");
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mTimer = new Runnable() {
            @Override
            public void run() {
                mSeries1.appendData(new DataPoint(graphLastXValue, FeedbackActivity.heelVal), true, NUM_DATA_POINTS);
                mSeries2.appendData(new DataPoint(graphLastXValue, FeedbackActivity.toeVal), true, NUM_DATA_POINTS);
                graphLastXValue++;
                mHandler.postDelayed(this, 200);
            }
        };

        mHandler.postDelayed(mTimer, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Disable menu on this activity
//        getMenuInflater().inflate(R.menu.menu_trends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    double mLastRandom = 2;
    Random mRand = new Random();
    private double getRandom() {
        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
    }

}
