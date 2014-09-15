package com.rizzishare.rizzi.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by David on 9/14/2014.
 */
public abstract class CallRestApiInBackground extends AsyncTask<String, Integer, JSONObject>{
    private static final String LOG_TAG = "CallRestApiInBackground";

    @Override
    protected JSONObject doInBackground(String... strings) {
        String url_str = strings[0];

        url_str = overWriteUrl(url_str);

        HttpURLConnection conn = null;
        StringBuilder response = new StringBuilder();
        JSONObject jsonResult = null;
        try {
            URL url = new URL(url_str);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            int len = conn.getContentLength();
            int progress = 0;
            int progress_pct = 0;
            double progress_pct_incr = 0.02 * 100;

            // Load the results into a StringBuilder
            int read;
            char[] buff = null;
            if (len > 0) {
                buff = new char[len * 2];
            }else{
                // sometimes, len may not be known
                buff = new char[1024 * 2];
            }
            while ((read = in.read(buff)) != -1) {
                response.append(buff, 0, read);
                if (len > 0) {
                    // update progress if necessary
                    progress++;
                    if (progress % (0.02 * len) == 0) {
                        progress_pct++;
                        publishProgress(progress_pct * (int) progress_pct_incr);
                    }
                }
            }
            response.trimToSize();
            jsonResult = new JSONObject(response.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return null;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error Parsing Http response into JSON Object", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return jsonResult;
    }

    /**
     * This method allow a subclass to change the URL to which a HTTP call will be made,
     * the default implementaion simply returns the oldUrl without make changes to it
     * @param oldUrl
     * @return a new URL either based on the oldUrl or a completely new url
     */
    protected String overWriteUrl(String oldUrl) {
        return oldUrl;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected abstract void onPostExecute(JSONObject result);
}
