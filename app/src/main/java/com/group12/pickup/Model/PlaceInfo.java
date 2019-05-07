package com.group12.pickup.Model;

import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class PlaceInfo {

    private String name;
    private String address;
    private String phone;
    private String id;
    private Uri website;
    private LatLng latLng;
    private float rating;

    public PlaceInfo(String name, String address, String phone, String id, Uri website, LatLng latLng, float rating) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.id = id;
        this.website = website;
        this.latLng = latLng;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    @Override
    public String toString() {

        return ""
                + checkNull("Name: ", name)
                + checkNull("Address: ", address)
                + checkNull("Phone No: ", phone)
                + "Latitude: " + round(latLng.latitude, 4)
                + "Longitude: " + round(latLng.longitude, 4)
                + "Rating: " + rating;
    }


    public String checkNull(String identifier, String toCheck) {

        if(toCheck == null || toCheck.equals(""))
            return "";

        return identifier + toCheck + "\n";
    }


    public static double round(double value, int places) {

        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}