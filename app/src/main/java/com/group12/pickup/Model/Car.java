package com.group12.pickup.Model;

import com.google.android.gms.maps.model.LatLng;

public class Car {

    private String license;
    private String type;
    private String name;
    private LatLng position;
    private String status;
    private double distance;        //Kilometers

    public Car(String name, String license, String type, String latitude, String longitude, String status, LatLng myLocation) {

        this.license = license;
        this.name = name;
        this.status = status;
        this.type = type;

        Double lat = Double.parseDouble(latitude);
        Double lng = Double.parseDouble(longitude);
        position = new LatLng(lat, lng);

        distance = distance(myLocation.latitude, myLocation.longitude, position.latitude, position.longitude);
    }


    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 1.609344;

            return dist;
        }
    }


    public String getLicense() {

        return license;
    }


    public String getType() {

        return type;
    }


    public String getName() {

        return name;
    }


    public LatLng getPosition() {

        return position;
    }


    public String getStatus() {

        return status;
    }


    public double getDistance() {

        return distance;
    }
}
