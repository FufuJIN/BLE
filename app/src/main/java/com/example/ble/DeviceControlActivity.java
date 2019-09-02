package com.example.ble;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 *  * and display GATT services and characteristics supported by the device.  The Activity
 *  * communicates with {@code BluetoothLeService}, which in turn interacts with the
 *  * Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;   //连接状态文本框
    private TextView mDataField;        //数据文本框
    private TextView mSPO2;             //SPO2数据文本框
    private TextView mPR;              //PR数据文本框
    private EditText mSendData;        //数据发送编辑文本框
    private Button  mBtnSendData;      //数据发送按钮
    private TextView mTextSendState;   //发送状态文本框
    private String mDeviceName;
    private String mDeviceAddress;
    private BLEservice mBluetoothLeService;   //蓝牙服务
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothGattService WriteGattService;
    private BluetoothGattCharacteristic Writecharacteristic;
    private BluetoothGattService ReadGattService;
    private BluetoothGattCharacteristic ReadCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private MyDatabaseHelper dbHelper;     //数据库类
    private int DataCnt=0;                 //防止数据库存的太大把储存空间爆了
    //MPAndroidChart库需要的变量或类
    private LineChart mLineChart;
    private ChartLineManager mChartLineManager;
    private List<Integer> list = new ArrayList<>(); //数据集合
    private List<String> names = new ArrayList<>(); //折线名字集合
    private List<Integer> colour = new ArrayList<>();//折线颜色集合
    private int STOP  = 0;
    private SQLiteDatabase db;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEservice.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver;
    {
        mGattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BLEservice.ACTION_GATT_CONNECTED.equals(action)) {
                    mConnected = true;
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                } else if (BLEservice.ACTION_GATT_DISCONNECTED.equals(action)) {
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    clearUI();
                } else if (BLEservice.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the user interface.
//                    WriteGattService = mBluetoothLeService.getSupportedGattService(UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb"));
//                    Writecharacteristic = WriteGattService.getCharacteristic(UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb"));
                    //读数据的服务和characteristic
                    //ReadGattService = mBluetoothLeService.getSupportedGattService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                    //ReadCharacteristic = ReadGattService.getCharacteristic(UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb"));
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                } else if (BLEservice.ACTION_DATA_AVAILABLE.equals(action)) {
                    displayData(intent.getStringExtra(BLEservice.EXTRA_DATA));
                    String Spo2_string = intent.getStringExtra(BLEservice.EXTRA_SPO2);
                    String PR_string = intent.getStringExtra(BLEservice.EXTRA_PR);

                    int Spo2_int = Integer.valueOf(Spo2_string).intValue();
                    int PR_int   = Integer.valueOf(PR_string).intValue();
                    mSPO2.setText(Spo2_string);
                    mPR.setText(PR_string);
                    if(Spo2_int>0 && PR_int>0 && STOP == 0) {
//                        && DataCnt <80
                        DataCnt=DataCnt+1;
                        db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.put("SPO2", Spo2_int);
                        values.put("PR", PR_int);
                        db.insert("PULSE", null, values);
                        values.clear();
                    }
                    if(STOP == 0) {
                        list.add(Spo2_int);
                        list.add(PR_int);
                        mChartLineManager.addEntry(list);
                        list.clear();
                    }
                    Log.e(TAG, "onReceive: time");
                }
            }
        };
    }

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_device);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mSPO2      = (TextView) findViewById(R.id.spo2_value);
        mPR        = (TextView) findViewById(R.id.pr_value);

        dbHelper = new MyDatabaseHelper(this,"PULSE.db",null,3);
        mSendData    =(EditText)findViewById(R.id.edit_text);
        mBtnSendData =(Button)findViewById(R.id.send_data);
        mTextSendState=(TextView)findViewById(R.id.send_state);
        mBtnSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputText = mSendData.getText().toString();
//                WriteGattService = mBluetoothLeService.getSupportedGattService(BLEservice.UUID_WRITE);
////                Writecharacteristic = WriteGattService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
//
////                WriteGattService = mBluetoothLeService.getSupportedGattService(UUID.fromString("00001822-0000-1000-8000-00805f9b34fb"));
////                Writecharacteristic = WriteGattService.getCharacteristic(UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb"));
////                mTextSendState.setText("正在发送");
////                mBluetoothLeService.writeCharacteristic(Writecharacteristic,inputText);
////                mTextSendState.setText("发送成功"+inputText);
////                Log.e(TAG, "write value: ");
            }
        });
        mLineChart = (LineChart)findViewById(R.id.chart2);
        names.add("SPO2");
        names.add("PR");
        //折线颜色
        colour.add(Color.RED);
        colour.add(Color.BLUE);
        mChartLineManager = new ChartLineManager(mLineChart, names, colour);
        mChartLineManager.setYAxis(150, -10, 10);


        Intent gattServiceIntent = new Intent(this, BLEservice.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.DATA_BEGIN:
                STOP = 0;
                break;
            case R.id.DATA_STOP:
                STOP = 1;
                break;
            case R.id.clean_database:
                db.execSQL("DELETE FROM PULSE");
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }



    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                if(uuid.equals("00002a5f-0000-1000-8000-00805f9b34fb")){
                    final BluetoothGattCharacteristic characteristic = gattCharacteristic;
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {//可读
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        Log.d(TAG, "onChildClick: "+characteristic.getUuid());
                        mBluetoothLeService.readCharacteristic(characteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {//可通知
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEservice.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEservice.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEservice.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEservice.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}

