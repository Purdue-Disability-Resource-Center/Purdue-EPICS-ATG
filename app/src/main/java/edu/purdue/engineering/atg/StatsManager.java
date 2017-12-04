package edu.purdue.engineering.atg;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

/**
 * All code herein is owned by Purdue-EPICS-DRC, and was created by the Fall 2017 team.
 */

class StatsManager implements Parcelable {
    static final int GPS_ACTIVE = 0;
    static final int GPS_INACTIVE = 1;
    static final int GPS_NEED_PERMISSION = 2;

    public LinearLayout layout;
    public TextView[] fields;
    private String[] baseTexts;

    private LocationCallback statsLocationCallback;
    /** Create a new StatsManager
     * @param a The layout in which to work
     * @param b The TextViews to put data into.
     */
    public StatsManager(LinearLayout a, TextView[] b) {
        layout = a;
        fields = b;

        baseTexts = new String[b.length];
        for (int i = 0; i < b.length; i++) { // Initialize baseTexts to contain the base text for all the fields. don't laugh. I'll make it better later.
            baseTexts[i] = b[i].getText().toString();
        }

        initLocationCallback();
    }
    /** Initialize the location callback. Redesign this if any changes need to be made. */
    private void initLocationCallback() {
        statsLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for( Location location : locationResult.getLocations()) {
                    StatsManager.this.updateLocation(location);
                }
            }
        };
    }
    /** Update the displayed location
     *
     * @param location The location to be displayed
     */
    public void updateLocation(Location location) {
        fields[1].setText((baseTexts[1]+location.getLatitude()));
        fields[2].setText((baseTexts[2]+location.getLongitude()));
    }

    /** Update the displayed GPS state
     *
     * @param state one of 0, 1, or 2, representing the state to be set.
     */
    public void updateGPSState(int state) {
        switch (state) {
            case (GPS_ACTIVE):
                fields[3].setText(baseTexts[3]+" ACTIVE");
                break;
            case (GPS_INACTIVE):
                fields[3].setText(baseTexts[3]+" INACTIVE"); //this is good code
                break;
            case (GPS_NEED_PERMISSION):
                fields[3].setText(baseTexts[3]+" NEED PERMISSION");

        }
    }

    /** Set a new layout for this StatsManager.
     * @param a The layout in which to display
     */
    public void setLayout(LinearLayout a) {
        layout = a;
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == null)
                fields[i] = new TextView(layout.getContext());
            layout.addView(fields[i]);
        }
    }
    /** Get the {@code LocationCallBack} for this {@code StatsManager} */
    public LocationCallback getLocationCallback() {
        return statsLocationCallback;
    }

    /*--------------------------- Parcelables -------------------------- */
    /** Describe contents. Not used here */
    public int describeContents() {
        return 0;
    }
    /** Turn this object into a Parcel */
    public void writeToParcel(Parcel out, int flags) {
        out.writeStringArray(baseTexts);
    }
    /** Creator method to rebuild StatsManagers from Parcels */
    public static final Parcelable.Creator<StatsManager> CREATOR = new Parcelable.Creator<StatsManager>() {
        /** Implementation for the Parcelable interface */
        public StatsManager[] newArray(int size) {
            return new StatsManager[size];
        }
        /** Implementation for the Parcelable interface */
        public StatsManager createFromParcel(Parcel in) {
            return new StatsManager(in.createStringArray());
        }

    };
    /** Special private constructor for unparceling */
    private StatsManager(String[] texts) {
        if(texts.length < 3)
            throw new IllegalArgumentException("Not enough strings to build stats manager!");
        baseTexts = texts;
        fields = new TextView[texts.length];
        initLocationCallback();

    }


}
