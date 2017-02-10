package fr.marin.cyril.belvedere.tools;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by cscm6014 on 10/02/2017.
 */

public final class WikiUrlGetterAsync extends AsyncTask<Placemark, Void, String> {
    private final String lang = Locale.getDefault().getLanguage();
    private OnPostExecuteListener onPostExecuteListener;

    public static WikiUrlGetterAsync getInstance(OnPostExecuteListener onPostExecuteListener) {
        WikiUrlGetterAsync async = new WikiUrlGetterAsync();
        async.onPostExecuteListener = onPostExecuteListener;
        return async;
    }

    public static String getLangUrlFromResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject pages = json.getJSONObject("query").getJSONObject("pages");
            return pages.getJSONObject(pages.keys().next())
                    .getJSONArray("langlinks")
                    .getJSONObject(0)
                    .getString("url");
        } catch (JSONException e) {
            // Do nothing
        }
        return null;
    }

    @Override
    protected String doInBackground(Placemark... placemarks) {
        for (Placemark p : placemarks)
            try {
                return this.doInBackground(p);
            } catch (IOException e) {
                // Do nothing
            }
        return null;
    }

    private String doInBackground(Placemark placemark) throws IOException {
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("en.wikipedia.org")
                .appendPath("w")
                .appendPath("api.php")
                .appendQueryParameter("action", "query")
                .appendQueryParameter("format", "json")
                .appendQueryParameter("prop", "langlinks")
                .appendQueryParameter("llprop", "url")
                .appendQueryParameter("lllang", lang)
                .appendQueryParameter("pageids", Integer.toString(placemark.getId()))
                .build();

        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setInstanceFollowRedirects(true);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            final StringBuilder sb = new StringBuilder();
            String buffer;
            while ((buffer = reader.readLine()) != null)
                sb.append(buffer);

            return sb.toString();
        } finally {
            connection.disconnect();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        this.onPostExecuteListener.onPostExecute(s);
    }

    public interface OnPostExecuteListener {
        void onPostExecute(String s);
    }
}
