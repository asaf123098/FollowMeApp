package com.example.followmeapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallBroadcastReceiver extends BroadcastReceiver {
    public static String numberToCall;

    public void onReceive(Context context, Intent intent) {
        Log.d("CallRecorder", "CallBroadcastReceiver::onReceive got Intent: " + intent.toString());
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            numberToCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d("CallRecorder", "CallBroadcastReceiver intent has EXTRA_PHONE_NUMBER: " + numberToCall);
            Intent response = new Intent(context, MainActivity.class);

            if (numberToCall.contains("*21*"))
                response.putExtra("isCallForwardingOn", true);
            else if (numberToCall.equals("#21#"))
                response.putExtra("isCallForwardingOn", false);
            context.startActivity(response);
        }
    }
}