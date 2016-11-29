package com.android.stella;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONObject;


public class TouristAttractionActivity extends ListActivity {
    JSONObject json;
    JSONArray jarray;
    ListAdapter adapter;

    public  ArrayList<HashMap<String, String>> touristAttrctionsList = new ArrayList<HashMap<String, String>>();
    public static final String distancekey="distancekey";
    public static final String  placeNameKey="placeName";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_attractions);
        Intent displayIntent = getIntent();
        try {
            json = new JSONObject(displayIntent.getStringExtra(MainActivity.MAPQUESTRESPONSE));
            jarray = json.getJSONArray("searchResults");
            for (int i = 0; i < jarray.length(); ++i) {
                HashMap<String, String> indiTouristAttrMap = new HashMap<String, String>();


                JSONObject rec = jarray.getJSONObject(i);
                String name = rec.getString("name");
                double distance = rec.getDouble("distance");
                indiTouristAttrMap.put(distancekey, Double.toString(distance) +" miles");
                indiTouristAttrMap.put(placeNameKey, name);
                touristAttrctionsList.add(indiTouristAttrMap);

                MainActivity.LOGGER.log(Level.SEVERE, "You can visit " + name + ". It is at a distance of " + distance);
            }
            adapter = new SimpleAdapter(TouristAttractionActivity.this, touristAttrctionsList, R.layout.individual_attracions, new String[]{placeNameKey, distancekey}, new int[]{R.id.attrInfoTextView, R.id.distanceInMiles});

            setListAdapter(adapter);


        }
        catch (Exception e){
            MainActivity.LOGGER.log(Level.SEVERE, "There was an exception in Tourist AttractionActivity" +e);

        }
    }
}
