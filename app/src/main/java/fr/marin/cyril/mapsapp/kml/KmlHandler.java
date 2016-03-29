package fr.marin.cyril.mapsapp.kml;

import com.google.android.gms.maps.model.LatLng;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashSet;
import java.util.Set;

import fr.marin.cyril.mapsapp.model.MapsMarker;

/**
 * Created by cscm6014 on 29/03/2016.
 */
class KmlHandler extends DefaultHandler {
    private static final String PLACEMARK = "Placemark";
    private static final String NAME = "name";
    private static final String POINT = "Point";
    private static final String DESCRIPTION = "description";
    private static final String COORDINATES = "coordinates";

    private Boolean inElement = false;
    private Boolean inPlacemark = false;

    private String elementValue;
    private MapsMarker data;
    private Set<MapsMarker> mapsMarkers = new HashSet<>();

    public Set<MapsMarker> getMapsMarkers() {
        return mapsMarkers;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        this.inElement = true;
        if (localName.equalsIgnoreCase(PLACEMARK)) {
            data = new MapsMarker();
            this.inPlacemark = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        this.inElement = false;
        if (localName.equalsIgnoreCase(PLACEMARK)) {
            this.mapsMarkers.add(data);
            this.inPlacemark = false;
        }
        if (localName.equalsIgnoreCase(NAME) && inPlacemark) {
            this.data.setTitle(elementValue);
        }
        if (localName.equalsIgnoreCase(DESCRIPTION) && inPlacemark) {
            this.data.setDescription(elementValue);
        }
        if (localName.equalsIgnoreCase(COORDINATES) && inPlacemark) {
            this.data.setLatLng(this.getLatLngFromKmlCoordinates(elementValue));
            this.data.setElevation(this.getElevationFromKmlCoordinates(elementValue));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.inElement)
            elementValue = new String(ch, start, length);
    }

    private LatLng getLatLngFromKmlCoordinates(String s) {
        String[] coordinates = s.split(",");
        if (coordinates.length < 3)
            System.out.println();
        return new LatLng(Double.valueOf(coordinates[1]), Double.valueOf(coordinates[0]));
    }
    private Integer getElevationFromKmlCoordinates(String s) {
        String[] coordinates = s.split(",");
        if (coordinates.length < 3)
            System.out.println();
        return Integer.valueOf(coordinates[2]);
    }
}
