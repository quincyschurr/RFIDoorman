package com.conradhappeliv.bledoorman;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final byte[] OUR_NAMESPACE = {0x6a, 0x5d, (byte) 0xef, (byte) 0x97, (byte) 0xfc, 0x23, 0x20, 0x28, 0x30, 0x1e};
    private static final ScanSettings SCAN_SETTINGS = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build();
    private static final List<ScanFilter> SCAN_FILTERS = Collections.singletonList(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
    private final ScanCallback SCAN_CALLBACK = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) return;
            String deviceAddress = result.getDevice().getAddress();
            int rssi = result.getRssi();
            long curTime = System.currentTimeMillis();
            byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
            byte[] namespace = Arrays.copyOfRange(serviceData, 2, 12);
            byte[] instance = Arrays.copyOfRange(serviceData, 12, 18);
            if(Arrays.equals(namespace, OUR_NAMESPACE)) { // make sure this is the right device
                Log.i(TAG, deviceAddress + " " + rssi);
                Beacon beacon = new Beacon(rssi, deviceAddress, curTime);
                addressTextview.setText(deviceAddress);
                rssiTextview.setText(String.valueOf(rssi));
                timeTextview.setText(String.valueOf(curTime));
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "SCAN_FAILED_ALREADY_STARTED");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "SCAN_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "SCAN_FAILED_INTERNAL_ERROR");
                    break;
                default:
                    Log.e(TAG, "Scan failed, unknown error code");
                    break;
            }
        }
    };

    private BluetoothLeScanner scanner;
    TextView addressTextview;
    TextView rssiTextview;
    TextView timeTextview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addressTextview = (TextView) findViewById(R.id.deviceAddress);
        rssiTextview = (TextView) findViewById(R.id.rssi);
        timeTextview = (TextView) findViewById(R.id.curTime);
        addressTextview.setText("test");
        rssiTextview.setText("test");
        timeTextview.setText("test");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs coarse location access");
                builder.setMessage("Please grant coarse location access so this app can scan for beacons");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                    }
                });
                builder.show();
            }
        }

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.i(TAG, "asking to enable BT");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, 1);
        } else {
            scanner = btAdapter.getBluetoothLeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 2: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "PERMISSION_REQUEST_COARSE_LOCATION granted");
                } else {
                    Toast.makeText(this, "Coarse location access is required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (scanner != null) {
            scanner.stopScan(SCAN_CALLBACK);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scanner != null) {
            scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, SCAN_CALLBACK);
            Log.i(TAG, "started BLE scan");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
