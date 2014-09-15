package com.rizzishare.rizzi.utils;

import android.content.Context;
import android.location.Location;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.rizzishare.rizzi.ParseClasses.Place;

import java.util.ArrayList;

import static com.rizzishare.rizzi.utils.PlaceUtils.autocomplete;

/**
 * Created by David on 9/13/2014.
 */
public class PlacesAutoCompleteAdapter extends ArrayAdapter<Place>
                                                implements Filterable {
    private ArrayList<Place> resultList;

    private Location mCenter = null;
    private int mRadius_km = 0;

    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Place getItem(int index) {
        return resultList.get(index);
    }

    /**
     * This is an optional method if location biasing is to be applied to the place search
     * @param center    center of the search radius
     * @param radius_km     search radius in km
     */
    public void setLocationBias(Location center, int radius_km){
        mCenter = center;
        mRadius_km = radius_km;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the auto-complete results.
                    resultList = autocomplete(constraint.toString(),mCenter,mRadius_km);

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }

}
