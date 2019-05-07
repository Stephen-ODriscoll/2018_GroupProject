package com.group12.pickup;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ReviewActivity extends AppCompatActivity {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = database.collection("generalFeedback");

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        final Intent i = getIntent();
        final boolean tripFeedback = i.getBooleanExtra("tripFeedback", false);

        final EditText text = findViewById(R.id.feedback);
        final RatingBar ratingBar = findViewById(R.id.ratingBar);

        Button cancel = findViewById(R.id.cancel2);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        Button submit = findViewById(R.id.submit2);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!tripFeedback) {

                    float stars = ratingBar.getRating();
                    String feedback = text.getText().toString();

                    HashMap<String, String> documentToAdd = new HashMap<>();

                    documentToAdd.put("user", user.getEmail());
                    documentToAdd.put("rating", String.valueOf(stars));
                    documentToAdd.put("feedback", feedback);

                    collectionReference.add(documentToAdd);

                    finish();
                }

                else {

                    i.putExtra("stars", ratingBar.getRating());
                    i.putExtra("text", text.getText().toString());

                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        });

    }
}
