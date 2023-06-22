package ru.startandroid.develop.gpstracker;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.annotation.NonNull;

class MyLocListener implements LocationListener {
    private LocListenerInterface locListenerInterface;


    @Override
    public void onLocationChanged(@NonNull Location location) {
        locListenerInterface.OnlocationChanged(location);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    public  void  setLocListenerInterface(LocListenerInterface locListenerInterface)
    {
        this.locListenerInterface = locListenerInterface;
    }
    }