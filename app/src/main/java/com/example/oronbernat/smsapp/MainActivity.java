package com.example.oronbernat.smsapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements SMSReceiver.Listener {

    private static final int SMS_PERMISSION_CODE = 50;
    private TextView textView;
    private String addressDestination;
    private EditText eText;
    private Button OKButton;
    private SMSReceiver receiver;
    private Switch submit;
    private SharedPreferences preferences;
    private NotificationManager ntfManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);

        preferences = this.getSharedPreferences ("lbltxt", 0);
        addressDestination = preferences.getString ("lbltxt", null);

        eText = findViewById (R.id.Label_id);
        submit = findViewById (R.id.switch2);
        textView = findViewById (R.id.SendToAddress);
        textView.setText (addressDestination);
        OKButton = findViewById (R.id.OKbutton);



    }

    private void requestReadAndSendSmsPermission() {
        ActivityCompat.requestPermissions (this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
    }

    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission (this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String permissions[],@NonNull int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setReceiver ();
                } else {
                    Toast.makeText (this, "You didn't permit read sms", Toast.LENGTH_SHORT).show ();
                }
            }
        }
    }

    private void setReceiver() {
        receiver = new SMSReceiver ();
        registerReceiver (receiver, new IntentFilter (Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
        receiver.setListener (this);
    }

    public void btnChangeLabel(View view) {

        eText.setVisibility (View.VISIBLE);
        OKButton.setVisibility (View.VISIBLE);
    }

    public void btnUpdateLabel(View view) {

        boolean eTextGo = true;
        String check = eText.getText ().toString ();
        for (int i = 0; i < check.length (); i++) {
            if (check.charAt (i) == ' ') {
                Toast.makeText (this, "Can't have spaces", Toast.LENGTH_SHORT).show ();
                eTextGo = false;
                break;
            }
        }
        if (eTextGo) {
            preferences.edit ().putString ("lbltxt",check).apply ();
            eText.setVisibility (View.INVISIBLE);
            OKButton.setVisibility (View.INVISIBLE);

            addressDestination = eText.getText ().toString ();
            textView.setText (addressDestination);
        }
    }

    @Override
    protected void onStart() {
        ntfManager = (NotificationManager) this.getSystemService (Context.NOTIFICATION_SERVICE);
        super.onStart ();
    }

    public void btnSwitch(View view) {
        boolean switchState = submit.isChecked ();
        if (!isSmsPermissionGranted ()) {
            requestReadAndSendSmsPermission ();
        } else {
            Notification ntf = new Notification.Builder (this).setContentText ("Service is Running")
                    .setSmallIcon (R.mipmap.icon_36_)
                    .setContentTitle ("SMS_Listener").setLargeIcon (BitmapFactory.decodeResource (this.getResources (), R.mipmap.icon_48_))
                    .build ();
            if (switchState){
                ntfManager.notify (3,ntf);
                setReceiver ();
                startService (new Intent (this,MyService.class));
            }else {
                ntfManager.cancel (3);
                stopService (new Intent (this, MyService.class));
                unregisterReceiver (receiver);
            }
        }



    }

    @Override
    public void onTextReceived(String sender, String body) {

        final String msg = "?sender="+sender+"&body="+body;

        /*final JSONObject jsonMessage = new JSONObject ();
        try {

            jsonMessage.put ("sender", sender);
            jsonMessage.put ("body", body);

        } catch (JSONException e) {
            e.printStackTrace ();
        }*/

        final Thread threadSendMsg = new Thread (new Runnable () {
            @Override
            public void run() {

                try {

                    URL url = new URL (addressDestination+URLEncoder.encode (msg, "UTF-8"));
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
                    conn.setDoInput (true);

                    /*conn.setRequestMethod ("POST");*/
                    /*conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                    conn.setRequestProperty ("Accept", "application/json");*/
                    /*conn.setDoOutput (true);*/
                    /*Log.i ("JSON", jsonMessage.toString ());*/

                    Log.i ("QueryStringMsg", msg);


                    /*DataOutputStream os = new DataOutputStream (conn.getOutputStream ());
                    os.writeBytes (msg.toString ());*/

                    //Receiving Data Back From server (OK/SUCCESS)
                    /*DataInputStream is = new DataInputStream (conn.getInputStream ());
                    byte[] dataReceived = new byte[1024];
                    int length = is.read (dataReceived);
                    String messageReceiver = new String (dataReceived);*/


                    Log.i ("STATUS", String.valueOf (conn.getResponseCode ()));
                    Log.i ("MSG", conn.getResponseMessage ());

                    conn.disconnect ();

                } catch (UnknownHostException e) {

                    Toast.makeText (MainActivity.this, "UnreachableHOST Exception", Toast.LENGTH_LONG).show ();

                } catch (Exception e) {

                    Toast.makeText (MainActivity.this, "Connection Exception", Toast.LENGTH_LONG).show ();

                }


            }
        });

        threadSendMsg.start ();


    }

    public void onPause() {
        super.onPause ();
    }

    @Override
    protected void onDestroy() {
        ntfManager.cancel (3);
        super.onDestroy ();
    }
}
