package edu.purdue.engineering.atg;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

/**
 * Created by Joseph Gerardot on 10/1/17.
 */

class StatsManager implements Parcelable {
    static final int GPS_ACTIVE = 0;
    static final int GPS_INACTIVE = 1;
    static final int GPS_NEED_PERMISSION = 2;

    public LinearLayout layout;
    public TextView[] fields;
    private String[] baseTexts;

    private LocationCallback statsLocationCallback;

    public StatsManager(LinearLayout a, TextView[] b) {
        layout = a;
        fields = b;

        baseTexts = new String[b.length];
        for( int i = 0; i < b.length; i++) { // Initialize baseTexts to contain the base text for all the fields. don't laugh. I'll make it better later.
            baseTexts[i] = b[i].getText().toString();
        }

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

    public void updateLocation(Location location) {
        fields[1].setText((baseTexts[1]+location.getLatitude()));
        fields[2].setText((baseTexts[2]+location.getLongitude()));
    }

    public void updateGPSState(int state) {
        switch (state) {
            case (GPS_ACTIVE):
                fields[3].setText(baseTexts[3]+" ACTIVE");
                break;
            case (GPS_INACTIVE):
                fields[3].setText(baseTexts[3]+" INACTIVE"); //really don't laugh at this. It's not even mission critical.
                break;
            case (GPS_NEED_PERMISSION):
                fields[3].setText(baseTexts[3]+" NEED PERMISSION");

        }
    }

    public void setLayout(LinearLayout a) {
        layout = a;
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == null)
                fields[i] = new TextView(layout.getContext());
            layout.addView(fields[i]);
        }
    }

    public LocationCallback getLocationCallback() {
        return statsLocationCallback;
    }

    /*--------------------------- Parcelables -------------------------- */

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeStringArray(baseTexts);
    }

    public final Parcelable.Creator<StatsManager> CREATOR = new Parcelable.Creator<StatsManager>() {

        public StatsManager[] newArray(int size) {
            return new StatsManager[size];
        }

        public StatsManager createFromParcel(Parcel in) {
            return new StatsManager(in.createStringArray());
        }

    };

    private StatsManager(String[] texts) {
        if(texts.length < 3)
            throw new IllegalArgumentException("Not enough strings to build stats manager!");
        baseTexts = texts;
        fields = new TextView[texts.length];

    }


}
