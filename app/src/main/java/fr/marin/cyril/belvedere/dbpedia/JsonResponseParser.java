package fr.marin.cyril.belvedere.dbpedia;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by cyril on 08/05/16.
 * <p>
 * see: http://developer.android.com/reference/android/util/JsonReader.html
 * <p>
 * {
 * head: {
 * link: [ ],
 * vars: [
 * "altitude"
 * ]
 * },
 * results: {
 * distinct: false,
 * ordered: true,
 * bindings: [
 * {
 * altitude: {
 * type: "typed-literal",
 * datatype: "http://www.w3.org/2001/XMLSchema#integer",
 * value: "2884"
 * }
 * }
 * ]
 * }
 * }
 */
public class JsonResponseParser {
    private static final String TAG = "DbPediaJsonResponsePars";

    public List<Placemark> readJsonStream(InputStream is) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {
            return readSparQLResponse(reader);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public List<Placemark> readJsonString(String json) {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            return readSparQLResponse(reader);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private List<Placemark> readSparQLResponse(JsonReader reader) throws IOException {
        List<Placemark> results = null;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("results")) {
                results = readResults(reader);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        return results;
    }

    private List<Placemark> readResults(JsonReader reader) throws IOException {
        List<Placemark> results = null;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("bindings")) {
                results = readBindingsArray(reader);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        return results;
    }

    private List<Placemark> readBindingsArray(JsonReader reader) throws IOException {
        List<Placemark> results = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            results.add(readBinding(reader));
        }
        reader.endArray();

        return results;
    }

    private Placemark readBinding(JsonReader reader) throws IOException {
        String name = null;
        double latitude = 0d;
        double longitude = 0d;
        double elevation = 0d;
        String comment = null;
        String thumbnail_uri = null;
        String wiki_uri = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "altitude":
                    elevation = readDoubleValue(reader);
                    break;
                case "latitude":
                    latitude = readDoubleValue(reader);
                    break;
                case "longitude":
                    longitude = readDoubleValue(reader);
                    break;
                case "nom":
                    name = readStringValue(reader);
                    break;
                case "comment":
                    comment = readStringValue(reader);
                    break;
                case "thumbnail":
                    thumbnail_uri = readStringValue(reader);
                    break;
                case "wiki":
                    wiki_uri = readStringValue(reader);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        return new Placemark(name, comment, latitude, longitude, elevation, wiki_uri, thumbnail_uri);
    }

    private String readStringValue(JsonReader reader) throws IOException {
        String value = null;
        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("value")) {
                value = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return value;
    }

    private double readDoubleValue(JsonReader reader) throws IOException {
        double value = 0d;
        reader.beginObject();
        while (reader.hasNext()) {
            try {
                if (reader.nextName().equals("value")) {
                    value = reader.nextDouble();
                } else {
                    reader.skipValue();
                }
            } catch (NumberFormatException ignore) {
                reader.skipValue();
            }
        }
        reader.endObject();
        return value;
    }

}
