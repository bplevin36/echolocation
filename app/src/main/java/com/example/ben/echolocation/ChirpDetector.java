package com.example.ben.echolocation;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;

/**
 * Created by Ben on 6/8/2016.
 */
public class ChirpDetector implements PitchDetectionHandler {

    public void run() {

    }

    @Override
    public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
        if(result.getPitch() > 12000.f){

        }
    }
}
