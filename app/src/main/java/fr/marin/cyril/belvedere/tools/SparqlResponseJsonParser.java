package fr.marin.cyril.belvedere.tools;

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
//    private final Collection<String> vars = new ArrayList<>();

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
//        vars.clear();

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

    private void parseJsonArray(JsonParser jsonParser, Realm realm, SparqlResponseJsonParser.JsonArrayParseFunction f) throws Exception {
        while (!jsonParser.nextToken().equals(JsonToken.END_ARRAY)) {
            f.parse(jsonParser, realm);
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
        // Do nothing
//        if (JsonToken.VALUE_STRING.equals(jsonParser.currentToken()))
//            vars.add(jsonParser.getText());
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

        while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
            if (JsonToken.FIELD_NAME.equals(jsonParser.currentToken())) {
                final String curName = jsonParser.getCurrentName();
                final String curValue = this.parseJsonValue(jsonParser);
                for (Field f : fieldMap.keySet()) {
                    if (fieldMap.containsKey(f) && fieldMap.get(f).equalsIgnoreCase(curName)) {
                        if (f.getType().equals(String.class)) {
                            f.set(o, curValue);
                        } else if (f.getType().equals(Double.class)) {
                            f.set(o, curValue.isEmpty() ? 0d : Double.parseDouble(curValue));
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