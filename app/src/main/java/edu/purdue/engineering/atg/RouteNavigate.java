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
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
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
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by joseph on 10/20/17.
 */

public class RouteNavigate extends Service implements TextToSpeech.OnInitListener {

    final long LOCKOUT_TIME = 45000;

    private RoutePtr route;
    private Notification notification;
    private LocationRequest locationRequest;
    private PowerManager.WakeLock wakelock;
    private RouteNode[] nodes;
    private MediaPlayer[] sounds;
    private String[] speeches;
    private double[] lockouts;
    private RouteLocationCallback callback;
    private FusedLocationProviderClient locator;
    private TextToSpeech speaker;
    private volatile boolean speaker_ready = false;

    public IBinder onBind(Intent intent) {
        return null; //might be bindable at some point, to return debug data
    }

    @TargetApi(16)
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        locationRequest = intent.getParcelableExtra("locationRequest");
        //intent.setExtrasClassLoader(RoutePtr.class.getClassLoader());
        route = intent.getParcelableExtra("route");
        callback = new RouteLocationCallback();
        locator = LocationServices.getFusedLocationProviderClient(this);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ATG Navigation");
        wakelock.acquire();

        notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.route_navigate_notification_title))
                .setContentText(getText(R.string.route_navigate_notification_content) + route.getName()) //doesn't work at all. Idk why and it's not really important to the project
                .build();

        startForeground(1, notification);

        speaker = new TextToSpeech(this,this);
        ArrayList<RoutePtr> routes = new ArrayList<>();
        routes.add(route);
        addStatics(routes);
        nodes = RoutePtr.concatRoutes(routes);
        sounds = new MediaPlayer[nodes.length];
        speeches = new String[nodes.length];
        lockouts = new double[nodes.length];
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
    public void onDestroy() {
        locator.removeLocationUpdates(callback);
        wakelock.release();
    }

    private class RouteLocationCallback extends LocationCallback {
        RouteLocationCallback() {}

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location userLoc = locationResult.getLastLocation();

            for(int i = 0;i < nodes.length;i++) {
                if((userLoc.distanceTo(nodes[i].getLocation()) < nodes[i].getRadius()) && (SystemClock.elapsedRealtime() - lockouts[i] > LOCKOUT_TIME)) {
                    if (sounds[i] != null)
                        sounds[i].start(); //the line that controls it all
                    else {
                        if(speaker_ready) {
                            if (Build.VERSION.SDK_INT > 21)
                                speaker.speak(speeches[i], TextToSpeech.QUEUE_ADD, null, "routenavigation"); //the other two lines that control it all
                            else
                                speaker.speak(speeches[i], TextToSpeech.QUEUE_ADD, null);
                        }
                    }
                    lockouts[i] = SystemClock.elapsedRealtime(); //introduces a bug where if the T2T engine isn't loaded when it's time to speak, the speech will still lock out. Probably will never happen.
                }
            }
        }
    }

    private void addStatics(ArrayList<RoutePtr> routes) {
        File staticsDir = new File(Environment.getExternalStorageDirectory() + File.separator + RouteSelect.ROUTES_DIRECTORY + File.separator + "static");
        Scanner scanner;
        try {
            scanner = new Scanner(new File(staticsDir,"settings.txt"));
        } catch (IOException e) {
            throw new SecurityException("Couldn't read settings file!");
        }
        while(scanner.hasNext()) {
            routes.add(new RoutePtr(new File(staticsDir,scanner.next())));
        }
        scanner.close();
    }
    //------------------------------- TexttoSpeech interface ---------------------------

    public void onInit(int status) {
        if( status == TextToSpeech.SUCCESS)
            speaker_ready = true;
    }

}
