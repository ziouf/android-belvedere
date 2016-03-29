package fr.marin.cyril.mapsapp.kml;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import fr.marin.cyril.mapsapp.model.MapsMarker;

/**
 * Created by cscm6014 on 29/03/2016.
 */
public class KmlReader {

    private Set<MapsMarker> mapsMarkers;
    private Set<InputStream> kmlfiles;

    public KmlReader(Set<InputStream> kmlfiles) {
        this.kmlfiles = kmlfiles;
        this.mapsMarkers = new HashSet<>();
    }

    public List<MapsMarker> getMapsMarkers() {
        return new ArrayList<>(mapsMarkers);
    }

    public KmlReader readKmlFiles() {
        for (InputStream is : this.kmlfiles) {
            this.readKmlFile(is);
        }
        return this;
    }

    public void readKmlFile(InputStream is) {
        try {
            SAXParserFactory saxPF = SAXParserFactory.newInstance();
            SAXParser saxP = saxPF.newSAXParser();
            XMLReader xmlR = saxP.getXMLReader();

            KmlHandler kmlHandler = new KmlHandler();
            xmlR.setContentHandler(kmlHandler);
            xmlR.parse(new InputSource(is));

            mapsMarkers.addAll(kmlHandler.getMapsMarkers());

        } catch (Exception ignore) {
            // Do nothing
        }
    }
}
