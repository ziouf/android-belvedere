package fr.marin.cyril.mapsapp.kml.parser;

import android.util.Xml;

import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.kml.model.Coordinates;
import fr.marin.cyril.mapsapp.kml.model.MapsMarker;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class KmlParser {

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

    private Collection<MapsMarker> markers = new HashSet<>();

    public Collection<MapsMarker> parseAll(Collection<InputStream> inputStreams) {

        try {
            for (InputStream in : inputStreams)
                parse(in);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return markers;
    }

    public void parse(InputStream in) throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            parser.setInput(in, null);

            parser.nextTag();

            readFeed(parser);

        } finally {
            in.close();
        }
    }

    private void readFeed(XmlPullParser parser)
            throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, START_TAG);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equals(DOCUMENT)) {
                readDocument(parser);
            } else {
                skip(parser);
            }

        }
    }

    private void readDocument(XmlPullParser parser)
            throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, DOCUMENT);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equalsIgnoreCase(PLACEMARK)) {
                markers.add(readPlacemark(parser));
            } else {
                skip(parser);
            }

        }
    }

    private MapsMarker readPlacemark(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, PLACEMARK);

        MapsMarker mapsMarker = new MapsMarker();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equalsIgnoreCase(NAME)) {
                parser.require(XmlPullParser.START_TAG, ns, NAME);
                mapsMarker.setTitle(readText(parser));
                parser.require(XmlPullParser.END_TAG, ns, NAME);

            } else if (name.equalsIgnoreCase(POINT)) {
                mapsMarker.setCoordinates(readCoordinates(parser));

            } else if (name.equalsIgnoreCase(DESCRIPTION)) {
                parser.require(XmlPullParser.START_TAG, ns, DESCRIPTION);
                mapsMarker.setDescription(readText(parser));;
                parser.require(XmlPullParser.END_TAG, ns, DESCRIPTION);
            } else {
                skip(parser);
            }
        }

        return mapsMarker;
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
