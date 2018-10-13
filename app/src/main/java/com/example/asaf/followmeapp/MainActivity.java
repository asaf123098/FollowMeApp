package com.example.asaf.followmeapp;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText phoneNumEditText;
    private TextView currentPhoneNumTextView;
    private Button enterButton;
    private Button enableButton;
    private Button disaleButton;

    private static String savedPhoneNum;

    private final String ENABLE = "enable";
    private final String DISABLE = "disable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (this.getSavedPhoneNumber() != null)
            savedPhoneNum = this.getSavedPhoneNumber();

        this.initiateViews();
        if (this.isFromWidget())
            handleWidgetAction();
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (this.isFromWidget())
//            handleWidgetAction();
//    }

    private void initiateViews() {
        this.phoneNumEditText = findViewById(R.id.numet);

        this.currentPhoneNumTextView = findViewById(R.id.currentNum);
        if (savedPhoneNum != null)
            this.currentPhoneNumTextView.setText(savedPhoneNum);//Set the current phone num in memory if exists
        else {
            this.currentPhoneNumTextView.setText("No phone number in memory");
        }

        this.enterButton = findViewById(R.id.enter);
        this.enterButton.setOnClickListener(this);

        this.enableButton = findViewById(R.id.enable);
        this.enableButton.setOnClickListener(this);

        this.disaleButton = findViewById(R.id.disable);
        this.disaleButton.setOnClickListener(this);
    }

    private String getSavedPhoneNumber() {

        String ret = "";

        try {
            InputStream inputStream = this.openFileInput("number.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return null;
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
            return null;
        }

        return ret;
    }

    private void saveEnteredNumToMemory() {
        writeToFile(this.phoneNumEditText.getText().toString(), this);
        savedPhoneNum = getSavedPhoneNumber();
        this.currentPhoneNumTextView.setText(savedPhoneNum);//Update the text view with the current phone num.
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("number.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void changeWidgetStatus(String status) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//        int widgetId = appWidgetManager.getAppWidgetIds(new ComponentName(this, "MainActivity"))[0];
//        int layoutId;
//        if (status.equals(this.ENABLE))
//            layoutId = R.layout.widget_on;
//        else
//            layoutId = R.layout.widget_off;
//
//        RemoteViews views = new RemoteViews(this.getPackageName(), layoutId);
//        appWidgetManager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == this.enterButton.getId()) {
            this.saveEnteredNumToMemory();
        } else if (view.getId() == this.enableButton.getId()) {
            this.setCallForwarding(ENABLE, this);
        } else if (view.getId() == this.disaleButton.getId()) {
            this.setCallForwarding(DISABLE, this);
        }
    }

    private boolean isFromWidget() {
        if (getIntent().hasExtra("is_from_widget?")) {
            Bundle extras = getIntent().getExtras();
            if (extras != null)
                return extras.getBoolean("is_from_widget?", false);
        }
        return false;
    }

    private void handleWidgetAction() {
        if (this.isCallForwardActive(this)) {
            this.setCallForwarding(this.DISABLE, this);
        } else {
            this.setCallForwarding(this.ENABLE, this);
        }
    }

    private void setCallForwarding(String enableOrDisable, Context context)
    {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            switch (enableOrDisable)
            {
                case ENABLE:
                {
                    callIntent.setData(Uri.parse("tel:" + "*21*" + MainActivity.savedPhoneNum + "%23")); //tel:(format for dialing), *21* (format for call forwarding)
                    context.startActivity(callIntent);                                                   //%23 in URI, equals "#" mark, that is also a part of the call forwarding format.
                    this.setCallForwardStatus(this, this.ENABLE);
                    if (this.isFromWidget())
                        this.changeWidgetStatus(ENABLE);
                    break;

                }
                case DISABLE:
                {
                    callIntent.setData(Uri.parse("tel:" + "%2321%23")); //tel:(format for dialing),//%23 in URI, equals "#" mark, that is also a part of the call forwarding format.
                    context.startActivity(callIntent);
                    this.setCallForwardStatus(this, this.DISABLE);
                    if(this.isFromWidget())
                        this.changeWidgetStatus(DISABLE);
                    break;
                }
            }
        }
        else
        {
            ActivityCompat.requestPermissions((Activity)context, new String [] {Manifest.permission.CALL_PHONE}, 1);
            Toast.makeText(context, String.format("Click %s again", enableOrDisable), Toast.LENGTH_SHORT).show();
        }
    }

    private void setCallForwardStatus(Context context, String data)
    {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("callForwardStatus.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private boolean isCallForwardActive(Context context)
    {
        try {
            InputStream inputStream = context.openFileInput("callForwardStatus.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                String ret = stringBuilder.toString();

                if(ret.equals(this.ENABLE))
                    return true;
                else if(ret.equals(this.DISABLE))
                    return false;

            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return true;// True so if there is a problem forwarding will be disabled for sure.
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
            return true;// True so if there is a problem forwarding will be disabled for sure.
        }

        return true;// True so if there is a problem forwarding will be disabled for sure.

    }

}
