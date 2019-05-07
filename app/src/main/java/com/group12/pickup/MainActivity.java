package com.group12.pickup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.group12.pickup.Model.PlaceInfo;
import com.group12.pickup.Model.WindowInfoAdapter;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marker;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MainActivity";

    private Boolean mLocationPermissionsGranted = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        new Drawer(toolbar, drawerLayout, navigationView);

        //Start map by getting permissions
        getLocationPermission();
    }


    /****************************************************
     *           Getting Location Permissions           *
     ****************************************************/


    private void getLocationPermission() {

        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationPermissionsGranted = true;
            initMap();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult: called.");

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "onRequestPermissionsResult: permission failed");
            return;
        }

        Log.d(TAG, "onRequestPermissionsResult: permission granted");
        mLocationPermissionsGranted = true;

        //initialize our map
        initMap();
    }


    /****************************************************
     *          Create Map & Associated Buttons         *
     ****************************************************/


    private void initMap() {

        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        getDeviceLocation();
        addButtons();
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }


    private void addButtons() {

        Button selectDestination = findViewById(R.id.select_destination);
        selectDestination.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);

                } catch (Exception e) {

                    Log.e(TAG, "Google Play Services Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        ImageView gps = findViewById(R.id.ic_gps);
        gps.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        ImageView info = findViewById(R.id.ic_info);
        info.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                try {

                    if(marker.isInfoWindowShown())
                        marker.hideInfoWindow();

                    else
                        marker.showInfoWindow();

                } catch (NullPointerException e) {

                    Log.e(TAG, "NullPointerException: " + e.getMessage());
                }
            }
        });
    }


    /****************************************************
     *          Get Device Location & Focus Map         *
     ****************************************************/


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                            Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MainActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();
        mMap.setInfoWindowAdapter(new WindowInfoAdapter(this));

        if(placeInfo != null)
            try {

            String info = placeInfo.toString();

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(placeInfo.getName())
                    .snippet(info);

            marker = mMap.addMarker(options);

            } catch (NullPointerException e) {

                Log.e(TAG, "NullPointerException: " + e.getMessage());
            }

         else {

            mMap.addMarker(new MarkerOptions().position(latLng));
        }
    }


    /****************************************************
     *     Google Place Picker - Choose Destination     *
     ****************************************************/


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {

            if (resultCode == RESULT_OK) {

                Place place = PlacePicker.getPlace(this, data);
                PlaceInfo placeInfo = new PlaceInfo(place.getName().toString(),
                        place.getAddress().toString(),
                        place.getPhoneNumber().toString(),
                        place.getId(),
                        place.getWebsiteUri(),
                        place.getLatLng(),
                        place.getRating());

                moveCamera(placeInfo.getLatLng(), DEFAULT_ZOOM, placeInfo);

                Log.i(TAG, "Place found:\n" + placeInfo.toString());
            }
        }
    }
}
