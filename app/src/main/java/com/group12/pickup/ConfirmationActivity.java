package com.group12.pickup;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.group12.pickup.Model.Car;
import com.group12.pickup.Model.DirectionsJSONParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

public class ConfirmationActivity extends AppCompatActivity {

    LatLng myLocation;
    TextView details;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        Intent i = getIntent();
        String collectionDetails = i.getStringExtra("Collection");
        String destinationDetails = i.getStringExtra("Destination");
        String distance = i.getStringExtra("Distance");
        String duration = i.getStringExtra("Duration");
        double lat = i.getDoubleExtra("Latitude", 0);
        double lng = i.getDoubleExtra("Longitude", 0);

        myLocation = new LatLng(lat, lng);

        TextView collection = findViewById(R.id.collection);
        TextView destination = findViewById(R.id.destination);
        details = findViewById(R.id.details);

        int number = 4;
        if(duration.contains("mins"))
            number = 5;

        collection.setText(collectionDetails.replace(", ", ",\n"));
        destination.setText(destinationDetails.replace(", ", ",\n"));
        details.setText("Distance: " + distance + "\nCost: €" +
            round(Double.valueOf(duration.substring(0, duration.length()-number)) * 1.25, 2) +
                "\nEstimated Driving Time: " + duration);

        getNearestFree();

        Button advance = findViewById(R.id.advance);
        advance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });
    }


    public static double round(double value, int places) {

        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


    @SuppressLint("StaticFieldLeak")
    public void getNearestFree (){

        new AsyncTask<Void, Integer, Void>() {

            private String result = "";

            @Override
            protected Void doInBackground(Void... args) {

                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpGet get = new HttpGet("https://webapp-5454.firebaseio.com/cars.json");
                    HttpResponse entity = client.execute(get);
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getEntity().getContent()));

                    result = in.readLine();

                } catch(IOException e) {
                    e.printStackTrace();
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void useless) {

                String[] temp = result.split("[,:\"{}]");    //,|:|"|\{|\}
                ArrayList<String> lines = new ArrayList<>();
                Car nearest = null;

                for(int i = 0; i < temp.length; i++)
                    if(!temp[i].equals(""))
                        lines.add(temp[i]);

                for(int i = 0; i < lines.size(); i+=7) {

                    Car car = new Car(lines.get(i), lines.get(i+2), lines.get(i+4), lines.get(i+6), myLocation);

                    if(nearest == null || car.getDistance() < nearest.getDistance())
                        if(car.getStatus().equals("free"))
                            nearest = car;
                }

                ArrayList<LatLng> points = new ArrayList<>();
                points.add(myLocation);
                points.add(nearest.getPosition());

                estimatedWait(points);
            }
        }.execute();
    }

    /******************************************
     *          Estimating Wait Time          *
     ******************************************/


    private void estimatedWait(ArrayList<LatLng> markerPoints) {

        // Checks, whether start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            // Start downloading json data from Google Directions API
            new ConfirmationActivity.DownloadTask().execute(url);
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

            ConfirmationActivity.ParserTask parserTask = new ConfirmationActivity.ParserTask();
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

            details.setText(details.getText() + "\nEstimated Wait: " + result.get(0).get(0).get("duration"));
        }
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
