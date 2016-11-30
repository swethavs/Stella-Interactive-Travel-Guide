package com.swetha.stella;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.widget.ListAdapter;

import android.widget.SimpleAdapter;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;


public class TouristAttractionActivity extends ListActivity implements TextToSpeech.OnInitListener{
    JSONObject json;
    JSONArray jarray;
    ListAdapter adapter;
    TextToSpeech tts;
    public static final int TTS_CODE = 1555;



    public  ArrayList<HashMap<String, String>> touristAttrctionsList = new ArrayList<HashMap<String, String>>();
    public static final String distancekey="distancekey";
    public static final String  placeNameKey="placeName";
    private static final String afterDisplayResultSpeech="The results are displayed on the screen";
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

            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, TTS_CODE);

        }
        catch (Exception e){
            MainActivity.LOGGER.log(Level.SEVERE, "There was an exception in Tourist AttractionActivity" +e);

        }
    }
    /**
     * Callback for speech recognition activity
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                tts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
    private void textToSpeech(String speech, int q) {

        tts.speak(speech, q, null);
    }
    /**
     * Executed when a new TTS is instantiated. Some static text is spoken via TTS here.
     * @param i
     */
    public void onInit(int i)
    {
        MainActivity.LOGGER.log(Level.SEVERE, "Inside init");
        if (i == TextToSpeech.SUCCESS) {
            textToSpeech(afterDisplayResultSpeech, TextToSpeech.QUEUE_FLUSH);
            boolean speakeing = tts.isSpeaking();
            do {
                speakeing = tts.isSpeaking();
            } while (speakeing);
        }
        else if (i == TextToSpeech.ERROR) {
            Toast.makeText(TouristAttractionActivity.this,
                    "Error occurred while initializing Text-To-Speech engine", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onDestroy() {
        tts.shutdown();

    }

    public void onBackPressed(){
        finish();
        Intent intent= new Intent(TouristAttractionActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
