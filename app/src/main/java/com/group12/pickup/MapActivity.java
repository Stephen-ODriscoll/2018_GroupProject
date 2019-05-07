package com.group12.pickup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.group12.pickup.Model.DirectionsJSONParser;
import com.group12.pickup.Model.WindowInfoAdapter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker destinationMarker;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "Map";

    private LatLng myLocation = null;
    private String distance;
    private String duration;
    private Place pickupPlace;
    private Place destinationPlace;

    private Boolean mLocationPermissionsGranted = false;
    private Boolean track = true;
    private Boolean estimate = false;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;
    private static final int CONFIRMATION_REQUEST = 3;
    private final int CHANGE_LOCATION_REQUEST = 5;
    private final int ESTIMATE_REQUEST = 6;

    private String status = "unconfirmed";
    private String previousStatus = "unconfirmed";

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = database.collection("journeys");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_navigation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        new Drawer(toolbar, drawerLayout, navigationView, MapActivity.this);

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
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
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
                    startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);

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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
            }
        });
    }


    /****************************************************
     *          Get Device Location & Focus Map         *
     ****************************************************/


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        final FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                updateLocation(fusedLocationProviderClient);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void updateLocation(final FusedLocationProviderClient fusedLocationProviderClient) {

        (new Thread() {

            private boolean first = true;

            @Override
            public void run() {

                while(true) {

                    if(track) {

                        final Task location = fusedLocationProviderClient.getLastLocation();
                        location.addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                if (task.isSuccessful() && track) {
                                    Location currentLocation = (Location) task.getResult();
                                    try {
                                        myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                                    } catch(NullPointerException e) {

                                            Log.e(TAG, "Currect Location Task Returned Null Pointer");
                                    }

                                    if(first) {

                                        Log.d(TAG, "moveCamera: moving the camera to: lat: " + myLocation.latitude + ", lng: " + myLocation.longitude);
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                                        first = false;
                                    }

                                } else {
                                    Log.d(TAG, "onComplete: current location is null");
                                }
                            }
                        });

                        try {
                            sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }).start();
    }


    public void displayTrips() {

        startActivity(new Intent(MapActivity.this, TripsActivity.class));
    }

    public void rateReview() {

        startActivity(new Intent(MapActivity.this, ReviewActivity.class));
    }


    public void changeLocation() {

        //If we have already set our location this resets it
        if(!track) {

            track = true;
            mMap.clear();
            mMap.setMyLocationEnabled(true);
            return;
        }

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(MapActivity.this), CHANGE_LOCATION_REQUEST);

        } catch (Exception e) {

            Log.e(TAG, "Google Play Services Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void setPickupMarker(Place place, float zoom) {

        Log.d(TAG, "moveCamera: moving the camera to: " + place.getLatLng());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), zoom));
        mMap.setMyLocationEnabled(false);
        mMap.clear();

        track = false;
        myLocation = place.getLatLng();
        this.pickupPlace = place;

        MarkerOptions options = new MarkerOptions()
                .title(place.getName().toString())
                .position(place.getLatLng())
                .snippet("Alternate Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        mMap.addMarker(options);
    }


    public void setDestinationMarker(Place place, float zoom) {

        Log.d(TAG, "moveCamera: moving the camera to: " + place.getLatLng());

        WindowInfoAdapter windowInfo = new WindowInfoAdapter(this);
        this.destinationPlace = place;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), zoom));
        mMap.setInfoWindowAdapter(windowInfo);
        mMap.clear();

        if(!track) {

            MarkerOptions options = new MarkerOptions()
                    .title(pickupPlace.getName().toString())
                    .position(pickupPlace.getLatLng())
                    .snippet("Alternate Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

            mMap.addMarker(options);
        }

        MarkerOptions options = new MarkerOptions()
                .title(place.getName().toString())
                .position(place.getLatLng());

        destinationMarker = mMap.addMarker(options);

        ArrayList<LatLng> points = new ArrayList<>();
        points.add(myLocation);
        points.add(destinationMarker.getPosition());

        makeRoute(points);
    }


    /****************************
     *     Price Estimation     *
     ****************************/

    public void estimate() {

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(MapActivity.this), ESTIMATE_REQUEST);

        } catch (Exception e) {

            Log.e(TAG, "Google Play Services Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /****************************************************
     *     Google Place Picker - Choose Destination     *
     ****************************************************/


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {

            if (resultCode == RESULT_OK) {

                Place place = PlacePicker.getPlace(this, data);
                setDestinationMarker(place, DEFAULT_ZOOM);
            }
        }

        else if (requestCode == CHANGE_LOCATION_REQUEST) {

            if (resultCode == RESULT_OK) {

                Place place = PlacePicker.getPlace(this, data);
                setPickupMarker(place, DEFAULT_ZOOM);
            }
        }

        else if(requestCode == ESTIMATE_REQUEST) {

            if (resultCode == RESULT_OK) {

                Place place = PlacePicker.getPlace(this, data);

                ArrayList<LatLng> points = new ArrayList<>();
                points.add(myLocation);
                points.add(place.getLatLng());
                estimate = true;

                makeRoute(points);
            }
        }

        else if(requestCode == CONFIRMATION_REQUEST && resultCode == RESULT_OK) {

            final String user = data.getStringExtra("user");
            final String date = data.getStringExtra("date");
            final String license = data.getStringExtra("license");
            boolean requested = data.getBooleanExtra("requested", false);

            mMap.setInfoWindowAdapter(new WindowInfoAdapter(this));

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {


                }
            });

            if (requested) {

                (new Thread() {

                    @Override
                    public void run() {

                        while (!status.equals("completed")) {

                            notify(status);

                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Log.e("Checking", "Not Done");
                            check(user, date);
                        }

                        Log.e("Checking", "Done");
                        status = "unconfirmed";

                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext(), "Channel")
                                .setSmallIcon(android.support.v4.R.drawable.notification_icon_background)
                                .setContentTitle("Trip Status")
                                .setContentText("Arrived at Destination, review your trip from the trips menu")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true);

                        createNotificationChannel();

                        //Add notification to our manager and start it
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
                        notificationManager.notify(4, mBuilder.build());
                    }

                    private void notify(String status) {

                        if(status.equals(previousStatus))
                            return;

                        previousStatus = status;
                        createNotificationChannel();

                        if(status.equals("confirmed")) {

                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext(), "Channel")
                                    .setSmallIcon(android.support.v4.R.drawable.notification_icon_background)
                                    .setContentTitle("Trip Status")
                                    .setContentText("Car is en route to pickup, License: " + license)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true);

                            //Add notification to our manager and start it
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
                            notificationManager.notify(4, mBuilder.build());
                        }

                        else if(status.equals("waiting")) {

                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext(), "Channel")
                                    .setSmallIcon(android.support.v4.R.drawable.notification_icon_background)
                                    .setContentTitle("Trip Status")
                                    .setContentText("Car is waiting at pickup")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true);

                            //Add notification to our manager and start it
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
                            notificationManager.notify(4, mBuilder.build());
                        }
                    }
                }).start(); // Start this thread
            }
        }
    }


    private void makeToast(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Channel", "PickUp", importance);
            channel.setDescription("This is my Notification Channel");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    /***************************************************
     *          Creating Route to Destination          *
     ***************************************************/


    private void makeRoute(ArrayList<LatLng> markerPoints) {

        // Checks, whether start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            // Start downloading json data from Google Directions API
            new DownloadTask().execute(url);
        }
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList points;
            PolylineOptions lineOptions = new PolylineOptions();
            final Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

            distance = result.get(0).get(0).get("distance");
            duration = result.get(0).get(0).get("duration");

            if(estimate) {

                int number = 4;

                if(duration.contains("mins"))
                    number = 5;

                estimate = false;
                makeToast("Estimated Price: €" + round(Double.valueOf(duration.substring(0, duration.length()-number)) * 1.25, 2));
                return;
            }

            destinationMarker.setSnippet("Distance: " + distance + "\nEstimated Time: " + duration);

            //At least 2 minutes but not an hour or more
            if(duration.contains("mins") && !duration.contains("hour"))
                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {

                        try {
                            List<Address> addresses = geocoder.getFromLocation(myLocation.latitude, myLocation.longitude, 1);

                            Intent i = new Intent(MapActivity.this, ConfirmationActivity.class);
                            i.putExtra("Collection", addresses.get(0).getAddressLine(0));
                            i.putExtra("Destination", destinationPlace.getAddress());
                            i.putExtra("Distance", distance);
                            i.putExtra("Duration", duration);
                            i.putExtra("Latitude", myLocation.latitude);
                            i.putExtra("Longitude", myLocation.longitude);
                            i.putExtra("requested", false);

                            startActivityForResult(i, CONFIRMATION_REQUEST);

                        } catch (IOException e) {

                            e.printStackTrace();
                        }
                    }
                });

            else
                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {

                        makeToast("Journey too Short or too Long");
                    }
                });

            if(!destinationMarker.isInfoWindowShown())
                destinationMarker.showInfoWindow();

            for (int i = 1; i < result.size(); i++) {
                points = new ArrayList();
                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }


    public static double round(double value, int places) {

        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


    private void check(String user, String date) {

        collectionReference
                .whereEqualTo("user", user)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("DataBaseRead", "Success");

                            if (task.getResult().size() <= 1) {
                                try {
                                    status = (String) task.getResult().getDocuments().get(0).getData().get("status");
                                } catch(Exception e) {
                                    Log.e("Checking Confirmation", "Failed");
                                }
                            }
                        } else {
                            Log.e("DataBaseRead", "Failed");
                        }

                    }
                });
    }


    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String key = "key=" + getResources().getString(R.string.google_maps_key);
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + key + "&" + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {

        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
