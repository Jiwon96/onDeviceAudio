package com.example.mic;
import android.Manifest;
import android.content.Context;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Button btnRecording;
    private static final int SAMPLING_RATE_IN_HZ = 16000;
    private AudioRecord audioRecord = null;
    private FileOutputStream fos;
    private static final int REQUEST_CODE_AUDIO_PERMISSION = 100;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    AudioClassifier audioClassifier;
    ByteBuffer byteBuffer = ByteBuffer.allocate(SAMPLING_RATE_IN_HZ);
    float [] audioBuffer = new float[SAMPLING_RATE_IN_HZ*2]; // 16bit, 2초
    private boolean isFirst=true;
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        requestAudioPermission();
        btnRecording = findViewById(R.id.btn_recording);
        audioClassifier = new AudioClassifier(this);
        btnRecording.setOnClickListener(v->{
//            if(v.getTag().toString().equals("stop")){
//                startRecording(); // 2초가 지난 뒤에는 stop recording 해야됨
//            }
//            else {
//                stopRecording();
//            }
            File audioFile = createNewAudioFile();
            try {
                fos = new FileOutputStream(audioFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            new CountDownTimer(2000, 1000) { // 2초 (2000ms) 동안, 1초마다 업데이트
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                public void onTick(long millisUntilFinished) {
                    // 매 1초마다 실행될 코드 (예: UI 업데이트)
                    // millisUntilFinished: 남은 시간 (밀리초)

                    startRecording();
                    byteBuffer.rewind();
                    if(isFirst) {
                        for (int i = 0; i < SAMPLING_RATE_IN_HZ; i++) {
                                audioBuffer[i] = byteBuffer.get();
//                            }
                        }
                        isFirst=false;
                    }else {
                        for(int i=0; i<SAMPLING_RATE_IN_HZ; i++)
                            audioBuffer[SAMPLING_RATE_IN_HZ+i] = byteBuffer.get();
                    }

                    byteBuffer.rewind(); // byteBuffer 16000Hz

                }

                public void onFinish() {
                    // 2초가 지났을 때 실행될 코드
                    stopRecording();
                    // TODO: classifying the wake word(marusys).
//                    float [] audioBuffer = new float[SAMPLING_RATE_IN_HZ*2];
                    byteBuffer.rewind();
//
//                    for(int i=0; i<SAMPLING_RATE_IN_HZ*2; i++){
//                        byte b = byteBuffer.get();
//                        audioBuffer[i] = b;
//                    }
//                    float [] answer= audioClassifier.classify(audioBuffer);

//                    if(answer[0] > 0.5){
//                        Toast.makeText(MainActivity.this, String.valueOf(answer[0] * 100) +"확률, marusys 인식", Toast.LENGTH_SHORT).show();
//                    }else{
//                        Toast.makeText(MainActivity.this, String.valueOf(answer[0] * 100) +"확률, 인식X", Toast.LENGTH_SHORT).show();
//                    }

                }
            }.start();
        });
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
        int minimumBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
        if(minimumBufferSize < AudioRecord.SUCCESS) {
            Toast.makeText(this, "잘못된 크기: " + minimumBufferSize, Toast.LENGTH_SHORT).show();
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT, minimumBufferSize);
        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED){
            btnRecording.setText("인식중");
            btnRecording.setTag("recording");

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[minimumBufferSize];

                    try {
                        audioRecord.startRecording();
                        byteBuffer.rewind();
                        Log.i("test1", String.valueOf(byteBuffer.position()));
                        while (byteBuffer.position()+minimumBufferSize < SAMPLING_RATE_IN_HZ){
                            audioRecord.read(buffer, 0, minimumBufferSize);
                            byteBuffer.put(buffer, 0, minimumBufferSize);
                            fos.write(buffer);
                            Log.i("test2", String.valueOf(byteBuffer.position()));
                        }
                        int mod = SAMPLING_RATE_IN_HZ - byteBuffer.position();
                        buffer = new byte[mod];
                        audioRecord.read(buffer, 0, mod);
                        byteBuffer.put(buffer, 0, mod);
                        fos.write(buffer);
                        Log.i("test3", String.valueOf(byteBuffer.position()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopRecording();
                    }
                }
            });
        }
        else{
            Toast.makeText(this, "잘못된 상태:" + audioRecord.getState(), Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    }
    private File createNewAudioFile(){
        String path = Environment.getExternalStorageDirectory().toString() + "/Download/";
        String fileName = "AUDIO_222" + System.currentTimeMillis();
        return new File(path, fileName + ".pcm");
    }

    private void stopRecording(){
        byteBuffer.position(0);

        btnRecording.setTag("stop");
        btnRecording.setText("녹음");
        if(audioRecord!=null && audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if(fos != null){
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos = null;
        }
    }
}