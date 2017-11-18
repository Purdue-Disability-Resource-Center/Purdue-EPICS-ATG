package edu.purdue.engineering.atg;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, TextToSpeech.OnInitListener {

    final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 0;
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    final String ROUTES_DIRECTORY = "ATG";

    private StatsManager stats;
    private FileManager fileManager;
    private TextView routeMenu;
    private TextToSpeech speaker;

    private volatile boolean requestingLocationUpdates = false;
    private volatile boolean location_permissions_ready = false;
    private volatile boolean file_permissions_ready = false;
    private volatile boolean speaker_ready = false;
    private boolean isInForeground = false;

    private FusedLocationProviderClient locator;
    private LocationRequest locationRequest;
    private GestureDetector gestureDetector;
/** Method for initializing the main activity. Sets up the layout and the statistics manager. Registers itself as a gesture listener
 *  and as a {@code TexttoSpeech.OnInitListener}. Makes calls to check app permissions and perform appropriate initialization.
 *  Initializes the {@code TexttoSpeech} speaker for list navigation. 
 *  @param savedInstanceState {@code Bundle} containing saved instance data. Not used in this code, simply passed into {@code super}.
 */
    @Override @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.mainToolBar));
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

        /*NotificationChannel channel = new NotificationChannel("routeNavigate","ATG Navigation", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = (NotificationManager)(getSystemService(Context.NOTIFICATION_SERVICE));
        notificationManager.createNotificationChannel(channel);*/ // I really have no clue what Android wants me to do with this stuff

        speaker = new TextToSpeech(this,this);

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
/** Initialization of the {@code FileManager}. Not safe to call unless app is known to have external file permissions. */
    private void initFileManager() {
        fileManager = new FileManager(
                new File(Environment.getExternalStorageDirectory().getPath()+ "/" + ROUTES_DIRECTORY) //should be the directory with routes
        );
    }
/** Called when the app needs to start up processes before coming onscreen. Simply calls {@code super}. */
    protected void onStart() {
        super.onStart();

    }
/** Called immediately when app becomes active onscreen. Sets the {@code isInForeground} flag true and sets the current onscreen route, so the
 *  description will play. Calls the GPS to start, and the {@code startGPS()} method handles permissions.
 */
    protected void onResume() {
        super.onResume();
        startGPS();
        isInForeground = true;
        if(file_permissions_ready)
            setCurrentRoute(fileManager.getRoute());

    }
/** Called immediately when app is covered. Simply stops the GPS and turns off the {@code isInForeGround} flag. */
    protected void onPause() {
        super.onPause();
        stopGPS();
        isInForeground = false;

    }
/** Called when app goes fully offscreen. Simply calls {@code super.onStop()} */
    protected void onStop() {
       super.onStop();             // I don't think we need to do anything in here
    }

    protected void onDestroy() {
        super.onDestroy();
    }
/** Handler for when the permissions request comes back from user. Uses a switch case to figure out which permission was questioned and 
 *  whether it was granted. If true, it calls the appropriate initializations. This does mean that if the app starts without permission, it
 *  will perform these processes asynchronously instead of synchronously. 
 *  @param requestCode The request code passed by the app when it made the request. Used to determine what we asked for.
 *  @param permissions Array containing the permissions we asked. Not used in this method.
 *  @param grantResults Array with the {@code PackageManager} success values from the user. We only check the first element here.
 */
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
/** Starts the GPS functionality of the main screen. Has its own check for permissions, so it can simply be called without regard. Will
 *  not do anything if the GPS is already registered, or if permissions are not enabled. 
 *  @see #stopGPS()
 */
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
/** Stops GPS operation. Has similar checks to {@code startGPS()}.
 *  @see #startGPS()
 */
    private void stopGPS() {
        if(location_permissions_ready && requestingLocationUpdates) {
            stats.updateGPSState(StatsManager.GPS_INACTIVE); //these lines set readout and boolean to reflect state
            requestingLocationUpdates = false;
            locator.removeLocationUpdates(stats.getLocationCallback()); //take off the request
        }
    }
/** Initializes everything related to location services and the GPS. Called upon confirmation of permissions, exactly once per app instance.
 */
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
/** Throws user out of MainActivty and into navigation on the given route. Called upon receipt of whatever UI interaction translates to
 *  "select route." 
 *  @param ptr The {@code RoutePtr} representing the route on which to start. In this program passed out of {@code FileManager}.
 */
    private void beginRoute(RoutePtr ptr) {
        startActivity( new Intent(this, RouteNavigateLaunch.class)
                .putExtra("stats", stats)
                .putExtra("route", ptr)
        );
    }
/** Plays the description of a given Route. This is used to read off the route to the visually-impaired user.
 *  @param ptr The route to be described.
 */
    private void playDesc(RoutePtr ptr) {
        Uri content = ptr.getDesc();
        File file = new File(content.getPath());
        String name = file.getName();
        if(name.substring(name.length()-4).equals(".mp3")) {
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(this, content);
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                    }
                });
                player.prepareAsync();
            } catch (IOException e) {
                //TODO: how do we want to handle these? Won't be so bad if it's just silent
            }
        }
        else {
            if(speaker_ready)
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    if(Build.VERSION.SDK_INT > 21 )
                        speaker.speak(new String(data, "UTF-8"), TextToSpeech.QUEUE_FLUSH, null, "description");
                    else
                        speaker.speak(new String(data, "UTF-8"), TextToSpeech.QUEUE_FLUSH,null);
                }
                catch (IOException e) {
                    //don't do anything. they messed up
                }
        }
    }
/** Sets the current displayed route to the given RoutePtr. Call whenever the current route needs to be updated to the user.
 *  @param route The new current route. Will be displayed on screen and have it's description played.
 */
    private void setCurrentRoute(RoutePtr route) {
        routeMenu.setText(R.string.route_menu_text + route.getName());
        playDesc(route);
    }
/** Go to the next route and set the current route to the new route. */
    private void nextRoute() {
        setCurrentRoute(fileManager.proceedRoute());
    }

    /*-------------------------- Gesture Handlers -----------------------------*/
/** Handler for when app recieves any {@code TouchEvent}. Immediately passes to {@code GestureDetector}
 *  @param e The {@code MotionEvent} to be handled.
 *  @return A boolean informing the handler whether the event is done.
 */
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }
    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityx, float velocityy) {
        return true;
    }
/** {@code GestureDetector} handler for a long press. Indicates selection of the current route. Calls {@link #beginRoute(RoutePtr) beginRoute}
 *  @param e The {@code MotionEvent} representing the input. Not actually used in this method.
 *  @see #beginRoute(RoutePtr)
 */
    public void onLongPress(MotionEvent e) {
        beginRoute(fileManager.getRoute());
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distancex, float distancey) {
        return true;
    }

    public void onShowPress(MotionEvent e) {

    }
/** Handler for when a single tap is received. Indicates the user would like to move to the next route in the list. 
 *  Calls {@link #nextRoute() nextRoute()}. 
 *  @see #nextRoute()
 */
    public boolean onSingleTapUp(MotionEvent e) {
        nextRoute();
        return true;
    }

    //-------------------------- TexttoSpeech Interface ----------------------------
/** Handler for when the TexttoSpeech initialization returns. Sets the flag to ready so the other operations can proceed without issue. */
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS)
            speaker_ready = true;


    }


}
