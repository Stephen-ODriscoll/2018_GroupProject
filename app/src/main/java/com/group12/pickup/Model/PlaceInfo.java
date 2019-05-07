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

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getId() {
        return id;
    }

    public Uri getWebsite() {
        return website;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public float getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return "Place{" +
                "name = '" + name + '\'' +
                ", address = '" + address + '\'' +
                ", phone = '" + phone + '\'' +
                ", id = '" + id + '\'' +
                ", website = " + website +
                ", latlng = " + latLng +
                ", rating = " + rating +
                '}';
    }
}