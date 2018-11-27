package com.green.auri;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.location.FusedLocationProviderClient;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    SupportMapFragment mapFragment;
    private Boolean mLocation = false;
    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private GoogleMap mMap;
    private int PROXIMITY_RADIUS = 500; // 500 meters or 0.3 miles away
    double latitude;
    Location mLastLocation;
    double longitude;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private String api_key = "AIzaSyCSSgGt6d67TiIUl0SiwEvkVkvGU1PL1-U";
    private Button auri_mode;

    private SharedPreferences sp;

//    String rname;
//    String raddress;
//    float rating;
//    private TextView txt_rname;
//    private TextView txt_raddress;
//    private RatingBar rb;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocation) {

            // Check for permission in manifest
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // Enable current Location on the google map and the map settings
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            getDeviceLocation(); // get the Location of device

            // Reference to the button to find nearby restaurants
            Button btnRestaurant = (Button) findViewById(R.id.restu);
            btnRestaurant.setOnClickListener(new View.OnClickListener() {
                String Restaurant = "restaurant";
                @Override
                public void onClick(View v) {
                    Log.d("onClick", "Button is Clicked");
                    mMap.clear();
                    String url = getUrl(latitude, longitude, Restaurant); // get the url of nearby restaurants
                    Object[] DataTransfer = new Object[2];
                    DataTransfer[0] = mMap;
                    DataTransfer[1] = url;
                    Log.d("onClick", url);
                    GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData(latitude, longitude);
                    getNearbyPlacesData.execute(DataTransfer);
                    Toast.makeText(MainActivity.this,"Nearby Restaurants", Toast.LENGTH_LONG).show();
                }
            });

            mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
        }
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {
        // parse the url function
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&opennow=true");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + api_key);
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    private void getDeviceLocation() { // get the device location
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        // use the location service
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocation){ // if allow to find device location
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    // Perform the location listener
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            mLastLocation = (Location) task.getResult();
                            // move map camera
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastLocation.getLatitude(),
                                            mLastLocation.getLongitude()), DEFAULT_ZOOM));

                            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                            latitude = mLastLocation.getLatitude();
                            longitude = mLastLocation.getLongitude();

                            Toast.makeText(MainActivity.this, "Your Current Location", Toast.LENGTH_LONG).show();
                            Log.d("onLocationChanged", String.format("latitude:%.3f longitude:%.3f",latitude,longitude));

                        }else{ // Error of finding the current location
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MainActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

//    private void moveCamera(LatLng latLng, float zoom){ //
//        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ask device for location permission
        getLocationPermission();

        // Reference to Auri Mode Button
        Button auri_mode = (Button) findViewById(R.id.AuriMode);
        auri_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Aurimode", "Button is Clicked");
                Intent auri_intent = new Intent(MainActivity.this, IndexActivity.class);
                startActivity(auri_intent);
            }
        });

        // Reference to Normal Mode Button (PlaceAPI)
        Button normal_mode = (Button) findViewById(R.id.NormalMode);
        normal_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Normalmode", "Button is Clicked");
                Intent normal_intent = new Intent(MainActivity.this, PlaceAPI.class);
                startActivity(normal_intent);
            }
        });

        // Reference to Camdir Mode Button (Camdir activity)
        Button camdir = (Button) findViewById(R.id.camdir);
        camdir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Camdir", "Button is Clicked");
                Intent camdir_intent = new Intent(MainActivity.this, Camdir.class);
                startActivity(camdir_intent);
            }
        });

//        txt_rname = (TextView) findViewById(R.id.txt_rname);
//        txt_raddress = (TextView) findViewById(R.id.txt_raddress);
//        rb = (RatingBar) findViewById(R.id.rb);
//
//        txt_rname.setText(rname);
//        txt_raddress.setText(raddress);
//        rb.setRating(rating);


    }

    private void getLocationPermission() {
        // ask for device permission to find location
        Log.d(TAG, "getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocation = true;
                initMap(); // initiate the map if permission allowed
            }else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }

        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    private void initMap() { // map initialization function
        Log.d(TAG, "initMap: initializing map");
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // menu inflator for the sign out textview
        // Inflate the Log Out menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // selecting Log Out from the menu
        // If click logout, go to logout class
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout(); // if clicked, signs the user out
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout(){
        // sign out of this user and go to the log in page
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        sp.edit().putBoolean("logged",false).apply();
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // request permission result function
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocation = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocation = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocation = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        String title = marker.getTitle();
        LatLng ll = marker.getPosition();
        String lat = String.valueOf(ll.latitude);
        String lng = String.valueOf(ll.longitude);

        Log.i("!!!",title);
        Log.i("!!!",lat);
        Log.i("!!!",lng);

        // info[0] is restaurant name; info[1] is address; info[2] is rating
        String[] info = title.split(":");


        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        Card curCard = new Card();
        curCard.setInfo(info);

        transaction.replace(R.id.fragment, curCard);
        transaction.commit();
//        rname = info[0];
//        raddress = info[1];
//        rating = Float.parseFloat(info[2]);

        return false;
    }
}
