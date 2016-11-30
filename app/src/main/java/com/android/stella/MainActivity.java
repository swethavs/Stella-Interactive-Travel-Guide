package com.android.stella;

import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.speech.tts.TextToSpeech;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.android.stella.AddressFromLatLng.*;
import android.location.LocationListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.SearchManager;
import android.content.ComponentName;

import android.graphics.Typeface;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener, OnMapReadyCallback, LocationListener, TextToSpeech.OnInitListener, View.OnTouchListener {

    private FragmentActivity fragmentActivity;
    private GoogleMap mMap;
    LocationManager locationManager;
    boolean gps_enabled, network_enabled;
    String provider;
    Location location = null;
    static double latitude=0.0;
    static double longitude=0.0;
    private static final int PERMISSION_CODE_1= 23;
    private static final int SPEECH_RECOGNITION_CODE_LOCATION = 1;
    private static final int SPEECH_RECOGNITION_CODE_RADIUS=2;
    private static final int SPEECH_RECOGNITION_CODE_CONFIRM=3;
    private static final int SPEECH_RECOGNITION_CODE_AGAIN=4;
    private static final int SPEECH_RECOGNITION_CODE_SearchType=5;


    private static final int MY_DATA_CHECK_CODE = 1234;
    private static String seachTypeCode="";
    private static final String touristAttractionCode="999333";
    private static final String restaurantsCode="581208";
    private static final String gasStationsCode="554101";
    private static final String[] possibleSearchType={"restaurants", "restaurant", "hotel", "attractions","touristattractions", "gas", "gasstations", "petrol","gasstation"};

    double radius=0.0;

    static int currentSpeechState= MainActivity.SPEECH_RECOGNITION_CODE_LOCATION;
    public static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    String welcomeSpeechpart1 = "Welcome to Stella, an interactive travel guide assistant. You need to enable location to access this app.";
    String welcomeSpeechpart2 = "With this app you can find the tourist attractions, restaurants, and many more at your intended destination";
    String welcomeSpeechpart3 = "You need to enable the location to access this app." +
            "Let's get started.";
    String chooseLocationSpeech = "Please tell me a US city name. You can say current location if you want to look for tourist attractions near you";
    String chooseRadiusSpeech="Please choose radius in miles";
    String sayYesOrNoSpeech = "say yes to confirm. To change location say location.";
    String changeRadiusSpeech = "To change radius say radius";
    String noResultsSpeech="I am not able to find any in the city you have chosen. If you want to choose another city say yes";
    String currentStateLocationSpeech="please specify a location";
    String currentStateRadiusSpeech="Please specify the radius";
    String chooseSearchTypeSpeech=" choose what you want to look for. You can look for tourist attractions, restaurants or gas stations";
    String confirmLocationAndRadiusSpeech;

    TextToSpeech t1;
    TextView tv_location1;
    TextView tv_radius1;
    TextView tv_searchType1;
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

        /* Change Fonts displaced on page. TODO: Make fontsetter accept a list */
        fontSetter("robot.ttf", R.id.textView4);
        fontSetter("universe.ttf", R.id.textView5);
        fontSetter("robot.ttf", R.id.textView3);
        fontSetter("universe.ttf", R.id.textView6);
        fontSetter("robot.ttf", R.id.textView7);
        fontSetter("universe.ttf", R.id.textView8);
        fontSetter("robot.ttf", R.id.textView10);
        fontSetter("IDroid.otf", R.id.textView12);
        fontSetter("IDroid.otf", R.id.tv_chosen_location);
        fontSetter("IDroid.otf", R.id.tv_location);
        fontSetter("IDroid.otf", R.id.tv_chosen_radius);
        fontSetter("IDroid.otf", R.id.tv_radius);
        fontSetter("IDroid.otf", R.id.tv_search);
        fontSetter("IDroid.otf", R.id.tv_chosen_search_type);


        tv_location1=(TextView) findViewById(R.id.tv_chosen_location);
        tv_radius1=(TextView) findViewById(R.id.tv_chosen_radius);
        tv_searchType1=(TextView) findViewById(R.id.tv_chosen_search_type);
        btn_speak=(Button) findViewById(R.id.speak_button);
        btn_speak.setOnTouchListener(this);

        if (Build.VERSION.SDK_INT >=23 && (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) )
        {  requestPermissions();
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_1);

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

    private void fontSetter(String fontName, int textViewId) {
        TextView myTextView = (TextView) findViewById(textViewId);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/"+fontName);
        myTextView.setTypeface(typeface);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        try{
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                continueFromWhereTheUserLeft();
            }
        }
        catch(Exception e){
            Toast.makeText(getBaseContext(), "Press speech button after welcome speech", Toast.LENGTH_LONG).show();


        }
        return true;
    }

    private  void continueFromWhereTheUserLeft(){
        LOGGER.log(Level.SEVERE, "Inside continueFromWhereTheUserLeft");
        boolean speakingEnd;
        switch(currentSpeechState){
            case SPEECH_RECOGNITION_CODE_LOCATION:
                textToSpeech(currentStateLocationSpeech, TextToSpeech.QUEUE_FLUSH);
                t1.isSpeaking();

                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);
                break;
            case SPEECH_RECOGNITION_CODE_RADIUS:
                textToSpeech(currentStateRadiusSpeech, TextToSpeech.QUEUE_FLUSH);
                t1.isSpeaking();

                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);

                break;
            case SPEECH_RECOGNITION_CODE_CONFIRM:
                confirmLocationAndRadiusSpeechMethod();
                break;
            case SPEECH_RECOGNITION_CODE_AGAIN:
                noResultsSpeech="I am not able to find any" +
                        tv_searchType1.getText().toString() +
                        " in the city you have chosen. If you want to choose another city say yes";

                textToSpeech(noResultsSpeech, TextToSpeech.QUEUE_FLUSH);

                t1.isSpeaking();
                do{
                    speakingEnd = t1.isSpeaking();
                } while (speakingEnd);
                startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);
                break;
        }
    }

    private void confirmLocationAndRadiusSpeechMethod(){
        confirmLocationAndRadiusSpeech="You have chosen to find  "
                +tv_searchType1.getText().toString()
                +"near  "
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

    private void textToSpeech(String speech, int q) {
        /*if (Build.VERSION.SDK_INT >= 21) {//Removed this part to make build compatible for versions lowers than LOllipop
            t1.speak(speech,q,null,null);
            //LOGGER.log(Level.SEVERE, "The value of status is " +status + " " +speech + "q1 is " +q);
        } else {*/
        t1.speak(speech, q, null);

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
                    {  requestPermissions();
                    }

                }
            }

        }
    }

    private void callLocationManager(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        try {
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
        catch(SecurityException e){
            Toast.makeText(getBaseContext(), "Problem with location services", Toast.LENGTH_SHORT).show();

        }

    }

    /**
     * Executed when a new TTS is instantiated. Some static text is spoken via TTS here.
     * @param i
     */
    public void onInit(int i)
    {
        /*can do this in better way. but no time!*/
        textToSpeech(welcomeSpeechpart1, TextToSpeech.QUEUE_FLUSH);

        //This is deprecated but still compatible with API>21. Use playSilenceUtterance instead
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);

        textToSpeech(welcomeSpeechpart2, TextToSpeech.QUEUE_ADD);

        //This is deprecated but still compatible with API>21. Use playSilenceUtterance instead
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        textToSpeech(welcomeSpeechpart3, TextToSpeech.QUEUE_ADD);
        boolean speakingEnd = t1.isSpeaking();
        do{
            speakingEnd = t1.isSpeaking();
        } while (speakingEnd);

        //This is deprecated but still compatible with API>21. Use playSilenceUtterance instead
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        textToSpeech(chooseLocationSpeech, TextToSpeech.QUEUE_ADD);
        speakingEnd = t1.isSpeaking();
        do{
            speakingEnd = t1.isSpeaking();
        } while (speakingEnd);
        startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);


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
                            boolean speakingEnd;
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
                        tv_radius1.setText(text);

                        if(!(tv_radius1.getText().toString().contains("radius"))){
                            try {
                                radius = Double.parseDouble(tv_radius1.getText().toString());
                                textToSpeech(chooseSearchTypeSpeech, TextToSpeech.QUEUE_ADD);
                                boolean speakingEnd;
                                do{
                                    speakingEnd = t1.isSpeaking();
                                } while (speakingEnd);
                                t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);

                                startSpeechToText(SPEECH_RECOGNITION_CODE_SearchType);

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
                case SPEECH_RECOGNITION_CODE_SearchType: {
                    currentSpeechState=SPEECH_RECOGNITION_CODE_SearchType;

                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = result.get(0);
                        String trimmedString= text.replace(" ","").toLowerCase();

                        tv_searchType1.setText(text);
                        if((!(text.contains("Search Type"))) && Arrays.asList(possibleSearchType).contains(trimmedString)){

                            // private static final String[] possibleSearchType={"restaurants", "restaurant", "hotel", "attractions","touristattractions", "gas", "gasstations", "petrol","gasstation"};
                            //Can't use switch here :(
                            if(trimmedString.equalsIgnoreCase(possibleSearchType[0]) | trimmedString.equalsIgnoreCase(possibleSearchType[1]) |  trimmedString.equalsIgnoreCase(possibleSearchType[2])){
                                seachTypeCode = restaurantsCode;
                            }
                            else if(trimmedString.equalsIgnoreCase(possibleSearchType[3]) | trimmedString.equalsIgnoreCase(possibleSearchType[4])){
                                seachTypeCode = touristAttractionCode;
                            }
                            else if(trimmedString.equalsIgnoreCase(possibleSearchType[5]) | trimmedString.equalsIgnoreCase(possibleSearchType[6]) | trimmedString.equalsIgnoreCase(possibleSearchType[7]) | trimmedString.equalsIgnoreCase(possibleSearchType[8])){
                                seachTypeCode = gasStationsCode;
                            }
                            confirmLocationAndRadiusSpeechMethod();
                        }
                        else{
                            textToSpeech("That's not valid search type. you can say restaurants, gas stations or attractions", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd;
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);

                            startSpeechToText(SPEECH_RECOGNITION_CODE_SearchType);

                        }
                    }
                    break;
                }
                case SPEECH_RECOGNITION_CODE_CONFIRM:
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
                            boolean speakingEnd;
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);

                        }
                        else if(text.equalsIgnoreCase("radius")){
                            textToSpeech("Please tell me radius", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd;
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_RADIUS);

                        }
                        else {
                            textToSpeech("That's not a valid response", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd;
                            do{
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_CONFIRM);
                        }
                    }
                    break;
                case SPEECH_RECOGNITION_CODE_AGAIN: {
                    currentSpeechState = SPEECH_RECOGNITION_CODE_AGAIN;

                    if (resultCode == RESULT_OK && null != data) {
                        ArrayList<String> result = data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = result.get(0);

                        if (text.equalsIgnoreCase("yes")) {
                            textToSpeech("Please tell me a city name", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd;
                            do {
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_LOCATION);
                        } else {
                            textToSpeech("That's not a valid response", TextToSpeech.QUEUE_ADD);
                            boolean speakingEnd;
                            do {
                                speakingEnd = t1.isSpeaking();
                            } while (speakingEnd);
                            startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);
                        }
                    }

                    break;
                }


            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // Fire off an intent to check if a TTS engine is installed
                if (locationManager == null) {
                    callLocationManager();
                }
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);


            }
        });
    }

    private void makeRequestToMapQuest(){
        String url;
        if((tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("currentlocation") || tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("location") ||
                tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("near me") || tv_location1.getText().toString().replace(" ","").equalsIgnoreCase("near"))&& latitude !=0 && longitude !=0){
            url="https://www.mapquestapi.com/search/v2/radius?shapePoints="+
                    latitude+","+longitude + "&radius=" + radius +"&maxMatches=25&ambiguities=ignore&hostedData=mqap.ntpois|group_sic_code=?|"+seachTypeCode+"&outFormat=json&key=key";
        }
        else {
            url = "https://www.mapquestapi.com/search/v2/radius?origin=" +
                    tv_location1.getText().toString().replace(" ", "") + "&radius=" + radius + "&maxMatches=25&ambiguities=ignore&hostedData=mqap.ntpois|group_sic_code=?|"+seachTypeCode+"&outFormat=json&key=key";
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String jsonString = response.body().string();

            if(jsonString.contains("searchResults")){
                touistAttrIntent= new Intent(MainActivity.this, TouristAttractionActivity.class);
                touistAttrIntent.putExtra(MAPQUESTRESPONSE, response.toString());
                startActivity(touistAttrIntent);

            }
            else{
                textToSpeech(noResultsSpeech, TextToSpeech.QUEUE_ADD);
                boolean speakingEnd;
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

    @Override
    public void onDestroy() {
        t1.shutdown();
        super.onDestroy();
    }


    private static class GeocoderHandler extends Handler {
        Location location;
        GoogleMap mMap;

        private GeocoderHandler(Location location, GoogleMap nMap) {
            this.location = location;
            this.mMap = nMap;
        }

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


    /* Searchview methods */


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(
                new ComponentName(this, SearchableActivity.class)));
        searchView.setIconifiedByDefault(false);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(this, "Searching by: "+ query, Toast.LENGTH_SHORT).show();

        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String uri = intent.getDataString();
            Toast.makeText(this, "Suggestion: "+ uri, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // User pressed the search button
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // User changed the text
        return false;
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
                getApplicationContext(), new GeocoderHandler(location, mMap));
    }
}