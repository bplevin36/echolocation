package com.example.ben.echolocation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;


public class MainActivity extends ActionBarActivity {

    private int chirpId, chirpStream;
    private final int BUFFER_SIZE = 1024;

    private SoundPool pool;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context con = this.getApplicationContext();

        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();

        pool = new SoundPool.Builder().setAudioAttributes(attributes).build();
        AssetFileDescriptor chirp;
        try {
            chirp = con.getAssets().openFd("chirp.wav");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        chirpId = pool.load(chirp, 1);
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(41400,
                BUFFER_SIZE,0);

        PitchDetectionHandler pdh = new ChirpDetector();
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                41400, BUFFER_SIZE, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher,"Audio Dispatcher").start();
    }

    public void makeChirp(View view){
        chirpStream = pool.play(chirpId, 1.0f, 1.0f, 0, 0, 1.0f);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
