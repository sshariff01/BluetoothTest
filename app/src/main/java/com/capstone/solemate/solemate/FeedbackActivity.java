package com.capstone.solemate.solemate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;


public class FeedbackActivity extends Activity {
    private ConnectedThread mConnectedThread;
    private static Handler mHandler;
    private static final int RECEIVE_MESSAGE = 1;
    private StringBuilder sb;

    private static final String OUTPUT_FILE_NAME = "testBTData.txt";

    // BT device connection attributes
    private BluetoothSocket btSocket;
    private static BluetoothDevice btDevice;
    private static boolean SOCKET_INSTREAM_ACTIVE = false;
    private static boolean SOCKET_CONNECTED = false;
    private static String hc05MacId = new String();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static TextView text;
    protected static TextView stepCount;

    protected static float HEEL_MODERATE = -1,
            LEFT_MODERATE = -1,
            RIGHTBRIDGE_MODERATE = -1,
            TOE_MODERATE = -1;
    private static final float SENSITIVITY_FACTOR = 13;

    protected static ImageView imageFootBase;
    protected static ImageView imageToeModerate,
            imageRightBridgeModerate,
            imageLeftSideModerate,
            imageHeelModerate;
    protected static ImageView
            imageToeHeavy,
            imageRightBridgeHeavy,
            imageLeftSideHeavy,
            imageHeelHeavy;
    protected static ImageView imageToeLight,
            imageRightBridgeLight,
            imageLeftSideLight,
            imageHeelLight;

    public static int pressureIndex_Heel = 1,
            pressureIndex_Left = 1,
            pressureIndex_RightBridge = 1,
            pressureIndex_Toe = 1;

    // Loading spinner
    public ProgressDialog connectingProgress, connectFailProgress, recalibratingProgress;

    // Init default bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    public static int index;

    public static int numSteps, prevNumSteps;
    public static boolean STEP_UP = true, STEP_DOWN = false;

    public static float heelVal = 0, leftVal = 0, rightVal = 0, toeVal = 0;
    private static float totalPressure = 0;

    public static long baseTime = System.currentTimeMillis(),
            currentTime = System.currentTimeMillis();
    public static int numStepsInterval, numStepsPeriod = 0;
    public static final int PERIOD_SIZE = 600;
    public static int stepsIntervalIndex = 0;
    public static int[] numStepsIntervalArray = new int[PERIOD_SIZE];
    public static boolean FIRST_PERIOD;
    public static float stepFreq;
    public static int idleCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        index = 0;

        RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
        mainLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.DEBUG_TEST_MODE) {
                    index = (index + 1) % 3;
//                } else {
//                    index = (index + 1) % 4;
                }
                Log.i("BT_TEST", "index val set to " + index);
            }
        });

        text = (TextView) findViewById(R.id.helloworld);

        Intent intent = getIntent();
        hc05MacId = intent.getStringExtra("hc05MacId");

        /*
         * Light GLOWS
         */
        imageToeLight = (ImageView) findViewById(R.id.toeLight);
        imageToeLight.setVisibility(View.GONE);
        imageRightBridgeLight = (ImageView) findViewById(R.id.rightBridgeLight);
        imageRightBridgeLight.setVisibility(View.GONE);
        imageLeftSideLight = (ImageView) findViewById(R.id.leftSideLight);
        imageLeftSideLight.setVisibility(View.GONE);
        imageHeelLight = (ImageView) findViewById(R.id.heelLight);
        imageHeelLight.setVisibility(View.GONE);

        /*
         * Heavy GLOWS
         */
        imageToeHeavy = (ImageView) findViewById(R.id.toeHeavy);
        imageToeHeavy.setVisibility(View.GONE);
        imageRightBridgeHeavy = (ImageView) findViewById(R.id.rightBridgeHeavy);
        imageRightBridgeHeavy.setVisibility(View.GONE);
        imageLeftSideHeavy = (ImageView) findViewById(R.id.leftSideHeavy);
        imageLeftSideHeavy.setVisibility(View.GONE);
        imageHeelHeavy = (ImageView) findViewById(R.id.heelHeavy);
        imageHeelHeavy.setVisibility(View.GONE);

        /*
         * Moderate GLOWS
         */
        imageToeModerate = (ImageView) findViewById(R.id.toeModerate);
        imageToeModerate.setVisibility(View.GONE);
        imageRightBridgeModerate = (ImageView) findViewById(R.id.rightBridgeModerate);
        imageRightBridgeModerate.setVisibility(View.GONE);
        imageLeftSideModerate = (ImageView) findViewById(R.id.leftSideModerate);
        imageLeftSideModerate.setVisibility(View.GONE);
        imageHeelModerate = (ImageView) findViewById(R.id.heelModerate);
        imageHeelModerate.setVisibility(View.GONE);

        /*
         * BASE FOOT IMAGE
         */
        imageFootBase = (ImageView) findViewById(R.id.footBase);

        numSteps = 0; prevNumSteps = 0;
        stepCount = (TextView) findViewById(R.id.stepsCountVal);
        stepCount.setText(String.valueOf(numSteps));

        // Init connect loading spinner
        connectingProgress = new ProgressDialog(this);
        connectingProgress.setTitle("Connecting");
        connectingProgress.setMessage("Please wait while we get in touch with your SoleMate...");

        // Init recalibrate loading spinner
        recalibratingProgress = new ProgressDialog(this);


        // Init recalibrate loading spinner
        connectFailProgress = new ProgressDialog(this);
        connectFailProgress.setTitle("Failed to Connect");
        connectFailProgress.setMessage("Long distance relationships are hard to maintain... " +
                "Please make sure your SoleMate is properly paired before trying again.");

        if (MainActivity.DEBUG_TEST_MODE) {
            new ImageFlipperTask().execute();
        } else {
            new ConnectToBtTask().execute();
        }

        numStepsInterval = numSteps;
        FIRST_PERIOD = true;

        stepFreq = 0;

        // Re-Calibrate moderate values
        new AlertDialog.Builder(FeedbackActivity.this)
                .setTitle("Calibration Instructions")
                .setMessage("Make sure you're standing relaxed in upright position. Straighten your back and look forward.\n\n" +
                        "Press \"OK\" when you're ready!")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        recalibratingProgress.setTitle("Let's Start Things Right");
                        recalibratingProgress.setMessage("Learning more about how you feel...");
                        recalibratingProgress.show();
                        HEEL_MODERATE = -1;
                        LEFT_MODERATE = -1;
                        RIGHTBRIDGE_MODERATE = -1;
                        TOE_MODERATE = -1;
                    }
                })
                .show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_about:
                new AlertDialog.Builder(FeedbackActivity.this)
                        .setTitle("Getting To Know You")
                        .setMessage(
                                Html.fromHtml(
                                        "Your SoleMate monitors the distribution of weight across your foot. " +
                                                "This screen shows the amount of weight you exert at certain pressure points.<br /><br />" +
                                                "<font color='#FF0000' size='7'>Red</font> means too much pressure (lighten up!)<br />" +
                                                "<font color='#0000FF' size='7'>Blue</font> means not enough pressure (put some back into it!)<br />" +
                                                "<font color='#00FF00' size='7'>Green</font> means just right.<br />"
                                )
                        )
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_statistics:
                // Launch statistics activity
                Intent myIntent = new Intent(FeedbackActivity.this, StatisticsActivity.class);
                FeedbackActivity.this.startActivity(myIntent);
                return true;
            case R.id.view_trends:
                // Launch trends activity
                Intent myIntent1 = new Intent(FeedbackActivity.this, TrendsActivity.class);
                FeedbackActivity.this.startActivity(myIntent1);
                return true;
            case R.id.reset_step_count:
                numSteps = 0; prevNumSteps = 0;
                numStepsInterval = 0;
                stepFreq = 0;
                numStepsPeriod = 0;
                stepsIntervalIndex = 0;
                numStepsIntervalArray = new int[PERIOD_SIZE];
                baseTime = System.currentTimeMillis();
                stepCount.setText(String.valueOf(numSteps));
                return true;
            case R.id.action_recalibrate:
                // Re-Calibrate moderate values
                new AlertDialog.Builder(FeedbackActivity.this)
                        .setTitle("Recalibration Instructions")
                        .setMessage("Make sure you're standing relaxed in upright position. Straighten your back and look forward.\n\n" +
                            "Press \"OK\" when you're ready!")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                recalibratingProgress.setTitle("It's Not You, It's Me :(");
                                recalibratingProgress.setMessage("Let's start over...");
                                recalibratingProgress.show();
                                HEEL_MODERATE = -1;
                                LEFT_MODERATE = -1;
                                RIGHTBRIDGE_MODERATE = -1;
                                TOE_MODERATE = -1;
                            }
                        })
                        .show();
