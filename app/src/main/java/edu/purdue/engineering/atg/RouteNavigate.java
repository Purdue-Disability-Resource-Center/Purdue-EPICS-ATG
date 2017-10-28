package edu.purdue.engineering.atg;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.vision.text.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by joseph on 10/20/17.
 */

public class RouteNavigate extends Service implements TextToSpeech.OnInitListener {

    private RoutePtr route;
    private Notification notification;
    private LocationRequest locationRequest;
    private RouteNode[] nodes;
    private MediaPlayer[] sounds;
    private String[] speeches;
    private RouteLocationCallback callback;
    private FusedLocationProviderClient locator;
    private TextToSpeech speaker;
    private volatile boolean speaker_ready = false;

    public IBinder onBind(Intent intent) {
        return null; //might be bindable at some point, to return debug data
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        route = intent.getParcelableExtra("route");
        locationRequest = intent.getParcelableExtra("locationRequest");
        callback = new RouteLocationCallback();
        locator = LocationServices.getFusedLocationProviderClient(this);


        notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.route_navigate_notification_title))
                .setContentText(getText(R.string.route_navigate_notification_content) + route.getName())
                .build();

        startForeground(1, notification);

        speaker = new TextToSpeech(this,this);

        nodes = route.getRouteNodes();
        sounds = new MediaPlayer[nodes.length];
        speeches = new String[nodes.length];
        File file;
        for(int i = 0;i < nodes.length;i++) {
            file = new File(nodes[i].getSound().getPath());
            String name = file.getName();
            if(name.substring(name.length()-4).equals(".mp3")) {
                sounds[i] = new MediaPlayer();
                try {
                    sounds[i].setDataSource(this, nodes[i].getSound());
                    sounds[i].prepare();
                } catch (IOException e) {
                    //do nothing
                }
            }
            else {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    speeches[i] = new String(data, "UTF-8");
                }
                catch (IOException e) {
                    //nothing
                }
            }

        }
        if(Build.VERSION.SDK_INT < 23)
            locator.requestLocationUpdates(locationRequest,callback,null);
        else
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                locator.requestLocationUpdates(locationRequest,callback,null);


        return START_STICKY;
    }

    private class RouteLocationCallback extends LocationCallback {
        RouteLocationCallback() {}

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location userLoc = locationResult.getLastLocation();

            for(int i = 0;i < nodes.length;i++) {
                if(userLoc.distanceTo(nodes[i].getLocation()) < nodes[i].getRadius()) {
                    if (sounds[i] != null)
                        sounds[i].start(); //the line that controls it all
                    else {
                        if(speaker_ready) {
                            if (Build.VERSION.SDK_INT > 21)
                                speaker.speak(speeches[i], TextToSpeech.QUEUE_FLUSH, null, "routenavigation");
                            else
                                speaker.speak(speeches[i], TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
            }
        }
    }

    //------------------------------- TexttoSpeech interface ---------------------------

    public void onInit(int status) {
        if( status == TextToSpeech.SUCCESS)
            speaker_ready = true;
    }

}
