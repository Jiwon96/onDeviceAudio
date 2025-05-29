package com.example.mic.classifier;

import android.content.Context;

public abstract class BaseAudioClassifier {
    protected Context context;

    public BaseAudioClassifier(Context context) {this.context=context;}

    public abstract float[] classify(short[] audioData);

    public String getLabel(float[] result){
        return "unsupported";
    }

}
