package com.group12.pickup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.group12.pickup.Model.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Drawer drawer;
    private GoogleMap mMap;
    private AutoCompleteTextView search;
    private ImageView gps;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter autocomplete;
    private GeoDataClient geoDataClient;
    private PlaceInfo placeInfo;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MapActivity";

    private Boolean mLocationPermissionsGranted = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final LatLngBounds bounds = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        drawer = new Drawer(toolbar, fab, drawerLayout, navigationView);

        getLocationPermission();
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

        getDeviceLocation();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }


    private void initMap() {

        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(NavigationActivity.this);

        search = findViewById(R.id.search);

        search.setOnItemClickListener(autoCompleteListener);

        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                if (i == EditorInfo.IME_ACTION_SEARCH || i == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    geoLocate();
                }
                return false;
            }
        });

        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this);
        autocomplete = new PlaceAutocompleteAdapter(this, geoDataClient, bounds, null);

        search.setAdapter(autocomplete);

        gps = findViewById(R.id.ic_gps);
        gps.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });
    }


    private void geoLocate() {

        Log.d(TAG, "geoLocate: geolocating");

        String searchString = search.getText().toString();

        Geocoder geocoder = new Geocoder(NavigationActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(NavigationActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

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

            mLocationPermissionsGranted = false;
            Log.d(TAG, "onRequestPermissionsResult: permission failed");
            return;
        }

        Log.d(TAG, "onRequestPermissionsResult: permission granted");
        mLocationPermissionsGranted = true;

        //initialize our map
        initMap();
    }

    /****************************************************
     *           Getting Google Places Object           *
     ****************************************************/

    private AdapterView.OnItemClickListener autoCompleteListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            final AutocompletePrediction item = autocomplete.getItem(i);
            final String placeId = item.getPlaceId();

            try {
                geoDataClient.getPlaceById(placeId).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {

                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) {

                        if (task.isSuccessful()) {
                            PlaceBufferResponse places = task.getResult();
                            final Place place = places.get(0);

                            placeInfo = new PlaceInfo(place.getName().toString(),
                                    place.getAddress().toString(),
                                    place.getPhoneNumber().toString(),
                                    place.getId(),
                                    place.getWebsiteUri(),
                                    place.getLatLng(),
                                    place.getRating());

                            moveCamera(placeInfo.getLatLng(), DEFAULT_ZOOM, placeInfo.getName());

                            Log.i(TAG, "Place found:\n" + placeInfo.toString());
                            places.release();
                        } else {
                            Log.e(TAG, "Place not found.");
                        }
                    }
                });

            } catch(SecurityException e) {

                //This will NEVER throw an exception. Permission is gotten on startup. Android sucks.
            }
        }
    };

}
