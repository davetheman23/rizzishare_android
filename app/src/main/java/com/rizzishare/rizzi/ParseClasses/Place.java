package com.rizzishare.rizzi.ParseClasses;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David on 9/13/2014.
 */
@ParseClassName("Place")
public class Place extends ParseObject{
    /*private String googlePlaceId;
    private ParseGeoPoint geoLocation;
    private String address;
    private String typeGoogle;
    private int category;

    private String placeName;*/
    private String descriptions;

    /*
	 * Keys for all fields in the class, will be used
	 * as column names in the Parse DataStore
	 */
    public static final String KEY_GOOGLE_PLACE_ID = "google_places_id";
    public static final String KEY_GEO_LOCATION = "geo_location";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_TYPE_GOOGLE = "type_google";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_PLACE_NAME = "placeName";
    public static final String KEY_DESCRIPTION = "descriptions";

    public String getGooglePlaceId() {
        return getString(KEY_GOOGLE_PLACE_ID);
    }

    public void setGooglePlaceId(String googlePlaceId) {
        put(KEY_GOOGLE_PLACE_ID, googlePlaceId);
    }

    public ParseGeoPoint getGeoLocation() {
        return getParseGeoPoint(KEY_GEO_LOCATION);
    }

    public Location getLocation(){
        ParseGeoPoint point = getGeoLocation();
        Location loc = new Location("ParsePlaceClass");
        loc.setLatitude(point.getLatitude());
        loc.setLongitude(point.getLongitude());
        return loc;
    }

    public LatLng getLatlng(){
        return new LatLng(getLocation().getLatitude(),getLocation().getLongitude());
    }

    public void setGeoLocation(ParseGeoPoint geoLocation) {
        put(KEY_GEO_LOCATION,geoLocation);
    }

    /**
     * convenient method to directly convert a Json string into a parseGeolocation object
     * @param jsonStr in this format ("location" : {"lat" : -33.8669710,"lng" : 151.1958750})
     */
    public void setGeoLocation(String jsonStr) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject locObject = jsonObject.getJSONObject("location");
        double lat = locObject.getDouble("lat");
        double lng = locObject.getDouble("lng");
        ParseGeoPoint geoPoint = new ParseGeoPoint(lat,lng);
        put(KEY_GEO_LOCATION,geoPoint);
    }

    public String getAddress() {
        return getString(KEY_ADDRESS);
    }

    public void setAddress(String address) {
        put(KEY_ADDRESS,address);
    }

    public String getTypeGoogle() {
        return getString(KEY_TYPE_GOOGLE);
    }

    public void setTypeGoogle(String typeGoogle) {
        put(KEY_TYPE_GOOGLE,typeGoogle);
    }

    public int getCategory() {
        return getInt(KEY_CATEGORY);
    }

    public void setCategory(int category) {
        put(KEY_CATEGORY,category);
    }

    public String getPlaceName() {
        return getString(KEY_PLACE_NAME);
    }

    public void setPlaceName(String placeName) {
        put(KEY_PLACE_NAME,placeName);
    }

    public String getDescriptions() {
        return getString(KEY_DESCRIPTION);
    }

    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
        put(KEY_DESCRIPTION,descriptions);
    }

    @Override
    public String toString() {
        String str = this.descriptions;
        if (str !=null && !str.equals("")){
            return str;
        }
        return super.toString();
    }
}
