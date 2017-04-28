package com.conradhappeliv.bledoorman;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.BleSignal;
import com.google.android.gms.nearby.messages.Distance;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import static com.google.android.gms.nearby.messages.Strategy.BLE_ONLY;
import static com.google.android.gms.nearby.messages.Strategy.TTL_SECONDS_INFINITE;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    GoogleApiClient mGoogleApiClient;
    MessageListener mMessageListener;
    String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                super.onFound(message);
                Log.i(TAG, "Found tag");
            }

            @Override
            public void onLost(Message message) {
                super.onLost(message);
                Log.i(TAG, "Lost tag");
            }

            @Override
            public void onDistanceChanged(Message message, Distance distance) {
                super.onDistanceChanged(message, distance);
                Log.i(TAG, "Distance changed " + distance.getMeters());
            }

            @Override
            public void onBleSignalChanged(Message message, BleSignal bleSignal) {
                super.onBleSignalChanged(message, bleSignal);
                Log.i(TAG, "BLE signal changed " + bleSignal.getRssi());
            }
        };
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        SubscribeCallback subscribeCallback = new SubscribeCallback() {
            @Override
            public void onExpired() {
                super.onExpired();
            }
        };
        MessageFilter messageFilter = new MessageFilter.Builder()
                .includeEddystoneUids("6a5def97fc232028301e", "")
                .build();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(BLE_ONLY)
                .setCallback(subscribeCallback)
                .setFilter(messageFilter)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        switch(cause) {
            case CAUSE_NETWORK_LOST:
                Log.w(TAG, "connection suspended: network lost");
                break;
            case CAUSE_SERVICE_DISCONNECTED:
                Log.w(TAG, "connection suspended: service disconnected");
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Connection failed");

    }
}
