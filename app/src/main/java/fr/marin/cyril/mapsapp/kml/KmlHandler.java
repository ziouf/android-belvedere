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
    private static final String DESCRIPTION = "description";
    private static final String COORDINATES = "coordinates";

    private Boolean inElement = false;
    private Boolean inPlacemark = false;
    private Boolean inName = false;
    private Boolean inDescription = false;
    private Boolean inCoordinates = false;

    private String elementValue;
    private MapsMarker data;
    private Set<MapsMarker> mapsMarkers = new HashSet<>();

    public Set<MapsMarker> getMapsMarkers() {
        return mapsMarkers;
    }

    public void setMapsMarkers(Set<MapsMarker> mapsMarkers) {
        this.mapsMarkers = mapsMarkers;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        this.inElement = true;
        this.elementValue = "";
        if (localName.equalsIgnoreCase(PLACEMARK)) {
            this.inPlacemark = true;
            data = new MapsMarker();
        }
        else if (localName.equalsIgnoreCase(NAME) && inPlacemark) {
            this.inName = true;
        }
        else if (localName.equalsIgnoreCase(DESCRIPTION) && inPlacemark) {
            this.inDescription = true;
        }
        else if (localName.equalsIgnoreCase(COORDINATES) && inPlacemark) {
            this.inCoordinates = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        this.inElement = false;
        if (localName.equalsIgnoreCase(PLACEMARK)) {
            this.inPlacemark = false;
            this.mapsMarkers.add(data);
        }
        else if (localName.equalsIgnoreCase(NAME) && inName) {
            this.inName = false;
            this.data.setTitle(elementValue);
        }
        else if (localName.equalsIgnoreCase(DESCRIPTION) && inDescription) {
            this.inDescription = false;
            this.data.setDescription(elementValue);
        }
        else if (localName.equalsIgnoreCase(COORDINATES) && inCoordinates) {
            this.inCoordinates = false;
            this.data.setLatLng(this.getLatLngFromKmlCoordinates(elementValue));
            this.data.setElevation(this.getElevationFromKmlCoordinates(elementValue));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.inElement) elementValue = new String(ch, start, length);
    }

    private LatLng getLatLngFromKmlCoordinates(String s) {
        String[] coordinates = s.split(",");
        return new LatLng(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
    }
    private Integer getElevationFromKmlCoordinates(String s) {
        String[] coordinates = s.split(",");
        return Double.valueOf(coordinates[2]).intValue();
    }
}
