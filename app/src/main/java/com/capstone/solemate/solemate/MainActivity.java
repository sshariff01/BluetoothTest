package com.capstone.solemate.solemate;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity implements OnClickListener {
    private final static int REQUEST_ENABLE_BT = 1;

    // List to pass to list view array adapter
    private List<String> discoveredDevices = new ArrayList<String>();

    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private static final int RECEIVE_MESSAGE = 1;
    private StringBuffer sb = new StringBuffer();
    private static boolean SOCKET_INSTREAM_ACTIVE = false;
    private static boolean SOCKET_CONNECTED = false;

    private static boolean WRITE_ENABLE_OPTION = true;
    private static final String OUTPUT_FILE_NAME = "testBTData.txt";

    // BT device connection attributes
    private BluetoothSocket btSocket;
    private static BluetoothDevice btDevice;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String hc05MacId = new String();

    // Init default bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.i ("BT_TEST: DEVICE FOUND", "DEVICE NAME: " + device.getName());
                Log.i ("BT_TEST: DEVICE FOUND", "DEVICE ADDRESS: " + device.getAddress());
                if (!discoveredDevices.contains(device.getName() + "\n" + device.getAddress())) {
                    discoveredDevices.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    };

    Button startDiscoveryButton;
    ListPopupWindow listPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDiscoveryButton();
        initPopup();

        /*
         * ENABLE BLUETOOTH

        final Button enableBTButton = (Button) findViewById(R.id.enableBT);
        enableBTButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if (mBluetoothAdapter == null) {
                    Log.i ("BT_TEST_DEBUG", "Device does not support Bluetooth");
                } else if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });
        */

        final TextView text = (TextView) findViewById(R.id.text);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String readMessage = new String(readBuf, 0, msg.arg1);

                        // Send to text file for now sdcard/debug/testBTData.txt
                        if (isExternalStorageWritable()) writeToSD(readMessage);

                        sb.append(readMessage);

                        try {
                            int startIndex = sb.indexOf("Analog1 reading =") + "Analog1 reading =".length() + 1;
                            int endIndex = sb.substring(startIndex).indexOf("\n");
                            if (startIndex > 0 & endIndex > 0) {
                                String value = sb.substring(startIndex, startIndex + endIndex);

                                text.setText("Value from BT module: " + value);
                            }
                        } catch (StringIndexOutOfBoundsException e) {
                            Log.i("BT_TEST: EXCEPTION ENCOUNTERED PARSING DATA", sb.toString());
                            e.printStackTrace();

                        }

                        sb.delete(0, sb.length());
                        break;
                }
            };
        };
    }

    protected void initDiscoveryButton() {
        startDiscoveryButton = (Button) findViewById(R.id.startDiscovery);
        startDiscoveryButton.setOnClickListener(this);
    }

    public void initPopup() {
        listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(new ArrayAdapter(this, R.layout.device_list_item, discoveredDevices));
        listPopupWindow.setAnchorView(findViewById(R.id.frameLayout));
        listPopupWindow.setHeight(1000);
        listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setVerticalOffset(-80);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO: CONNECT TO DEVICE HERE!
//                listPopupWindow.getSelectedItem().
                // MAC ID for HC-05 module: 98:D3:31:40:20:D9
                hc05MacId = "98:D3:31:40:20:D9";
                if (mBluetoothAdapter == null) {
                    Log.i("BT_TEST: FATAL ERROR", "Bluetooth adapter is null!");
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.i("BT_TEST", "Bluetooth is not enabled!");
                    } else {
                        // Connect to BT in async task thread
                        new ConnectToBtTask().execute();
                    }
                }
            }
        });

    }

    public void onClick(View v) {
        if (v.getId() == R.id.startDiscovery) {
            /*
             * ENABLE BLUETOOTH ADAPTER
             */

            // Perform action on click
            if (mBluetoothAdapter == null) {
                Log.i ("BT_TEST_DEBUG", "Device does not support Bluetooth");
            } else if (!mBluetoothAdapter.isEnabled()) {
                /*
                 * Begin AsyncTask to enable BT
                 */
                new EnableBtTask().execute();
            } else {
                /*
                 * Begin AsyncTask to start discovery
                 */
                if (mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering()) {
                    new DiscoveryTask().execute();
                } else {
                    if (mBluetoothAdapter.isDiscovering()) {
                        listPopupWindow.show();
                        Log.i("BT_TEST", "Bluetooth is already discovering!");
                    } else if (!mBluetoothAdapter.isEnabled()) {
                        Log.i("BT_TEST", "Bluetooth is not enabled!");
                    }
                }
            }

        } else {
            listPopupWindow.dismiss();
        }
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
            pw.println(readMessage);
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
    private class EnableBtTask extends AsyncTask<Void, Void, Void> {
        protected boolean START_DISCOVERY = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            int count = 0;
            while (!START_DISCOVERY && count < 3) {
                if (mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering()) {
                    START_DISCOVERY = true;
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.i("BT_TEST", "Bluetooth is not yet enabled...");
                    }

                    // Put this thread to sleep for 0.5s
                    try {
                        Thread.sleep(3000);
                        Log.i("BT_TEST", "EnableBtTask sleeping for 3000ms...");
                        count++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            if (START_DISCOVERY && !mBluetoothAdapter.isDiscovering()) {
                new DiscoveryTask().execute();
            }

        }
    }

    private class DiscoveryTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

            listPopupWindow.show();
        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            if (mBluetoothAdapter.isEnabled()) {
                boolean isDiscovering = mBluetoothAdapter.startDiscovery();
                while (isDiscovering) {
                    try {
                        Thread.sleep(2500);
                        isDiscovering = mBluetoothAdapter.isDiscovering();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            mBluetoothAdapter.cancelDiscovery();
        }
    }

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

            return null;
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            // Create data stream to talk to device
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
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
            byte[] buffer = new byte[256];
            int bytes;

            while (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                try {
                    bytes = inStream.read(buffer);
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
