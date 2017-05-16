package com.conradhappeliv.bledoorman;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

                // sampling
                if(isSampling) {
                    curamount += rssi;
                    numSamples++;
                    ((TextView) findViewById(R.id.curamtsamples)).setText(Integer.toString(numSamples));
                    if(numSamples == totalSamples) {
                        isSampling = false;
                        double avgrssi = curamount/numSamples;
                        ((TextView) findViewById(R.id.sampled)).setText(Double.toString(avgrssi));
                    }
                }

                // threshold
                sample_history.add(rssi);
                while(sample_history.size() > samplesToThreshold) sample_history.remove(0);
                int curavg = 0;
                for(int i = 0; i < sample_history.size(); i++) {
                    curavg += sample_history.get(i);
                }
                curavg /= sample_history.size();
                if(curavg > threshold) {
                    ((TextView) findViewById(R.id.inorout)).setText("IN");
                    if(notifications) sendNotification("file:///android_asset/index.html");
                } else ((TextView) findViewById(R.id.inorout)).setText("OUT");
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

    boolean isSampling = false;
    int numSamples = 0;
    int totalSamples = 0;
    int curamount = 0;
    int threshold = -65;
    int samplesToThreshold = 12;
    ArrayList<Integer> sample_history = new ArrayList<>();
    boolean notifications = false;
    int notifId = 88;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.testWVButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebView("file:///android_asset/index.html");
                sendNotification("file:///android_asset/index.html");
            }
        });

        addressTextview = (TextView) findViewById(R.id.deviceAddress);
        rssiTextview = (TextView) findViewById(R.id.rssi);
        timeTextview = (TextView) findViewById(R.id.curTime);
        addressTextview.setText("test");
        rssiTextview.setText("test");
        timeTextview.setText("test");

        ((EditText) findViewById(R.id.numsamples)).setText("50");
        ((EditText) findViewById(R.id.numsamples2)).setText("12");
        ((EditText) findViewById(R.id.numsamples2)).addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                samplesToThreshold = Integer.parseInt(((EditText) findViewById(R.id.numsamples2)).getText().toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        ((EditText) findViewById(R.id.threshold)).setText("-65");
        ((EditText) findViewById(R.id.threshold)).addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                threshold = Integer.parseInt(((EditText) findViewById(R.id.threshold)).getText().toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        ((ToggleButton) findViewById(R.id.notiftoggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                notifications = isChecked;
            }
        });

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
        if (scanner != null && !notifications) {
            scanner.stopScan(SCAN_CALLBACK);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scanner != null && !notifications) {
            scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, SCAN_CALLBACK);
            Log.i(TAG, "started BLE scan");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void openWebView(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    private void sendNotification(String url) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.smallicon)
                .setContentTitle("BLEDoorman Activated!")
                .setContentText("Click to navigate to " + url)
                .setAutoCancel(true);
        Intent resultIntent = new Intent(this, WebViewActivity.class);
        resultIntent.putExtra("url", url);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(WebViewActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifId, builder.build());
    }

    public void takeSamples(View v) {
        totalSamples = Integer.parseInt(((EditText) findViewById(R.id.numsamples)).getText().toString());
        numSamples = 0;
        curamount = 0;
        isSampling = true;
    }
}
