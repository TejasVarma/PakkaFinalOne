package com.example.splashscreen;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.Handler;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Home extends AppCompatActivity {
    Button btnRed, btnGreen;

    MediaPlayer player = new MediaPlayer();
    SharedPreferences sharedPreferences;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    String phoneNo = "";
    String message = "";
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    String latitude, longitude;
    StringBuffer smsBody = new StringBuffer();
    boolean isRedClick = false;
    boolean isGreenClick = false;
    boolean isYellowClick = false;
    boolean isTorchOn = false;
    private SharedPreferenceConfig preferenceConfig;

    private Thread sosThread = new Thread() {
        public void run() {
            try {
                for (int x = 0; x < 20; x++) {
                    for (int i = 0; i < 9; i++) {
                        torchToggle("on");
                        if (i == 0 || i == 1 || i == 2 || i == 6 || i == 7 || i == 8) {
                            Thread.sleep(250);
                        } else {
                            Thread.sleep(500);
                        }
                        torchToggle("off");
                        if (i == 0 || i == 1 || i == 2 || i == 6 || i == 7 || i == 8) {
                            Thread.sleep(250);
                        } else {
                            Thread.sleep(500);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        sharedPreferences = getSharedPreferences("myPreference", Context.MODE_PRIVATE);
        preferenceConfig = new SharedPreferenceConfig(getApplicationContext());
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS}, REQUEST_LOCATION);
        btnRed = findViewById(R.id.btnRed);
        btnGreen = findViewById(R.id.btnGreen);

        btnGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!getSender().equalsIgnoreCase("invalid")) {
                    message = getGreenMsg() + " Name:" + getSender() + " Cell:" + getContact() + getMapLink();
                    sendOneByOne();
                    isYellowClick = false;
                    isRedClick = false;
                    isGreenClick = true;
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid Sender Cell Number", Toast.LENGTH_SHORT).show();
                }
                if (player.isPlaying()) {
                    player.stop();
                }
                if (sosThread.isAlive()) {
                    sosThread.stop();
                }
                //torchToggle("off");
            }
        });
    }

    public void userLogout(View view) {

        preferenceConfig.writeLoginStatus(false);
        startActivity(new Intent(this, Login.class));
        finish();
    }

    public void redClick(final View view) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Intent intent1= new Intent(Intent.ACTION_SEND);
        intent.setData(Uri.parse("tel:7259508561"));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(intent);

        if (!getSender().equalsIgnoreCase("invalid")) {
            message = getRedMsg() + " Name:" + getSender() + " Cell:" + getContact() + getMapLink();
            isRedClick = true;
            isGreenClick = false;
            isYellowClick = false;
            sendOneByOne();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isRedClick && !isGreenClick && !isYellowClick) {
                        redClick(view);
                    } else if (isGreenClick)
                        finish();
                }
            }, 10000000);
        } else {
            Toast.makeText(this, "Invalid Sender Cell Number", Toast.LENGTH_SHORT).show();
            toggle(view);
        }
        if (player.isPlaying()) {
            player.stop();
        } else {
            player = MediaPlayer.create(this, R.raw.siren);
            player.start();
            player.setLooping(true);
        }
        sosPatternFlashToggle();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually back camera is at 0 position.
            try {
                if (camManager != null) {
                    cameraId = camManager.getCameraIdList()[0];
                }
                if (camManager != null) {
                    if (command.equals("on")) {
                        camManager.setTorchMode(cameraId, true);   // Turn ON
                        isTorchOn = true;
                    } else {
                        camManager.setTorchMode(cameraId, false);  // Turn OFF
                        isTorchOn = false;
                    }
                }
            } catch (CameraAccessException e) {
                e.getMessage();
            }
        }*/
    }

    private void sosPatternFlashToggle() {
        sosThread.start();
    }

    private String getMapLink() {
        getMapData();
        String uri = "https://maps.google.com/maps?q=" + latitude + "," + longitude;
        StringBuffer smsBody = new StringBuffer();
        smsBody.append("  Map Link : ");
        smsBody.append(Uri.parse(uri));
        return smsBody.toString();
    }

    private void getMapData() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            getLocation();
        }
    }

    private String getSender() {
        if (sharedPreferences.contains("name"))
            return sharedPreferences.getString("name", "");
        else
            return "invalid";
    }

    private String getContact() {
        if (sharedPreferences.contains("contact"))
            return sharedPreferences.getString("contact", "");
        else
            return "invalid";
    }

    private String getAddress() {
        if (sharedPreferences.contains("address"))
            return sharedPreferences.getString("address", "");
        else
            return "invalid";
    }

    public void yellowClick(final View view) {
        if (!getSender().equalsIgnoreCase("invalid")) {
            message = getYellowMsg() + " Name:" + getSender() + " Cell:" + getContact() + getMapLink();
            sendOneByOne();
            isRedClick = false;
            isGreenClick = false;
            isYellowClick = true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isYellowClick && !isGreenClick && !isRedClick) {
                        yellowClick(view);
                    } else if (isGreenClick)
                        finish();
                }
            }, 10000);
        } else {
            Toast.makeText(this, "Invalid Sender Cell Number", Toast.LENGTH_SHORT).show();
        }
    }

    public void greenClick(View view) {
        if (!getSender().equalsIgnoreCase("invalid")) {
            message = getGreenMsg() + " Name:" + getSender() + " Cell:" + getContact() + getMapLink();
            sendOneByOne();
            isYellowClick = false;
            isRedClick = false;
            isGreenClick = true;
        } else {
            Toast.makeText(this, "Invalid Sender Cell Number", Toast.LENGTH_SHORT).show();
        }
        if (player.isPlaying()) {
            player.stop();
        }
        if (sosThread.isAlive()) {
            sosThread.stop();
        }
        //torchToggle("off");
    }

    private void sendOneByOne() {
        String p1 = sharedPreferences.contains("p1") ? sharedPreferences.getString("p1", "") : "invalid";
        Log.d("sendOneByOne p1 be ", "sendOneByOne: p1=" + p1);
        if (Patterns.PHONE.matcher(p1).matches()) {
            phoneNo = p1;
            Log.d("sendOneByOne phone1 ", "sendOneByOne: phone1=" + phoneNo);
            sendSMSBySMSManager();
        }
        String p2 = sharedPreferences.contains("p2") ? sharedPreferences.getString("p2", "") : "invalid";
        Log.d("sendOneByOne p2 be ", "sendOneByOne: p2=" + p2);
        if (Patterns.PHONE.matcher(p2).matches()) {
            phoneNo = p2;
            Log.d("sendOneByOne phone2 ", "sendOneByOne: phone2=" + phoneNo);
            sendSMSBySMSManager();
        }
        String p3 = sharedPreferences.contains("p3") ? sharedPreferences.getString("p3", "") : "invalid";
        Log.d("sendOneByOne p3 be ", "sendOneByOne: p3=" + p3);
        if (Patterns.PHONE.matcher(p3).matches()) {
            phoneNo = p3;
            Log.d("sendOneByOne phone3 ", "sendOneByOne: phone3=" + phoneNo);
            sendSMSBySMSManager();
        }
        String p4 = sharedPreferences.contains("p4") ? sharedPreferences.getString("p4", "") : "invalid";
        Log.d("sendOneByOne p4 be ", "sendOneByOne: p4=" + p4);
        if (Patterns.PHONE.matcher(p4).matches()) {
            phoneNo = p4;
            Log.d("sendOneByOne phone4 ", "sendOneByOne: phone4=" + phoneNo);
            sendSMSBySMSManager();
        }
        String p5 = sharedPreferences.contains("p5") ? sharedPreferences.getString("p5", "") : "invalid";
        Log.d("sendOneByOne p5 be ", "sendOneByOne: p5=" + p5);
        if (Patterns.PHONE.matcher(p5).matches()) {
            phoneNo = p5;
            Log.d("sendOneByOne phone5 ", "sendOneByOne: phone5=" + phoneNo);
            sendSMSBySMSManager();
        }
        phoneNo = "";
        message = "";
    }

    private void sendSMSBySMSManager() {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNo, null, message, null, null);
    }

    public void soundClick(View view) {
        if (player.isPlaying()) {
            player.stop();
        } else {
            player = MediaPlayer.create(this, R.raw.siren);
            player.start();
            player.setLooping(false);
        }
    }

    //Code for flashlight
    public void toggle(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Switch On")) {
            button.setText(R.string.switch_off_text);
            button.setBackgroundResource(R.drawable.btn10);
            torchToggle("on");
        } else {
            button.setText(R.string.switch_on_text);
            button.setBackgroundResource(R.drawable.btn1);
            torchToggle("off");
        }
    }

    private void torchToggle(String command) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually back camera is at 0 position.
            try {
                if (camManager != null) {
                    cameraId = camManager.getCameraIdList()[0];
                }
                if (camManager != null) {
                    if (command.equals("on")) {
                        camManager.setTorchMode(cameraId, true);   // Turn ON
                        isTorchOn = true;
                    } else {
                        camManager.setTorchMode(cameraId, false);  // Turn OFF
                        isTorchOn = false;
                    }
                }
            } catch (CameraAccessException e) {
                e.getMessage();
            }
        }
    }

    public void settingClick(View view) {
        Intent intent = new Intent(Home.this, SettingActivity.class);
        startActivity(intent);
    }

    private String getRedMsg() {
        if (sharedPreferences.contains("redMsg"))
            return sharedPreferences.getString("redMsg", "");
        else
            return "Hi, I am in trouble. Please help..!!!";
    }

    private String getYellowMsg() {
        if (sharedPreferences.contains("yellowMsg"))
            return sharedPreferences.getString("yellowMsg", "");
        else
            return "Hi, just being cautious..!!!";
    }

    private String getGreenMsg() {
        if (sharedPreferences.contains("greenMsg"))
            return sharedPreferences.getString("greenMsg", "");
        else
            return "Hi, sending a status update..!!!";
    }

    public void morebutton(View view) {
        Intent intent = new Intent(Home.this, More.class);
        startActivity(intent);
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                Home.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                Home.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();
                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);
                Log.d("getLocation", "getLocation: lat = " + latitude + " lon = " + longitude);
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
