package edu.purdue.engineering.atg;

import android.location.Location;
import android.net.Uri;
import java.io.File;

/**
 * Created by joseph on 10/11/17.
 * Class routeNode represents a single waypoint on a route. Holds a Location and Uri for the sound to play at that location.
 * Set it up with Google Locations Services Geofences
 */

class RouteNode {
    public final String LOCATION_NAME = ".txt";
    public final String SOUND_NAME = ".mp3";

    private Location loc;
    private double rad;
    private Uri sound;

    public RouteNode(File dir) {
        if(!dir.isDirectory())
            throw new IllegalArgumentException(
                    "This waypoint isn't a directory!"
            );

        File[] files = dir.listFiles(); //get the files in the waypoint directory

        int mask = 0B0000; //bitmask because I'm cool
        int LOC_FOUND = 0B0001;
        int SOUND_FOUND = 0B0010;
        int i = 0; //iterator

        while(mask != 0B0011) {
            if (((mask & 0B0001) != 0B0001) && (files[i].getName().equals(LOCATION_NAME))) { //if we haven't found a location and this file has the right name
                loc = parseLocation(files[i]); //get the location
                rad = parseRadius(files[i]); //get the radius
                mask += LOC_FOUND;
            }

            if(((mask & 0B0010) != 0B0010) && (files[i].getName().equals(SOUND_NAME))){ //if we haven't found a sound, and this file has the right name
                sound = Uri.fromFile(files[i]); //get the Uri for the sound
                mask += SOUND_FOUND;
            }
            i++;
            if(i >= files.length) //if we've reached the end without finding the files we want
                throw new IllegalArgumentException(
                        "Route waypoint directory does not contain necessary files!"
                );

        }
    }

    public Location getLocation() {
        return loc;
    }

    public double getRadius() {
        return rad;
    }

    public Uri getSound() {
        return sound;
    }

    private Location parseLocation(File file) {
        return null; //TODO: What format are the text files with radius and location in? Placeholder
    }

    private double parseRadius(File file) {
        return 10; //TODO: figure out how this really works. Placeholder
    }
}
