package com.example.myLocal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;


import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private Button startLocationUpdatesButton, stopLocationUpdatesButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentlocation;


    private Boolean isLocationUpdatesActive = true;
    private String locationUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdatesButton = findViewById(R.id.startLocationUpdatesButton);
        stopLocationUpdatesButton = findViewById(R.id.stopLocationUpdatesButton);
        locationTextView = findViewById(R.id.locationTextView);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);

        fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startLocationUpdates();
            }
        });

        stopLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationUpdates();
            }
        });
        buildLocationRequest();
        buildLocalCallBack();
        buildLocationSettingRequest();
    }

    private void stopLocationUpdates() {
        if(!isLocationUpdatesActive){
            return;
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    .addOnCompleteListener(this,new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            isLocationUpdatesActive = false;//
                            startLocationUpdatesButton.setEnabled(true);
                            stopLocationUpdatesButton.setEnabled(false);
                        }
                    });


        }
    }

    private void startLocationUpdates() {
        isLocationUpdatesActive = true;
        startLocationUpdatesButton.setEnabled(false);
        stopLocationUpdatesButton.setEnabled(true);
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                           );
                        updateLocationUI();
                    }

                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode){
                            case LocationSettingsStatusCodes
                                .RESOLUTION_REQUIRED:
                                try{
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(MainActivity.this,
                                            CHECK_SETTINGS_CODE);
                                } catch (IntentSender.SendIntentException sie){
                                    sie.printStackTrace();
                                }
                                break;
                            case  LocationSettingsStatusCodes // невозможно изменить вручную настройки
                                        .SETTINGS_CHANGE_UNAVAILABLE:
                                String message =" Adjust location setting in your device";
                                Toast.makeText(MainActivity.this, message,Toast.LENGTH_LONG).show();
                                isLocationUpdatesActive = false;
                                startLocationUpdatesButton.setEnabled(true);
                                stopLocationUpdatesButton.setEnabled(false);
                        }
                        updateLocationUI();
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // результат обработки CHECK_SETTINGS_CODE
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (requestCode) {
                    case Activity.RESULT_OK:
                        Log.d("MainActivity", "User has agreed to change location" + "settings");
                        startLocationUpdates();
                        break;

                    case Activity.RESULT_CANCELED:
                        Log.d("MainActivity", "User has agreed to change location" + "settings");
                        isLocationUpdatesActive = false;
                        startLocationUpdatesButton.setEnabled(true);
                        stopLocationUpdatesButton.setEnabled(false);
                        updateLocationUI();
                        break;
                }
                break;
        }
    }

    private void buildLocationSettingRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocalCallBack() {
        locationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                    super.onLocationResult(locationResult);
                    currentlocation = locationResult.getLastLocation();
                    updateLocationUI();
            }
        };

    }

    private void updateLocationUI() {
        if(currentlocation != null) {
            locationTextView.setText("" + currentlocation.getLatitude() + "/" +
                    currentlocation.getLongitude()
            );
            locationUpdateTimeTextView.setText(
                    DateFormat.getTimeInstance().format(new Date())
            );
        }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()){
            startLocationUpdates();
        } else if(!checkLocationPermission()){
            requestLocationPermission();
        }

    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
           this,Manifest.permission.ACCESS_FINE_LOCATION
        );
        if(shouldProvideRationale){
         showSnackBar(
                 "Location permission is needed for " + "app functionality", "OK", new View.OnClickListener() { // отказ в разрешениии пользователем использования геоданных
                     @Override
                     public void onClick(View v) {
                         ActivityCompat.requestPermissions(
                                 MainActivity.this, new String[]{
                                         Manifest.permission.ACCESS_FINE_LOCATION
                                 },
                                 REQUEST_LOCATION_PERMISSION
                         );
                     }
                 }
         );
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );

        }
    }

    private void showSnackBar(final String mainText,
                              final String action,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                mainText,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(action,listener)
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_LOCATION_PERMISSION){
            if(grantResults.length <= 0){
                Log.d("onResultPermissionResult","Request was cancelled");
            }
            else  if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(!isLocationUpdatesActive){
                    startLocationUpdates();
                } else { // если пользователь отказался
                  showSnackBar("Turn on location on setting", "Setting",
                          new View.OnClickListener() {
                              @Override
                              public void onClick(View v) {
                                  Intent intent = new Intent();
                                  intent.setAction(
                                          Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                  Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                  intent.setData(uri);
                                  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                  startActivity(intent);
                              }

                          });
                }
            }
        }
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}