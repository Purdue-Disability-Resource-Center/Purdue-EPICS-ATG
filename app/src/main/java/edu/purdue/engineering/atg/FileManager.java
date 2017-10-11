package edu.purdue.engineering.atg;

/**
 * Created by joseph on 10/11/17.
 */

class FileManager {
    private RoutePtr[] routes;
    private int index = 0; //where the pointer currently is in the route array

    public FileManager(String dir) {
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

    private void initRoutes(String dir) {
        //TODO: initialize routes
    }
}