//                recalibratingProgress.show();
//                HEEL_MODERATE = -1;
//                LEFT_MODERATE = -1;
//                RIGHTBRIDGE_MODERATE = -1;
//                TOE_MODERATE = -1;
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Go back to home screen
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /*
     * ASYNC TASKS AND OTHER THREADS
     */
    private class ImageFlipperTask extends AsyncTask<Void, Void, Void> {
        int value = index;
        boolean SHOW_IMAGES = true;
        ImageView[] toeImages = {imageToeLight, imageToeModerate, imageToeHeavy};
        ImageView[] rightBridgeImages = {imageRightBridgeLight, imageRightBridgeModerate, imageRightBridgeHeavy};
        ImageView[] leftSideImages = {imageLeftSideLight, imageLeftSideModerate, imageLeftSideHeavy};
        ImageView[] heelImages = {imageHeelLight, imageHeelModerate, imageHeelHeavy};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            while(true) {
                try {
                    if (MainActivity.DEBUG_TEST_MODE) {
                        value = index;
                        onProgressUpdate();
                        Thread.sleep(500);
                        value = 3;
                        onProgressUpdate();
                        Thread.sleep(500);
                        numSteps++;
                    } else {
                        SHOW_IMAGES = true;
                        onProgressUpdate();
                        Thread.sleep(500);
                        SHOW_IMAGES = false;
                        onProgressUpdate();
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

            }

        }

        @Override
        protected void onProgressUpdate(Void... unusedVoid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.DEBUG_TEST_MODE) {
                        stepCount.setText(String.valueOf(numSteps));
                        if (value == 0) {
                            toeImages[0].setVisibility(View.GONE);
                            rightBridgeImages[0].setVisibility(View.GONE);
                            leftSideImages[0].setVisibility(View.GONE);
                            heelImages[0].setVisibility(View.GONE);

                            toeImages[1].setVisibility(View.VISIBLE);
                            rightBridgeImages[1].setVisibility(View.VISIBLE);
                            leftSideImages[1].setVisibility(View.VISIBLE);
                            heelImages[1].setVisibility(View.VISIBLE);

                            toeImages[2].setVisibility(View.GONE);
                            rightBridgeImages[2].setVisibility(View.GONE);
                            leftSideImages[2].setVisibility(View.GONE);
                            heelImages[2].setVisibility(View.GONE);

//                            Log.i("BT_TEST", "Moderate set ON");
                        } else if (value == 1) {
                            toeImages[0].setVisibility(View.GONE);
                            rightBridgeImages[0].setVisibility(View.GONE);
                            leftSideImages[0].setVisibility(View.GONE);
                            heelImages[0].setVisibility(View.GONE);

                            toeImages[1].setVisibility(View.GONE);
                            rightBridgeImages[1].setVisibility(View.GONE);
                            leftSideImages[1].setVisibility(View.GONE);
                            heelImages[1].setVisibility(View.GONE);

                            toeImages[2].setVisibility(View.VISIBLE);
                            rightBridgeImages[2].setVisibility(View.VISIBLE);
                            leftSideImages[2].setVisibility(View.VISIBLE);
                            heelImages[2].setVisibility(View.VISIBLE);

//                            Log.i("BT_TEST", "Heavy set ON");
                        } else if (value == 2) {
                            toeImages[0].setVisibility(View.VISIBLE);
                            rightBridgeImages[0].setVisibility(View.VISIBLE);
                            leftSideImages[0].setVisibility(View.VISIBLE);
                            heelImages[0].setVisibility(View.VISIBLE);

                            toeImages[1].setVisibility(View.GONE);
                            rightBridgeImages[1].setVisibility(View.GONE);
                            leftSideImages[1].setVisibility(View.GONE);
                            heelImages[1].setVisibility(View.GONE);

                            toeImages[2].setVisibility(View.GONE);
                            rightBridgeImages[2].setVisibility(View.GONE);
                            leftSideImages[2].setVisibility(View.GONE);
                            heelImages[2].setVisibility(View.GONE);

//                            Log.i("BT_TEST", "Light set ON");
                        } else {
                            toeImages[0].setVisibility(View.GONE);
                            rightBridgeImages[0].setVisibility(View.GONE);
                            leftSideImages[0].setVisibility(View.GONE);
                            heelImages[0].setVisibility(View.GONE);

                            toeImages[1].setVisibility(View.GONE);
                            rightBridgeImages[1].setVisibility(View.GONE);
                            leftSideImages[1].setVisibility(View.GONE);
                            heelImages[1].setVisibility(View.GONE);

                            toeImages[2].setVisibility(View.GONE);
                            rightBridgeImages[2].setVisibility(View.GONE);
                            leftSideImages[2].setVisibility(View.GONE);
                            heelImages[2].setVisibility(View.GONE);

//                            Log.i("BT_TEST", "ALL set OFF");
                        }
                    } else {
                        toeImages[0].setVisibility(View.GONE);
                        toeImages[1].setVisibility(View.GONE);
                        toeImages[2].setVisibility(View.GONE);

                        rightBridgeImages[0].setVisibility(View.GONE);
                        rightBridgeImages[1].setVisibility(View.GONE);
                        rightBridgeImages[2].setVisibility(View.GONE);

                        leftSideImages[0].setVisibility(View.GONE);
                        leftSideImages[1].setVisibility(View.GONE);
                        leftSideImages[2].setVisibility(View.GONE);

                        heelImages[0].setVisibility(View.GONE);
                        heelImages[1].setVisibility(View.GONE);
                        heelImages[2].setVisibility(View.GONE);

                        if (SHOW_IMAGES) {
                            heelImages[pressureIndex_Heel].setVisibility(View.VISIBLE);
                            leftSideImages[pressureIndex_Left].setVisibility(View.VISIBLE);
                            rightBridgeImages[pressureIndex_RightBridge].setVisibility(View.VISIBLE);
                            toeImages[pressureIndex_Toe].setVisibility(View.VISIBLE);
                        }

                    }
                }
            });

            // Check if 3 secs have passed
            currentTime = System.currentTimeMillis();

            if ((currentTime-baseTime) >= 3000) {
                if (prevNumSteps == numSteps) {
                    idleCount++;
                } else {
                    idleCount = 0;
                }
                prevNumSteps = numSteps;
                baseTime = currentTime;
                numStepsPeriod = numStepsPeriod - numStepsIntervalArray[stepsIntervalIndex];
                numStepsIntervalArray[stepsIntervalIndex] = numSteps - numStepsInterval;
                numStepsPeriod = numStepsPeriod + numStepsIntervalArray[stepsIntervalIndex];
                stepsIntervalIndex = ++stepsIntervalIndex % PERIOD_SIZE;
                numStepsInterval = numSteps;

                if (!FIRST_PERIOD) {
                    stepFreq = numStepsPeriod / (float) (PERIOD_SIZE/20);
                } else {
                    if (stepsIntervalIndex < numStepsIntervalArray.length-1) {
                        int numMinutesPassed = 0;
                        for (int i = stepsIntervalIndex; i > 0; i=i-20) {
                            ++numMinutesPassed;
                        }
                        stepFreq = numStepsPeriod / (float) numMinutesPassed;
                    } else {
                        FIRST_PERIOD = false;
                    }
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (idleCount == 99) {
                        idleCount = 0;
                        if(!(isFinishing())) {
                            new AlertDialog.Builder(FeedbackActivity.this)
                                    .setTitle("Let's Go for a Walk")
                                    .setMessage("You've been stationary for a while. It's important to keep your" +
                                            "blood circulating to stay healthy.")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            idleCount = 0;
                                        }
                                    })
                                    .show();
                        }

                    }
                }
            });

            if (recalibratingProgress.isShowing()) {
                if (!MainActivity.DEBUG_TEST_MODE) {
                    if (
                            HEEL_MODERATE != -1
                                    && LEFT_MODERATE != -1
                                    && RIGHTBRIDGE_MODERATE != -1
                                    && TOE_MODERATE != -1
                            ) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recalibratingProgress.dismiss();
                            }
                        });
                    }
                } else {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recalibratingProgress.dismiss();
                        }
                    });
                }
            }

        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);
        }
    }

    private class ConnectToBtTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            connectingProgress.show();

            // Close discovery
            if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();

            // Initialize the remote device's address
            if (!hc05MacId.isEmpty()) btDevice = mBluetoothAdapter.getRemoteDevice(hc05MacId);
        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            // Create socket
            try {
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                Log.i("BT_TEST: SUCCESS", "Created socket...");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to create socket");
            }

            // Connect to remote device
            try {
                btSocket.connect();
                SOCKET_CONNECTED = true;
                Log.i("BT_TEST: SUCCESS", "Connected to socket...");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to connect to socket. Closing socket...");
                try {
                    btSocket.close();
                } catch (IOException ioe2) {
                    ioe2.printStackTrace();
                    Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
                }
            }

            onProgressUpdate();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... unusedVoid) {
            connectingProgress.dismiss();

            if (SOCKET_CONNECTED) {
                new ImageFlipperTask().execute();
//            } else {
//
//                try {
//                    Thread.sleep(1300);
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                }
            }
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);
//            SOCKET_CONNECTED = false;
            if (SOCKET_CONNECTED) {
                // Create data stream to talk to device
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
            } else {
                Log.i("BT_TEST: FAIL", "Failed to connect to HC-05 Bluetooth socket");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        connectFailProgress.show();
                        new AlertDialog.Builder(FeedbackActivity.this)
                                .setTitle("Failed to Connect")
                                .setMessage("Long distance relationships are hard to maintain... \n\n" +
                                        "Please make sure your SoleMate is turned on before trying again.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                });

//                connectFailProgress.dismiss();

//                finish();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream inStream;

//        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();

                if (tmpIn != null) SOCKET_INSTREAM_ACTIVE = true;

                mHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        if (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                            switch (msg.what) {
                                case RECEIVE_MESSAGE:
                                    byte[] readByte = (byte[]) msg.obj;
                                    int identifier = (readByte[0] & 0xC0) >> 6;
                                    int adcReading = readByte[0] & 0x3F; // 0x3F = 0011 1111

                                    totalPressure = heelVal + leftVal + rightVal + toeVal;

                                    switch (identifier) {
                                        case 0:

                                            if (HEEL_MODERATE == -1) {
                                                Log.i("NEW_VALUE:", "Setting heelVal...");
                                                HEEL_MODERATE = adcReading;
                                                heelVal = adcReading;
                                                pressureIndex_Heel = 1;
                                            } else {
                                                heelVal = adcReading;
                                                if (totalPressure > 0) {
                                                    if ((heelVal/totalPressure)*100 > ((HEEL_MODERATE / totalPressure)*100 + SENSITIVITY_FACTOR / 2)) {
                                                        pressureIndex_Heel = 2;
                                                    } else if ((heelVal/totalPressure)*100 < ((HEEL_MODERATE / totalPressure)*100 - SENSITIVITY_FACTOR / 2)) {
                                                        pressureIndex_Heel = 0;
                                                    } else {
                                                        pressureIndex_Heel = 1;
                                                    }
                                                } else {
//                                                    Log.i("FATAL:", "totalPressure is equal to 0...");
//                                                    HEEL_MODERATE = -1;
//                                                    heelVal = 0;
//                                                    pressureIndex_Heel = 1;
//
//                                                    LEFT_MODERATE = -1;
//                                                    RIGHTBRIDGE_MODERATE = -1;
//                                                    TOE_MODERATE = -1;
//
                                                }
                                            }


                                            if (isExternalStorageWritable()) {
                                                writeToSD("HEEL: " + adcReading + "\n");
                                            }

                                            break;

                                        case 1:
                                            if (RIGHTBRIDGE_MODERATE == -1) {
                                                Log.i("NEW_VALUE:", "Setting rightBridge...");
                                                RIGHTBRIDGE_MODERATE = adcReading;
                                                rightVal = adcReading;
                                                pressureIndex_RightBridge = 1;
                                            } else {
                                                rightVal = adcReading;
                                                if (totalPressure > 0) {
                                                    if ((rightVal/totalPressure)*100 > ((RIGHTBRIDGE_MODERATE / totalPressure)*100 + SENSITIVITY_FACTOR)) {
                                                        pressureIndex_RightBridge = 2;
                                                    } else if ((rightVal/totalPressure)*100 < ((RIGHTBRIDGE_MODERATE / totalPressure)*100 - SENSITIVITY_FACTOR)) {
                                                        pressureIndex_RightBridge = 0;
                                                    } else {
                                                        pressureIndex_RightBridge = 1;
                                                    }
                                                } else {
//                                                    Log.i("FATAL:", "totalPressure is equal to 0...");
//                                                    HEEL_MODERATE = -1;
//                                                    LEFT_MODERATE = -1;
//                                                    RIGHTBRIDGE_MODERATE = -1;
//                                                    TOE_MODERATE = -1;
//                                                    rightVal = 0;
//                                                    pressureIndex_RightBridge = 1;
//
                                                }
                                            }

                                            if (isExternalStorageWritable()) {
                                                writeToSD("RIGHT: " + adcReading + "\n");
                                            }

                                            stepDetect(adcReading);

                                            break;

                                        case 2:
                                            if (TOE_MODERATE == -1) {
                                                Log.i("NEW_VALUE:", "Setting toeVal...");
                                                TOE_MODERATE = adcReading;
                                                toeVal = adcReading;
                                                pressureIndex_Toe = 1;
                                            } else {
                                                toeVal = adcReading;
                                                if (totalPressure > 0) {
                                                    if ((toeVal/totalPressure)*100 > ((TOE_MODERATE / totalPressure)*100 + SENSITIVITY_FACTOR)) {
                                                        pressureIndex_Toe = 2;
                                                    } else if ((toeVal/totalPressure)*100 < ((TOE_MODERATE / totalPressure)*100 - SENSITIVITY_FACTOR)) {
                                                        pressureIndex_Toe = 0;
                                                    } else {
                                                        pressureIndex_Toe = 1;
                                                    }
                                                } else {
//                                                    Log.i("FATAL:", "totalPressure is equal to 0...");
//                                                    HEEL_MODERATE = -1;
//                                                    LEFT_MODERATE = -1;
//                                                    RIGHTBRIDGE_MODERATE = -1;
//                                                    TOE_MODERATE = -1;
//                                                    toeVal = 0;
//                                                    pressureIndex_Toe = 1;
//
                                                }
                                            }

                                            if (isExternalStorageWritable()) {
                                                writeToSD("TOE: " + adcReading + "\n");
                                            }

                                            break;

                                        case 3:
                                            if (LEFT_MODERATE == -1) {
                                                Log.i("NEW_VALUE:", "Setting leftVal...");
                                                LEFT_MODERATE = adcReading;
                                                leftVal = adcReading;
                                                pressureIndex_Left = 1;
                                            } else {
                                                leftVal = adcReading;
                                                if (totalPressure > 0) {
                                                    if ((leftVal/totalPressure)*100 > ((LEFT_MODERATE / totalPressure)*100 + SENSITIVITY_FACTOR)) {
                                                        pressureIndex_Left = 2;
                                                    } else if ((leftVal/totalPressure)*100 < ((LEFT_MODERATE / totalPressure)*100 - SENSITIVITY_FACTOR)) {
                                                        pressureIndex_Left = 0;
                                                    } else {
                                                        pressureIndex_Left = 1;
                                                    }
                                                } else {
//                                                    Log.i("FATAL:", "totalPressure is equal to 0...");
//                                                    HEEL_MODERATE = -1;
//                                                    LEFT_MODERATE = -1;
//                                                    RIGHTBRIDGE_MODERATE = -1;
//                                                    TOE_MODERATE = -1;
//                                                    leftVal = 0;
//                                                    pressureIndex_Left = 1;
//
                                                }
                                            }

                                            if (isExternalStorageWritable()) {
                                                writeToSD("LEFT: " + adcReading + "\n");
                                            }

                                            break;

                                    }

                                    break;
                            }
                        }
                        ;
                    }
                };
