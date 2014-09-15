package com.rizzishare.rizzi.utils;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.rizzishare.rizzi.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Defines app-wide constants and utilities for Location Services
 */
/*
 *  LocationUtils is meant to be only a holder of all variables methods and inner classes 
 *  related to location services, LocationUtils shall never be instantiated, so all its 
 *  members shall be declared as public and static so as to be accessed by outside  
 *  
 */
public final class LocationUtils {
	
	// don't allow instantiation by making it private
	private LocationUtils(){}

    // Debugging tag for the application
    public static final String APPTAG = "LocationSample";

    // Name of shared preferences repository that stores persistent state
    public static final String SHARED_PREFERENCES =
            						RizziApp.SHARED_PREF;

    // Key for storing the "updates requested" flag in shared preferences
    public static final String KEY_UPDATES_REQUESTED =
            RizziApp.APP_PACKAGE_PREFIX + ".KEY_UPDATES_REQUESTED";

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
 
    /*
     * Constants for location update parameters
     */
    // a smallest location displacement before getting a location update
	public static final float LOCATION_UPDATE_DISTANCE_METERS = 50;
	
    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;

    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;

    // Create an empty string for initializing strings
    public static final String EMPTY_STRING = new String();

    /**
     * Get the latitude and longitude from the Location object returned by
     * Location Services.
     *
     * @param currentLocation A Location object containing the current location
     * @return The latitude and longitude of the current location, or null if no
     * location is available.
     */
    public static String getLatLng(Location currentLocation) {
        // If the location is valid
        if (currentLocation != null) {

            // Return the latitude and longitude as strings
            return RizziApp.appContext.getString(
                    R.string.latitude_longitude,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {

            // Otherwise, return the empty string
            return EMPTY_STRING;
        }
    }
    
    /**
     * An AsyncTask that calls getFromLocation() in the background.
     * The class uses the following generic types:
     * Location - A {@link android.location.Location} object containing the current location,
     *            passed as the input parameter to doInBackground()
     * Void     - indicates that progress units are not used by this subclass
     * String   - An address passed to onPostExecute()
     */
    public static abstract class GetAddressTask extends AsyncTask<Location, Void, String> {
        // the context passed to the AsyncTask when the system instantiates it.
        private Context localContext;
        
        // the address format to be outputted passed in as parameter 
        private String format;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context, String addressFormat) {
            // Required by the semantics of AsyncTask
            super();

            localContext = context;
            format = addressFormat;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected String doInBackground(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
            Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

            // Get the current location from the input parameter list
            Location location = params[0];

            // Create a list to contain the result address
            List <Address> addresses = null;

            // Try to get an address for the current location. Catch IO or network problems.
            try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
                addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1
                );

                // Catch network or other I/O problems.
                } catch (IOException exception1) {

                    // Log an error and return an error message
                    Log.e(LocationUtils.APPTAG, localContext.getString(
                    									R.string.IO_Exception_getFromLocation));

                    // print the stack trace
                    exception1.printStackTrace();

                    // Return an error message
                    return (Resources.getSystem().getString(R.string.IO_Exception_getFromLocation));

                // Catch incorrect latitude or longitude values
                } catch (IllegalArgumentException exception2) {

                    // Construct a message containing the invalid arguments
                    String errorString = localContext.getString(
                            R.string.illegal_argument_exception,
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    // Log the error and print the stack trace
                    Log.e(LocationUtils.APPTAG, errorString);
                    exception2.printStackTrace();

                    //
                    return errorString;
                }
                // If the reverse geocode returned an address
                if (addresses != null && addresses.size() > 0) {

                    // Get the first address
                    Address address = addresses.get(0);

                    // Format the first line of address
                    String addressText = String.format(format, 

                            // If there's a street address, add it
                            (address.getMaxAddressLineIndex() > 0 ?
                                    address.getAddressLine(0) : ""),

                            // Locality is usually a city
                            address.getLocality(),

                            // The country of the address
                            address.getCountryName()
                    );

                    // Return the text
                    return addressText;

                // If there aren't any addresses, post a message
                } else {
                	return localContext.getString(R.string.no_address_found);
                }
        }

        /**
         * once the doInBackground is completed, this method is called. 
         * This method runs on the UI thread.
         */
        @Override
        protected abstract void onPostExecute(String formmatedAddress);
    }
    
    /**
     * Map error codes to error messages for the location service, 
     * it can be called whenever a connection to the location service 
     * is failed
     */
    public static class ErrorMessages {

        // Don't allow instantiation, by making it private
        private ErrorMessages() {}

        public static String getErrorString(Context context, int errorCode) {

            // Get a handle to resources, to allow the method to retrieve messages.
            Resources mResources = context.getResources();

            // Define a string to contain the error message
            String errorString;

            // Decide which error message to get, based on the error code.
            switch (errorCode) {
                case ConnectionResult.DEVELOPER_ERROR:
                    errorString = mResources.getString(R.string.connection_error_misconfigured);
                    break;

                case ConnectionResult.INTERNAL_ERROR:
                    errorString = mResources.getString(R.string.connection_error_internal);
                    break;

                case ConnectionResult.INVALID_ACCOUNT:
                    errorString = mResources.getString(R.string.connection_error_invalid_account);
                    break;

                case ConnectionResult.LICENSE_CHECK_FAILED:
                    errorString = mResources.getString(R.string.connection_error_license_check_failed);
                    break;

                case ConnectionResult.NETWORK_ERROR:
                    errorString = mResources.getString(R.string.connection_error_network);
                    break;

                case ConnectionResult.RESOLUTION_REQUIRED:
                    errorString = mResources.getString(R.string.connection_error_needs_resolution);
                    break;

                case ConnectionResult.SERVICE_DISABLED:
                    errorString = mResources.getString(R.string.connection_error_disabled);
                    break;

                case ConnectionResult.SERVICE_INVALID:
                    errorString = mResources.getString(R.string.connection_error_invalid);
                    break;

                case ConnectionResult.SERVICE_MISSING:
                    errorString = mResources.getString(R.string.connection_error_missing);
                    break;

                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    errorString = mResources.getString(R.string.connection_error_outdated);
                    break;

                case ConnectionResult.SIGN_IN_REQUIRED:
                    errorString = mResources.getString(
                            R.string.connection_error_sign_in_required);
                    break;
                default:
                    errorString = mResources.getString(R.string.connection_error_unknown);
                    break;
            }

            // Return the error message
            return errorString;
        }
    }
}
