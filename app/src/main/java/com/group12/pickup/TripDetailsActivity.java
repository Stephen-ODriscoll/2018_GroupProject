package com.group12.pickup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TripDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = database.collection("journeys");

    private TextView tripDetails;
    private Button review;

    private String id;
    private Map<String, Object> details;

    private int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        tripDetails = findViewById(R.id.tripDetails);
        review = findViewById(R.id.review);

        Intent i = getIntent();
        String user = i.getStringExtra("user");
        String date = i.getStringExtra("date");

        readData(user, date);
    }


    private void readData(String user, String date) {

        collectionReference
                .whereEqualTo("user", user)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("DataBaseRead", "Success");

                            try {

                                if (task.getResult().size() <= 1) {

                                    details = task.getResult().getDocuments().get(0).getData();
                                    id = task.getResult().getDocuments().get(0).getId();

                                    String fullDetails = "Car ID:  " + details.get("car") +
                                            "\n\n Car Type:  " + details.get("type") +
                                            "\n\n Collection Address:  " + details.get("collection") +
                                            "\n\n Destination Address:  " + details.get("destination") +
                                            "\n\n Distance:  " + details.get("distance") +
                                            "\n\n Estimated Wait:  " + details.get("estimatedWait") +
                                            "\n\n Estimated Drive:  " + details.get("estimatedDrive") +
                                            "\n\n Price:  €" + details.get("price") +
                                            "\n\n Status: " + details.get("status");

                                    tripDetails.setText(fullDetails);

                                    review.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            Intent i = new Intent(TripDetailsActivity.this, ReviewActivity.class);
                                            i.putExtra("tripFeedback", true);

                                            startActivityForResult(i, REQUEST_CODE);
                                        }
                                    });
                                }

                            } catch(Exception e) {
                                Log.e("Checking Confirmation", "Failed");
                            }
                        } else {
                            Log.e("DataBaseRead", "Failed");
                        }

                    }
                });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE) {

            if (resultCode == RESULT_OK) {

                float stars = data.getFloatExtra("stars", 0.0f);
                String comment = data.getStringExtra("text");

                collectionReference
                        .document(id)
                        .delete();

                details.put("stars", String.valueOf(stars));
                details.put("comment", comment);
                collectionReference.add(details);
            }
        }
    }
}
