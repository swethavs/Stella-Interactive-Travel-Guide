package com.android.stella;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class SearchableActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public static final String MAPQUESTRESPONSE = "com.android.stella.MAPQUESTRESPONSE";
    private MyHandler mHandler;
    double radius= 100.0;
    Intent touistAttrIntent;
    private TextView txt;
    private TextView tv_location1;
    private static final String searchTypeCode="999333";
    private static final int SPEECH_RECOGNITION_CODE_AGAIN=4;
    String noResultsSpeech="I am not able to find any in the city you have chosen. If you want to choose another city say yes";
    TextToSpeech t1;

    @Override
    public void onInit(int i)
    {
        t1 = new TextToSpeech(this, this);
        //This is deprecated but still compatible with API>21. Use playSilenceUtterance instead
        t1.playSilence(500, TextToSpeech.QUEUE_ADD, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_location1 = (TextView) findViewById(R.id.tv_chosen_location);

        Bundle bundle= getIntent().getExtras();
        if (bundle!= null) {// to avoid the NullPointerException
            Intent intent = getIntent();
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                txt.setText("Searching by: " + query);

            } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                mHandler = new MyHandler(this);
                mHandler.startQuery(0, null, intent.getData(), null, null, null, null);
            }
        }
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

    /*public void updateText(String text){
        tv_location1.setText(text);
    }*/

    class MyHandler extends AsyncQueryHandler {
        // avoid memory leak
        WeakReference<SearchableActivity> activity;
        TextToSpeech t1 = new TextToSpeech(SearchableActivity.this, SearchableActivity.this);

        public MyHandler(SearchableActivity searchableActivity) {
            super(searchableActivity.getContentResolver());
            activity = new WeakReference<>(searchableActivity);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            String url;
            super.onQueryComplete(token, cookie, cursor);
            if (cursor == null || cursor.getCount() == 0) return;

            cursor.moveToFirst();

            long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            String text = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
            long dataId =  cursor.getLong(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID));

            cursor.close();

            if (activity.get() != null) {
                String newText = text.split(",")[0];
                url = "https://www.mapquestapi.com/search/v2/radius?origin=" +
                        newText + "&radius=" + radius + "&maxMatches=25&ambiguities=ignore&hostedData=mqap.ntpois|group_sic_code=?|"+searchTypeCode+"&outFormat=json&key="+"9uF5DAGd4DIqfh2e4vw5BQ30AyC2HyeN";

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Log.d("checking", request.toString());
                try {
                    Response response = client.newCall(request).execute();
                    String jsonString = response.body().string();
                    Log.d("sample-response", jsonString);
                    if(jsonString.contains("searchResults")) {
                        touistAttrIntent = new Intent(getApplicationContext(), TouristAttractionActivity.class);
                        touistAttrIntent.putExtra(MAPQUESTRESPONSE, jsonString);
                        SearchableActivity.this.startActivity(touistAttrIntent);
                    } else{
                        boolean speakingEnd;
                        do{
                            speakingEnd = t1.isSpeaking();
                        } while (speakingEnd);
                        startSpeechToText(SPEECH_RECOGNITION_CODE_AGAIN);

                        Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        SearchableActivity.this.startActivity(mainActivity);
                        textToSpeech(noResultsSpeech, TextToSpeech.QUEUE_ADD);

                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}