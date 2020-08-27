package au.gov.amsa.geo;

import au.gov.amsa.gt.Shapefile;

public class Eez {
    
    public static Shapefile loadEezLine() {
        // good for crossing checks
        return Shapefile.fromZip(Eez.class.getResourceAsStream("/eez_aust_mainland_line.zip"));
    }

    public static Shapefile loadEezPolygon() {
        // good for contains checks
        return Shapefile.fromZip(Eez.class.getResourceAsStream("/eez_aust_mainland_pl.zip"));
    }


}
