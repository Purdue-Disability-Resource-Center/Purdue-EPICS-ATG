package edu.purdue.engineering.atg;
/**
 * All code herein is owned by Purdue-EPICS-DRC, and was created by the Fall 2017 team.
 */

import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SettingsScreen extends AppCompatActivity implements GestureDetector.OnGestureListener, TextToSpeech.OnInitListener {
    File[] statics;
    boolean[] toggles;
    File settingsFile;
    TextToSpeech speaker;
    boolean speaker_ready = false;
    GestureDetector gestureDetector;
    TextView currentOption;
    TextView currentToggle;
    int index = 0;

    @Override
    /** Create the app */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_screen); //tell android where the layout is
        setSupportActionBar((Toolbar) findViewById(R.id.settingsToolBar)); //make the toolbar have the title
        currentOption = (TextView) findViewById(R.id.settingsScreenOptionText); //get the option TextView
        currentToggle = (TextView) findViewById(R.id.settingsScreenToggleText); //get the Toggle TextView
        gestureDetector = new GestureDetector(this, this); //register this class as its own gesture detector
        speaker = new TextToSpeech(this, this); //create a T2T engine for this activity
        File staticDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + RouteSelect.ROUTES_DIRECTORY + File.separator + "static"); //get the directory with the static routes
        statics = staticDir.listFiles(new FileFilter() { //get all the static routes, but not the settings file
            public boolean accept(File pathname) {
                return !pathname.getName().equals("settings.txt");
            }
        });
        settingsFile = new File(staticDir, "settings.txt"); //get the settings file
        String[] currentStatics = extractLines(settingsFile); //get the saved settings out of the saved file
        toggles = new boolean[statics.length]; //create a boolean array tracking whether the statics are active or inactive
        for (int i = 0; i < toggles.length; i++) {
            toggles[i] = isStringInside(statics[i].getName(), currentStatics);
        }
        showOptionState();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        writeSettings();
    }

    /**
     * Select the current option
     */
    protected void selectOption() {
        toggles[index] = !toggles[index];
        showOptionState();
        writeSettings();
    }

    /**
     * Go to the next option
     */
    protected void nextOption() {
        index++;
        if (index >= statics.length)
            index = 0;
        showOptionState();
    }

    /**
     * Go to the last option
     */
    protected void backOption() {
        index--;
        if (index < 0)
            index = statics.length - 1;
        showOptionState();
    }

    /**
     * Display the current option state
     */
    protected void showOptionState() {
        String state;
        if (toggles[index])
            state = getString(R.string.on);
        else
            state = getString(R.string.off);
        currentOption.setText(getString(R.string.current_option) + statics[index].getName());
        currentToggle.setText(getString(R.string.toggled) + state);
        if (speaker_ready) {
            if (Build.VERSION.SDK_INT > 21)
                speaker.speak(statics[index].getName() + " " + getString(R.string.is_currently) + " " + state, TextToSpeech.QUEUE_ADD, null, "ATG Settings");
            else
                speaker.speak(statics[index].getName() + " " + getString(R.string.is_currently) + " " + state, TextToSpeech.QUEUE_ADD, null);
        }
    }

    /**
     * Check if a string is inside an array of strings
     *
     * @param string  the string to look for
     * @param strings the strings to check against
     * @return whether the string is in there
     */
    protected boolean isStringInside(String string, String[] strings) {
        if (strings == null || strings.length == 0)
            return false;
        for (String s : strings)
            if (s.equals(string))
                return true;
        return false;
    }

    /**
     * Write the settings file
     */
    protected void writeSettings() {
        new Thread() {
            public void run() {
                Log.d("ATG","Starting settings write thread");
                settingsFile.delete();
                FileWriter writer;
                try {
                    Log.d("ATG","Creating settings file writer");
                    writer = new FileWriter(settingsFile, true);
                } catch (IOException e) {
                    throw new SecurityException("Can't write settings file!");
                }
                for (int i = 0; i < statics.length; i++) {
                    if (toggles[i]) {
                        try {
                            Log.d("ATG","Writing line " + i + " to settings file");
                            writer.write(statics[i].getName() + "\n");
                        } catch (IOException e) {
                            throw new SecurityException("Can't write settings file!");
                        }
                    }
                }
                try {
                    writer.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }.start();
        Log.d("ATG","Exiting writesettings method");
    }



    /** Get each line from a file as a string, in an array
     *
     * @param file The file from which to extract
     * @return all the lines from the file
     */
    protected String[] extractLines(File file) {
        ArrayList<String> nameList = new ArrayList<>();
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File for parsing doesn't exist!");
        }
        while(scanner.hasNext()) {
            nameList.add(scanner.next());
        }
        return nameList.toArray(new String[0]);
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
        if (status == TextToSpeech.SUCCESS)
            speaker_ready = true;
        if(Build.VERSION.SDK_INT > 21)
            speaker.speak(getString(R.string.now_at_settings_screen),TextToSpeech.QUEUE_ADD,null,"ATG Settings");
        else
            speaker.speak(getString(R.string.now_at_settings_screen),TextToSpeech.QUEUE_ADD,null);
    }

}
