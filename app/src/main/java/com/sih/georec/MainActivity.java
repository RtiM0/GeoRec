package com.sih.georec;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.yashovardhan99.timeit.Stopwatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Stopwatch.OnTickListener {

    public File da;
    Button play;
    TextView inf;
    JSONArray data = new JSONArray();
    int flag = 0;
    FileOutputStream fileOutputStream;
    MapView map = null;
    ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
    GeoPoint geoPoint;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Stopwatch stopwatch;
    long universalTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_main);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        da = new File(Environment.getExternalStorageDirectory().toString(), "georec");
        if (!da.exists()) {
            da.mkdirs();
        }
        play = findViewById(R.id.letsgo);
        inf = findViewById(R.id.des);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        stopwatch = new Stopwatch();
        stopwatch.setOnTickListener(this);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 0) {
                    data = new JSONArray();
                    play.setText("STOP RECORDING");
                    inf.setText("");
                    flag = 1;
                    createLocationRequest();
                } else {
                    stopLocationUpdates();
                    flag = 0;
                    play.setText("START RECORDING");
                    try {
                        File fileto = new File(da.toString(), "data.json");
                        fileOutputStream = new FileOutputStream(fileto);
                        fileOutputStream.write(data.toString().getBytes());
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(int index, OverlayItem item) {
                            return false;
                        }

                        @Override
                        public boolean onItemLongPress(int index, OverlayItem item) {
                            return false;
                        }
                    }, getApplicationContext());
                    mOverlay.setFocusItemsOnTap(true);
                    map.getOverlays().add(mOverlay);
                    IMapController mapController = map.getController();
                    mapController.setZoom(15);
                    mapController.setCenter(geoPoint);
                    Log.d("JSON", data.toString());
                }

            }
        });
    }

    protected void createLocationRequest() {
        stopwatch.start();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {

            @Override

            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                // All location settings are satisfied. The client can initialize

                // location requests here.

                // ...
                startLocationUpdates();
            }

        });
        task.addOnFailureListener(this, new OnFailureListener() {

            @Override

            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:

                        // Location settings are not satisfied, but this can be fixed

                        // by showing the user a dialog.

                        try {

                            // Show the dialog by calling startResolutionForResult(),

                            // and check the result in onActivityResult().

                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this, 1);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }

        });
    }

    private void startLocationUpdates() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    JSONObject cur = new JSONObject();
                    String co = location.getLatitude() + "," + location.getLongitude();
                    String ex = inf.getText().toString();
                    String infer = ex + "\nTime: " + universalTime / 1000 + " secs\n CO: " + co;
                    inf.setText(infer);
                    try {
                        cur.put("time", universalTime / 1000);
                        cur.put("loc", co);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d("onLocationResult: ", location.getLatitude() + "," + location.getLongitude());
                    data.put(cur);
                    items.add(new OverlayItem("Time", universalTime / 1000 + " Seconds", new GeoPoint(location.getLatitude(), location.getLongitude())));
                    geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // TODO: Consider calling

            //    ActivityCompat#requestPermissions

            // here to request the missing permissions, and then overriding

            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,

            //                                          int[] grantResults)

            // to handle the case where the user grants the permission. See the documentation

            // for ActivityCompat#requestPermissions for more details.

            Toast.makeText(getApplicationContext(), "location permission required !!", Toast.LENGTH_SHORT).show();

            return;

        }
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,

                mLocationCallback,

                null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        stopwatch.stop();
    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onTick(Stopwatch stopwatch) {
        universalTime = stopwatch.getElapsedTime();
    }
}