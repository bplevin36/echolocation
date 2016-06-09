package com.example.ben.echolocation;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;


public class MainActivity extends ActionBarActivity implements PitchDetectionHandler, Runnable{

    private int chirpId, chirpStream;
    private final int BUFFER_SIZE = 512;
    public double beepTime = 0;
    public double heardTime = 0;
    double latency = 1; //this is empirical
    private long buttonTime;
    private boolean posSet = false;

    private SensorManager sensorManager;
    private Sensor mSensor;
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

        PitchDetectionHandler pdh = this;
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                41400, BUFFER_SIZE, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher,"Audio Dispatcher").start();
    }

    public void makeChirp(View view){
        buttonTime = SystemClock.elapsedRealtime();
        chirpStream = pool.play(chirpId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void calibrate(View view){
        final TextView mTextView = (TextView) findViewById(R.id.posView);

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://192.168.0.9:8000/beep";

        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest( url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.

                        double sentTime = 0.0;
                        try {
                            sentTime = response.getDouble("beep_time");
                            beepTime = sentTime;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mTextView.setText("Sent: "+sentTime);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText(error.toString());
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(8000,
                0,//no retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        try {
            Map headers = stringRequest.getHeaders();
            Toast.makeText(this, headers.toString(), Toast.LENGTH_LONG).show();
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
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

    @Override
    public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
        if(result.getPitch() > 12000.f){
            heardTime = System.currentTimeMillis() / 1000.0;
            runOnUiThread(this);
        }
    }

    public void calculateTime(double heard){
        double difference = heard - beepTime;
        difference -= latency;
        double distance = difference*0.34029;
        TextView timeText = (TextView)findViewById(R.id.timeText);
        timeText.setText(distance+" meters away from speaker");
    }

    @Override
    public void run() {
        TextView timeView = (TextView)findViewById(R.id.soundText);
        timeView.setText("Heard: "+heardTime);
        calculateTime(heardTime);
    }
}
