package com.tehilat.sidur;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
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
    private float smoothedAzimuth = 0f; // Сглаженный азимут для плавного движения
    private float alpha = 0.1f; // Коэффициент сглаживания (low-pass filter)

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

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
                directionText.setText(getString(R.string.location_permission_required));
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
                                calculateBearingToWailingWall(location.getLatitude(), location.getLongitude(), location);
                                hasLocation = true; // Устанавливаем флаг, что местоположение получено
                            } else {
                                directionText.setText(getString(R.string.location_not_determined));
                            }
                        }
                    });
        } catch (SecurityException e) {
            directionText.setText(getString(R.string.location_permission_required));
        }
    }

    private void calculateBearingToWailingWall(double userLat, double userLon, Location userLocation) {
        Location userLoc = new Location("");
        userLoc.setLatitude(userLat);
        userLoc.setLongitude(userLon);

        Location wailingWallLocation = new Location("");
        wailingWallLocation.setLatitude(Double.parseDouble(getString(R.string.wailing_wall_latitude)));
        wailingWallLocation.setLongitude(Double.parseDouble(getString(R.string.wailing_wall_longitude)));

        // Вычисляем азимут (направление) на Стену Плача
        bearingToWailingWall = userLoc.bearingTo(wailingWallLocation);

        // Корректируем направление с учётом магнитного склонения
        GeomagneticField geoField = new GeomagneticField(
                (float) userLat,
                (float) userLon,
                (float) userLocation.getAltitude(),
                System.currentTimeMillis()
        );
        bearingToWailingWall += geoField.getDeclination();
    }

    private void updateCompass(float azimuth) {
        if (!hasLocation) return; // Не обновляем компас, пока не получили местоположение

        // Применяем low-pass filter для сглаживания азимута
        smoothedAzimuth = smoothedAzimuth + alpha * (azimuth - smoothedAzimuth);

        // Вычисляем угол, который нужно повернуть компас (сглаженный азимут устройства минус направление на Стену Плача)
        float adjustedDegree = -smoothedAzimuth - bearingToWailingWall;

        // Обновляем текст направления с учётом текущего азимута
        float displayedBearing = (bearingToWailingWall + smoothedAzimuth + 360) % 360; // Нормализуем угол (0–360 градусов)
        directionText.setText(String.format(getString(R.string.direction_format), displayedBearing));

        // Анимация вращения компаса
        RotateAnimation rotateAnimation = new RotateAnimation(
                currentDegree,
                adjustedDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(getResources().getInteger(R.integer.compass_animation_duration));
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
            directionText.setText(getString(R.string.calibrate_compass_message));
        }
    }
}