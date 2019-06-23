# 2018_GroupProject

As part of my group project in year 3 I created this Android App; a clone of the Uber App:

This app requires login or to set up an account.
Once past the login screen you get a google maps screen with your current location and options.
You can then request a car which tracks a route to your destination and processes payment.
You can also view past trips and leave feedback.

## Android App

The main android app code is in: \2018_GroupProject\app\src\main\java\com\group12\pickup

## Arduino

The car tracking was simulated by using an arduino, the code for this can be found in the ArduinoConnection folder.

## Using this App

This data was stored and accessed using Google Firebase but the Firebase project has since been disabled. If you wish to test this project change the API key in this file: 2018_GroupProject/app/src/debug/res/values/google_maps_api.xml. The API key you create will need to have access to the Google Directions API.
