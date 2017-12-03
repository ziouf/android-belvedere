package fr.marin.cyril.belvedere.tools;

import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmModel;

/**
 * Created by cyril on 15/11/17.
 */

public class SparqlResponseJsonParser<T> {
    private static final String TAG = SparqlResponseJsonParser.class.getSimpleName();
    private static final JsonFactory jsonFactory = new JsonFactory();

    private final Class<T> clazz;
    private final Map<Field, String> fieldMap;

    public SparqlResponseJsonParser(Class<T> clazz) {
        this.clazz = clazz;
        this.fieldMap = Stream.of(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(JsonAlias.class))
                .peek(f -> f.setAccessible(true))
                .collect(Collectors.toMap(f -> f, f -> f.getAnnotation(JsonAlias.class).value()[0]));
    }

    public void parseJsonResponse(InputStream json, Realm realm) throws Exception {
        final JsonParser jsonParser = jsonFactory.createParser(json);

        while (!jsonParser.isClosed()) {
            final JsonToken jsonToken = jsonParser.nextToken();

            if (Objects.equals(JsonToken.FIELD_NAME, jsonToken)) {
                if (Objects.equals("head", jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    jsonParser.skipChildren();
                } else if (Objects.equals("results", jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    this.parseJsonResults(jsonParser, realm);
                }
            }
        }
    }

    private void parseJsonArray(JsonParser jsonParser, Realm realm, SparqlResponseJsonParser.JsonArrayParseFunction f) throws Exception {
        while (!Objects.equals(JsonToken.END_ARRAY, jsonParser.nextToken())) {
            f.parse(jsonParser, realm);
        }
    }

    private void parseJsonResults(JsonParser jsonParser, Realm realm) throws Exception {
        while (!Objects.equals(JsonToken.END_OBJECT, jsonParser.nextToken())) {
            if (Objects.equals(JsonToken.FIELD_NAME, jsonParser.currentToken())) {
                if (Objects.equals("bindings", jsonParser.getCurrentName())) {
                    Log.v(TAG, jsonParser.getCurrentName());
                    if (Objects.equals(JsonToken.START_ARRAY, jsonParser.nextToken()))
                        this.parseJsonArray(jsonParser, realm, this::parseJsonBinding);
                }
            }
        }
    }

    private void parseJsonBinding(JsonParser jsonParser, Realm realm) throws Exception {
        final T o = clazz.newInstance();

        while (!Objects.equals(JsonToken.END_OBJECT, jsonParser.nextToken())) {
            if (Objects.equals(JsonToken.FIELD_NAME, jsonParser.currentToken())) {
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
        }

        if (RealmModel.class.isAssignableFrom(clazz))
            realm.insertOrUpdate((RealmModel) o);
    }

    private String parseJsonValue(JsonParser jsonParser) throws Exception {
        String value = "";
        while (!Objects.equals(JsonToken.END_OBJECT, jsonParser.nextToken())) {
            if (Objects.equals(JsonToken.FIELD_NAME, jsonParser.currentToken())) {
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