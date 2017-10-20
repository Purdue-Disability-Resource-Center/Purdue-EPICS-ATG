package edu.purdue.engineering.atg;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.instantapps.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 0;
    private StatsManager stats;

    private volatile boolean requestingLocationUpdates = false;
    private volatile boolean permissions_ready = false;

    private FusedLocationProviderClient locator;
    private LocationRequest locationRequest;

    @Override @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stats = new StatsManager(                                   // pass the layout and its text fields into a new StatsManager
                (LinearLayout)findViewById(R.id.statsLayout),       // This will be used to hold onto and update the stats on the screen
                new TextView[]{(TextView)findViewById(R.id.identifier_text),
                        (TextView)findViewById(R.id.latitude_text),
                        (TextView)findViewById(R.id.longitude_text),
                        (TextView)findViewById(R.id.GPS_state_text)
                }
        );

        //TODO: create the route window

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Thread thread = new Thread(){
                public void run(){
                    permissions_ready = true;
                    MainActivity.this.initLocationServices();
                }
            };
            thread.start();
        }
        else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION);
        }
    }

    protected void onStart() {
        super.onStart();

    }

    protected void onResume() {
        super.onResume();
        startGPS();

    }

    protected void onPause() {
        super.onPause();
        stopGPS();

    }

    protected void onStop() {
       super.onStop();             // I don't think we need to do anything in here
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case(MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION):
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissions_ready = true;
                    initLocationServices();
                }
                else{
                    stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION);
                }
                break;
            default:
                break;
        }
    }

    private void startGPS() {
        if(permissions_ready && !requestingLocationUpdates) {
              //check if we somehow got here while already requesting location updates
                try {
                    locator.requestLocationUpdates(locationRequest, stats.getLocationCallback(), null); //start asking for updates
                    stats.updateGPSState(StatsManager.GPS_ACTIVE);
                    requestingLocationUpdates = true;
                } catch (SecurityException e) {
                    stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION); //if we don't have permission
                }

        }
    }

    private void stopGPS() {
        if(permissions_ready && requestingLocationUpdates) {
            stats.updateGPSState(StatsManager.GPS_INACTIVE); //these lines set readout and boolean to reflect state
            requestingLocationUpdates = false;
            locator.removeLocationUpdates(stats.getLocationCallback()); //take off the request
        }
    }

    private void initLocationServices() {
        locator = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);       //setup for how fast we can receive locations. chosen arbitrarily
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //tell Google we want the best data

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder() //get a package of all our location settings requests
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);               //get a client for the current settings on the phone
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build()); //create a new task checking if the two are compatible


        task.addOnFailureListener(this, new OnFailureListener() { //listen if task fails, and handle if it does
            @Override
            public void onFailure(@NonNull Exception e) {           //switch to deal with different errors
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED: { //in this case, need to ask user to change settings for us
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e; //change type of exception
                            resolvable.startResolutionForResult(MainActivity.this, 2); //ask Android to bring up the dialog to change. Check this line if there are problems.
                        } catch (IntentSender.SendIntentException sendEx) {
                        } //ignore if it fails
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;


                }
            }
        });
        startGPS();
    }



}
