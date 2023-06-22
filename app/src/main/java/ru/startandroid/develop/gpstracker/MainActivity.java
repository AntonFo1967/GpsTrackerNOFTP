package ru.startandroid.develop.gpstracker;



import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MainActivity extends AppCompatActivity implements LocListenerInterface, View.OnClickListener {
    private TextView tvCoordinate, tvTime, tvVelocity, tvStatusGPS;
    private LocationManager locationManager;
    private MyLocListener myLocListener;

    private ArrayList<String> gpsDataHistory;
    private ArrayAdapter<String> gpsAdapter;

    private ArrayList<String> msgDataHistory;
    private ArrayAdapter<String> msgAdapter;
    private Button start_Button, sent_Button;

    private int i = 1;
    private boolean isLocEnable = false;
    private long readGPSDelay = 500;
    private EditText ipString, portString;

    private SharedPreferences savePreferences;

    private String constIPAddress = "94.228.243.168";
    //private String constIPAddress = "172.30.34.70";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ListView GPSDataHistory = (ListView) findViewById((R.id.GPSDataHistoryList));
        gpsDataHistory = new ArrayList<>();
        gpsAdapter = new ArrayAdapter<>(this,R.layout.gps_list_items, gpsDataHistory);
        GPSDataHistory.setAdapter(gpsAdapter);

        ListView MSGDataHistory = (ListView) findViewById((R.id.MSGDataHistoryList));
        msgDataHistory = new ArrayList<>();
        msgAdapter = new ArrayAdapter<>(this,R.layout.msg_list_items, msgDataHistory);
        MSGDataHistory.setAdapter(msgAdapter);


        start_Button = (Button) findViewById(R.id.start_button);
        start_Button.setOnClickListener(this);

        sent_Button = (Button) findViewById(R.id.sent_button);
        sent_Button.setOnClickListener(this);

        ipString = findViewById(R.id.IPAddress);
        portString = findViewById(R.id.Port);

        init();

    }




    @Override
    protected void onPause() {
        super.onPause();
        //locationManager.removeUpdates(myLocListener);
        savePreferencesToFile();
    }

    @Override
    protected void onResume() {

        super.onResume();
        loadPreferencesFromFile();
        //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        //        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, readGPSDelay, 0, myLocListener);

    }


    private void savePreferencesToFile() {
        savePreferences = getPreferences(MODE_PRIVATE);
        Editor ed = savePreferences.edit();
        ed.putString("SAVED_IP", String.valueOf(ipString.getText()));
        ed.putString("SAVED_PORT", String.valueOf(portString.getText()));
        ed.commit();
    }

    private void loadPreferencesFromFile(){
        savePreferences = getPreferences(MODE_PRIVATE);

        String savedText = savePreferences.getString("SAVED_IP", "");
        ipString.setText(savedText);

        savedText = savePreferences.getString("SAVED_PORT", "");
        portString.setText(savedText);
    }

    private void init() {
        updateMSG("Init");

        tvCoordinate = findViewById(R.id.tvLocationGPS);
        tvTime = findViewById(R.id.tvTime);
        tvVelocity = findViewById(R.id.tvVelocity);
        tvStatusGPS = findViewById(R.id.tvStatusGPS);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocListener = new MyLocListener();
        myLocListener.setLocListenerInterface(this);
        loadPreferencesFromFile();


        //checkPermissions();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                startGPSTracking();
                break;
            case R.id.sent_button:
                sent_ONE_pack();
                break;
            default:
                break;
        }
    }

    void startGPSTracking()
    {
        if (isLocEnable)
        {
            locationManager.removeUpdates(myLocListener);
            isLocEnable = false;
            start_Button.setText("START");
            updateMSG("STOP pressed");
        }

        else
        {
            checkPermissions();
            isLocEnable = true;
            start_Button.setText("STOP");
            updateMSG("START pressed");

        }
    }

    void sent_ONE_pack(){
        String msg = "*/1/1/0/55/0/0-0/#";
        updateMSG("Sent pressed");
        new Thread(new Runnable() {

            @Override
            public void run() {

                try

                {
                    DatagramSocket udpSocket = new DatagramSocket(40001);
                    byte[] buf = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(constIPAddress), 40001);
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
            }
        }).start();
    }

    private void updateList(String gpsData)
    {
        gpsDataHistory.add(0, gpsData);
        gpsAdapter.notifyDataSetChanged();

    }


    private void updateMSG(String msgData)
    {
        msgDataHistory.add(0,msgData);
        msgAdapter.notifyDataSetChanged();
    }

    private String createDataString(Location loc)
    {
        return (String.format("#"+i+" Time : %1$td %1$tb %1$tY %1$ta %tT"+ "| lat = %2$.4f"+ "| lon = %3$.4f"+" | Speed= %4$.1f"+System.lineSeparator(),
                loc.getTime(),
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getSpeed()*3.6));

    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateDistance(Location loc)
    {
        updateMSG("Updating");

        tvCoordinate.setText(String.format("lat = %1$.4f, lon = %2$.4f",
                loc.getLatitude(), loc.getLongitude()));


        tvTime.setText(String.format("Time : %1$td %1$tb %1$tY %1$ta %tT", loc.getTime()));


        tvVelocity.setText("Speed = "+loc.getSpeed()*3.6);
        tvStatusGPS.setText("Provider = "+loc.getProvider());

        updateList(createDataString(loc));
        UDPUpdate(createDataString(loc));

        i++;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode ==100 && grantResults[0] == RESULT_OK)
        {
            checkPermissions();
            //Toast.makeText(this, "Done GPS permission", Toast.LENGTH_SHORT).show();
            updateMSG("Done GPS permission");
            //return;
        }
        else
        {
            //Toast.makeText(this, "No GPS permission", Toast.LENGTH_SHORT).show();
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
            //Toast.makeText(this, "Try update", Toast.LENGTH_SHORT).show();
            updateMSG("Try to update");

        }
    }

    private void UDPUpdate(String gpsData) {
        String msg = gpsData;
        updateMSG("gps data received");
        new Thread(new Runnable() {

            @Override
            public void run() {

                try

                {
                    DatagramSocket udpSocket = new DatagramSocket(40001);
                    byte[] buf = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(constIPAddress), 40001);
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
            }
        }).start();
    }

    @Override
    public void OnlocationChanged(Location loc) {
        updateDistance(loc);
    }

}