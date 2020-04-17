package com.example.followmeapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity implements View.OnClickListener
{
    private EditText phoneNumEditText;
    private EditText turnOnStartPatternEditText;
    private EditText turnOnEndPatternEditText;
    private EditText turnOffPatternEditText;

    private TextView phoneNumberPreView;
    private TextView enablePatternPreView;
    private TextView disablePatternPreView;
    private Button saveButton;
    private Button enableButton;
    private Button disableButton;

    private final String ENABLE = "enable";
    private final String DISABLE = "disable";

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);


        this.initiateViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.isFromWidget())
        {
            handleWidgetAction();
            finish();
        }
    }

    private void initiateViews() {
        this.initEditTexts();
        this.initPreViews();

        this.saveButton = findViewById(R.id.save_button);
        this.saveButton.setOnClickListener(this);

        this.enableButton = findViewById(R.id.enable);
        this.enableButton.setOnClickListener(this);

        this.disableButton = findViewById(R.id.disable);
        this.disableButton.setOnClickListener(this);
    }

    private void initEditTexts() {
        this.phoneNumEditText = findViewById(R.id.phone_number_edit_text);
        this.turnOnStartPatternEditText = findViewById(R.id.turn_on_start_pattern_edit_text);
        this.turnOnEndPatternEditText = findViewById(R.id.turn_on_end_pattern_edit_text);
        this.turnOffPatternEditText = findViewById(R.id.turn_off_pattern_edit_text);
    }

    private void initPreViews() {
        this.phoneNumberPreView = findViewById(R.id.saved_number_preview);
        this.phoneNumberPreView.setText(this.getPhoneNumber());

        this.enablePatternPreView = findViewById(R.id.enable_pattern_preview);
        this.enablePatternPreView.setText(this.getEnablePattern());

        this.disablePatternPreView = findViewById(R.id.disable_pattern_preview);
        this.disablePatternPreView.setText(this.getPhoneNumber());
    }

    private void saveEnablePattern()
    {
        String start = this.turnOnStartPatternEditText.getText().toString();
        String end = this.turnOnEndPatternEditText.getText().toString();
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString(getString(R.string.turn_on_start_key), start);
        sharedPrefEditor.putString(getString(R.string.turn_on_end_key), end);
        sharedPrefEditor.commit();
        this.enablePatternPreView.setText(getEnablePattern());//Update the text view with the current phone num.
    }

    private String getEnablePattern()
    {
        String start = this.sharedPref.getString(getString(R.string.turn_on_start_key), "");
        String end = this.sharedPref.getString(getString(R.string.turn_on_end_key), "");
        String phoneNumber = this.getPhoneNumber();

        return start + phoneNumber + end;
    }

    private void saveDisablePattern()
    {
        String disableStr = this.turnOffPatternEditText.getText().toString();
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString(getString(R.string.turn_off_key), disableStr);
        sharedPrefEditor.apply();
        this.disablePatternPreView.setText(this.getDisablePattern());//Update the text view with the current phone num.
    }

    private String getDisablePattern()
    {
        return this.sharedPref.getString(getString(R.string.turn_off_key), "");
    }

    private void savePhoneNumber() {
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString(getString(R.string.phone_number_key), this.phoneNumEditText.getText().toString());
        sharedPrefEditor.apply();
        this.phoneNumberPreView.setText(getPhoneNumber());//Update the text view with the current phone num.
    }

    private String getPhoneNumber() {
        return this.sharedPref.getString(getString(R.string.phone_number_key), getString(R.string.couldnt_find_value));
    }

    private void raiseAlert(String alertMessage)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(alertMessage);
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == this.saveButton.getId()) {
            this.savePhoneNumber();
            this.saveEnablePattern();
            this.saveDisablePattern();
        } else if (view.getId() == this.enableButton.getId()) {
            this.setCallForwarding(ENABLE);
        } else if (view.getId() == this.disableButton.getId()) {
            this.setCallForwarding(DISABLE);
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
            this.setCallForwarding(this.DISABLE);
        } else {
            this.setCallForwarding(this.ENABLE);
        }
    }

    private void setCallForwarding(String enableOrDisable)
    {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            switch (enableOrDisable)
            {
                case ENABLE:
                {
                    callIntent.setData(Uri.parse("tel:" + this.fix_hashtags_if_needed(this.getEnablePattern())));
                    this.startActivity(callIntent);
                    this.setCallForwardStatus(this, this.ENABLE);
                    break;
                }
                case DISABLE:
                {
                    callIntent.setData(Uri.parse("tel:" + this.fix_hashtags_if_needed(this.getDisablePattern())));
                    this.startActivity(callIntent);
                    this.setCallForwardStatus(this, this.DISABLE);
                    break;
                }
            }
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String [] {Manifest.permission.CALL_PHONE}, 1);
            Toast.makeText(this, String.format("Click %s again", enableOrDisable), Toast.LENGTH_SHORT).show();
        }
    }

    private String fix_hashtags_if_needed(String strToFix)
    {
        return strToFix.replaceAll("#", Uri.encode("#"));
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
            Log.e("Is forwarding active", "File not found: " + e.toString());
            return true;// True so if there is a problem forwarding will be disabled for sure.
        } catch (IOException e) {
            Log.e("Is forwarding active", "Can not read file: " + e.toString());
            return true;// True so if there is a problem forwarding will be disabled for sure.
        }

        return true;// True so if there is a problem forwarding will be disabled for sure.

    }

}
