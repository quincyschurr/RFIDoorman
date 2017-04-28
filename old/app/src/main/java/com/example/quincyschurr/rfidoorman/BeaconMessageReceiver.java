package com.example.quincyschurr.rfidoorman;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import static com.google.android.gms.internal.zzt.TAG;

/**
 * Created by quincyschurr on 4/20/17.
 */

public class BeaconMessageReceiver implements onReceive {
    @Override
    public void onReceive(Context context, Intent intent) {
        Nearby.Messages.handleIntent(intent, new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.i(TAG, "Found message via PendingIntent: " + message);
            }

            @Override
            public void onLost(Message message) {
                Log.i(TAG, "Lost message via PendingIntent: " + message);
            }
        });
    }
}
