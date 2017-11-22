package edu.purdue.engineering.atg;

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
import java.util.ServiceConfigurationError;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, TextToSpeech.OnInitListener {
    protected final String openingSpeech = "Welcome to the Purdue DRC Automated Tour Guide. Other disclaimer stuff here.";
    protected Intent[] options;
    protected String[] screenOptions;
    protected String speechStem = "Press the screen to proceed to ";
    protected int index = 0;
    protected TextToSpeech speaker;
    protected boolean speaker_ready = false;
    protected TextView screenOption;
    protected GestureDetector gestureDetector;

    @Override @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.settingsToolBar));

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        speaker = new TextToSpeech(this,this);
        gestureDetector = new GestureDetector(this,this);
        screenOption = (TextView) findViewById(R.id.mainScreenOptionText);
        options = new Intent[] {new Intent(this, RouteSelect.class), new Intent(this, SettingsScreen.class)};
        screenOptions = new String[] {"Route selection", "Settings"};
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);
        SettingsFixer settingsFixer = new SettingsFixer(); //spawn a thread to check directories and settings and fix bad settings
        settingsFixer.start();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onStop() { super.onStop(); }

    protected void onDestroy() { super.onDestroy(); }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speaker_ready = true;
                    speak(openingSpeech);
                }
                else {
                    this.finish();
                }
        }
    }
    private void speak(String speech) {
        if(speaker_ready) {
            if(Build.VERSION.SDK_INT > 21)
                speaker.speak(speech,TextToSpeech.QUEUE_FLUSH,null,"Main ATG Screen");
            else
                speaker.speak(speech,TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    private void nextOption() {
        index++;
        if(index >= options.length)
            index = 0;
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);
        speak(speechStem + screenOptions[index]);
    }

    private void backOption() {
        index--;
        if(index <= 0)
            index = options.length-1;
        screenOption.setText(getString(R.string.current_option) + screenOptions[index]);
        speak(speechStem + screenOptions[index]);
    }

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
    }

}

class SettingsFixer extends Thread {
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
