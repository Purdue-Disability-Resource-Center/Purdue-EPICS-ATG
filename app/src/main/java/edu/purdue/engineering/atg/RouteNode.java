package edu.purdue.engineering.atg;

import android.location.Location;
import android.net.Uri;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.Scanner;

/**
 * All code herein is owned by Purdue-EPICS-DRC, and was created by the Fall 2017 team.
 * Class routeNode represents a single waypoint on a route. Holds a Location and Uri for the sound to play at that location.
 *
 */

class RouteNode {
    public final String LOCATION_NAME = "node.txt";
    public final String SOUND_NAME = "desc.mp3";
    public final String SPEECH_NAME = "speech.txt";

    private Location loc;
    private double rad;
    private Uri sound;

    /** Create a new RouteNode from a file pointer
     *
     * @param dir The file pointer
     */
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
            if(i >= files.length) //if we've reached the end without finding the files we want
                throw new IllegalArgumentException(
                        "Route waypoint directory does not contain necessary files!"
                );

            if (((mask & 0B0001) != 0B0001) && (files[i].getName().equals(LOCATION_NAME))) { //if we haven't found a location and this file has the right name
                parseNode(files[i]);
                mask += LOC_FOUND;
            }

            if(((mask & 0B0010) != 0B0010) && (files[i].getName().equals(SOUND_NAME) || files[i].getName().equals(SPEECH_NAME))){ //if we haven't found a sound, and this file has the right name
                sound = Uri.fromFile(files[i]); //get the Uri for the sound
                mask += SOUND_FOUND;
            }

            i++;
        }
    }

    /** Get the location of this node
     *
     * @return The location of this node
     */
    public Location getLocation() {
        return loc;
    }

    /** Get the radius of this node
     *
     * @return the radius in meters
     */
    public double getRadius() {
        return rad;
    }

    /** Get the sound to play at this node. Might be a text file
     *
     * @return The description file
     */
    public Uri getSound() {
        return sound;
    }

    /** Parse node data out of a node file
     *
     * @param file The node file, probably named "node.txt"
     */
    private void parseNode(File file) {
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        }
        catch(FileNotFoundException e) {
            throw new InvalidParameterException("File not found!");
        }


        loc = new Location("ATG");
        loc.setLatitude(scanner.nextDouble()); //try to get the three numbers? Idk if this works either.
        loc.setLongitude(scanner.nextDouble()); //Need to see format. Also probably catch IOexceptions here

        rad = scanner.nextDouble();

    }

}
