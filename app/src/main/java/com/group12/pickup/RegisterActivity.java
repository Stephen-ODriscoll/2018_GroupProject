package com.group12.pickup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = database.collection("userRequests");

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.nameInput1);
        emailInput = findViewById(R.id.emailInput1);
        passwordInput = findViewById(R.id.passwordInput1);

        Button submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String name = nameInput.getText().toString();
                String email = emailInput.getText().toString();
                String password = passwordInput.getText().toString();

                if(name.equals(""))
                    nameInput.setHintTextColor(Color.RED);

                else if(email.equals(""))
                    emailInput.setHintTextColor(Color.RED);

                else if(!validate(email))
                    Toast.makeText(getBaseContext(), "Email not Valid", Toast.LENGTH_SHORT).show();

                else if (password.equals(""))
                    passwordInput.setHintTextColor(Color.RED);

                else {
                    requestAccount(name, email, password);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext(), "Channel")
                            .setSmallIcon(android.support.v4.R.drawable.notification_icon_background)
                            .setContentTitle("Created Account")
                            .setContentText("Your account is awaiting approval")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

                    createNotificationChannel();

                    //Add notification to our manager and start it
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
                    notificationManager.notify(4, mBuilder.build());;

                    finish();
                }
            }
        });

        Button cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
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


    private void requestAccount(String name, String email, String password) {

        HashMap<String, String> documentToAdd = new HashMap<>();

        documentToAdd.put("name", name);
        documentToAdd.put("email", email);
        documentToAdd.put("password", password);

        collectionReference.add(documentToAdd);
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validate(String emailStr) {

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }
}
