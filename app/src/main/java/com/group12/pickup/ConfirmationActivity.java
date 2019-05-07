package com.group12.pickup;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.group12.pickup.Model.Car;
import com.group12.pickup.Model.DirectionsJSONParser;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ConfirmationActivity extends AppCompatActivity {

    private Car nearest = null;
    private LatLng myLocation;
    private TextView details;
    private String TAG = "Confirmation";
    private String estimatedWait = "";
    private Spinner dropdown;

    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        final Intent i = getIntent();
        final String collectionDetails = i.getStringExtra("Collection");
        final String destinationDetails = i.getStringExtra("Destination");
        final String distance = i.getStringExtra("Distance");
        final String duration = i.getStringExtra("Duration");
        double lat = i.getDoubleExtra("Latitude", 0);
        double lng = i.getDoubleExtra("Longitude", 0);

        int number = 4;
        if(duration.contains("mins"))
            number = 5;

        final double price = round(Double.valueOf(duration.substring(0, duration.length()-number)) * 1.25, 2);

        myLocation = new LatLng(lat, lng);

        TextView collection = findViewById(R.id.collection);
        TextView destination = findViewById(R.id.destination);
        details = findViewById(R.id.details);
        dropdown = findViewById(R.id.spinner);

        String[] items = new String[]{"Select Type", "5 Seater", "7 Seater"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        collection.setText(collectionDetails.replace(", ", ",\n"));
        destination.setText(destinationDetails.replace(", ", ",\n"));
        details.setText("Distance: " + distance + "\nCost: €" + price +
                "\nEstimated Driving Time: " + duration);

        getNearestFree();

        Button advance = findViewById(R.id.advance);
        advance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CardInputWidget cardInput = findViewById(R.id.card_input_widget);

                Card card = cardInput.getCard();

                if (card == null) {
                    Toast.makeText(getBaseContext(),"Invalid Card Data",Toast.LENGTH_SHORT).show();
                }

                else if(dropdown.getSelectedItem().equals("Select Type")) {
                    Toast.makeText(getBaseContext(),"Select Vehicle Type",Toast.LENGTH_SHORT).show();
                }

                else if(submitted) {}

                else {

                    submitted = true;

                    Stripe stripe = new Stripe(getBaseContext(), "pk_test_TYooMQauvdEDq54NiTphI7jx");
                    stripe.createToken(
                            card,
                            new TokenCallback() {
                                public void onSuccess(Token token) {

                                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                                    CollectionReference collectionReference = database.collection("journeys");
                                    FirebaseAuth auth = FirebaseAuth.getInstance();
                                    FirebaseUser user = auth.getCurrentUser();

                                    DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss");
                                    String date = df.format(Calendar.getInstance().getTime());

                                    HashMap<String, String> documentToAdd = new HashMap<>();

                                    documentToAdd.put("car", nearest.getName());
                                    documentToAdd.put("license", nearest.getLicense());
                                    documentToAdd.put("type", nearest.getType());
                                    documentToAdd.put("user", user.getEmail());
                                    documentToAdd.put("collection", collectionDetails);
                                    documentToAdd.put("destination", destinationDetails);
                                    documentToAdd.put("distance", distance);
                                    documentToAdd.put("estimatedWait", estimatedWait);
                                    documentToAdd.put("estimatedDrive", duration);
                                    documentToAdd.put("price", String.valueOf(price));
                                    documentToAdd.put("tokenID", token.getId());
                                    documentToAdd.put("date", date);
                                    documentToAdd.put("status", "unconfirmed");

                                    collectionReference.add(documentToAdd);

                                    //addToRealtime(user, collectionDetails, destinationDetails, distance, duration, price, token, date);

                                    i.putExtra("requested", true);
                                    i.putExtra("user", user.getEmail());
                                    i.putExtra("date", date);
                                    i.putExtra("license", nearest.getLicense());

                                    setResult(RESULT_OK, getIntent());
                                    finish();
                                }
                                public void onError(Exception error) {

                                    // Show localized error message
                                    Log.e(TAG,"Failed to get stripe Token");
                                }
                            }
                    );
                }
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

                for(int i = 0; i < temp.length; i++)
                    if(!temp[i].equals(""))
                        lines.add(temp[i]);

                for(int i = 0; i < lines.size(); i+=11) {

                    Car car = new Car(lines.get(i), lines.get(i+2), lines.get(i+4), lines.get(i+6), lines.get(i+8), lines.get(i+10), myLocation);

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

            estimatedWait = result.get(0).get(0).get("duration");
            details.setText(details.getText() + "\nEstimated Wait: " + estimatedWait);
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