//                tmpOut = socket.getOutputStream();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to get input stream from socket");
            }

            inStream = tmpIn;
//            outStream = tmpOut;
        }

        public void run() {
            Log.i("BT_TEST", "ConnectedThread running (receiving data) ...");
            byte[] buffer = new byte[1];
            int bytes;

            while (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                try {
                    bytes = inStream.read(buffer);
                    if (
                            HEEL_MODERATE != -1
                                    && LEFT_MODERATE != -1
                                    && RIGHTBRIDGE_MODERATE != -1
                                    && TOE_MODERATE != -1
                            ) {
                        totalPressure = heelVal + leftVal + rightVal + toeVal;
                    } else {
                        totalPressure = 0;
                    }
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();


                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Log.i("BT_TEST: FATAL ERROR", "Failed to read data. Closing btSocket...");
                    try {
                        SOCKET_INSTREAM_ACTIVE = false; SOCKET_CONNECTED = false;
                        btSocket.close();
                    } catch (IOException ioe2) {
                        ioe2.printStackTrace();
                        Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
                    }
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(FeedbackActivity.this)
                            .setTitle("Lost Connection")
                            .setMessage("Long distance relationships are hard to maintain... \n\n" +
                                    "Please make sure your SoleMate is turned on before reconnecting.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                }
            });

            }
        }

    /*
     * HELPER METHODS
     */
    private void stepDetect(int adcReading) {
        /*
        * STEP COUNTER ALGORITHM
        */
        if (!STEP_DOWN && STEP_UP) {
            if (
                    (adcReading > RIGHTBRIDGE_MODERATE + SENSITIVITY_FACTOR)
                    ) {
                STEP_DOWN = true;
                STEP_UP = false;
                numSteps++;
                stepCount.setText(String.valueOf(numSteps));
                Log.i("BT_TEST", "Number of Steps Taken: " + numSteps);
            }
        } else {
            STEP_DOWN = false;
        }
        if (
                (adcReading >= 0)
                        && (adcReading < 15)
                ) {
            STEP_UP = true;
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) & MainActivity.WRITE_ENABLE_OPTION) return true;
        return false;
    }

    private void writeToSD(String readMessage) {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/debug");
        dir.mkdirs();
        File file = new File(dir, OUTPUT_FILE_NAME);

        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fos);
            pw.print(readMessage);
            pw.flush();
            pw.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }


}
