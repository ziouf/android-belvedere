package fr.marin.cyril.belvedere.async;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import fr.marin.cyril.belvedere.Belvedere;
import fr.marin.cyril.belvedere.async.interfaces.OnPostExecuteListener;
import fr.marin.cyril.belvedere.model.Placemark;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by cscm6014 on 10/02/2017.
 */

public final class WikiUrlGetterAsync extends AsyncTask<Placemark, Void, String> {
    private static final String TAG = WikiUrlGetterAsync.class.getSimpleName();
    private final String lang = Locale.getDefault().getLanguage();
    private OnPostExecuteListener<String> onPostExecuteListener;

    public static WikiUrlGetterAsync getInstance(OnPostExecuteListener<String> onPostExecuteListener) {
        WikiUrlGetterAsync async = new WikiUrlGetterAsync();
        async.onPostExecuteListener = onPostExecuteListener;
        return async;
    }

    public static String getLangUrlFromResponse(String response) {
        if (response == null || response.isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(response);
            JSONObject pages = json.getJSONObject("query").getJSONObject("pages");
            return pages.getJSONObject(pages.keys().next())
                    .getJSONArray("langlinks")
                    .getJSONObject(0)
                    .getString("url");

        } catch (JSONException e) {
            // Do nothing
            Log.e(TAG + ".getLangUrlFromResponse()", e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        return null;
    }

    public void setOnPostExecuteListener(OnPostExecuteListener<String> onPostExecuteListener) {
        this.onPostExecuteListener = onPostExecuteListener;
    }

    @Override
    protected String doInBackground(Placemark... placemarks) {
        for (Placemark p : placemarks)
            try {
                return this.doInBackground(p);
            } catch (IOException e) {
                // Do nothing
                Log.e(TAG + ".doInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        return null;
    }

    private String doInBackground(Placemark placemark) throws IOException {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.mediawiki.org")
                .addPathSegment("w")
                .addPathSegment("api.php")
                .addQueryParameter("action", "query")
                .addQueryParameter("format", "json")
                .addQueryParameter("prop", "langlinks")
                .addQueryParameter("llprop", "url")
                .addQueryParameter("lllang", lang)
                .addQueryParameter("pageids", placemark.getId().replaceAll(".*/", ""))
                .build();

        final Request request = new Request.Builder().url(url).get().build();

        try (final Response response = Belvedere.getHttpClient().newCall(request).execute()) {
            return response.body().string();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        this.onPostExecuteListener.onPostExecute(s);
    }

}
