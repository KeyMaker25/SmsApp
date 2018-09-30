package com.example.oronbernat.smsapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class MyService extends Service {

    SMSReceiver receiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        receiver = new SMSReceiver ();
        registerReceiver (receiver,new IntentFilter ());
        return super.onStartCommand (intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver (receiver);
        super.onDestroy ();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


}
