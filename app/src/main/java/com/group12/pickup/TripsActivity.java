package com.group12.pickup;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TripsActivity extends AppCompatActivity {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = database.collection("journeys");

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips);

        readData();
    }


    private void readData() {

        collectionReference
                .whereEqualTo("user", user.getEmail())
                .get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("DataBaseRead", "Success");

                            try {

                                List<String> dates = new ArrayList<>();

                                if(task.getResult().getDocuments().size() == 0) {

                                    dates.add("No Trips have been made");

                                    ListView listView = findViewById(R.id.list);
                                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(TripsActivity.this, android.R.layout.simple_list_item_multiple_choice, dates);
                                    listView.setAdapter(arrayAdapter);
                                    return;
                                }

                                for(int i = 0; i < task.getResult().getDocuments().size(); i++)
                                    dates.add((String) task.getResult().getDocuments().get(i).getData().get("date"));

                                ListView listView = findViewById(R.id.list);
                                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(TripsActivity.this, android.R.layout.simple_list_item_multiple_choice, dates);
                                listView.setAdapter(arrayAdapter);

                                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {

                                        Object clickItemObj = adapterView.getAdapter().getItem(index);

                                        Intent i = new Intent(TripsActivity.this, TripDetailsActivity.class);
                                        i.putExtra("user", user.getEmail());
                                        i.putExtra("date", clickItemObj.toString());

                                        startActivity(i);
                                    }
                                });

                            } catch(Exception e) {
                                Log.e("Checking Confirmation", "Failed");
                            }
                        } else {
                            Log.e("DataBaseRead", "Failed");
                        }

                    }
                });
    }
}
