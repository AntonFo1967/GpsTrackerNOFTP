package ru.startandroid.develop.gpstracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import android.content.SharedPreferences.Editor;

public class MainActivity extends AppCompatActivity implements LocListenerInterface, View.OnClickListener {
    private TextView tvCoordinate, tvTime, tvVelocity, tvStatusGPS, tvBattery;
    private LocationManager locationManager;
    private MyLocListener myLocListener;

    private ArrayList<String> gpsDataHistory;
    private ArrayAdapter<String> gpsAdapter;

    private ArrayList<String> msgDataHistory;
    private ArrayAdapter<String> msgAdapter;
    private Button start_Button, sent_Button;

    private int i = 1;
    private boolean isLocEnable = false;
    private final long readGPSDelay = 100;
    private EditText ipString, portString;
    private CheckBox isBroadCast, isUDP;

    private SharedPreferences savePreferences;

    //    private final String constIPAddress = "94.228.243.168";
    private final String constIPAddress = "172.30.34.70";
    private final String constIPBroadcast = "255.255.255.255";
    private final Integer constPort = 40001;
    private SensorManager sensorManager;
    private Sensor sensorAccel;
    private Sensor sensorLinAccel;
    private Sensor sensorGravity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ListView GPSDataHistory = findViewById((R.id.GPSDataHistoryList));
        gpsDataHistory = new ArrayList<>();
        gpsAdapter = new ArrayAdapter<>(this, R.layout.gps_list_items, gpsDataHistory);
        GPSDataHistory.setAdapter(gpsAdapter);

        ListView MSGDataHistory = findViewById((R.id.MSGDataHistoryList));
        msgDataHistory = new ArrayList<>();
        msgAdapter = new ArrayAdapter<>(this, R.layout.msg_list_items, msgDataHistory);
        MSGDataHistory.setAdapter(msgAdapter);


        start_Button = findViewById(R.id.startButton);
        start_Button.setOnClickListener(this);

        sent_Button = findViewById(R.id.sentButton);
        sent_Button.setOnClickListener(this);

        ipString = findViewById(R.id.IPAddress);
        portString = findViewById(R.id.Port);

        isBroadCast = findViewById(R.id.checkBoxbroadcast);
        isBroadCast.setOnClickListener(this);

        isUDP = findViewById(R.id.checkBoxUDP);

        tvCoordinate = findViewById(R.id.tvLocationGPS);
        tvTime = findViewById(R.id.tvTime);
        tvVelocity = findViewById(R.id.tvVelocity);
        tvStatusGPS = findViewById(R.id.tvStatusGPS);
        tvBattery = findViewById(R.id.tvBattery);

