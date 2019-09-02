package com.example.ble;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Intent;


import android.util.Log;
import java.util.ArrayList;
//AppCompatActivity

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 100000;
    private boolean mScanning;
    private Handler mHandler;
    private Button mButton,mButton_Connect;
    private ListView mList_Device;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mButton = findViewById(R.id.button);
        mButton_Connect =findViewById(R.id.btn_bleconnect);

        mList_Device =findViewById(R.id.list_device);
        mLeDeviceListAdapter = new LeDeviceListAdapter();

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initBluetooth();
            }
        });
        mButton_Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeDeviceListAdapter.clear();
                Toast.makeText(MainActivity.this,"正在搜索",Toast.LENGTH_SHORT).show();
                scanLeDevice(true);
            }
        });
        mList_Device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);
                if (device == null) return;
                final Intent intent = new Intent(MainActivity.this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivity(intent);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_begin:
                Toast.makeText(MainActivity.this,"begin",Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_stop:
                Toast.makeText(MainActivity.this,"stop",Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (!mBluetoothAdapter.isEnabled()) {
//            if (!mBluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            }
//        }

    }

    @Override
    //从下一个界面回调后调用
    //requestCode 启动活动时的请求码
    //resultCode  返回数据的处理结果
    //data        带着数据的Itent
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




    private void initBluetooth(){
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if(manager != null){
            mBluetoothAdapter = manager.getAdapter();
            if(mBluetoothAdapter != null){
                if(!mBluetoothAdapter.isEnabled()){
                    openBle();
                }
                else{
                    Toast.makeText(MainActivity.this,"蓝牙已打开",Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                openBle();
            }
        }

    }

    private void openBle(){
        //打开蓝牙
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
    }

    private  void scanLeDevice(final boolean enable){
        if(enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else{
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            mList_Device.setAdapter(mLeDeviceListAdapter);
                        }

                    });
                }

            };


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
