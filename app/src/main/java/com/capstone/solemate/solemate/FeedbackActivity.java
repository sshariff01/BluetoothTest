package com.capstone.solemate.solemate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

    private static boolean WRITE_ENABLE_OPTION = true;
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

    protected static int HEEL_MODERATE = -1,
            LEFT_MODERATE = -1,
            RIGHTBRIDGE_MODERATE = -1,
            TOE_MODERATE = -1;

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

    public static int numSteps;
    public static boolean STEP_UP = true, STEP_DOWN = false;
    protected static int MAX_PRESSURE_VAL = 64;

    public static int heelVal = 0, leftVal = 0, rightVal = 0, toeVal = 0;

    private static long baseTime = System.currentTimeMillis(),
            currentTime = System.currentTimeMillis();
    private static int numStepsInterval, numStepsPeriod = 0;
    private static final int PERIOD_SIZE = 600;
    private static int stepsIntervalIndex = 0;
    private static int[] numStepsIntervalArray = new int[PERIOD_SIZE];
    private static boolean FIRST_PERIOD;
    public static float stepFreq;


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

        numSteps = 0;
        stepCount = (TextView) findViewById(R.id.stepsCountVal);
        stepCount.setText(String.valueOf(numSteps));

        // Init connect loading spinner
        connectingProgress = new ProgressDialog(this);
        connectingProgress.setTitle("Connecting");
        connectingProgress.setMessage("Please wait while we get in touch with your SoleMate...");

        // Init recalibrate loading spinner
        recalibratingProgress = new ProgressDialog(this);
        recalibratingProgress.setTitle("Recalibrating");
        recalibratingProgress.setMessage("Let's start over...");

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
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_statistics:
                // Launch statistics activity
                Intent myIntent = new Intent(FeedbackActivity.this, StatisticsActivity.class);
                FeedbackActivity.this.startActivity(myIntent);
                return true;
            case R.id.action_recalibrate:
                // Re-Calibrate moderate values
                recalibratingProgress.show();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                HEEL_MODERATE = -1;
                LEFT_MODERATE = -1;
                RIGHTBRIDGE_MODERATE = -1;
                TOE_MODERATE = -1;

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

                        // Check if one minute has passed
                        currentTime = System.currentTimeMillis();

                        if ((currentTime-baseTime) >= 3000) {
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
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectFailProgress.show();
                    }
                });
                try {
                    Thread.sleep(1300);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
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

                connectFailProgress.dismiss();

                finish();
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

//                                    char readMessage = new String(readByte, 0, msg.arg1).toCharArray()[0];
//                                    int readMessage = readByte[0];

                                    switch (identifier) {
                                        case 0:
                                            if (HEEL_MODERATE == -1) {
                                                HEEL_MODERATE = adcReading;
                                            }

                                            if (adcReading > (HEEL_MODERATE + 10)) {
                                                pressureIndex_Heel = 2;
                                            } else if (adcReading < (HEEL_MODERATE - 10)) {
                                                pressureIndex_Heel = 0;
                                            } else {
                                                pressureIndex_Heel = 1;
                                            }
                                            heelVal = adcReading;

//                                            if (isExternalStorageWritable()) {
//                                                writeToSD("HEEL: " + adcReading + "\n");
//                                            }
//                                            try {
//                                                text.setText("HEEL: " + String.valueOf(adcReading) + "\n");
//                                            } catch (Exception e) {
//                                                sb = new StringBuilder();
//                                                sb = sb.append(adcReading);
//                                                sb = sb.delete(0, sb.length()-1);
//                                                Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
//                                                e.printStackTrace();
//                                            }

                                            break;

                                        case 1:
                                            if (RIGHTBRIDGE_MODERATE == -1) {
                                                RIGHTBRIDGE_MODERATE = adcReading;
                                            }

                                            if (adcReading > (RIGHTBRIDGE_MODERATE + 10))
                                                pressureIndex_RightBridge = 2;
                                            else if (adcReading < (RIGHTBRIDGE_MODERATE - 10))
                                                pressureIndex_RightBridge = 0;
                                            else pressureIndex_RightBridge = 1;

                                            rightVal = adcReading;

//                                            if (isExternalStorageWritable()) {
//                                                writeToSD("RIGHT: " + adcReading + "\n");
//                                            }
//                                            try {
//                                                text.setText("RIGHT: " + String.valueOf(adcReading) + "\n");
//                                            } catch (Exception e) {
//                                                sb = new StringBuilder();
//                                                sb = sb.append(adcReading);
//                                                sb = sb.delete(0, sb.length()-1);
//                                                Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
//                                                e.printStackTrace();
//                                            }
                                        /*
                                        * STEP COUNTER ALGORITHM
                                        */
                                            if (!STEP_DOWN && STEP_UP) {
                                                if (
                                                        (adcReading > MAX_PRESSURE_VAL - 15)
                                                                && (adcReading <= MAX_PRESSURE_VAL)
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

                                            break;

                                        case 2:
                                            if (TOE_MODERATE == -1) {
                                                TOE_MODERATE = adcReading;
                                            }

                                            if (adcReading > (TOE_MODERATE + 10))
                                                pressureIndex_Toe = 2;
                                            else if (adcReading < (TOE_MODERATE - 10))
                                                pressureIndex_Toe = 0;
                                            else pressureIndex_Toe = 1;

                                            toeVal = adcReading;

//                                            if (isExternalStorageWritable()) {
//                                                writeToSD("TOE: " + adcReading + "\n");
//                                            }
//                                            try {
//                                                text.setText("TOE: " + String.valueOf(adcReading) + "\n");
//                                            } catch (Exception e) {
//                                                sb = new StringBuilder();
//                                                sb = sb.append(adcReading);
//                                                sb = sb.delete(0, sb.length()-1);
//                                                Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
//                                                e.printStackTrace();
//                                            }

                                            break;

                                        case 3:
                                            if (LEFT_MODERATE == -1) {
                                                LEFT_MODERATE = adcReading;
                                            }

                                            if (adcReading > (LEFT_MODERATE + 10))
                                                pressureIndex_Left = 2;
                                            else if (adcReading < (LEFT_MODERATE - 10))
                                                pressureIndex_Left = 0;
                                            else pressureIndex_Left = 1;

                                            leftVal = adcReading;

//                                            if (isExternalStorageWritable()) {
//                                                writeToSD("LEFT: " + adcReading + "\n");
//                                            }
//                                            try {
//                                                text.setText("LEFT: " + String.valueOf(adcReading) + "\n");
//                                            } catch (Exception e) {
//                                                sb = new StringBuilder();
//                                                sb = sb.append(adcReading);
//                                                sb = sb.delete(0, sb.length()-1);
//                                                Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
//                                                e.printStackTrace();
//                                            }

                                            break;
                                    }

                                    if (
                                            HEEL_MODERATE != -1
                                                    && LEFT_MODERATE != -1
                                                    && RIGHTBRIDGE_MODERATE != -1
                                                    && TOE_MODERATE != -1
                                            ) {
                                        recalibratingProgress.dismiss();
                                    }

                                    break;
                            }
                        }
                        ;
                    }
                };
//                tmpOut = socket.getOutputStream();


                // Check if one minute has passed
                currentTime = System.currentTimeMillis();

                if ((currentTime-baseTime) >= 3000) {
                    baseTime = currentTime;
                    numStepsPeriod = numStepsPeriod - numStepsIntervalArray[stepsIntervalIndex];
                    numStepsIntervalArray[stepsIntervalIndex] = numSteps - numStepsInterval;
                    numStepsPeriod = numStepsPeriod + numStepsIntervalArray[stepsIntervalIndex];
                    stepsIntervalIndex = ++stepsIntervalIndex % PERIOD_SIZE;
                    numStepsInterval = numSteps;

                    if (!FIRST_PERIOD) {
                        stepFreq = numStepsPeriod / (float) PERIOD_SIZE;
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
        }
    }

    /*
     * HELPER METHODS
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) & WRITE_ENABLE_OPTION) return true;
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
