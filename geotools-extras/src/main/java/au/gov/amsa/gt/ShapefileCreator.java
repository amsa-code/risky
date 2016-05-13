package au.gov.amsa.gt;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

final class ShapefileCreator {

    public static void createPolygon(List<Coordinate> coords, File output) {
        List<SimpleFeature> features = new ArrayList<>();

        final SimpleFeatureType type = createFeatureType();
        // final SimpleFeatureType type = BasicFeatureTypes.POLYGON;
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder f = new SimpleFeatureBuilder(type);

        f.add(gf.createPolygon(coords.toArray(new Coordinate[] {})));
        SimpleFeature feature = f.buildFeature(null);
        features.add(feature);
        saveFeaturesToShapefile(features, type, output);
    }

    private static SimpleFeatureType createFeatureType() {
        try {
            final SimpleFeatureType type = DataUtilities.createType("Region",
                    "the_geom:Polygon:srid=4326");
            return type;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveFeaturesToShapefile(List<SimpleFeature> features,
            final SimpleFeatureType type, File output) {
        try {
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", output.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
                    .createNewDataStore(params);

            // type is used as a template to describe the file contents
            newDataStore.createSchema(type);

            // Write the features to the shapefile
            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            // SimpleFeatureType shapeType = featureSource.getSchema();
            /*
             * The Shapefile format has a couple limitations: - "the_geom" is
             * always first, and used for the geometry attribute name -
             * "the_geom" must be of type Point, MultiPoint, MuiltiLineString,
             * MultiPolygon - Attribute names are limited in length - Not all
             * data types are supported (example Timestamp represented as Date)
             * 
             * Each data store has different limitations so check the resulting
             * SimpleFeatureType.
             */
            // System.out.println("SHAPE:" + shapeType);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                /*
                 * SimpleFeatureStore has a method to add features from a
                 * SimpleFeatureCollection object, so we use the
                 * ListFeatureCollection class to wrap our list of features.
                 */
                SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                throw new RuntimeException(typeName + " does not support read/write access");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
