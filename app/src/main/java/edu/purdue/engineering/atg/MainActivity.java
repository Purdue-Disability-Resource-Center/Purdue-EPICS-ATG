package edu.purdue.engineering.atg;

/**
 * All code herein is owned by Purdue-EPICS-DRC, and was created by the Fall 2017 team.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, TextToSpeech.OnInitListener {
    protected Intent[] options;
    protected String[] screenOptions;
    protected String speechStem = "Press the screen to proceed to ";
    protected int index = 0;
    protected TextToSpeech speaker;
    protected boolean speaker_ready = false;
    protected TextView screenOption;
    protected GestureDetector gestureDetector;
/** Create the app */
    @Override @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.settingsToolBar));

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        } else {
            SettingsFixer settingsFixer = new SettingsFixer(); //spawn a thread to check directories and settings and fix bad settings
            settingsFixer.start();
        }
        speaker = new TextToSpeech(this,this);
        gestureDetector = new GestureDetector(this,this);
        screenOption = (TextView) findViewById(R.id.mainScreenOptionText);
        options = new Intent[] {new Intent(this, RouteSelect.class), new Intent(this, SettingsScreen.class)};
        screenOptions = new String[] {"Route selection", "Settings"};
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);

    }
/** Start the app */
    protected void onStart() {
        super.onStart();
    }
/** Resume the app */
    protected void onResume() {
        super.onResume();
    }
/** Pause the app */
    protected void onPause() {
        super.onPause();
    }
/** Stop the app */
    protected void onStop() { super.onStop(); }
/** Destroy the app */
    protected void onDestroy() { super.onDestroy(); }
/** Receive the permissions results */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SettingsFixer settingsFixer = new SettingsFixer(); //spawn a thread to check directories and settings and fix bad settings
                    settingsFixer.start();
                }
                else {
                    this.finish();
                }
        }
    }

    /** Speak something
     *
     * @param speech the string to speak
     */
    private void speak(String speech) {
        if(speaker_ready) {
            if(Build.VERSION.SDK_INT > 21)
                speaker.speak(speech,TextToSpeech.QUEUE_ADD,null,"Main ATG Screen");
            else
                speaker.speak(speech,TextToSpeech.QUEUE_ADD,null);
        }
    }
/** Go to the next option in the menu */
    private void nextOption() {
        index++;
        if(index >= options.length)
            index = 0;
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);
        speak(speechStem + screenOptions[index]);
    }
/** Go to the last option in the menu */
    private void backOption() {
        index--;
        if(index <= 0)
            index = options.length-1;
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);
        speak(speechStem + screenOptions[index]);
    }
/** Select the current option */
    private void selectOption() {
        startActivity(options[index]);
    }
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
        if(velocityx > 0)
            nextOption();
        else
            backOption();
        return true;
    }
    /** {@code GestureDetector} handler for a long press. Indicates selection of the current route. Calls {@link #selectOption() beginRoute}
     *  @param e The {@code MotionEvent} representing the input. Not actually used in this method.
     *  @see #selectOption()
     */
    public void onLongPress(MotionEvent e) {
        selectOption();
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distancex, float distancey) {
        return true;
    }

    public void onShowPress(MotionEvent e) {

    }
    /** Handler for when a single tap is received. Indicates the user would like to move to the next route in the list.
     *  Does nothing.
     */
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    //-------------------------- TexttoSpeech Interface ----------------------------//
    /** Handler for when the TexttoSpeech initialization returns. Sets the flag to ready so the other operations can proceed without issue. */
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS)
            speaker_ready = true;
        speak(getString(R.string.disclaimer));
        speak(getString(R.string.now_at_main_screen));
    }

}
/** Fixer for the settings file. */
class SettingsFixer extends Thread {
    /** Do the work */
    public void run() {
        File settingsDir = new File(Environment.getExternalStorageDirectory().getPath()+ File.separator + RouteSelect.ROUTES_DIRECTORY + File.separator + "static");
        File settingsConfig = new File(settingsDir, "settings.txt");
        settingsDir.mkdirs(); //make the static directory if it doesn't exist, along with the ATG directory
        if(!settingsConfig.exists()) {
            FileWriter writer;
            try {
                writer = new FileWriter(settingsConfig, true);
            }
            catch (IOException e) {
                throw new SecurityException("Settings file could not be created!");
            }
            File[] statics = settingsDir.listFiles(new FileFilter() { //haha a bunch of this other code could've used FileFilters. I did a bunch of unnecessary work.
                public boolean accept(File pathname) {
                    return !pathname.getName().equals("settings.txt");
                }
            });
            for(File file : statics) {
                try {
                    writer.write(file.getName() + "\n");
                } catch (IOException e) {
                    throw new SecurityException("Settings file could not be written to!");
                }
            }
            try {
                writer.close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }
}
