package edu.purdue.engineering.atg;

import android.net.Uri;
import java.io.File;


/**
 * Created by joseph on 10/11/17.
 * Class RoutePtr represents a pointer to a single route in the routes directory. Holds onto the directory pointer and a Uri to the description sound file
 */

class RoutePtr {
    public final String ROUTE_DESC_NAME = "desc.mp3"; //constant for name of description file
    private File dir; //the directory in which this route resides
    private Uri desc; //the URI to the description MP3

    public RoutePtr(File d) {
        dir = d;
        File[] files = dir.listFiles(); //directory with this route

        for(int i = 0;i < files.length;i++) { //find the description file
            if(files[i].getName().equals(ROUTE_DESC_NAME) && files[i].isFile()) { // don't us == here, it doesn't work with strings well
                desc = Uri.fromFile(files[i]); //set desc to be the URI to the description file. DON'T try File.toURI() here. Gets into a weird fight with java.io.URI and android.net.Uri
                i = files.length;
            }

        }

    }

    public RouteNode[] getRouteNodes() {
        File[] files = dir.listFiles();
        RouteNode[] nodes = new RouteNode[files.length];

        for(int i = 0; i < nodes.length; i++) {
            if(files[i].isDirectory()) { //if the file is a directory
                nodes[i] = new RouteNode(files[i]); //make it a new routeNode in the array
            }
            else { //anything else just makes a hole in the array. Set up later interpreters to ignore these.
                nodes[i] = null;
            }
        }

        return nodes;
    }

    public Uri getDesc() { //return the MP3 file describing this route. For convenience. No playMP3 method because I think that would hang the UI on the other end.
        return desc;
    }
}
