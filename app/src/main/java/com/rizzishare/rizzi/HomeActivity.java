package com.rizzishare.rizzi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.rizzishare.rizzi.ParseClasses.Place;
import com.rizzishare.rizzi.Utils.LocationUtils;
import com.rizzishare.rizzi.Utils.MyAutoCompleteTextView;
import com.rizzishare.rizzi.Utils.PlaceUtils;
import com.rizzishare.rizzi.Utils.PlacesAutoCompleteAdapter;
import com.rizzishare.rizzi.Utils.RizziApp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeActivity extends FragmentActivity implements
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private static final String LOG_TAG = "HomeActivity";

    /*
     * Define a request code to send to Google Play services This code is returned in
     * Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /*
     * google map objects
     */
    // the map itself
    private GoogleMap mGoogleMap;
    // the destination marker for the current user
    private Marker mMyDestMarker = null;
    // the origin and destination markers from other users
    //private List<Marker> mDestMarkers = new ArrayList<Marker>();
    private List<Marker> mOriginMarkers = new ArrayList<Marker>();

    PlaceUtils.GetPlaceDetailsTask getPlaceDetailsTask = null;

    PlaceUtils.GetNearbyPlacesTask getNearbyPlacesTask = null;


    private final static int MARKER_TYPE_ORIGIN = 1;
    private final static int MARKER_TYPE_DESTINATION = 2;

    /*
     * manage the Location Services connection and callbacks
     */
    private LocationClient mLocationClient;

    /*
     * hold the location update parameters used by the LocationClient instance
     */
    private LocationRequest mLocationRequest;

    /*
     *	Location variables that hold current and last location respectively
     */
    private Location mCurrentLocation;
    private Location mLastLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /*
         * Setup autocomplete text box
         */
        setupAutoCompPlace(this, R.id.activity_home_autoCompleteTextView,
                R.layout.list_item_place_autocomplete);

		/*
		 * setup the google map object
		 */
        // get a map reference
        mGoogleMap = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.activity_home_mappane))
                .getMap();
        // set a map long click listener
        mGoogleMap.setOnMapLongClickListener(longClickListener);
        UiSettings mapSettings = mGoogleMap.getUiSettings();
        mapSettings.setTiltGesturesEnabled(false);
        mapSettings.setZoomControlsEnabled(false);
        mapSettings.setCompassEnabled(false);
        mapSettings.setMyLocationButtonEnabled(false);
        mapSettings.setRotateGesturesEnabled(false);

        MapsInitializer.initialize(this);

        /*
         * create a location client and setup location request for the client
         */
        //  Create a new global location parameters object, and set request params
        mLocationRequest = LocationRequest.create();
        // set location request parameters
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setSmallestDisplacement(LocationUtils.LOCATION_UPDATE_DISTANCE_METERS);
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Create a new location client, using the enclosing class to handle callbacks
        mLocationClient = new LocationClient(this, this, this);

    }

    OnMapLongClickListener longClickListener = new OnMapLongClickListener() {

        @Override
        public void onMapLongClick(LatLng point) {

            // the the current user location as the origin
            Location origin = (mCurrentLocation == null) ?
                    mLastLocation : mCurrentLocation;
            if (origin == null){
                Toast.makeText(HomeActivity.this,
                        "Please try again after your location appears on the map.",
                        Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // obtain a location object where the user clicked as destination
            Location destination = new Location(RizziApp.APPTAG);
            destination.setLatitude(point.latitude);
            destination.setLongitude(point.longitude);
            destination.setTime(new Date().getTime());

            // add or move the marker on the map
            if (mMyDestMarker == null){
                mMyDestMarker = mGoogleMap.addMarker(
                        new MarkerOptions()
                                .position(point)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_drop_pin_dest))
                                .title(point.toString())
                );
                mMyDestMarker.setDraggable(true);
            }else{
                mMyDestMarker.setPosition(point);
            }

            getNearbyPlacesTask = new PlaceUtils.GetNearbyPlacesTask(
                    HomeActivity.this,
                    mGoogleMap,
                    PlaceUtils.GetNearbyPlacesTask.TYPE_LATLNG);
            getNearbyPlacesTask
                    .setCenter(destination)
                    .execute("");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null || !ParseFacebookUtils.isLinked(user)){
            logoutUser();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLocationClient.isConnected()){
            stopLocationUpdates();
        }
        mLocationClient.disconnect();
        if (getPlaceDetailsTask != null){
            getPlaceDetailsTask.cancel(true);
        }
        if (getNearbyPlacesTask != null){
            getNearbyPlacesTask.cancel(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.home_menu_log_out:
                logoutUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to start
     * an Activity that handles Google Play services problems. The result of this call returns here,
     * to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode){
		/*
         * In the case the request was by google play service connection
         */
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode){
                    case Activity.RESULT_OK:
                        break;
                    case ConnectionResult.RESOLUTION_REQUIRED:
                        Toast.makeText(this,"Resolution Required",Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(this,Integer.toString(resultCode), Toast.LENGTH_LONG).show();
                }
        }
    }

    /**
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.hasResolution()){
			/*
	         * Google Play services can resolve some errors it detects.
	         * If the error has a resolution, try sending an Intent to
	         * start a Google Play services activity that can resolve
	         * error.
	         */
            try {
                result.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

        }else{
            //option 1: show dialog fragment
            showErrorDialog(result.getErrorCode());
            // option 2: just show a toast
            Toast.makeText(
                    this,
                    LocationUtils.ErrorMessages.getErrorString(this, result.getErrorCode()),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = getLastKnownLocation();
        startLocationUpdates();
        findMyLocation(false);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by LocationListener, trigger events when system location
     * has changed.
     * But the location obtained is not necessarily a GPS location, if need
     * GPS location, need to
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (location != null){
            findMyLocation(true);
        }
        if (mLastLocation != null){
            // if last distance between the two locations
            /*if (CustomGeoPoints.getDistanceBetweenInMeters(
                    mCurrentLocation, mLastLocation)<10){
                return;
            }*/
        }
        mLastLocation = location;
    }

    private void setupAutoCompPlace(final Context context,
                                    int autoCompTvResId, int autoCompItemResId){
        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(context,autoCompItemResId);
        adapter.setLocationBias(mCurrentLocation, 10);

        MyAutoCompleteTextView autoCompView = (MyAutoCompleteTextView) findViewById(autoCompTvResId);
        final IBinder windowToken = autoCompView.getWindowToken();
        autoCompView.setThreshold(3);
        autoCompView.setAdapter(new PlacesAutoCompleteAdapter(context, autoCompItemResId));
        autoCompView.setEllipsize(TextUtils.TruncateAt.END);
        autoCompView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // hide the soft input (currently doesn't work)
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);

                // get the place item selected by the user
                Place item = (Place) adapterView.getItemAtPosition(position);

                /* ==== go fetch the details of the place ====*/
                // 1. get the google place id
                String googlePlaceId = item.get(Place.KEY_GOOGLE_PLACE_ID).toString();
                // 2. make an api call to fetch google place detail
                getPlaceDetailsTask = new PlaceUtils.GetPlaceDetailsTask(
                        HomeActivity.this,mGoogleMap,googlePlaceId);
                getPlaceDetailsTask
                        .setMarker(mMyDestMarker)
                        .execute("");
            }

        });
    }

    /**
     * Logout User and go back to the log in activity
     */
    private void logoutUser(){
        ParseUser.logOut();
        Intent intent = new Intent(this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected(){
        //check if Google Play services is available
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);

        // if it is available
        if (resultCode == ConnectionResult.SUCCESS){
            return true;
        }else{
            // if it isn't available, then error
            showErrorDialog(resultCode);
            return false;
        }
    }

    /**
     * get last known location of the device,
     * @param animateToLocationWhenFound
     */
    public void findMyLocation(boolean animateToLocationWhenFound){

        // If Google Play services is available
        if (servicesConnected() && mLocationClient.isConnected()){

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            if (currentLocation != null ){
                final LatLng CIU = new LatLng(currentLocation.getLatitude(),
                        currentLocation.getLongitude());

                // enable current location layer
                if (mGoogleMap != null){
                    mGoogleMap.setMyLocationEnabled(true);
                }

                // add a marker to the location
                CameraPosition cameraPosition
                        = new CameraPosition.Builder()
                        .target(CIU)
                        .zoom(15)
                        .tilt(45)
                        .build();
                CameraUpdate camUpdate = CameraUpdateFactory
                        .newCameraPosition(cameraPosition);
                //move map camera to my location, either animate or non-animate
                if(animateToLocationWhenFound){
                    mGoogleMap.animateCamera(camUpdate);
                }else{
                    mGoogleMap.moveCamera(camUpdate);
                }
            }

            // -- Get an address from the current location,
            // step 2: get the address in the background
            //(new GetAddressTask(this)).execute(currentLocation);
            // step 3: set the address text to mMyLocationMarker,
            // which is done after execute defined in the GetAddressTask class
        }
    }

    private void startLocationUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        mLocationClient.removeLocationUpdates(this);
    }

    /**
     * get the last known location from android location services provider,
     * if no location available, it will prompt user for turning on the location
     * service. It will immediately return a null location object, and wait for
     * user action to turn on any location service, once turned on, actions can be
     * taken in {@link #onLocationChanged} method.
     *
     * @return the location object, null if no location available
     */
    public Location getLastKnownLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = mLocationClient.getLastLocation();
            if (loc == null){
                // no location available, prompt user to turn on location services
                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Location Services Not Available");
                builder.setMessage("Please enable Location Services and GPS");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Show location settings when the user acknowledges the alert dialog
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                Dialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
            return loc;
        } else {
            return null;
        }
    }

    /**
     * Get the address of the current location, using reverse geocoding. This only works if
     * a geocoding service is available.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void getAddress(View v) {

        // In Gingerbread (API-9) and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (!Geocoder.isPresent()) {
            // No geocoder is present. Issue an error message
            Toast.makeText(this, "R.string.no_geocoder_available", Toast.LENGTH_LONG).show();
            return;
        }

        if (servicesConnected()) {
            // Get the current location
            //Location currentLocation = mLocationClient.getLastLocation();
            Location currentLocation = mCurrentLocation;

            // Turn the indefinite activity indicator on
            //mActivityIndicator.setVisibility(View.VISIBLE);

            // Start the background task
            (new LocationUtils.GetAddressTask(this, getString(R.string.address_output_string)){
                @Override
                protected void onPostExecute(String formmatedAddress) {
                    // do something here once the address is returned
                }
            }).execute(currentLocation);
        }
    }


    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog =
                GooglePlayServicesUtil.getErrorDialog(errorCode, this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(), "Location Updates");
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /*
     * clear markers on the map by its types
     */
    /*private void clearMarkers(int type){
        switch (type){
            case MARKER_TYPE_ORIGIN:
                for (Marker marker : mOriginMarkers){
                    marker.remove();
                }
                break;
            case MARKER_TYPE_DESTINATION:
                for (Marker marker : mDestMarkers){
                    marker.remove();
                }
                break;
        }
    }*/



}