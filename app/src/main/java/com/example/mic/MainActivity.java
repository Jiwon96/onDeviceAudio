package com.example.mic;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mic.classifier.AudioClassifier;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Button btnRecording;
    private Button btnSave;
    private static final int SAMPLING_RATE_IN_HZ = 16000;
    private static final int CHANNEL_CONF16= AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORD_DURATIONS_MS=2000; //record 2 sec
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_CONF16, AUDIO_FORMAT);
    private boolean isRecording=false;
    private short[] audioBuffer;
    private AudioRecord audioRecord = null;
    private FileOutputStream fos;
    private static final int REQUEST_CODE_AUDIO_PERMISSION = 100;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    AudioClassifier audioClassifier;
    private String savepath;
    private void requestAudioPermission() {
        // 권한이 이미 부여되었는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 부여되었으므로 오디오 관련 기능을 실행
            return;
        }

        // 권한 요청이 필요하다면, 권한 요청 다이얼로그 표시
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // 권한 요청의 이유를 설명하는 다이얼로그를 보여줍니다.
            Toast.makeText(this, "오디오 권한이 필요합니다. 설정에서 허용해주세요.", Toast.LENGTH_SHORT).show();
        } else {
            // 권한 요청 다이얼로그를 표시하고 사용자에게 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSION);
        }
    }

    private void initViews(){
        btnRecording = findViewById(R.id.btn_recording);
        btnSave = findViewById(R.id.btn_save);
        btnSave.setEnabled(false);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void setupClickListeners(){
        btnRecording.setOnClickListener(v->{
            if(!isRecording){
                startRecording();
            }else{
                stopRecording();
            }
        });
        btnSave.setOnClickListener(v->{
            saveFile();
        });
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        initViews();
        requestAudioPermission();
        setupClickListeners();
        audioClassifier = new AudioClassifier(this);
    }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여되었으므로 오디오 관련 기능을 실행
                return;
            } else {
                // 권한이 거부되었으므로 사용자에게 알림
                Toast.makeText(this, "오디오 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();

                // 만약 사용자가 권한을 영구적으로 거부한 경우, 설정 화면으로 이동하여 권한을 부여하도록 안내할 수 있습니다.
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        }
    }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startRecording(){
        try{
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                    SAMPLING_RATE_IN_HZ,
                    CHANNEL_CONF16,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );
            int totalSamples = SAMPLING_RATE_IN_HZ * RECORD_DURATIONS_MS / 1000; // record 2sec
            audioBuffer = new short[totalSamples];

            btnRecording.setText("recoding");
            btnRecording.setEnabled(false);

            audioRecord.startRecording();
            isRecording= true;
            btnRecording.setText("recoding");
            btnRecording.setEnabled(false);

            new Thread(() ->{
                int samplesRead =0;
                short[] buffer = new short[BUFFER_SIZE];

                while(isRecording && (samplesRead < totalSamples)){
                    int read = audioRecord.read(buffer, 0, Math.min(buffer.length, totalSamples - samplesRead));
                    if(read>0){
                        System.arraycopy(buffer, 0, audioBuffer, samplesRead, read);
                        samplesRead+=read;
                    }
                    Log.i("LOG_TAG", String.valueOf(isRecording) + " " + String.valueOf(samplesRead));
                }
                runOnUiThread(()->{
                    stopRecording();
                });


            }).start();

        }catch (Exception e){
            Toast.makeText(this, "record failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private interface DialogCallback{
        void onResult(String result);
    }

    private void showAlertDialog(Context context, DialogCallback callback){
        new AlertDialog.Builder(this).setMessage("마르시스를 말하셨습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onResult("true");
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onResult("false");
                        dialog.dismiss();
                    }
                }).show();
    }

    private void saveFile(){
        showAlertDialog(this, new DialogCallback() {
            @Override
            public void onResult(String result) {
                savePCMFILE(result);
            }
        });
    }

    private void savePCMFILE(String label){
        savepath = Environment.getExternalStorageDirectory().toString() + "/Download/"+label;
        File Folder = new File(savepath);
        if(!Folder.exists()){
            try {
                Folder.mkdir();
            }catch(Exception e){
                e.getStackTrace();
                Log.i("Folder", "생성 에러");
            }
        }else{
            Log.i("Folder", "이미 존재");
        }

        String fileName = "AUDIO_" + System.currentTimeMillis();

        try {
            File file = new File(savepath, fileName+".pcm");
            FileOutputStream fos = new FileOutputStream(file);
            ByteBuffer byteBuffer = ByteBuffer.allocate(audioBuffer.length * 2);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            Log.i("LOg", String.valueOf(audioBuffer.length));
            for(short sample: audioBuffer){
                byteBuffer.putShort(sample);
            }
            Toast.makeText(this, "PCM file is saved successfully", Toast.LENGTH_SHORT).show();
            fos.write(byteBuffer.array());
            fos.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "file path doesn't exist", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, "PCM file isn't saved", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void stopRecording(){
//        byteBuffer.position(0);

        float [] answer= audioClassifier.classify(audioBuffer);
        if(answer[0] > 0.5){
            Toast.makeText(MainActivity.this, String.valueOf(answer[0] * 100) +"확률, label 인식", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this, String.valueOf(answer[0] * 100) +"확률, 인식X", Toast.LENGTH_SHORT).show();
        }
        btnRecording.setTag("stop");
        if(audioRecord!=null && audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        isRecording=false;
        btnRecording.setText("recoding start");
        btnRecording.setEnabled(true);
        btnSave.setEnabled(true);
    }
}