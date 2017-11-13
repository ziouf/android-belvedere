package fr.marin.cyril.belvedere.async;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import fr.marin.cyril.belvedere.annotations.JsonField;
import fr.marin.cyril.belvedere.datasource.WikiDataQueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;
import io.realm.RealmModel;

/**
 * Created by cyril on 09/11/17.
 */

public class DataGetterAsync extends AsyncTask<Void, Integer, Void> {
    private OnPostExecuteListener onPostExecuteListener;

    @Override
    protected void onPostExecute(Void v) {
        Log.i(this.getClass().getSimpleName() + ".onPostExecute()", "Processing finished");

        if (Objects.nonNull(this.onPostExecuteListener))
            this.onPostExecuteListener.onPostExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

    }

    @Override
    protected void onCancelled() {

    }

    @Override
    protected Void doInBackground(Void... voids) {
        final int pageSize = 1000;

        // Count results
        final int count = this.countInBackground(WikiDataQueryManager.WikiDataQueries.COUNT_ALL_MOUNTAINS.getQuery(0, 0));
        Log.i(this.getClass().getSimpleName() + ".doInBackground()", "Count : " + count + ", Pages : " + ((count / pageSize) + 1));

        // Run paged queries
        for (int page = 0; page <= (count / pageSize) + 1; ++page) {
            final int offset = page * pageSize;
            Log.i(this.getClass().getSimpleName(), "Page number : " + page);
            final String json = this.doInBackground(WikiDataQueryManager.WikiDataQueries.GET_ALL_MOUNTAINS.getQuery(offset, pageSize));
            parseAndSave(json);
        }
        return null;
    }

    private void parseAndSave(final String json) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(new Realm.Transaction() {
            final MyJsonParser<Placemark> parser = new MyJsonParser<>(Placemark.class);

            @Override
            public void execute(@NonNull Realm realm) {
                try {
                    parser.parseJsonResponse(json, realm);
                } catch (Exception e) {
                    Log.e(this.getClass().getSimpleName(), e.getClass().getSimpleName() + " : " + e.getMessage());
                    realm.cancelTransaction();
                }
            }
        });
    }

    private int countInBackground(String query) {
        try {
            final String result = this.doInBackground(query);

            final JSONObject jsonObject = new JSONObject(result);

            return jsonObject
                    .getJSONObject("results")
                    .getJSONArray("bindings")
                    .getJSONObject(0)
                    .getJSONObject("count")
                    .getInt("value");

        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName() + ".countInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        return 0;
    }

    private String doInBackground(String query) {
        try {
            final HttpURLConnection conn = this.getConnection(query);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return br.lines().collect(Collectors.joining());
            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName() + ".doInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage());
            try {
                Log.e(this.getClass().getSimpleName() + ".doInBackground()", "Wait 5s and retry");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                // Do nothing
            }
        }
        return this.doInBackground(query);
    }

    private HttpURLConnection getConnection(String query) throws Exception {

        final HttpsURLConnection conn = (HttpsURLConnection) new URL(WikiDataQueryManager.uri().toString()).openConnection();
        conn.setHostnameVerifier((hostname, session) -> true);

        conn.setConnectTimeout(50000);
        conn.setReadTimeout(50000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        try (Writer w = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
            w.write("query=" + query);
            w.flush();
        }
        conn.connect();

        Log.i(this.getClass().getSimpleName() + ".getConnection()", "response code : " + conn.getResponseCode());

        return conn;
    }

    public void setOnPostExecuteListener(OnPostExecuteListener onPostExecuteListener) {
        this.onPostExecuteListener = onPostExecuteListener;
    }

    public interface OnPostExecuteListener {
        void onPostExecute();
    }

    /**
     *
     */
    private static class MyJsonParser<T> {
        private static final JsonFactory jsonFactory = new JsonFactory();
        private final Class<T> clazz;
        private Collection<String> vars = new ArrayList<>();

        private MyJsonParser(Class<T> clazz) {
            this.clazz = clazz;
        }

        private void parseJsonArray(JsonParser jsonParser, Realm realm, JsonArrayParseFunction f) throws Exception {
            while (!jsonParser.nextToken().equals(JsonToken.END_ARRAY)) {
                f.parse(jsonParser, realm);
            }
        }

        private void parseJsonResponse(String json, Realm realm) throws Exception {
            final JsonParser jsonParser = jsonFactory.createParser(json);
            vars.clear();

            while (!jsonParser.isClosed()) {
                final JsonToken jsonToken = jsonParser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    final String fieldName = jsonParser.getCurrentName();

                    if ("head".equals(fieldName)) {
                        this.parseJsonHead(jsonParser, realm);
                    } else if ("results".equals(fieldName)) {
                        this.parseJsonResults(jsonParser, realm);
                    }
                }
            }
        }

        private void parseJsonHead(JsonParser jsonParser, Realm realm) throws Exception {
            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                if (JsonToken.FIELD_NAME.equals(jsonParser.getCurrentToken())) {
                    if ("vars".equals(jsonParser.getCurrentName())) {
                        this.parseJsonArray(jsonParser, realm, this::parseJsonVars);
                    }
                }
            }
        }

        private void parseJsonVars(JsonParser jsonParser, Realm realm) throws Exception {
            if (JsonToken.VALUE_STRING.equals(jsonParser.currentToken()))
                vars.add(jsonParser.getText());
        }

        private void parseJsonResults(JsonParser jsonParser, Realm realm) throws Exception {
            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                    if ("bindings".equals(jsonParser.getCurrentName())) {
                        this.parseJsonArray(jsonParser, realm, this::parseJsonBinding);
                    }
                }
            }
        }

        private void parseJsonBinding(JsonParser jsonParser, Realm realm) throws Exception {
            final T o = clazz.newInstance();
            final Field[] fields = clazz.getDeclaredFields();

            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                    final String curName = jsonParser.getCurrentName();
                    final String curValue = this.parseJsonValue(jsonParser);
                    for (Field f : fields) {
                        if (f.isAnnotationPresent(JsonField.class)) {
                            final JsonField a = f.getAnnotation(JsonField.class);
                            if (a.value().equalsIgnoreCase(curName)) {
                                if (f.getType().equals(String.class)) {
                                    f.setAccessible(true);
                                    f.set(o, curValue);
                                    f.setAccessible(false);
                                } else if (f.getType().equals(Double.class)) {
                                    f.setAccessible(true);
                                    f.set(o, curValue.isEmpty() ? 0d : Double.parseDouble(curValue));
                                    f.setAccessible(false);
                                }
                            }
                        }
                    }
                }
            }

            if (RealmModel.class.isAssignableFrom(clazz))
                realm.insertOrUpdate((RealmModel) o);
        }

        private String parseJsonValue(JsonParser jsonParser) throws Exception {
            String value = "";
            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                    if ("value".equals(jsonParser.getCurrentName())) {
                        value = jsonParser.nextTextValue();
                    }
                }
            }
            return value;
        }

        private interface JsonArrayParseFunction {
            void parse(JsonParser jsonParser, Realm realm) throws Exception;
        }

    }
}
