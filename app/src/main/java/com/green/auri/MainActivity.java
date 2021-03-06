package com.green.auri;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager;
import com.gigamole.infinitecycleviewpager.OnInfiniteCyclePageTransformListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.green.auri.augmentedreality.ARActivity;
import com.green.auri.favorites.FavoriteView;
import com.green.auri.restaurant.RestaurantCardAdapter;
import com.green.auri.restaurant.RestaurantResult;
import com.green.auri.utils.location.LocationListener;
import com.green.auri.utils.location.LocationUtils;
import com.green.auri.utils.photo.PhotoLoadingUtil;
import com.green.auri.utils.place.GetNearbyPlacesTask;
import com.green.auri.utils.place.PlaceSearchListener;
import com.green.auri.utils.place.PlaceSearchUtils;
import com.green.auri.utils.place.PlaceSearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.yavski.fabspeeddial.FabSpeedDial;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, PlaceSearchListener, LocationListener {

    private static final String TAG = "MapActivity";

    /* Constants */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String RESTAURANT_SEARCH = "restaurant";

    /* Bundle Keys */
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    /* Google Maps */
    private boolean locationPermissionGranted = false;
    protected GeoDataClient mGeoDataClient;
    private GoogleMap mMap;

    /* Last Updated Location */
    private double latitude;
    private double longitude;
    private Map<String, Marker> mMarkers;
    private List<RestaurantResult> restaurantList;

    /* User Preferences */
    public static String accountName;
    private SharedPreferences sharedPreferences;
    private String email;

    private static BitmapDescriptor RED_MARKER;
    private static BitmapDescriptor SELECTED_MARKER;

    private String selectedPlaceId;
    private boolean fullyUpdated;
    Handler mainHandler;

    private FabSpeedDial fab2;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the status bar if it is shown.
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        // Request location permissions before starting.
        getLocationPermission();

        mainHandler = new Handler(getMainLooper());

        // Fetch account information from the SharedPreferences
        sharedPreferences = getSharedPreferences("login", MODE_PRIVATE);
        accountName = sharedPreferences.getString("account", "N/A");
        email = sharedPreferences.getString("email", "N/A");

        // Initialize constants
        mGeoDataClient = Places.getGeoDataClient(this);
        restaurantList = new ArrayList<>();
        mMarkers = new HashMap<>();

        initCards();

        // Initialize Action Listeners
        fab2 = findViewById(R.id.fab2);
        initializeMenuActions();
        initializeOnPlaceSelectedAction();
        PhotoLoadingUtil.initPhotoLoadingUtil(mGeoDataClient, BitmapFactory.decodeResource(getResources(), R.drawable.na));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!fab2.isMenuOpen()){
            fab2.openMenu();
        }
    }

    /******************* Permission Checks *******************/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // request permission result function
        Log.d(TAG, "onRequestPermissionsResult: called.");
        locationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }

                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    locationPermissionGranted = true;

                    // initialize our map
                    initMap();
                }
            }
        }
    }

    private void getLocationPermission() {
        Log.d(TAG, "getting location permissions");

        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        /* First, Check for Fine Location */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            /* Secondly, Check that Course Location also allowed */
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // All permissions granted, can start the map.
                locationPermissionGranted = true;
                initMap();

            } else {

                // Request for the Location Permissions needed to run the app.
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {

            // Request for the Location Permissions needed to run the app.
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initializeMenuActions() {
        fab2 = findViewById(R.id.fab2);
        fab2.setMenuListener(new FabSpeedDial.MenuListener() {
            @Override
            public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                return true; // false: don't show menu
            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                boolean isNetworkConnected = isNetworkAvailable();

                String title = menuItem.getTitle().toString();
                if (isNetworkConnected){
                    switch (title) {
                        case "Auri Mode":
                            goToAuriMode();
                            break;
                        case "Settings":
                            Toast.makeText(MainActivity.this, "settings", Toast.LENGTH_LONG).show();
                            Intent settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
                            settings_intent.putExtra("email", email);
                            settings_intent.putExtra("accountName", accountName);
                            startActivity(settings_intent);
                            break;
                        case "Nearby Restaurant":
                            // Reference to the button to find nearby restaurants

                            Log.d(TAG, "Nearby Restaurant is Clicked");

                            // Get the URL to send a get request for the nearby restaurants.
                            String url = PlaceSearchUtils.buildQueryUrl(latitude, longitude, RESTAURANT_SEARCH);

                            new GetNearbyPlacesTask().execute(url, MainActivity.this);
                            Toast.makeText(MainActivity.this, "Nearby Restaurants", Toast.LENGTH_LONG).show();
                            break;
                        case "Favorites":
                            Log.d("Favorite", "Favorites Menu Button is Clicked");
                            Intent fav_intent = new Intent(MainActivity.this, FavoriteView.class);
                            startActivity(fav_intent);
                            break;
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Please connect the Internet and WIFI", Toast.LENGTH_LONG).show();
                }
                return true;
            }

            @Override
            public void onMenuClosed() {
                // Nothing needs to be done.
            }
        });
    }

    private void goToAuriMode() {
        Log.d("Aurimode", "Button is Clicked");
        Intent auri_intent = new Intent(MainActivity.this, ARActivity.class);
        startActivity(auri_intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");

        // Set the activity map to the ready map
        mMap = googleMap;

        if (locationPermissionGranted) {

            // Check for permission in manifest
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Both permissions were not granted, return.
                return;
            }

            // Enable current Location on the google map and the map settings
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            LocationUtils.getCurrentLocation(MainActivity.this, this);
            SELECTED_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
            RED_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            mMap.setOnMarkerClickListener(this);
        }
    }

    @Override
    public void onLocationUpdated(boolean success, double latitude, double longitude) {
        if (!success) {
            LocationUtils.getCurrentLocation(this, this);
            return;
        }

        this.latitude = latitude;
        this.longitude = longitude;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latitude, longitude), DEFAULT_ZOOM));
        /* Here, ViewCompat.isAttachedToWindow == true, then we can openMenu */
        if(!fab2.isMenuOpen()){
            fab2.openMenu();
        }
    }

    /******************* Search functionality *******************/

    @SuppressWarnings("deprecation")
    private void initializeOnPlaceSelectedAction() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override
            public void onPlaceSelected(Place place) {

                // Get place id after selecting the place from the fragment
                selectedPlaceId = place.getId();

                // Get Geo Data by using place id
                mGeoDataClient.getPlaceById(selectedPlaceId).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Clear the previous results
                        mMap.clear();
                        mMarkers.clear();
                        //restaurantList.clear();
                        restaurantList = new ArrayList<>();

                        fullyUpdated = false;

                        // Get the response of the place
                        PlaceBufferResponse places = task.getResult();

                        // Get restaurant information
                        Place myPlace = places.get(0);

                        // Add restaurant info to the result list
                        addRestaurantResult(myPlace);
                        addMarker(myPlace.getLatLng(), myPlace.getName().toString(), myPlace.getId());
                        selectMarker(mMarkers.get(myPlace.getId()));

                        notifyPager();

                        places.release();
                    }
                });
            }

            @Override
            public void onError(Status status) {
                Log.i("Error", "An error of autocomplete occurred: " + status);
            }
        });
    }

    /******************* Map Marker Functionality *******************/

    @Override
    public boolean onMarkerClick(Marker marker) {

        selectMarker(marker);

        HorizontalInfiniteCycleViewPager pager = findViewById(R.id.horizontal_cycle);

        if (selectedPlaceId != null) {
            for (int i = 0; i < restaurantList.size(); i++) {
                if (selectedPlaceId.equals(restaurantList.get(i).getRestaurantId())) {
                    pager.setCurrentItem(i, true);
                    return false;
                }
            }
        }

        return false;
    }

    private void addMarker(LatLng location, String placeName, String placeId) {

        if (mMarkers.containsKey(placeId)) {
            return;
        }

        // Place new colored marker on the map
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        markerOptions.title(placeName);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        Marker marker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        marker.setTag(placeId);

        mMarkers.put(placeId, marker);
    }

    private void selectMarker(Marker marker) {

        if (selectedPlaceId != null && mMarkers.containsKey(selectedPlaceId)) {
            mMarkers.get(selectedPlaceId).setIcon(RED_MARKER);
        }

        marker.setIcon(SELECTED_MARKER);
        marker.showInfoWindow();

        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        selectedPlaceId = (String) marker.getTag();
    }


    /******************* Restaurant Result Functions *******************/

    @Override
    public void onPlaceSearchComplete(List<HashMap<String, String>> nearbyPlacesList) {
        mMap.clear();
        mMarkers.clear();
        restaurantList = new ArrayList<>();
        selectedPlaceId = null;

        notifyPager();
        fullyUpdated = false;

        // Iterate through the nearby places that were returned and place a marker for each.
        for (int i = 0; i < nearbyPlacesList.size(); i++) {

            try {
                HashMap<String, String> googlePlace = nearbyPlacesList.get(i);
                PlaceSearchResult newGooglePlace = new PlaceSearchResult(googlePlace);

                addRestaurantResult(
                        newGooglePlace.getPlaceName(),
                        newGooglePlace.getVicinity(),
                        newGooglePlace.getRating(),
                        newGooglePlace.getPlaceId()
                );

                // Add the marker on the map
                addMarker(new LatLng(newGooglePlace.getLat(), newGooglePlace.getLng()), newGooglePlace.getPlaceName(), newGooglePlace.getPlaceId());
            } catch (NullPointerException e) {
                continue;
            }
        }

    }

    private void addRestaurantResult(Place place) {

        addRestaurantResult(
                place.getName().toString(),
                place.getAddress().toString(),
                Double.parseDouble(Float.toString(place.getRating())),
                place.getId()
        );

    }

    private void addRestaurantResult(String name, String address, double rating, String placeId) {
        // Add restaurant info to the result list
        RestaurantResult restaurantInfo = new RestaurantResult(
                name,
                address,
                rating,
                placeId
        );

        waitForPhotoResponse(restaurantInfo);
    }

    private void waitForPhotoResponse(RestaurantResult restaurantInfo) {
        PhotoLoadingUtil.getPhotoFromPlaceId(restaurantInfo.getRestaurantId(), (bitmapString) -> {
            restaurantInfo.setRestaurantPhoto(bitmapString);
            restaurantList.add(restaurantInfo);
            notifyPager();
        });
    }

    private void notifyPager() {
        HorizontalInfiniteCycleViewPager pager = findViewById(R.id.horizontal_cycle);
        pager.setAdapter(new RestaurantCardAdapter(restaurantList, getBaseContext()));
        setCurrentCard();
    }

    private void initCards() {
        HorizontalInfiniteCycleViewPager pager = findViewById(R.id.horizontal_cycle);
        RestaurantCardAdapter adapter = new RestaurantCardAdapter(restaurantList, getBaseContext());
        pager.setAdapter(adapter);

        setCurrentCard();

        pager.setOnInfiniteCyclePageTransformListener(new OnInfiniteCyclePageTransformListener() {

            @Override
            public void onPreTransform(View page, float position) {

                if (position == 0) {
                    TextView txtView = page.findViewById(R.id.txt_restaurant_name);
                    int index = Integer.valueOf(txtView.getText().toString().split(". ")[0]) - 1;
                    String restaurantId = restaurantList.get(index).getRestaurantId();
                    if (mMarkers.containsKey(restaurantId)) {
                        selectMarker(mMarkers.get(restaurantId));
                    }
                }
            }

            @Override
            public void onPostTransform(View page, float position) {
            }
        });

        fullyUpdated = true;
    }

    private void setCurrentCard() {
        HorizontalInfiniteCycleViewPager pager = findViewById(R.id.horizontal_cycle);

        if (pager.getAdapter() == null) {
            return;
        }

        if (selectedPlaceId != null) {
            for (int i = 0; i < restaurantList.size(); i++) {
                if (selectedPlaceId.equals(restaurantList.get(i).getRestaurantId())) {
                    pager.setCurrentItem(i, true);
                    return;
                }
            }
        }
    }

    /******************* Activity lifecycle *******************/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putDouble(KEY_LATITUDE, latitude);
            outState.putDouble(KEY_LONGITUDE, longitude);
            super.onSaveInstanceState(outState);
        }
    }
}
