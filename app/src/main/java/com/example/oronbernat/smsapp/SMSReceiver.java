package com.example.oronbernat.smsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

class SMSReceiver extends BroadcastReceiver{

    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "SMSBroadcastReceiver";

    private Listener listener;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction ().equals (Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {

            String smsSender = "";
            String smsBody = "";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent (intent)){
                    smsSender = smsMessage.getDisplayOriginatingAddress ();
                    smsBody += smsMessage.getMessageBody ();
                }
            }else{
                Bundle smsBundle =  intent.getExtras ();
                if (smsBundle != null){
                    Object[] pdus = (Object[]) smsBundle.get ("pdus");
                    if (pdus == null){
                        Log.e (TAG,"SmsBundle have no pdus key");
                        return;
                    }
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < messages.length ; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        smsBody += messages[i].getMessageBody ();
                    }
                    smsSender = messages[0].getOriginatingAddress ();
                }
            }
            listener.onTextReceived (smsSender,smsBody);
        }
    }
    void setListener(Listener listener){
        this.listener = listener;
    }


    interface Listener{
        void onTextReceived(String sender,String body);
    }
}
