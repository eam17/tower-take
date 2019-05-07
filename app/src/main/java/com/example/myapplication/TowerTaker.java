package com.example.myapplication;

import android.Manifest;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.SubMenu;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/*
Program: Tower Taker
Programmers: Kat Molostvova & Weston Laity
Date: 6 May 2019
Desc: Tower Taker is a real world game that has you on the move to score more points. Place towers and keep them until the end of the day and you'll be rewarded.
        Take towers from others, and make sure they don't swipe them back. Continue the charge day after day to amass an empire.
 */

public class TowerTaker extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCircleClickListener {

    //Map
    public static GoogleMap mMap;

    //User location
    public static LatLng currentLocation;
    public static Marker mrkUser;
    public static Circle cirUser;

    //Keeping track
    HashMap<Marker, Integer> mapMarkers;
    HashMap<Marker, Circle> mapCircle;

    //Permissions
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //UI Items
    public static MenuItem chkCamLock;
    public static boolean freelook = false;
    public static MenuItem miPoints;
    public static MenuItem miTowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower_taker);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //Left side nav menu assignments
        NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = nav.getMenu();
        miPoints = menu.findItem(R.id.nav_points);
        miTowers = menu.findItem(R.id.nav_towers);

        //Initialize lists for linking
        mapMarkers = new HashMap<Marker, Integer>();
        mapCircle = new HashMap<Marker, Circle>();

        getLocationPermission();
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                initMap();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCircleClickListener(this);

        //Disable scrolling until user selects freelook
        mMap.setMinZoomPreference(14);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14));
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //Get location
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new CameraFollower();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) 2500, 5f, locationListener);

        //Add user's marker to the map
        mrkUser = mMap.addMarker(new MarkerOptions().title("You").position(new LatLng(0, 0)));
        cirUser = mMap.addCircle(new CircleOptions().radius(100).strokeColor(Color.BLACK).fillColor(Color.argb(100, 0, 255, 255)).center(new LatLng(0, 0)));

        //Populate the map with other user's towers
        //Build sql prepared statement
        String strStatement = "select * from fort f join account a on f.idaccount = a.id";
        try {
            PreparedStatement psSelect = Settings.getInstance().getConnection().prepareStatement(strStatement);
            psSelect.execute();

            //Get sql results
            ResultSet rsSelect = psSelect.getResultSet();
            while (rsSelect.next()) {

                int id = rsSelect.getInt("idaccount");
                if (id != -1) {

                    // Add a marker in Sydney and move the camera
                    LatLng latlng = new LatLng(rsSelect.getDouble("lat"), rsSelect.getDouble("long"));
                    String strName = rsSelect.getString("username");
                    int towerid = rsSelect.getInt("id");
                    if (id == Settings.userid) {
                        //Friendly tower
                        Marker m = mMap.addMarker(new MarkerOptions()
                                .position(latlng)
                                .title("Your tower")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mdpi)));
                        mapMarkers.put(m, towerid);

                        Circle c = mMap.addCircle(new CircleOptions()
                                .center(latlng)
                                .radius(100)
                                .strokeColor(Color.BLACK)
                                .fillColor(Color.argb(100, 0, 0, 255)));
                        mapCircle.put(m, c);
                    } else {

                        //Enemy towers
                        Marker m = mMap.addMarker(new MarkerOptions()
                                .position(latlng)
                                .title(formatTowerName(strName))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mdpi)));
                        mapMarkers.put(m, towerid);

                        Circle c = mMap.addCircle(new CircleOptions()
                                .center(latlng)
                                .radius(100)
                                .strokeColor(Color.BLACK)
                                .fillColor(Color.argb(100, 255, 0, 0)));
                        mapCircle.put(m, c);
                    }

                }
            }
            rsSelect.close();
            psSelect.close();

            Log.v(this.getClass().toString(), "Forts loaded");

            //Populate UI
            getPoints();
            getTowerCount();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String formatTowerName(String strTowerOwner) {
        //Formatter for apostraphe s
        if (strTowerOwner.endsWith("s")) {
            strTowerOwner += "'";
        } else {
            strTowerOwner += "'s";
        }
        strTowerOwner += " tower";
        return strTowerOwner;
    }

    @Override
    public void onMapLongClick(LatLng latlng) {
        //This action attempts to places a tower

        if (getDistance(latlng, currentLocation) > 100) {
            Toast.makeText(this, "Too far reach!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Is the closest tower too close to this location?
        //Build sql prepared statement
        String strStatement = "select lat, long, power(lat - ?, 2) + power(long - ?, 2) as diff from fort order by diff";

        try {
            PreparedStatement psSelect = Settings.getInstance().getConnection().prepareStatement(strStatement);
            psSelect.setDouble(1, latlng.latitude);
            psSelect.setDouble(2, latlng.longitude);
            psSelect.execute();

            //Get sql results
            ResultSet rsSelect = psSelect.getResultSet();
            if (rsSelect.next()) {

                LatLng closest = new LatLng(rsSelect.getDouble("lat"), rsSelect.getDouble("long"));
                Log.d("", "onMapLongClick: The distance between these points is " + getDistance(latlng, closest));
                if (getDistance(latlng, closest) < 200) {
                    Toast.makeText(this, "Too close to another tower!", Toast.LENGTH_SHORT).show();
                    return;
                }


            }
            rsSelect.close();
            psSelect.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d("", "onMapLongClick: " + e.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            Log.d("", "onMapLongClick: " + e.toString());
        }

        strStatement = "insert into fort (lat, long, idaccount, iditem) values (?, ?, ?, ?)";
        try {
            PreparedStatement psInsert = Settings.getInstance().getConnection().prepareStatement(strStatement, new String[]{"id"});
            psInsert.setDouble(1, latlng.latitude);
            psInsert.setDouble(2, latlng.longitude);
            psInsert.setInt(3, Settings.userid);
            psInsert.setInt(4, 1);
            psInsert.executeUpdate();


            //TODO: check result code to make sure it went through

            int towerid = -1;
            try (ResultSet generatedKeys = psInsert.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    towerid = (generatedKeys.getInt("id"));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            psInsert.close();
            Toast.makeText(getApplicationContext(), "Fort created.", Toast.LENGTH_LONG).show();

            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title("Your tower")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.mdpi)));
            mapMarkers.put(m, towerid);

            Circle c = mMap.addCircle(new CircleOptions()
                    .center(latlng)
                    .radius(100)
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.argb(100, 0, 0, 255)));
            mapCircle.put(m, c);

            getTowerCount();
            addPoints(-2);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double getDistance(LatLng l1, LatLng l2) {
        return Math.acos(Math.sin(Math.toRadians(l1.latitude)) * Math.sin(Math.toRadians(l2.latitude)) + Math.cos(Math.toRadians(l1.latitude)) *
                Math.cos(Math.toRadians(l2.latitude)) * Math.cos(Math.toRadians(l2.longitude) - Math.toRadians(l1.longitude))) * 6371000;
    }


    @Override
    public void onCircleClick(Circle circle) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        //Take enemy tower
        int towerid = -1;
        if (mapMarkers.get(marker) == null || (towerid = mapMarkers.get(marker)) < 0) {
            Log.d("", "onMarkerClick: Not valid tower " + towerid);
            return false;
        }

        chkCamLock.setChecked(false);
        setFreelook(true);

        //If the marker/tower is within reach
        if (getDistance(marker.getPosition(), currentLocation) > 100) {
            Toast.makeText(this, "Too far reach!", Toast.LENGTH_SHORT).show();
            return false;
        }

        //If they have points, take the tower
        try {
            int points = getPoints();
            if (points <= 0) {
                return false;
            }

            //update for ownership
            String strStatement = "update fort set idaccount = ? where id = ?";
            PreparedStatement psUpdate = Settings.getInstance().getConnection().prepareStatement(strStatement);
            psUpdate.setInt(1, Settings.userid);
            psUpdate.setInt(2, towerid);
            psUpdate.executeUpdate();

            //update points and ui
            addPoints(-1);
            getTowerCount();

            //update tower marker
            marker.setTitle("Your tower");
            mapCircle.get(marker).setFillColor(Color.BLUE);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public int getPoints() throws SQLException, ClassNotFoundException {
        //Gets points from user account

        String strStatement = "select points from account where id = ?";

        PreparedStatement psSelect = Settings.getInstance().getConnection().prepareStatement(strStatement);
        psSelect.setInt(1, Settings.userid);
        psSelect.execute();

        //Get sql results
        ResultSet rsSelect = psSelect.getResultSet();
        int points = 0;
        while (rsSelect.next()) {

            points = rsSelect.getInt("points");
            miPoints.setTitle((points) + " points!");
            return points;
        }
        return 0;
    }

    public void addPoints(int add) throws SQLException, ClassNotFoundException {
        //Adds points to user account

        String strStatement = "update account set points = points + ? where id = ?";
        PreparedStatement psUpdate = Settings.getInstance().getConnection().prepareStatement(strStatement);
        psUpdate.setInt(1, add);
        psUpdate.setInt(2, Settings.userid);
        psUpdate.executeUpdate();

        //Update UI
        getPoints();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tower_taker, menu);

        chkCamLock = menu.findItem(R.id.action_lock_camera);
        chkCamLock.setCheckable(true);
        chkCamLock.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_lock_camera) {
            //Manually change checkbox and set settings
            item.setChecked(!item.isChecked());
            setFreelook(!item.isChecked());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setFreelook(boolean freelook) {

        this.freelook = freelook;
        if (freelook) {
            mMap.getUiSettings().setAllGesturesEnabled(true);
        } else {
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_points) {
            //These are for display
        } else if (id == R.id.nav_towers) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public int getTowerCount() throws SQLException, ClassNotFoundException {
        //Build sql prepared statement
        String strStatement = "select count(id) as towers from fort f where ? = f.idaccount";
        PreparedStatement psSelect = Settings.getInstance().getConnection().prepareStatement(strStatement);
        psSelect.setInt(1, Settings.userid);
        psSelect.execute();

        //Get sql results
        ResultSet rsSelect = psSelect.getResultSet();
        if (rsSelect.next()) {

            int towers = rsSelect.getInt("towers");
            //Update ui
            miTowers.setTitle(towers + " towers!");
            return towers;
        }

        miTowers.setTitle(0 + " towers!");
        return 0;
    }
}
