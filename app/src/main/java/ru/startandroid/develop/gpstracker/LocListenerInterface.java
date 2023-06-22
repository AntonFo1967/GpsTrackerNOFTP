package ru.startandroid.develop.gpstracker;

import android.location.Location;
import android.view.View;

public interface LocListenerInterface {
    void onClick(View v);

    public void OnlocationChanged(Location loc);
}
