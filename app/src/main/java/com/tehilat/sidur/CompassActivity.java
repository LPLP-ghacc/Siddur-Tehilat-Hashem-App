package com.tehilat.sidur;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView compassImage;
    private TextView directionText;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private FusedLocationProviderClient fusedLocationClient;

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float currentDegree = 0f;
    private float bearingToWailingWall = 0f; // Направление на Стену Плача
    private boolean hasLocation = false; // Флаг, указывающий, получили ли мы местоположение

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final double WAILING_WALL_LATITUDE = 31.7767;
    private static final double WAILING_WALL_LONGITUDE = 35.2345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        compassImage = findViewById(R.id.compass_image);
        directionText = findViewById(R.id.direction_text);

        // Инициализация сенсоров
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Инициализация геолокации
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Проверка разрешений
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                directionText.setText("Требуется разрешение на доступ к местоположению");
            }
        }
    }

    private void getUserLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                calculateBearingToWailingWall(location.getLatitude(), location.getLongitude());
                                hasLocation = true; // Устанавливаем флаг, что местоположение получено
                            } else {
                                directionText.setText("Не удалось определить местоположение");
                            }
                        }
                    });
        } catch (SecurityException e) {
            directionText.setText("Требуется разрешение на доступ к местоположению");
        }
    }

    private void calculateBearingToWailingWall(double userLat, double userLon) {
        Location userLocation = new Location("");
        userLocation.setLatitude(userLat);
        userLocation.setLongitude(userLon);

        Location wailingWallLocation = new Location("");
        wailingWallLocation.setLatitude(WAILING_WALL_LATITUDE);
        wailingWallLocation.setLongitude(WAILING_WALL_LONGITUDE);

        // Вычисляем азимут (направление) на Стену Плача
        bearingToWailingWall = userLocation.bearingTo(wailingWallLocation);
        directionText.setText(String.format("Направление на Стену Плача: %.0f°", bearingToWailingWall));
    }

    private void updateCompass(float azimuth) {
        if (!hasLocation) return; // Не обновляем компас, пока не получили местоположение

        // Вычисляем угол, который нужно повернуть компас (азимут устройства минус направление на Стену Плача)
        float adjustedDegree = -azimuth - bearingToWailingWall;

        // Анимация вращения компаса
        RotateAnimation rotateAnimation = new RotateAnimation(
                currentDegree,
                adjustedDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(500);
        rotateAnimation.setFillAfter(true);
        compassImage.startAnimation(rotateAnimation);
        currentDegree = adjustedDegree;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }

        if (gravity != null && geomagnetic != null) {
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]); // Текущий азимут устройства
                updateCompass(azimuth); // Обновляем компас с учетом текущего азимута
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            directionText.setText("Пожалуйста, откалибруйте компас (сделайте движение в форме восьмерки)");
        }
    }
}