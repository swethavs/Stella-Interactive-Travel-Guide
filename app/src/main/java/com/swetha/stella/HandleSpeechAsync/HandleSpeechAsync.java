/*package com.swetha.stella.HandleSpeechAsync;


import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

import com.swetha.stella.MainActivity;

import java.util.Locale;
import java.util.logging.Level;

public class HandleSpeechAsync {
    public static  void initSpeech(
                                   final Context context, final Handler handler) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                TextToSpeech t1 =new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR) {
                            MainActivity.status= status;
                            t1.setLanguage(Locale.US);
                            MainActivity.isSpeechInitialized = true;

                        }
                    }

                });
            }
        };
        thread.start();

}*/
