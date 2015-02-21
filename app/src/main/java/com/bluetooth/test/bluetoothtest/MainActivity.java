package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnClickListener {
    private final static int REQUEST_ENABLE_BT = 1;
    private List<String> discoveredDevices = new ArrayList<String>();
    protected final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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

    LinearLayout layoutOfPopup;
    TextView popupText;
    Button startDiscoveryButton;
    Button insidePopupButton;
    ListPopupWindow listPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        popupInit();

        /*
         * ENABLE BLUETOOTH
         */
        final Button enableBTButton = (Button) findViewById(R.id.enableBT);
        enableBTButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if (mBluetoothAdapter == null) {
                    // Device does not support Bluetooth
                    Log.i ("BT_TEST_DEBUG", "Device does not support Bluetooth");
                } else if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });
    }

    protected void init() {
        startDiscoveryButton = (Button) findViewById(R.id.startDiscovery);
    }

    public void popupInit() {
        startDiscoveryButton.setOnClickListener(this);
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
            }
        });


    }


    public void onClick(View v) {
        if (v.getId() == R.id.startDiscovery) {
            /*
             * START DISCOVERY
             */
            listPopupWindow.show();
            if (mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering()) {
                new DiscoveryTask().execute();
            } else {
                if (mBluetoothAdapter.isDiscovering()) {
                    Log.i("BT_TEST", "Bluetooth is already discovering!");
                } else if (!mBluetoothAdapter.isEnabled()) {
                    Log.i("BT_TEST", "Bluetooth is not enabled!");
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

    private class DiscoveryTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
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

//        @Override
//        protected void onProgressUpdate(Void... values) {
//            super.onProgressUpdate(values);
//        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            mBluetoothAdapter.cancelDiscovery();
        }
    }
}
