package fr.marin.cyril.mapsapp.kml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
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

    SAXParserFactory saxPF;
    SAXParser saxP;
    XMLReader xmlR;

    public KmlReader(Set<InputStream> kmlfiles) {
        this.kmlfiles = kmlfiles;
        this.mapsMarkers = new HashSet<>();

        try {
            this.saxPF = SAXParserFactory.newInstance();
            this.saxP = saxPF.newSAXParser();
            this.xmlR = saxP.getXMLReader();
        } catch (Exception ignore) {
            // Do nothing
        }
    }

    public List<MapsMarker> readKmlFiles() {
        KmlHandler kmlHandler = new KmlHandler();
        xmlR.setContentHandler(kmlHandler);

        for (InputStream is : this.kmlfiles) {
            try {
                xmlR.parse(new InputSource(is));

                mapsMarkers.addAll(kmlHandler.getMapsMarkers());

            } catch (SAXException ignore) {
                // Do nothing
            } catch (IOException ignore) {
                // Do nothing
            }
        }

        return new ArrayList<>(mapsMarkers);
    }
}
