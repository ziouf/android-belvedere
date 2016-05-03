package fr.marin.cyril.belvedere.kml.parser;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.belvedere.kml.model.Coordinates;
import fr.marin.cyril.belvedere.kml.model.Placemark;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class KmlParser {
    private static final String TAG = "KmlParser";
    // On n'utilise pas les namespaces
    private static final String ns = null;
    // Tags KML
    private static final String START_TAG = "kml";
    private static final String DOCUMENT = "Document";
    private static final String PLACEMARK = "Placemark";
    private static final String NAME = "name";
    private static final String POINT = "Point";
    private static final String DESCRIPTION = "description";
    private static final String COORDINATES = "coordinates";

    private final Context context;

    public KmlParser(Context context) {
        this.context = context;
    }

    public Collection<Placemark> parse(int id) {
        try (InputStream is = context.getResources().openRawResource(id)) {
            return this.parse(is);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public Collection<Placemark> parse(InputStream in) throws IOException {
        Collection<Placemark> buffer = new HashSet<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            parser.setInput(in, null);

            parser.nextTag();

            readFeed(parser, buffer);

        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage());
        }
        return buffer;
    }

    private void readFeed(XmlPullParser parser, Collection<Placemark> buffer)
            throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, START_TAG);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equals(DOCUMENT)) {
                readDocument(parser, buffer);
            } else {
                skip(parser);
            }
        }

        Log.i(TAG, String.format("Importation de %s Placemark en base", buffer.size()));
    }

    private void readDocument(XmlPullParser parser, Collection<Placemark> buffer)
            throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, DOCUMENT);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equalsIgnoreCase(PLACEMARK)) {
                buffer.add(readPlacemark(parser));
            } else {
                skip(parser);
            }

        }
    }

    private Placemark readPlacemark(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, PLACEMARK);

        Placemark placemark = new Placemark();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equalsIgnoreCase(NAME)) {
                parser.require(XmlPullParser.START_TAG, ns, NAME);
                placemark.setTitle(readText(parser));
                parser.require(XmlPullParser.END_TAG, ns, NAME);

            } else if (name.equalsIgnoreCase(POINT)) {
                placemark.setCoordinates(readCoordinates(parser));

            } else if (name.equalsIgnoreCase(DESCRIPTION)) {
                parser.require(XmlPullParser.START_TAG, ns, DESCRIPTION);
                String description = readText(parser);
                String url = description.substring(description.indexOf("http"), description.indexOf("\">"));
                placemark.setUrl(url);

                parser.require(XmlPullParser.END_TAG, ns, DESCRIPTION);
            } else {
                skip(parser);
            }
        }

        return placemark;
    }

    private Coordinates readCoordinates(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, POINT);

        Coordinates coordinates = new Coordinates();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equalsIgnoreCase(COORDINATES)) {
                parser.require(XmlPullParser.START_TAG, ns, COORDINATES);
                String[] c = readText(parser).split(",");
                parser.require(XmlPullParser.END_TAG, ns, COORDINATES);

                coordinates.setLatLng(new LatLng(Double.parseDouble(c[1]), Double.parseDouble(c[0])));
                coordinates.setElevation(Double.parseDouble(c[2]));
            } else {
                skip(parser);
            }
        }
        return coordinates;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}