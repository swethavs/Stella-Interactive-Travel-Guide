package com.android.stella;



import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;

import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.android.stella.AddressFromLatLng.*;

import android.location.LocationListener;


import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;

import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;


import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, TextToSpeech.OnInitListener, View.OnTouchListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    boolean gps_enabled, network_enabled;
    String provider;
    Location location = null;
    static double latitude;
    static double longitude;
    private static final int PERMISSION_CODE_1= 23;
    private static final int SPEECH_RECOGNITION_CODE_LOCATION = 1;
    private static final int SPEECH_RECOGNITION_CODE_RADIUS=2;
    private static final int SPEECH_RECOGNITION_CODE_CONFIRM=3;
    private static final int SPEECH_RECOGNITION_CODE_AGAIN=4;
    private static final int MY_DATA_CHECK_CODE = 1234;
    static int status;
    double radius=0.0;
    static int currentSpeechState= MainActivity.SPEECH_RECOGNITION_CODE_LOCATION;
    public static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    String welcomeSpeech = "Welcome to Stella, an interactive travel guide assistant."+
            "With this app you can find the tourist attractions and also get  driving  directions you need. You need to enable the location to access this app." +
            "Let's get started.";
    String chooseLocationSpeech = "Please tell me a US city name. You can say current location if you want to look for tourist attractions  near you";
    String chooseRadiusSpeech="Please choose radius in miles";
    String pleaseSayAgain = "Please say again";
    String sayYesOrNoSpeech = "say yes to confirm. To change location say location.";
    String changeRadiusSpeech = "To change radius say radius";
    String noResultsSpeech="I am not able to find any tourist attraction in the city you have chosen. If you want to choose another city say yes";
    String currentStateLocationSpeech="please specify a location";
    String currentStateRadiusSpeech="Please specify the radius";
    String confirmLocationAndRadiusSpeech;

    //String confirmLocationAndRadius="You have chosen to go to "
    public static boolean isSpeechInitialized=false;
    TextToSpeech t1;
    TextView tv_location1;
    TextView tv_radius1;
    Button btn_speak;

    Intent touistAttrIntent;
    public static final String MAPQUESTRESPONSE = "com.android.stella.MAPQUESTRESPONSE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        tv_location1=(TextView) findViewById(R.id.tv_chosen_location);

        tv_radius1=(TextView) findViewById(R.id.tv_chosen_radius);
        btn_speak=(Button) findViewById(R.id.speak_button);
        btn_speak.setOnTouchListener(this);


        if (Build.VERSION.SDK_INT >=23 && (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) )
        {  requestpermisions();
        }

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

       // LOGGER.log(Level.SEVERE, "after setContentView");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            continueFromWhereTheUserLeft();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE_1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                   callLocationManager();
                } else {
                    if (Build.VERSION.SDK_INT >=23 && (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) )
                    {  requestpermisions();
                    }

                }
                return;
            }

        }
    }

    private void callLocationManager(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        if (gps_enabled) {
            Criteria criteria = new Criteria();


            provider = locationManager.getBestProvider(criteria, true);




            if (provider != null && !provider.equals("")) {

                locationManager.requestLocationUpdates(provider, 10 * 60 * 1000, 1, MainActivity.this);
                // Get the location from the given provider
                location = locationManager.getLastKnownLocation(provider);


            }
        }
        if (location == null && network_enabled) {

            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 60 * 1000, 1, MainActivity.this);

        }

        if (location == null && !network_enabled && !gps_enabled) {
            Toast.makeText(getBaseContext(), "Enable your location services", Toast.LENGTH_LONG).show();
        }
        if (location != null)
            onLocationChanged(location);
        else {

            Toast.makeText(getBaseContext(), "Location can't be retrieved", Toast.LENGTH_SHORT).show();
        }


    }
    /**
     * Executed when a new TTS is instantiated. Some static text is spoken via TTS here.
     * @param i
     */
    public void onInit(int i)
    {
        textToSpeech(welcomeSpeech, TextToSpeech.QUEUE_FLUSH);

        //This is deprecated but still compatible with API>21. Use playSilenceUtterance instead
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        textToSpeech(chooseLocationSpeech, TextToSpeech.QUEUE_ADD);
        boolean speakingEnd = t1.isSpeaking();
        do{
            speakingEnd = t1.isSpeaking();
        } while (speakingEnd);
        startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);


    }

    private void textToSpeech(String speech, int q) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            t1.speak(speech,q,null,null);
            //LOGGER.log(Level.SEVERE, "The value of status is " +status + " " +speech + "q1 is " +q);

        } else {
            t1.speak(speech, q, null);
            //LOGGER.log(Level.SEVERE, "The value of status is " +status + "   "+ speech+ "q2 is " +q);

        }

    }
    /**
     * Start speech to text intent. This opens up Google Speech Recognition API dialog box to listen the speech input.
     * */
    private void startSpeechToText(int speech_code) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak something...");

        try {
            startActivityForResult(intent, speech_code);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Speech recognition is not supported in this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback for speech recognition activity
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_DATA_CHECK_CODE)
        {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
            {
                // success, create the TTS instance
                t1 = new TextToSpeech(this, this);
            }
            else
            {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
        else {//TODO: change this to switch
            switch (requestCode) {
                case SPEECH_RECOGNITION_CODE_LOCATION: {
                    currentSpeechState=SPEECH_RECOGNITION_CODE_LOCATION;
                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                        String text = result.get(0);
                        tv_location1.setText(text);
                        if(!(tv_location1.getText().toString().contains("Your location comes here"))){
                            textToSpeech(chooseRadiusSpeech, TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd = t1.isSpeaking();
                            do{
                                tv_location1.setText(text);

                                speakingEnd = t1.isSpeaking();

                            } while (speakingEnd);

                            startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);

                        }
                    }

                    break;
                }
                case SPEECH_RECOGNITION_CODE_RADIUS: {
                    currentSpeechState=SPEECH_RECOGNITION_CODE_RADIUS;
                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = result.get(0);
                        tv_radius1.setText(text);
                        if(!(tv_radius1.getText().toString().contains("radius"))){
                            try {
                                radius = Double.parseDouble(tv_radius1.getText().toString());
                                tv_radius1.setText(text);

                                confirmLocationAndRadiusSpeechMethod();
                            }
                            catch(Exception e){
                                textToSpeech("That's not valid radius. Please say valid radius", TextToSpeech.QUEUE_ADD);
                                boolean speakingEnd = t1.isSpeaking();
                                do{
                                    speakingEnd = t1.isSpeaking();
                                } while (speakingEnd);
                                t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);

                                startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);


                            }

                        }
                    }
                    break;
                }
                case SPEECH_RECOGNITION_CODE_CONFIRM:{
                    currentSpeechState=SPEECH_RECOGNITION_CODE_CONFIRM;
                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = result.get(0);

                        if(text.equalsIgnoreCase("yes")){
                          makeRequestToMapQuest();
                        }
                        else if(text.equalsIgnoreCase("location")){
                            textToSpeech("Please tell me a city name", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd = t1.isSpeaking();
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);

                        }
                        else if(text.equalsIgnoreCase("radius")){
                            textToSpeech("Please tell me radius", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd = t1.isSpeaking();
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);

                        }
                        else {
                            textToSpeech("That's not a valid response", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd = t1.isSpeaking();
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_CONFIRM);
                        }
                    }
                    break;
                }
                case SPEECH_RECOGNITION_CODE_AGAIN: {
                    currentSpeechState=SPEECH_RECOGNITION_CODE_AGAIN;

                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = result.get(0);

                        if(text.equalsIgnoreCase("yes")) {
                            startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);
                        }
                        else{
                            textToSpeech("That's not a valid response", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd = t1.isSpeaking();
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);
                        }
                        }


                }
                break;

            }
        }
    }

    private void confirmLocationAndRadiusSpeechMethod(){
        confirmLocationAndRadiusSpeech="You have chosen to find tourist attractions near"
                + tv_location1.getText().toString()
                + " within "
                + tv_radius1.getText().toString()
                +"miles radius";
        textToSpeech(confirmLocationAndRadiusSpeech, TextToSpeech.QUEUE_ADD);
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        textToSpeech(sayYesOrNoSpeech, TextToSpeech.QUEUE_ADD);
        t1.playSilence(150, TextToSpeech.QUEUE_ADD, null);
        textToSpeech(changeRadiusSpeech, TextToSpeech.QUEUE_ADD);
        boolean speakingEnd = t1.isSpeaking();
        do{
            speakingEnd = t1.isSpeaking();
        } while (speakingEnd);

        startSpeechToText(SPEECH_RECOGNITION_CODE_CONFIRM);
    }
    private  void continueFromWhereTheUserLeft(){
           LOGGER.log(Level.SEVERE, "Inside continueFromWhereTheUserLeft");
        boolean speakingEnd;
        switch(currentSpeechState){
            case SPEECH_RECOGNITION_CODE_LOCATION:
                textToSpeech(currentStateLocationSpeech, TextToSpeech.QUEUE_FLUSH);
                speakingEnd = t1.isSpeaking();

                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);
                break;
            case SPEECH_RECOGNITION_CODE_RADIUS:
                textToSpeech(currentStateRadiusSpeech, TextToSpeech.QUEUE_FLUSH);
                speakingEnd = t1.isSpeaking();

                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);

                break;
            case SPEECH_RECOGNITION_CODE_CONFIRM:
                confirmLocationAndRadiusSpeechMethod();
                break;
            case SPEECH_RECOGNITION_CODE_AGAIN:
                textToSpeech(noResultsSpeech, TextToSpeech.QUEUE_FLUSH);
                 speakingEnd = t1.isSpeaking();
                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // Fire off an intent to check if a TTS engine is installed
                if(locationManager == null){
                    callLocationManager();
                }
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);


            }
        });
        //LOGGER.log(Level.SEVERE, "after supportfragment");


    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @SuppressWarnings("static-access")
    @Override
    public void onLocationChanged(Location location) {
        if (location == null)
        {
            Toast.makeText(getApplicationContext(), "Please Enable your location services",Toast.LENGTH_LONG).show();
        }
        AddressFromLatLng locationAddress = new AddressFromLatLng();
        latitude=location.getLatitude();
        longitude=location.getLongitude();
        locationAddress.getAddressFromLocation(latitude, longitude,
                getApplicationContext(), new GeocoderHandler());
    }
    public void requestpermisions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_1);
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE_1);


    }

    private void makeRequestToMapQuest(){
        String url;
        if((tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("currentlocation") || tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("location") ||
                tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("near me") || tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("near"))&& latitude !=0 && longitude !=0){
            url="https://www.mapquestapi.com/search/v2/radius?shapePoints="+
                    latitude+","+longitude + "&radius=" + radius +"&maxMatches=25&ambiguities=ignore&hostedData=mqap.ntpois|group_sic_code=?|999333&outFormat=json&key=mapquestkey";

        }
        else {
            url = "https://www.mapquestapi.com/search/v2/radius?origin=" +
                    tv_location1.getText().toString().replace(" ", "") + "&radius=" + radius + "&maxMatches=25&ambiguities=ignore&hostedData=mqap.ntpois|group_sic_code=?|999333&outFormat=json&key=NKFhx5F063cGMqmi4NQ1n39tgZ0GjdaK";
        }
        URL obj;


        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            //LOGGER.log(Level.SEVERE,"Sending 'GET' request to URL : " + url);
            //LOGGER.log(Level.SEVERE,"Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();



            //LOGGER.log(Level.SEVERE,"The response was " + response.toString());

            if(response.toString().contains("searchResults")){
                touistAttrIntent= new Intent(MainActivity.this, TouristAttractionActivity.class);
                touistAttrIntent.putExtra(MAPQUESTRESPONSE, response.toString());
                startActivity(touistAttrIntent);

            }
            else{
                textToSpeech(noResultsSpeech, TextToSpeech.QUEUE_ADD);
                boolean speakingEnd = t1.isSpeaking();
                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    private class GeocoderHandler extends Handler {

        private static final String TAG = "MainActivity";
        @Override
        public void handleMessage(Message message) {
            try
            {
                String locationAddress;
                switch (message.what) {
                    case 1:
                        Bundle bundle = message.getData();
                        locationAddress = bundle.getString("address");
                        break;
                    default:
                        locationAddress = null;
                }

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(locationAddress)).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));

            }
            catch(Exception e)
            {
                LOGGER.log(Level.SEVERE, "An exception occurred in geocode handler");
            }

        }
    }
    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

}
