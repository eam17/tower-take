package com.example.myapplication;

import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import static com.example.myapplication.MapsHolder.cirUser;
import static com.example.myapplication.MapsHolder.currentLocation;
import static com.example.myapplication.MapsHolder.freelook;
import static com.example.myapplication.MapsHolder.mMap;
import static com.example.myapplication.MapsHolder.mrkUser;

public class CameraFollower implements LocationListener {

    @Override
    public void onLocationChanged(Location loc) {

        currentLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
        if (!freelook) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
        mrkUser.setPosition(currentLocation);
        cirUser.setCenter(currentLocation);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}