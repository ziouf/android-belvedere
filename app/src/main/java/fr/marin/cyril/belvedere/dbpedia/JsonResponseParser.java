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
import fr.marin.cyril.belvedere.model.PlacemarkType;

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
public class JsonResponseParser<T> {
    private static final String TAG = "DbPediaJsonResponsePars";

    public <T extends Placemark> List<T> readJsonStream(InputStream is, PlacemarkType type, Class<T> clazz) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {
            return readSparQLResponse(reader, type, clazz);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public <T extends Placemark> List<T> readJsonString(String json, PlacemarkType type, Class<T> clazz) {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            return readSparQLResponse(reader, type, clazz);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private <T extends Placemark> List<T> readSparQLResponse(JsonReader reader, PlacemarkType type, Class<T> clazz) throws IOException {
        List<T> results = null;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("results")) {
                results = readResults(reader, type, clazz);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        return results;
    }

    private <T extends Placemark> List<T> readResults(JsonReader reader, PlacemarkType type, Class<T> clazz) throws IOException {
        List<T> results = null;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("bindings")) {
                results = readBindingsArray(reader, type, clazz);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        return results;
    }

    private <T extends Placemark> List<T> readBindingsArray(JsonReader reader, PlacemarkType type, Class<T> clazz) throws IOException {
        List<T> results = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            results.add(readBinding(reader, type, clazz));
        }
        reader.endArray();

        return results;
    }

    private <T extends Placemark> T readBinding(JsonReader reader, PlacemarkType type, Class<T> clazz) throws IOException {
        int id = 0;
        String name = null;
        double latitude = 0d;
        double longitude = 0d;
        double elevation = 0d;
        String comment = null;
        String wiki_uri = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "id":
                    id = readIntegerValue(reader);
                    break;
                case "elevation":
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
                case "wiki_url":
                    wiki_uri = readStringValue(reader);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        try {
            T item = clazz.newInstance();
            item.setId(id);
            item.setTitle(name);
            item.setComment(comment);
            item.setLatitude(latitude);
            item.setLongitude(longitude);
            item.setElevation(elevation);
            item.setWiki_uri(wiki_uri);
            item.setType(type);

            return item;
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(JsonResponseParser.class.getSimpleName(), "Exception lors de l'instenciation de l'item parsé", e);
        }

        return null;
    }

    private int readIntegerValue(JsonReader reader) throws IOException {
        int value = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("value")) {
                value = reader.nextInt();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return value;
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
