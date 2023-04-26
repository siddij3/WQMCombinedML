/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.mcmaster.testsuitecommon;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import android.view.View;

import android.widget.Button;
//import ca.mcmaster.waterqualitymonitorsuite.DeviceScanActivity;
import ca.mcmaster.waterqualitymonitorsuite.R;

/**
 * Start up activity, allows user to select experiment application to use
 */
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = SplashActivity.class.getSimpleName();

    private static final int PERMISSION_RESPONSE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        //getSupportActionBar().setTitle(R.string.title_splash);// TODO: 2017-11-29

        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                        Log.w("BleActivity", "Coarse location access not granted!");
                        ActivityCompat.requestPermissions(this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                PERMISSION_RESPONSE);
                            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Fine location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_RESPONSE);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Fine location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_RESPONSE);
            }

        }

        Button btnLaunchWQM = (Button) findViewById(R.id.btnLaunchWQM);
        btnLaunchWQM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(SplashActivity.this,DeviceScanActivity.class);
                intent.putExtra(DeviceScanActivity.EXTRAS_SELECTED_APP, "WQM");
                startActivity(intent);
            }
        });

        Button btnLaunchPStat = (Button) findViewById(R.id.btnLaunchPStat);
        btnLaunchPStat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(SplashActivity.this,DeviceScanActivity.class);
                intent.putExtra(DeviceScanActivity.EXTRAS_SELECTED_APP, "PStat");
                startActivity(intent);
            }
        });


    }


}