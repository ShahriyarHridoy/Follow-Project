package com.examples.akshay.bluetoothfiletransfer.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.examples.akshay.bluetoothfiletransfer.adapters.BluetoothDeviceAdapter;
import com.examples.akshay.bluetoothfiletransfer.Tasks.ConnectTask;
import com.examples.akshay.bluetoothfiletransfer.R;
import com.examples.akshay.bluetoothfiletransfer.Utils;

import java.util.ArrayList;
import java.util.Set;

import static com.examples.akshay.bluetoothfiletransfer.Constants.REQUEST_ENABLE_BT;

public class Client extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "===Client";
    TextView textViewBluetoothState;
    Button buttonViewPairedDevices;
    Button buttonStartScanDevices;
    Button buttonStopScanDevices;
    Button buttonConnect;
    Button buttonTransferData;
    RecyclerView recyclerViewPairedDevices;
    RecyclerView recyclerViewScannedDevices;
    BluetoothDeviceAdapter bluetoothDeviceAdapterPairedDevices;
    BluetoothDeviceAdapter bluetoothDeviceAdapterScanedDevices;

    ProgressBar progressBarScanDevices;
    AlertDialog alertDialog;
    ArrayList<BluetoothDevice> arrayListPairedDevices;
    ArrayList<BluetoothDevice> arrayListScannedDevices = new ArrayList<>();

    BluetoothAdapter mBluetoothAdapter;

    private static BluetoothDevice bluetoothDeviceSelected;
    IntentFilter filter;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothManager bluetoothManager;

        setupUI();
        setState();

        arrayListPairedDevices = new ArrayList<>();
        arrayListScannedDevices = new ArrayList<>();

        bluetoothDeviceAdapterPairedDevices = new BluetoothDeviceAdapter(arrayListPairedDevices,this);
        bluetoothDeviceAdapterScanedDevices = new BluetoothDeviceAdapter(arrayListScannedDevices,this);

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(Client.TAG,"onReceive()");

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    //BluetoothDevice customBluetoothDevice = new BluetoothDevice(device,deviceName,deviceHardwareAddress);
                    arrayListScannedDevices.add(device);
                    bluetoothDeviceAdapterScanedDevices.setArrayListBluetoothDevice(arrayListScannedDevices);
                    recyclerViewScannedDevices.setAdapter(bluetoothDeviceAdapterScanedDevices);

                    Log.d(Client.TAG,"Found device : " + deviceName + " " + deviceHardwareAddress);
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.d(Client.TAG,"ACTION_ACL_DISCONNECTED");
                    buttonTransferData.setEnabled(false);
                    Toast.makeText(Client.this, R.string.device_disconnected,Toast.LENGTH_SHORT).show();


                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d(Client.TAG,"ACTION_ACL_CONNECTED");
                    if(alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }

                    //For logging purpose
                    ArrayList<BluetoothDevice> Connecteddevices = Utils.getPairedDevices(mBluetoothAdapter);
                    for (BluetoothDevice d : Connecteddevices) {
                        Log.d(Client.TAG,d.getName() + " " + d.getAddress() + " " + d.getBondState());
                    }

                    Toast.makeText(Client.this, R.string.device_connected,Toast.LENGTH_SHORT).show();
                    buttonTransferData.setEnabled(true);

                    //Automate Stuff
                    Log.d(Client.TAG," click transfer data");
                    Intent startDataTransfer = new Intent(Client.this,DataTransfer.class);
                    startActivity(startDataTransfer);

                } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    Log.d(Client.TAG,"ACTION_ACL_DISCONNECT_REQUESTED");

                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.d(Client.TAG,"ACTION_DISCOVERY_STARTED");
                    buttonStartScanDevices.setEnabled(false);
                    buttonStopScanDevices.setEnabled(true);
                    progressBarScanDevices.setVisibility(View.VISIBLE);

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    progressBarScanDevices.setVisibility(View.GONE);
                    buttonStartScanDevices.setEnabled(true);
                    buttonStopScanDevices.setEnabled(false);

                    Log.d(Client.TAG,"ACTION_DISCOVERY_FINISHED");
                } else if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    Log.d(Client.TAG,"Connection State Changed");
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    Log.d(Client.TAG,"Adapter State Changed");
                    setState();
                }
            }
        };


        arrayListScannedDevices.clear();
        bluetoothDeviceSelected = null;
        Log.d(Client.TAG,"Attempting Scan start from onCreate()...");
        mBluetoothAdapter.startDiscovery();

    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, filter);
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.d(TAG, "OFF");

        } else {
            Log.d(TAG, "ON");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void setupUI(){
        buttonViewPairedDevices = findViewById(R.id.activity_client_button_view_paired_devices);
        buttonViewPairedDevices.setOnClickListener(this);
        buttonViewPairedDevices.setVisibility(View.GONE);

        buttonStartScanDevices = findViewById(R.id.activity_client_button_start_scan_devices);
        buttonStartScanDevices.setOnClickListener(this);

        buttonStopScanDevices = findViewById(R.id.activity_client_button_stop_scan_devices);
        buttonStopScanDevices.setOnClickListener(this);

        buttonConnect = findViewById(R.id.activity_client_button_connect);
        buttonConnect.setOnClickListener(this);

        buttonTransferData = findViewById(R.id.activity_client_transfer_data);
        buttonTransferData.setOnClickListener(this);
        buttonTransferData.setEnabled(false);

        RecyclerView.LayoutManager layoutManagerPairedDevices = new LinearLayoutManager(getApplicationContext());
        RecyclerView.LayoutManager layoutManagerScannedDevices = new LinearLayoutManager(getApplicationContext());

        textViewBluetoothState = findViewById(R.id.activity_client_textview_bluetooth_state);

        recyclerViewPairedDevices = findViewById(R.id.activity_client_recycler_view_paired_devices);
        recyclerViewPairedDevices.setLayoutManager(layoutManagerPairedDevices);
        recyclerViewPairedDevices.setItemAnimator(new DefaultItemAnimator());
        recyclerViewPairedDevices.setAdapter(bluetoothDeviceAdapterPairedDevices);

        recyclerViewScannedDevices = findViewById(R.id.activity_client_recycler_view_scanned_devices);
        recyclerViewScannedDevices.setLayoutManager(layoutManagerScannedDevices);
        recyclerViewScannedDevices.setItemAnimator(new DefaultItemAnimator());
        recyclerViewScannedDevices.setAdapter(bluetoothDeviceAdapterScanedDevices);


        progressBarScanDevices = findViewById(R.id.acitivity_client_progressBar_scan_devices);
        progressBarScanDevices.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connecting...");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(Client.TAG,"Cancel button Clicked");
            }
        });

        builder.setCancelable(true);
        alertDialog = builder.create();


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_client_button_view_paired_devices:
                Log.d(Client.TAG,"Clicked View Paired Devices");
                arrayListPairedDevices = getPairedDevices();
                bluetoothDeviceAdapterPairedDevices.setArrayListBluetoothDevice(arrayListPairedDevices);
                //bluetoothDeviceAdapterPairedDevices.notifyDataSetChanged();
                recyclerViewPairedDevices.setAdapter(bluetoothDeviceAdapterPairedDevices);
                break;

            case R.id.activity_client_button_start_scan_devices:
                arrayListScannedDevices.clear();
                bluetoothDeviceSelected = null;
                Log.d(Client.TAG," Scan start click...");
                mBluetoothAdapter.startDiscovery();
                break;

            case R.id.activity_client_button_stop_scan_devices:
                Log.d(Client.TAG," Scan stop click...");
                if(mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                    Log.d(Client.TAG,"calling mBluetoothAdapter.cancelDiscovery()");
                } else {
                    Log.d(Client.TAG,"mBluetoothAdapter.isDiscovering() is false...cannot mBluetoothAdapter.cancelDiscovery()");
                }
                break;
            case R.id.activity_client_button_connect:

                if(!alertDialog.isShowing()) {
                    alertDialog.show();
                }
                Log.d(Client.TAG,"connect click");
                if(bluetoothDeviceSelected == null) {
                    Log.d(Client.TAG,"bluetoothDeviceSelected is null");
                    return;
                }
                ConnectTask connectThread = new ConnectTask(this,bluetoothDeviceSelected,mBluetoothAdapter);
                connectThread.execute();
                break;

            case R.id.activity_client_transfer_data:
                Log.d(Client.TAG," click transfer data");
                Intent intent = new Intent(this,DataTransfer.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private ArrayList<BluetoothDevice> getPairedDevices(){
        ArrayList<BluetoothDevice> bluetoothDeviceArrayList = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                bluetoothDeviceArrayList.add(device);

                Log.d(Client.TAG,"Paired : " + deviceName + " " + deviceHardwareAddress);
            }
        }
        return bluetoothDeviceArrayList;
    }

    public static BluetoothDevice getBluetoothDeviceSelected() {
        return bluetoothDeviceSelected;
    }

    public static void setBluetoothDeviceSelected(BluetoothDevice bluetoothDeviceSelected,Context context) {
        Client.bluetoothDeviceSelected = bluetoothDeviceSelected;
        Toast.makeText(context,"Device selected : " + bluetoothDeviceSelected.getName(),Toast.LENGTH_SHORT).show();
    }

    public void setState() {
        int state = mBluetoothAdapter.getState();
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                textViewBluetoothState.setText(R.string.STATE_OFF);
                buttonStartScanDevices.setEnabled(false);
                buttonStopScanDevices.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                textViewBluetoothState.setText(R.string.STATE_TURNING_ON);
                break;
            case BluetoothAdapter.STATE_ON:
                textViewBluetoothState.setText(R.string.STATE_ON);
                buttonStartScanDevices.setEnabled(true);
                buttonStopScanDevices.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                if(mBluetoothAdapter.isDiscovering()) {
                    Log.d(Client.TAG,"Cancelling discovery");
                    mBluetoothAdapter.cancelDiscovery();
                }
                textViewBluetoothState.setText(R.string.STATE_TURNING_OFF);
                break;
            default:
                textViewBluetoothState.setText(R.string.unknown);
                break;
        }
        if(mBluetoothAdapter.isDiscovering()) {
            buttonStartScanDevices.setEnabled(false);
            buttonStopScanDevices.setEnabled(true);
        } else {
            buttonStartScanDevices.setEnabled(true);
            buttonStopScanDevices.setEnabled(false);
        }
    }
}
