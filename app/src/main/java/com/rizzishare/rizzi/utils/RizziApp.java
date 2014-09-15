package com.rizzishare.rizzi.utils;

import android.app.Application;
import android.content.Context;

import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.rizzishare.rizzi.parseclass.Place;
import com.rizzishare.rizzi.R;

/**
 * Created by David on 9/7/2014.
 */
public class RizziApp extends Application{

    public static final String APPTAG = "Rizzi App";

    public static final String SHARED_PREF = "com.rizzi.rizzi.shared_preference";

    public static final String APP_PACKAGE_PREFIX = "com.rizzi.rizzi.shared_preference";

    public static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(Place.class);

        // Required - Initialize the Parse SDK
        Parse.initialize(this, getString(R.string.parse_app_id),
                getString(R.string.parse_client_key));

        //Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Optional - If you don't want to allow Facebook login, you can
        // remove this line (and other related ParseFacebookUtils calls)
        ParseFacebookUtils.initialize(getString(R.string.facebook_app_id));

        // Optional - If you don't want to allow Twitter login, you can
        // remove this line (and other related ParseTwitterUtils calls)
        //ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key),
        //       getString(R.string.twitter_consumer_secret));

        appContext = getApplicationContext();
    }
}
