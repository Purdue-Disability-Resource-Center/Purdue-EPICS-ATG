package edu.purdue.engineering.atg;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 0;
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    final String ROUTES_DIRECTORY = "ATG";

    private StatsManager stats;
    private FileManager fileManager;
    private TextView routeMenu;

    private volatile boolean requestingLocationUpdates = false;
    private volatile boolean location_permissions_ready = false;
    private volatile boolean file_permissions_ready = false;
    private boolean isInForeground = false;

    private FusedLocationProviderClient locator;
    private LocationRequest locationRequest;
    private GestureDetector gestureDetector;

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

        gestureDetector = new GestureDetector(this,this);

        routeMenu = (TextView)findViewById(R.id.main_route_display_name);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    initLocationServices();
                    location_permissions_ready = true;
        }
        else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initFileManager();
            file_permissions_ready = true;
        }
        else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private void initFileManager() {
        fileManager = new FileManager(
                new File(Environment.getExternalStorageDirectory().getPath()+ "/" + ROUTES_DIRECTORY) //should be the directory with routes
        );
    }

    protected void onStart() {
        super.onStart();

    }

    protected void onResume() {
        super.onResume();
        startGPS();
        isInForeground = true;
        if(file_permissions_ready)
            setCurrentRoute(fileManager.getRoute());

    }

    protected void onPause() {
        super.onPause();
        stopGPS();
        isInForeground = false;

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
                    initLocationServices();
                    location_permissions_ready = true;
                    if(isInForeground)
                        MainActivity.this.onResume();

                }
                else{
                    stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION);
                }
                break;
            case(MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE):
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initFileManager();
                    file_permissions_ready = true;
                }
                else {
                    this.finish(); //cheekily exit
                }
                break;

            default:
                break;
        }
    }

    private void startGPS() {
        if(location_permissions_ready && !requestingLocationUpdates) {
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
        if(location_permissions_ready && requestingLocationUpdates) {
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
                        }
                        catch (IntentSender.SendIntentException sendEx) {
                        MainActivity.this.stats.updateGPSState(StatsManager.GPS_NEED_PERMISSION);
                        }
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;


                }
            }
        });

    }

    private void beginRoute(RoutePtr ptr) {
        startActivity( new Intent(this, RouteNavigateLaunch.class)
                .putExtra("stats", stats)
                .putExtra("route", ptr)
        );
    }

    private void playDesc(RoutePtr ptr) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(this, ptr.getDesc());
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            player.prepareAsync();
        }
        catch(IOException e) {
            //TODO: how do we want to handle these? Won't be so bad if it's just silent
        }
    }

    private void setCurrentRoute(RoutePtr route) {
        routeMenu.setText(R.string.route_menu_text + route.getName());
        playDesc(route);
    }

    private void nextRoute() {
        setCurrentRoute(fileManager.proceedRoute());
    }

    /*-------------------------- Gesture Handlers -----------------------------*/
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }
    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityx, float velocityy) {
        beginRoute(fileManager.getRoute());
        return true;
    }

    public void onLongPress(MotionEvent e) {

    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distancex, float distancey) {
        return true;
    }

    public void onShowPress(MotionEvent e) {

    }

    public boolean onSingleTapUp(MotionEvent e) {
        nextRoute();
        return true;
    }
}
