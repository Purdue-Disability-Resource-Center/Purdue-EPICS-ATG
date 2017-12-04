package edu.purdue.engineering.atg;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * All code herein is owned by Purdue-EPICS-DRC, and was created by the Fall 2017 team.
 * Class RoutePtr represents a pointer to a single route in the routes directory. Holds onto the directory pointer and a Uri to the description sound file
 */

class RoutePtr implements Parcelable{
    private final String ROUTE_DESC_NAME = "desc.mp3"; //constant for name of description file
    private final String ROUTE_SPEECH_NAME = "desc.txt";
    /** the directory in which this route resides */
    private File dir;
    /** the URI to the description MP3 */
    private Uri desc;

    /** Construct a new RoutePtr from an abstract {@code File} path.
     *
     * @param d the path to the file.
     */
    RoutePtr(File d) {
        dir = d;
        File[] files = dir.listFiles(); //directory with this route

        for(int i = 0;i < files.length;i++) { //find the description file
            if((files[i].getName().equals(ROUTE_DESC_NAME) || files[i].getName().equals(ROUTE_SPEECH_NAME)) && files[i].isFile()) { // don't us == here, it doesn't work with strings well
                desc = Uri.fromFile(files[i]); //set desc to be the URI to the description file. DON'T try File.toURI() here. Gets into a weird fight with java.io.URI and android.net.Uri
                i = files.length;
            }

        }

    }
    /** Constructor for unparceling */
    private RoutePtr(File file, Uri uri) { //constructor for unparceling.
        dir = file;
        desc = uri;
    }

    /** Get an array of the {@code RouteNodes} contained by this route.
     *
     * @return The array of {@code RouteNodes}
     */
    public RouteNode[] getRouteNodes() {
        File[] files = dir.listFiles();
        RouteNode[] nodes = new RouteNode[files.length-1];

        for(int i = 0, k = 0; k < files.length; k++) {
            if(files[k].isDirectory()) { //if the file is a directory
                nodes[i] = new RouteNode(files[k]); //make it a new routeNode in the array
                i++;
            }

        }

        return nodes;
    }

    /** Get the Uri to the description of this route. Note that there is both a Java URI class and an Android Uri class.
     *
     * @return The description file of this route.
     */
    public Uri getDesc() { //return the MP3 file describing this route. For convenience. No playMP3 method because I think that would hang the UI on the other end.
        return desc;
    }

    /** Get the name of this route
     *
     * @return The name of the route
     */
    public String getName() {
        return dir.getName();
    }

    /** Concatenate an {@code ArrayList} of routes together
     *
     * @param routes {@code ArrayList} containing the routes to be piled together
     * @return An array of the {@code RouteNodes} from every route in the list
     */
    public static RouteNode[] concatRoutes(ArrayList<RoutePtr> routes) {
        ArrayList<RouteNode> nodes = new ArrayList<>();
        for(RoutePtr r : routes) {
            nodes.addAll(Arrays.asList(r.getRouteNodes()));
        }
        return nodes.toArray(new RouteNode[0]);
    }



    /*------------------------ Parcelable Implementation -----------------------------*/
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{dir.toString(), desc.toString()});
    }

    public static final Parcelable.Creator<RoutePtr> CREATOR = new Parcelable.Creator<RoutePtr>() {
        public RoutePtr createFromParcel(Parcel in) {
            String[] strings = in.createStringArray();
            return new RoutePtr(new File(strings[0]), Uri.parse(strings[1]));
        }

        public RoutePtr[] newArray(int size) {
            return new RoutePtr[size];
        }
    };


}
