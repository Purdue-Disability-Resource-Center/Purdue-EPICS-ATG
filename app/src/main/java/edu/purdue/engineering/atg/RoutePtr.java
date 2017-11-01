package edu.purdue.engineering.atg;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;


/**
 * Created by joseph on 10/11/17.
 * Class RoutePtr represents a pointer to a single route in the routes directory. Holds onto the directory pointer and a Uri to the description sound file
 */

class RoutePtr implements Parcelable{
    private final String ROUTE_DESC_NAME = "desc.mp3"; //constant for name of description file
    private final String ROUTE_SPEECH_NAME = "desc.txt";
    private File dir; //the directory in which this route resides
    private Uri desc; //the URI to the description MP3

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

    private RoutePtr(File file, Uri uri) { //constructor for unparceling.
        dir = file;
        desc = uri;
    }

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

    public Uri getDesc() { //return the MP3 file describing this route. For convenience. No playMP3 method because I think that would hang the UI on the other end.
        return desc;
    }

    public String getName() {
        return dir.getName();
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
