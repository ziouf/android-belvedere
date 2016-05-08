package fr.marin.cyril.belvedere.sparql.parser;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import fr.marin.cyril.belvedere.sparql.model.PeakInfo;

/**
 * Created by cyril on 08/05/16.
 *
 * see: http://developer.android.com/reference/android/util/JsonReader.html
 *
 * {
     head: {
         link: [ ],
         vars: [
             "altitude"
             ]
     },
     results: {
     distinct: false,
     ordered: true,
     bindings: [
            {
            altitude: {
                 type: "typed-literal",
                 datatype: "http://www.w3.org/2001/XMLSchema#integer",
                 value: "2884"
                 }
             }
         ]
     }
 * }
 *
 */
public class JsonResponseParser {

    public List<PeakInfo> readJsonStream(InputStream is) {

        try (JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {

            return readSparQLResponse(reader);

        } catch (IOException e) {

        }
        return null;
    }

    private List<PeakInfo> readSparQLResponse(JsonReader reader) throws IOException {
        List<PeakInfo> results = null;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("bindings")) {
                results = readBindingsArray(reader);
            }
        }

        reader.endObject();

        return results;
    }

    private List<PeakInfo> readBindingsArray(JsonReader reader) throws IOException {
        List<PeakInfo> results = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            results.add(readBinding(reader));
        }
        reader.endArray();

        return results;
    }

    private PeakInfo readBinding(JsonReader reader) throws IOException {
        double altitude = 0f;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("value")) {
                altitude = reader.nextDouble();
            }
        }
        reader.endObject();

        return new PeakInfo(null, null, altitude);
    }

}
