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

public class TowerTaker extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCircleClickListener {

    public static GoogleMap mMap;
    public static LatLng currentLocation;
    public static Marker mrkUser;
    public static Circle cirUser;
    public static boolean freelook = false;

    HashMap<Marker, Integer> mapMarkers;
    HashMap<Marker, Circle> mapCircle;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private boolean mLocationPermissionGranted;

    public static MenuItem chkCamLock;
    public static MenuItem miPoints;
    public static MenuItem miTowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower_taker);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = nav.getMenu();
        miPoints = menu.findItem(R.id.nav_points);
        miTowers = menu.findItem(R.id.nav_towers);

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
                mLocationPermissionGranted = true;
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
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionGranted = false;
                        return;
                    }
                }
                mLocationPermissionGranted = true;
                initMap();
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCircleClickListener(this);
        mMap.setMinZoomPreference(14);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14));
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new CameraFollower();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) 2500, 5f, locationListener);

        mrkUser = mMap.addMarker(new MarkerOptions().title("You").position(new LatLng(0, 0)));
        cirUser = mMap.addCircle(new CircleOptions().radius(100).strokeColor(Color.BLACK).fillColor(Color.argb(100, 0, 255, 255)).center(new LatLng(0, 0)));

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

                } else {
                    //Not a valid user
                }
            }
            rsSelect.close();
            psSelect.close();

            Log.v(this.getClass().toString(), "Forts loaded");

            getPoints();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String formatTowerName(String strTowerOwner) {
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
        if (getDistance(latlng, currentLocation) > 100) {

            Toast.makeText(this, "Too far reach!", Toast.LENGTH_SHORT).show();
            return;
        }

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


            } else {
                Log.d("", "onMapLongClick: I found nothing lol");
            }
            rsSelect.close();
            psSelect.close();

            Log.v(this.getClass().toString(), "Forts loaded");
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

            addPoints(-1);
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

        chkCamLock.setChecked(false);
        setFreelook(true);

        if (getDistance(marker.getPosition(), currentLocation) > 100) {
            Toast.makeText(this, "Too far reach!", Toast.LENGTH_SHORT).show();
            return false;
        }

        int towerid = mapMarkers.get(marker);
        if (towerid < 0) {
            Log.d("", "onMarkerClick: Not valid tower " + towerid);
        }

        try {
            int points = getPoints();
            if (points <= 0) {
                return false;
            }

            String strStatement = "update fort set idaccount = ? where id = ?";
            PreparedStatement psUpdate = Settings.getInstance().getConnection().prepareStatement(strStatement);
            psUpdate.setInt(1, Settings.userid);
            psUpdate.setInt(2, towerid);
            psUpdate.executeUpdate();

            addPoints(-1);

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
        String strStatement = "update account set points = points + ? where id = ?";
        PreparedStatement psUpdate = Settings.getInstance().getConnection().prepareStatement(strStatement);
        psUpdate.setInt(1, add);
        psUpdate.setInt(2, Settings.userid);
        psUpdate.executeUpdate();

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_lock_camera) {
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
            // Handle the camera action
        } else if (id == R.id.nav_towers) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
