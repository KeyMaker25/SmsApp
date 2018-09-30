package com.example.oronbernat.smsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SMSReceiver.Listener {

    private static final int SMS_PERMISSION_CODE = 50;
    private TextView txtAddress, finalNumber, txtSource;
    private TextView txtViewsSource_lbl, txtViewsNumber_lbl, txtViewsAddress_lbl ;
    private String addressDestination, phoneNumber, source;
    private EditText eText, ePhoneNumber, eSource;
    private Button OKButton, OKBtnNumber, OKBtnSource;
    private SMSReceiver receiver;
    private Switch submit, alert;
    private SharedPreferences preferences;
    private NotificationManager ntfManager;


    @SuppressLint("ServiceCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);

        //getting old data (local)
        preferences = getSharedPreferences ("Start", MODE_PRIVATE);
        phoneNumber = preferences.getString ("lblPhoneNumber", null);
        source = preferences.getString ("lblSourse", null);
        addressDestination = preferences.getString ("lbltxt", null);

        //Init first view

        txtViewsSource_lbl = findViewById (R.id.textViewSource);
        txtSource = findViewById (R.id.txtSource);
        txtSource.setText (source);

        txtViewsNumber_lbl = findViewById (R.id.textViewNumber);
        finalNumber = findViewById (R.id.FinalNumber);
        finalNumber.setText (phoneNumber);

        txtViewsAddress_lbl = findViewById (R.id.textViewAddress);
        txtAddress = findViewById (R.id.SendToAddress);
        txtAddress.setText (addressDestination);

        OKBtnNumber = findViewById (R.id.OKBtnNumber);
        OKBtnSource = findViewById (R.id.OKBtnSource);
        OKButton = findViewById (R.id.OKbutton);

        ePhoneNumber = findViewById (R.id.lblPhoneNumber);
        ePhoneNumber.setText (phoneNumber);

        eSource = findViewById (R.id.lblSource);
        eSource.setText (source);

        eText = findViewById (R.id.Label_id);
        eText.setText (addressDestination);

        //Switch's
        submit = findViewById (R.id.switch2);
        alert = findViewById (R.id.switch3);

        initView ();

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


    private void initView(){

       if (!txtSource.toString ().isEmpty ()){
           eSource.setVisibility (View.INVISIBLE);
           OKBtnSource.setVisibility (View.INVISIBLE);
       }if (!finalNumber.toString ().isEmpty ()){
           ePhoneNumber.setVisibility (View.INVISIBLE);
           OKBtnNumber.setVisibility (View.INVISIBLE);
       }if(!txtAddress.toString ().isEmpty ()){
           OKButton.setVisibility (View.INVISIBLE);
           eText.setVisibility (View.INVISIBLE);
       }

    }



    @Override
    protected void onStart() {
        ntfManager = (NotificationManager) this.getSystemService (Context.NOTIFICATION_SERVICE);
        super.onStart ();
    }

    public void btnSwitch(View view) {

        Notification ntf = new Notification.Builder (this).setContentText ("Service is Running")
                .setSmallIcon (R.mipmap.icon_36_)
                .setContentTitle ("SMS_Listener")
                .setLargeIcon (BitmapFactory.decodeResource (this.getResources (), R.mipmap.icon_48_)).build ();

        boolean switchState = submit.isChecked ();
        if (!isSmsPermissionGranted ()) {
            requestReadAndSendSmsPermission ();
        }
            if (switchState){
                alert.setVisibility (View.VISIBLE);
                ntfManager.notify (3,ntf);
                setReceiver ();
                startService (new Intent (this,MyService.class));
            }else {
                alert.setVisibility (View.INVISIBLE);
                ntfManager.cancel (3);
                stopService (new Intent (this, MyService.class));
                unregisterReceiver (receiver);
            }




    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onTextReceived(final String sender, final String body) {

        new AsyncTask<Void, Void, String> () {


            @Override
            protected void onPostExecute(String result) {
                messageBox (result);
                super.onPostExecute (result);
            }

            @Override
            protected String doInBackground(Void... voids) {

                final String msg;
                String result = "server respond mismatch 0/2/4";
                Date currentTime = Calendar.getInstance ().getTime ();
                HttpURLConnection conn = null;
                URL url;

                try {
                    msg = "/?sender=" +sender+"&body="+URLEncoder.encode (body,"UTF-8")+"&apphone="+phoneNumber
                            +"&source="+source;
                    Log.i ("SENDER",sender);
                    Log.i ("MESSAGE", body);
                    Log.i ("TIME&DATE", String.valueOf (currentTime));
                    url = new URL (addressDestination + msg);
                    conn = (HttpURLConnection) url.openConnection ();
                    conn.setDoOutput (true);
                    conn.setRequestMethod ("GET");

                    //Input check :
                    InputStream is = conn.getInputStream ();
                    int actuallyRead;
                    byte[] arr = new byte[512];
                    actuallyRead = is.read (arr);
                    String input = new String (arr,0,actuallyRead);
                    Log.i ("Respond", input);
                    if (input.contains ("<status>0</status>"))
                        result = "OK";
                    if (input.contains ("<status>2</status>"))
                        result = "Missing Data";
                    if (input.contains ("<status>4</status>")){
                        result = "No Credit";
                    }
                } catch (IOException e) {
                    result ="ERROR: "+e.toString ();
                    Log.i ("ERROR",  e.toString ());
                }finally {
                    if (conn!= null)
                        conn.disconnect ();
                }

                return result;
            }
        }.execute ();

    }

    private void messageBox(String message) {
        if (alert.isChecked ())
            Toast.makeText (this, "Sending: " + message, Toast.LENGTH_SHORT).show ();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy ();
        ntfManager.cancel (3);
        if (receiver != null)
            unregisterReceiver (receiver);

    }

    public void btnUpdateNumber(View view) {

        phoneNumber = ePhoneNumber.getText ().toString ();
        finalNumber.setText (phoneNumber);
        preferences.edit ().putString ("lblPhoneNumber", phoneNumber).apply ();
        OKBtnNumber.setVisibility (View.INVISIBLE);
        ePhoneNumber.setVisibility (View.INVISIBLE);

        txtViewsNumber_lbl.setVisibility (View.VISIBLE);
        finalNumber.setVisibility (View.VISIBLE);



    }

    public void btnUpdateSource(View view) {
        source = eSource.getText ().toString ();
        preferences.edit ().putString ("lblSourse", source).apply ();
        OKBtnSource.setVisibility (View.INVISIBLE);
        eSource.setVisibility (View.INVISIBLE);
        txtSource.setText (source);

        txtViewsSource_lbl.setVisibility (View.VISIBLE);
        txtSource.setVisibility (View.VISIBLE);



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
            txtAddress.setText (addressDestination);

            txtViewsAddress_lbl.setVisibility (View.VISIBLE);
            txtAddress.setVisibility (View.VISIBLE);
        }
    }

    public void OKVisible(View view) {



        eSource.setVisibility (View.VISIBLE);
        ePhoneNumber.setVisibility (View.VISIBLE);
        OKBtnNumber.setVisibility (View.VISIBLE);
        OKBtnSource.setVisibility (View.VISIBLE);
        OKButton.setVisibility (View.VISIBLE);
        eText.setVisibility (View.VISIBLE);
        eText.setText (addressDestination,TextView.BufferType.EDITABLE);
        eSource.setVisibility (View.VISIBLE);


        txtViewsAddress_lbl.setVisibility (View.INVISIBLE);
        txtAddress.setVisibility (View.INVISIBLE);
        txtViewsNumber_lbl.setVisibility (View.INVISIBLE);
        finalNumber.setVisibility (View.INVISIBLE);
        txtViewsSource_lbl.setVisibility (View.INVISIBLE);
        txtSource.setVisibility (View.INVISIBLE);


    }
}