        initSensors();

    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(listener, sensorAccel,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, sensorLinAccel,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, sensorGravity,
                SensorManager.SENSOR_DELAY_NORMAL);

    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(listener);
        //locationManager.removeUpdates(myLocListener);

    }


    @Override
    protected void onStop() {
        super.onStop();
        savePreferencesToFile();
    }

    private void savePreferencesToFile() {
        savePreferences = getPreferences(MODE_PRIVATE);
        Editor ed = savePreferences.edit();
        ed.putString("SAVED_IP", String.valueOf(ipString.getText()));
        ed.putString("SAVED_PORT", String.valueOf(portString.getText()));
        ed.putBoolean("IChecked", isBroadCast.isChecked());
        ed.apply();
    }

    private void loadPreferencesFromFile() {
        savePreferences = getPreferences(MODE_PRIVATE);

        String savedText = savePreferences.getString("SAVED_IP", "");
        ipString.setText(savedText);

        savedText = savePreferences.getString("SAVED_PORT", "");
        portString.setText(savedText);

    }

    private void initSensors() {
        updateMSG("Init sensors");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        myLocListener = new MyLocListener();
        myLocListener.setLocListenerInterface(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorLinAccel = sensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        tvBattery.setText(getString(R.string.power_dialog)+": " + getBatteryLevel());



//        loadPreferencesFromFile();

    }


    String format(float[] values) {
        return String.format("%1$.1f\t%2$.1f\t%3$.1f", values[0], values[1],
                values[2]);
    }


    float[] valuesAccel = new float[3];
    float[] valuesAccelMotion = new float[3];
    float[] valuesAccelGravity = new float[3];
    float[] valuesLinAccel = new float[3];
    float[] valuesGravity = new float[3];

    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    for (int i = 0; i < 3; i++) {
                        valuesAccel[i] = event.values[i];
                        valuesAccelGravity[i] = (float) (0.1 * event.values[i] + 0.9 * valuesAccelGravity[i]);
                        valuesAccelMotion[i] = event.values[i]
                                - valuesAccelGravity[i];
                    }
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.arraycopy(event.values, 0, valuesLinAccel, 0, 3);
                    break;
                case Sensor.TYPE_GRAVITY:
                    System.arraycopy(event.values, 0, valuesGravity, 0, 3);
                    break;
            }

        }

    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                startGPSTracking();
                break;
            case R.id.sentButton:
                sent_ONE_pack();
            case R.id.checkBoxbroadcast:
                if (isBroadCast.isChecked()) {

                    savePreferencesToFile();
                    ipString.setText(constIPBroadcast);
                    break;
                } else
                    loadPreferencesFromFile();

                break;
            default:
                break;
        }
    }

    void startGPSTracking() {
        if (isLocEnable) {
            locationManager.removeUpdates(myLocListener);
            isLocEnable = false;
            start_Button.setText("START GPS");
            updateMSG("STOP pressed");
        } else {
            checkPermissions();
            isLocEnable = true;
            start_Button.setText("STOP GPS");
            updateMSG("START pressed");

        }
    }

    void sent_ONE_pack() {
        String msg = "Test string from Anton";
        new Thread(() -> {

            try {
                DatagramSocket udpSocket = new DatagramSocket(constPort);
                byte[] buf = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(constIPAddress), constPort);
                udpSocket.send(packet);
                udpSocket.close();
            } catch (SocketException e) {
                updateMSG("Socket Error:" + e);
            } catch (IOException e) {
                updateMSG("IO Error:" + e);
            }
        }).start();
    }

    private void updateList(String gpsData) {
        gpsDataHistory.add(0, gpsData);
        gpsAdapter.notifyDataSetChanged();

    }


    private void updateMSG(String msgData) {
        msgDataHistory.add(0, msgData);
        msgAdapter.notifyDataSetChanged();
    }

    private String createDataString(Location loc) {
        return (String.format("#" + i + " Time : %1$td %1$tb %1$tY %1$ta %tT" + "| lat = %2$.4f" + "| lon = %3$.4f" + " | Speed= %4$.1f" + System.lineSeparator(),
                loc.getTime(),
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getSpeed() * 3.6));

    }

    private String createDataStringSensors() {
        return (String.format("@" + i +
                " |Ac: " + format(valuesAccel) +
                " |AM: " + format(valuesAccelMotion) +
                " |AG: " + format(valuesAccelGravity) +
                " |LA: " + format(valuesLinAccel) +
                " |Gr: " + format(valuesGravity)
                + System.lineSeparator()));

    }

    public String getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);


        return ((float) level / (float) scale) * 100.0f + "%";
    }

    private void updateDistance(Location loc)
    {
        updateMSG("Updating");

        tvCoordinate.setText(String.format("lat = %1$.4f, lon = %2$.4f",
                loc.getLatitude(), loc.getLongitude()));


        tvTime.setText(String.format(getString(R.string.time_dialog) + " : %1$td %1$tb %1$tY %1$ta %tT", loc.getTime()));
        tvVelocity.setText(getString(R.string.velocity_dialog) + " = " +loc.getSpeed()*3.6);
        tvStatusGPS.setText(getString(R.string.GPSStatus_dialog) + " = " +loc.getProvider());
        tvBattery.setText(getString(R.string.power_dialog)+": " + getBatteryLevel());

        updateList(createDataString(loc));
        updateList(createDataStringSensors());

        if (isUDP.isChecked()) {
            UDPUpdate(createDataString(loc));
            UDPUpdate(createDataStringSensors());
        }
        i++;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode ==100 && grantResults[0] == RESULT_OK)
        {
            checkPermissions();
            updateMSG("Done GPS permission");
            //return;
        }
        else
        {
            updateMSG("No GPS permission");
        }
    }

    private void  checkPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,},100);
        }
        else
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, readGPSDelay,0,myLocListener);
            updateMSG("Try to update");

        }
    }

    private void UDPUpdate(String gpsData) {
        updateMSG("gps data received");
        new Thread(() -> {

            try

            {
                DatagramSocket udpSocket = new DatagramSocket(constPort);
                byte[] buf = gpsData.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(constIPAddress), constPort);
                udpSocket.send(packet);
                udpSocket.close();
            }

            catch(SocketException e)
            {
                updateMSG("Socket Error:" + e);
            }

            catch(IOException e)
            {
                updateMSG("IO Error:" + e);
            }
        }).start();
    }

    @Override
    public void OnlocationChanged(Location loc) {
        updateDistance(loc);
    }

}