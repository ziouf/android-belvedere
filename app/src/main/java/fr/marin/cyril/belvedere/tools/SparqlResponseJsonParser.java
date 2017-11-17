package fr.marin.cyril.belvedere.tools;

import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import fr.marin.cyril.belvedere.annotations.JsonField;
import io.realm.Realm;
import io.realm.RealmModel;

/**
 * Created by cyril on 15/11/17.
 */

public class SparqlResponseJsonParser<T> {
    private static final String TAG = SparqlResponseJsonParser.class.getSimpleName();

    private static final JsonFactory jsonFactory = new JsonFactory();

    private final Class<T> clazz;
    private final Map<Field, String> fieldMap = new HashMap<>();

    public SparqlResponseJsonParser(Class<T> clazz) {
        this.clazz = clazz;

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(JsonField.class)) {
                f.setAccessible(true);
                final JsonField a = f.getAnnotation(JsonField.class);
                fieldMap.put(f, a.value());
            }
        }
    }

    public void parseJsonResponse(InputStream json, Realm realm) throws Exception {
        final JsonParser jsonParser = jsonFactory.createParser(json);

        while (!jsonParser.isClosed()) {
            final JsonToken jsonToken = jsonParser.nextToken();

            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                if ("head".equals(jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    jsonParser.skipChildren();
                } else if ("results".equals(jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    this.parseJsonResults(jsonParser, realm);
                }
            }
        }
    }

    private void parseJsonArray(JsonParser jsonParser, Realm realm, SparqlResponseJsonParser.JsonArrayParseFunction f) throws Exception {
        while (!JsonToken.END_ARRAY.equals(jsonParser.nextToken())) {
            f.parse(jsonParser, realm);
        }
    }

    private void parseJsonResults(JsonParser jsonParser, Realm realm) throws Exception {
        while (!JsonToken.END_OBJECT.equals(jsonParser.nextToken())) {
            if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                if ("bindings".equals(jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    if (JsonToken.START_ARRAY.equals(jsonParser.nextToken()))
                        this.parseJsonArray(jsonParser, realm, this::parseJsonBinding);
                }
            }
        }
    }

    private void parseJsonBinding(JsonParser jsonParser, Realm realm) throws Exception {
        final T o = clazz.newInstance();

        do {
            if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                final String curName = jsonParser.getCurrentName();
                final String curValue = this.parseJsonValue(jsonParser);

                for (Field f : fieldMap.keySet()) {
                    if (fieldMap.containsKey(f) && fieldMap.get(f).equalsIgnoreCase(curName)) {
                        Log.v(TAG, curName + " : " + curValue);
                        if (f.getType().equals(String.class)) {
                            f.set(o, curValue);
                        } else if (f.getType().equals(Integer.class)) {
                            f.set(o, curValue.isEmpty() ? 0 : Integer.parseInt(curValue));
                        } else if (f.getType().equals(Long.class)) {
                            f.set(o, curValue.isEmpty() ? 0L : Long.parseLong(curValue));
                        } else if (f.getType().equals(Float.class)) {
                            f.set(o, curValue.isEmpty() ? 0f : Float.parseFloat(curValue));
                        } else if (f.getType().equals(Double.class)) {
                            f.set(o, curValue.isEmpty() ? 0d : Double.parseDouble(curValue));
                        } else if (f.getType().equals(Byte.class)) {
                            f.set(o, curValue.isEmpty() ? null : Byte.decode(curValue));
                        }
                    }
                }
            }

        } while (!JsonToken.END_OBJECT.equals(jsonParser.nextToken()));

        if (RealmModel.class.isAssignableFrom(clazz))
            realm.insertOrUpdate((RealmModel) o);
    }

    private String parseJsonValue(JsonParser jsonParser) throws Exception {
        String value = "";
        while (!JsonToken.END_OBJECT.equals(jsonParser.nextToken())) {
            if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                if ("value".equals(jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
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