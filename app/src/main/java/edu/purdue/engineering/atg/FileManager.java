package edu.purdue.engineering.atg;

import java.io.File;

/**
 * Created by joseph on 10/11/17.
 * The FileManager class contains code to manage the filesystem for the ATG.
 * Initialize it with whatever directory you think contains routes, probably Context.getExternalStorageDirectory() + "somestring"
 */

class FileManager {
    private RoutePtr[] routes;
    private int index = 0; //where the pointer currently is in the route array

    public FileManager(File dir) { //dir should be the ATG directory on the filesystem
        initRoutes(dir);
    }

    public RoutePtr getRoute() { //gets the current route

        return routes[index];
    }

    public RoutePtr proceedRoute() { //forward one route in the list, return the new RoutePtr
        index++;
        if(index >= routes.length)
            index = 0;
        return getRoute();
    }

    public RoutePtr backRoute() { //back one route in the list, return the new RoutePtr
        index--;
        if(index < 0)
            index = routes.length-1;
        return getRoute();
    }

    private void initRoutes(File dir) {
        File[] routeList = dir.listFiles();
        routes = new RoutePtr[routeList.length];
        int i = 0;

        for(File r : routeList) { //probably want to catch exceptions for "not directory" somewhere. Maybe do that in the RoutPtr constructor
            routes[i] = new RoutePtr(r);
            i++;
        }

    }
}
