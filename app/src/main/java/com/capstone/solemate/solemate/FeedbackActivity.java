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

    // Loading spinner
    public ProgressDialog progress;

    // Init default bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        text = (TextView) findViewById(R.id.helloworld);

        Intent intent = getIntent();
        hc05MacId = intent.getStringExtra("hc05MacId");

        // Init loading spinner
        progress = new ProgressDialog(this);
        progress.setTitle("Connecting");
        progress.setMessage("Please wait while we get in touch with your SoleMate...");
        progress.show();

        new ConnectToBtTask().execute();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    /*
     * ASYNC TASKS AND OTHER THREADS
     */
    private class ConnectToBtTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

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
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to create socket");
            }

            // Connect to remote device
            try {
                btSocket.connect();
                SOCKET_CONNECTED = true;
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
            progress.dismiss();
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

                if (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                    mHandler = new Handler() {
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                                case RECEIVE_MESSAGE:
                                    byte[] readBuf = (byte[]) msg.obj;
                                    String readMessage = new String(readBuf, 0, msg.arg1);
                                    String newReadMessage = new String();

//                                    readMessage = readMessage.replaceAll("\\s+","");

                                    if (isExternalStorageWritable()) {
                                        writeToSD("\nSTART\n" + readMessage + "\nEND\n");
                                    }


//                                    if (readMessage.contains("SHL0")) {
//                                        readMessage = readMessage.replaceAll("\\s+","");
//
//                                        if (isExternalStorageWritable()) {
//                                            writeToSD("\nREADMESSAGE_START\n" + readMessage + "\nEND\n");
//                                        }
//
//                                        if (readMessage.length() > (readMessage.indexOf("SHL0") + 50)) {
//                                            newReadMessage = readMessage.substring(
//                                                    readMessage.indexOf("SHL0"),
//                                                    readMessage.indexOf("SHL0") + 50
//                                            );

                                            try {
                                                text.setText(readMessage);
                                            } catch (StringIndexOutOfBoundsException e) {
                                                sb = new StringBuilder();
                                                sb = sb.append(readMessage);
                                                Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
                                                e.printStackTrace();
                                            }

//                                        }
//                                    sb = sb.delete(0, sb.length()-1);

//                                    }


                                    break;
                            }
                        };
                    };
                }
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
//                    if (isExternalStorageWritable()) writeToSD("\nINSTREAM_START\n" + inStream.toString() + "\nEND\n");
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Log.i("BT_TEST: FATAL ERROR", "Failed to read data. Closing btSocket...");
                    try {
                        btSocket.close();
                    } catch (IOException ioe2) {
                        ioe2.printStackTrace();
                        Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
                    }
                }
            }
        }
    }
}
