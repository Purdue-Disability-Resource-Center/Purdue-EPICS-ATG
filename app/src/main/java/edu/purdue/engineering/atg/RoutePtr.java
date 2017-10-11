package edu.purdue.engineering.atg;

/**
 * Created by joseph on 10/11/17.
 */

class RoutePtr {
    private String dir;
    private String desc;

    public RoutePtr(String d) {
        dir = d;
        //TODO: figure out how to find desc in dir
        desc = "";
    }

    public RouteNode[] getRouteNodes() {
        return new RouteNode[0]; //TODO: implement a getRouteNodes() method for a RoutePtr.
    }

    public Object getDesc() { //return the MP3 file describing this route. TODO:Change "Object" for whatever MP3 files turn out to be
        return new Object();
    }
}
