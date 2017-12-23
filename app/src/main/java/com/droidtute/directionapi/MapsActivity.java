package com.droidtute.directionapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final int overview = 0;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM = 201;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE_TO = 202;
    private String TAG = getClass().getSimpleName();
    private GoogleMap mMap;
    private TextView mTvFrom;
    private TextView mTvTo;
    private String GOOGLE_PLACES_API_KEY = "YOUR GOOGLE PLACES API KEY";

    private List<Marker> markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        bindActivity();

    }

    private void bindActivity() {


        mTvFrom = (TextView) findViewById(R.id.tv_from);
        mTvTo = (TextView) findViewById(R.id.tv_to);

        findViewById(R.id.get_directions).setOnClickListener(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mTvFrom.setOnClickListener(this);
        mTvTo.setOnClickListener(this);
        mTvTo.setOnClickListener(this);

    }

    private void setupGoogleMapScreenSettings(GoogleMap mMap) {
        mMap.setBuildingsEnabled(true);
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupGoogleMapScreenSettings(googleMap);
        // Add a marker in Sydney and move the camera
        LatLng latLng = new LatLng(13.060530, 80.242329);
        mMap.addMarker(new MarkerOptions().position(latLng).title("Nungambakkam, Chennai, Tamil Nadu"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_from:
                pickLocation(PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM);
                break;
            case R.id.tv_to:
                pickLocation(PLACE_AUTOCOMPLETE_REQUEST_CODE_TO);
                break;
            case R.id.get_directions:
                if (mTvFrom.length() <= 0) {
                    showMessage("Please pick from address");
                    return;
                }
                if (mTvTo.length() <= 0) {
                    showMessage("Please pick to address");
                    return;
                }
                getResult();
                break;
        }
    }


    private void getResult() {
        DirectionsResult results = getDirectionsDetails(mTvFrom.getText().toString(), mTvTo.getText().toString(), TravelMode.DRIVING);
        if (results != null) {
            addPolyline(results, mMap);
            addMarkersToMap(results, mMap);
            positionCamera(results.routes[overview], mMap);
        }
    }

    private void addMarkersToMap(DirectionsResult results, GoogleMap mMap) {
        Marker markerSrc = mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[overview].legs[overview].startLocation.lat, results.routes[overview].legs[overview].startLocation.lng)).title(results.routes[overview].legs[overview].startAddress));
        Marker markerDes = mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[overview].legs[overview].endLocation.lat, results.routes[overview].legs[overview].endLocation.lng)).title(results.routes[overview].legs[overview].endAddress).snippet(getEndLocationTitle(results)));
        markers = new ArrayList<>();
        markers.add(markerSrc);
        markers.add(markerDes);
    }


    private void positionCamera(DirectionsRoute route, GoogleMap mMap) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 150);
        mMap.animateCamera(cu);

    }


    private void addPolyline(DirectionsResult results, GoogleMap mMap) {
        mMap.clear();
        List<LatLng> decodedPath = PolyUtil.decode(results.routes[overview].overviewPolyline.getEncodedPath());
        mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
    }

    private String getEndLocationTitle(DirectionsResult results) {
        return "Time :" + results.routes[overview].legs[overview].duration.humanReadable + " Distance :" + results.routes[overview].legs[overview].distance.humanReadable;
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext
                .setQueryRateLimit(3)
                .setApiKey(GOOGLE_PLACES_API_KEY)
                .setConnectTimeout(100, TimeUnit.SECONDS)
                .setReadTimeout(100, TimeUnit.SECONDS)
                .setWriteTimeout(100, TimeUnit.SECONDS);
    }


    private DirectionsResult getDirectionsDetails(String origin, String destination, TravelMode mode) {
        DateTime now = new DateTime();
        try {
            return DirectionsApi.newRequest(getGeoContext())
                    .mode(mode)
                    .origin(origin)
                    .destination(destination)
                    .departureTime(now)
                    .await();
        } catch (ApiException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
            return null;
        }
    }

    private void pickLocation(int requestCode) {
        try {

            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setCountry("IN")
                    .build();

            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setFilter(typeFilter)
                            .build(this);
            startActivityForResult(intent, requestCode);
        } catch (GooglePlayServicesRepairableException e) {
            showMessage(e.getMessage());
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
            showMessage(e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(this, data);
            Log.i(TAG, "Place: " + place.getName());
            setResultText(place, requestCode);
        } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            Status status = PlaceAutocomplete.getStatus(this, data);
            showMessage(status.getStatusMessage());
        }
    }

    private void setResultText(Place place, int requestCode) {
        switch (requestCode) {
            case PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM:

                mTvFrom.setText(place.getAddress());
                break;
            case PLACE_AUTOCOMPLETE_REQUEST_CODE_TO:

                mTvTo.setText(place.getAddress());
                break;
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
