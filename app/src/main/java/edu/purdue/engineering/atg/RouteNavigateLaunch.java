package edu.purdue.engineering.atg;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by joseph on 10/20/17.
 */

public class RouteNavigateLaunch extends AppCompatActivity {
    private StatsManager stats;
    private RoutePtr route;
    private FusedLocationProviderClient locator;
    private LocationRequest locationRequest;
    private Intent intent;
    private Intent routeIntent;

    private boolean requestingLocationUpdates = false;
    private boolean locationPermissions = false;

    @TargetApi(26)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_navigate);
        intent = getIntent();
        stats = intent.getParcelableExtra("stats");
        route = intent.getParcelableExtra("route");

        stats.setLayout((LinearLayout)findViewById(R.id.route_navigate_layout));

        locator = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest()
                .setInterval(5000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        routeIntent = new Intent(this, RouteNavigate.class)
                .putExtra("route",route)
                .putExtra("locationRequest",locationRequest)
                ;
        if(Build.VERSION.SDK_INT < 26)
            locationPermissions = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        else
            locationPermissions = true;


    }

    public void onStart() {
        super.onStart();
        startService(routeIntent);
    }

    public void onResume() {
        super.onResume();
        startGPS();
    }

    public void onPause() {
        super.onPause();
        stopGPS();
    }

    public void onStop() {
        super.onStop();
        stopService(routeIntent);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    private void startGPS() {
        if(locationPermissions && !requestingLocationUpdates) {
            try {
                locator.requestLocationUpdates(locationRequest, stats.getLocationCallback(), null);
            }
            catch(SecurityException e) {
                stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION);
            }

        }

    }

    private void stopGPS() {
        if(locationPermissions && requestingLocationUpdates) {
            try {
                locator.removeLocationUpdates(stats.getLocationCallback());
            }
            catch(SecurityException e) {
                stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION);
            }

        }
    }

}
