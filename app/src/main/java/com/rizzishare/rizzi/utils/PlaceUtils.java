package com.rizzishare.rizzi.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.rizzishare.rizzi.parseclass.Place;
import com.rizzishare.rizzi.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class provides methods to interact with google place objects,
 * including place autocomplete api, and others
 * Created by David on 9/13/2014.
 */
public class PlaceUtils {
    public static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    public static final String TYPE_DETAILS = "/details";
    public static final String TYPE_NEARBY = "/nearbysearch";
    public static final String OUT_JSON = "/json";

    private static final String LOG_TAG = "PlaceUtils";
    private static final String PLACES_API_BASE = Constants.URL_PLACE_API_BASE;
    private static final String API_KEY = Constants.GOOGLE_BROWSER_KEY;

    private static final String STATUS_AUTOCOMP_OK = "OK";
    private static final String STATUS_AUTOCOMP_ZERO_RESULTS = "ZERO_RESULTS";
    private static final String STATUS_AUTOCOMP_OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
    private static final String STATUS_AUTOCOMP_REQUEST_DENIED = "REQUEST_DENIED";
    private static final String STATUS_AUTOCOMP_INVALID_REQUEST = "INVALID_REQUEST";


    /**
     * This method makes an API call to google for a list of places that matches the users input,
     * location biasing can be applied to search only those matches near a center with radius applied.
     * This method cannot be called in the UI thread
     * @param input  the string to be searched
     * @param center  the center at which location biasing is applied, if null, then no location biasing is applied
     * @param radius_Km only used if loc is not null
     * @return a list of string that contains the autocomplete results
     */
    public static ArrayList<Place> autocomplete(String input, Location center, int radius_Km) {
        ArrayList<Place> resultList = null;

        // TODO one can also first retrieve data from local database before making a call

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:us");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            if (center != null){
                String loc = "location=" + center.getLatitude() + "," + center.getLongitude();
                loc = loc + "radius=" + Integer.toString(radius_Km);
                sb.append(loc);
            }
            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            String status = jsonObj.getString("status");
            if (status.equals(STATUS_AUTOCOMP_OK)) {
                JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

                // Extract the Place descriptions from the results
                resultList = new ArrayList<Place>(predsJsonArray.length());
                for (int i = 0; i < predsJsonArray.length(); i++) {
                    Place result = new Place();
                    result.setDescriptions(predsJsonArray.getJSONObject(i).getString("description"));
                    result.setGooglePlaceId(predsJsonArray.getJSONObject(i).getString("place_id"));
                    /*result.put(Place.KEY_DESCRIPTION,
                                predsJsonArray.getJSONObject(i).getString("description"));
                    result.put(Place.KEY_GOOGLE_PLACE_ID,
                            predsJsonArray.getJSONObject(i).getString("place_id"));*/
                    resultList.add(result);
                }
            }else{
                // if other status, need to handle errors returned from the server

            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    public abstract class GeocodeTask extends AsyncTask<String, Void, Address>{

        Context mContext = null;

        public void GeocodeTask(Context context){
            mContext = context;
        }

        @Override
        protected Address doInBackground(String... strings) {
            String locName = strings[0];
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocationName(locName,1);
                return addresses.get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected abstract void onPostExecute(Address address);
    }

    /**
     * This class makes a Google Place details API call. Input for the execute() method
     * is not needed, can supply null or any string. GooglePlaceID parameter, however,
     * will dictate where exactly the api call will be placed. In the constructor, make
     * sure the google place id is a valid one.
     */
    public static class GetPlaceDetailsTask extends CallRestApiInBackground {

        GoogleMap mMap = null;
        Context mContext = null;
        String googlePlaceId = null;
        Marker mMyDestMarker = null;

        public GetPlaceDetailsTask(Context context, GoogleMap map, String placeId){
            mMap = map;
            mContext = context;
            googlePlaceId = placeId;
        }

        public GetPlaceDetailsTask setMarker(Marker marker){
            mMyDestMarker = marker;
            return this;
        }

        @Override
        protected String overWriteUrl(String oldUrl) {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAILS + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&placeid=" + googlePlaceId);
            return sb.toString();
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (isCancelled()){
                return;
            }
            Place resultPlace = null;
            try {
                String status = result.getString("status");
                if (!status.equals(STATUS_AUTOCOMP_OK)) {
                    // TODO handle cases when return is not OK
                    Toast.makeText(mContext,"GetPlaceDetailsTask have response" + status,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Create a JSON object hierarchy from the results
                JSONObject resultObj = result.getJSONObject("result");

                resultPlace = new Place();
                resultPlace.setPlaceName(resultObj.getString("name"));
                resultPlace.setAddress(resultObj.getString("formatted_address"));
                resultPlace.setGooglePlaceId(resultObj.getString("place_id"));
                resultPlace.setGeoLocation(resultObj.getString("geometry"));

                ParseGeoPoint center = resultPlace.getGeoLocation();
                if (center != null) {
                    LatLng CIU = new LatLng(center.getLatitude(), center.getLongitude());


                    // move the map camera position to the center
                    CameraPosition cameraPosition
                            = new CameraPosition.Builder()
                            .target(CIU)
                            .zoom(15)
                            .tilt(45)
                            .build();
                    CameraUpdate camUpdate = CameraUpdateFactory
                            .newCameraPosition(cameraPosition);
                    if (mMap!= null) {
                        mMap.animateCamera(camUpdate);
                    }
                    // add marker on the map to show it
                    if (mMyDestMarker == null){
                        mMyDestMarker = mMap.addMarker(
                                new MarkerOptions()
                                        .position(CIU)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_drop_pin_dest))
                        );
                    }else{
                        mMyDestMarker.setPosition(CIU);
                        if (!mMyDestMarker.isVisible()){
                            mMyDestMarker.setVisible(true);
                        }
                    }
                    mMyDestMarker.setTitle(resultPlace.getDescriptions());
                    mMyDestMarker.setSnippet(resultPlace.getDescriptions());
                }else {
                    //TODO show error message that
                    Toast.makeText(mContext,
                            "cannot find location from address", Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }


    public static class GetNearbyPlacesTask extends CallRestApiInBackground{

        public static final int TYPE_LATLNG = 1;
        public static final int TYPE_TEXT = 2;

        private int mType = 1;

        private Context mContext = RizziApp.appContext;
        private GoogleMap mMap = null;
        private Location mCenter=null;

        public GetNearbyPlacesTask(Context context, GoogleMap map, int type){
            mContext = context;
            mMap = map;
            mType = type;
        }

        public GetNearbyPlacesTask setCenter(Location center){
            mCenter = center;
            return this;
        }


        @Override
        protected String overWriteUrl(String oldUrl) {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_NEARBY + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&radius=" + 1000);
            sb.append("&types=" + "food");
            switch (mType){
                case TYPE_LATLNG:
                    sb.append("&location=" + mCenter.getLatitude() + "," + mCenter.getLongitude());
                    break;
                case TYPE_TEXT:

                    break;
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (isCancelled()){
                return;
            }
            List<Place> resultPlaces = null;
            try {
                String status = result.getString("status");
                if (!status.equals(STATUS_AUTOCOMP_OK)) {
                    // TODO handle cases when return is not OK
                    Toast.makeText(mContext,"GetNearbyPlacesTask have response" + status,
                                                            Toast.LENGTH_LONG).show();
                    return;
                }
                // Create a JSON object hierarchy from the results
                JSONArray resultObjs = result.getJSONArray("results");

                // Parse the Json response from api call, and save them as Place objects
                resultPlaces = new ArrayList<Place>(resultObjs.length());
                for (int i = 0; i < resultObjs.length(); i++) {
                    JSONObject resultObj = resultObjs.getJSONObject(i);
                    Place place = new Place();
                    place.setPlaceName(resultObj.getString("name"));
                    place.setGooglePlaceId(resultObj.getString("place_id"));
                    place.setGeoLocation(resultObj.getString("geometry"));
                    resultPlaces.add(place);
                }

                // display these Place objects in the map
                for (Place place : resultPlaces){
                    mMap.addMarker(
                        new MarkerOptions()
                                .position(place.getLatlng())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_drop_pin_dest))
                    );
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }



}
