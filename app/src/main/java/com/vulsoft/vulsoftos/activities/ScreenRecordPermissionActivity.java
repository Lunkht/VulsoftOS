package com.vulsoft.vulsoftos.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vulsoft.vulsoftos.ScreenRecorderService;

public class ScreenRecordPermissionActivity extends Activity {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_AUDIO_PERMISSION = 1002;
    
    private MediaProjectionManager mediaProjectionManager;
    private boolean isAudioEnabled = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // Retrieve audio preference passed from service
        if (getIntent() != null) {
            isAudioEnabled = getIntent().getBooleanExtra(ScreenRecorderService.EXTRA_AUDIO_ENABLED, true);
        }

        if (isAudioEnabled) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSION);
            } else {
                startProjectionRequest();
            }
        } else {
            startProjectionRequest();
        }
    }

    private void startProjectionRequest() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with audio enabled
                startProjectionRequest();
            } else {
                // Permission denied, disable audio and proceed
                isAudioEnabled = false;
                Toast.makeText(this, "Permission audio refusée, enregistrement sans son.", Toast.LENGTH_SHORT).show();
                startProjectionRequest();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // Permission granted, start service with result
                Intent serviceIntent = new Intent(this, ScreenRecorderService.class);
                serviceIntent.setAction(ScreenRecorderService.ACTION_START_RECORDING);
                serviceIntent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenRecorderService.EXTRA_RESULT_DATA, data);
                serviceIntent.putExtra(ScreenRecorderService.EXTRA_AUDIO_ENABLED, isAudioEnabled);
                startService(serviceIntent);
            } else {
                Toast.makeText(this, "Permission d'enregistrement refusée", Toast.LENGTH_SHORT).show();
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
