package com.sih.georec;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import mumayank.com.airlocationlibrary.AirLocation;

public class MainActivity extends AppCompatActivity {

    public File da;
    Button play;
    TextView inf;
    JSONArray data = new JSONArray();
    int flag = 0;
    int uni = 0;
    int i = 0;
    FileOutputStream fileOutputStream;
    MapView map = null;
    ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
    GeoPoint geoPoint;
    private AirLocation airLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_main);
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
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 0) {
                    data = new JSONArray();
                    uni = 0;
                    play.setText("STOP RECORDING");
                    inf.setText("");
                    flag = 1;
                    repeater();
                } else {
                    uni = -1;
                    i = 0;
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

    void repeater() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (uni != -1) {
                    getloc(i);
                    i = i + 2;
                    repeater();
                }
            }
        }, 2000);
    }

    void getloc(final int secs) {
        airLocation = new AirLocation(this, true, true, new AirLocation.Callbacks() {
            @Override
            public void onSuccess(@NotNull Location location) {
                // do something

                JSONObject cur = new JSONObject();
                String co = location.getLatitude() + "," + location.getLongitude();
                String ex = inf.getText().toString();
                String infer = ex + "\nTime: " + secs + " secs\n CO: " + co;
                inf.setText(infer);
                try {
                    cur.put("time", secs);
                    cur.put("loc", co);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                data.put(cur);
                items.add(new OverlayItem("Time", secs + " Seconds", new GeoPoint(location.getLatitude(), location.getLongitude())));
                geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onFailed(@NotNull AirLocation.LocationFailedEnum locationFailedEnum) {
                // do something
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        airLocation.onActivityResult(requestCode, resultCode, data);
    }

    // override and call airLocation object's method by the same name
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
}