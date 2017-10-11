package edu.purdue.engineering.atg;

import android.location.Location;

/**
 * Created by joseph on 10/11/17.
 */

class RouteNode {
    private Location loc;
    private double rad;
    //member MP3 file, whatever format that is

    public RouteNode(String dir) {
        //TODO: logic to get location, radius, and MP3 file out of dir
    }

    public Location getLocation() {
        return loc;
    }

    public double getRadius() {
        return rad;
    }

    //TODO: getMP3()
}
