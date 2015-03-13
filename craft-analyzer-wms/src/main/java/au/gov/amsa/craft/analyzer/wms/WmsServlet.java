package au.gov.amsa.craft.analyzer.wms;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.davidmoten.grumpy.wms.Capabilities;
import com.github.davidmoten.grumpy.wms.Layer;
import com.github.davidmoten.grumpy.wms.WmsServletRequestProcessor;

public class WmsServlet extends HttpServlet {
    private static final long serialVersionUID = 1518113833457077766L;

    private static final String SERVICE_TITLE = "Custom OGC Services";
    private static final String SERVICE_NAME = "CustomOGC";
    private static final String SERVICE_ABSTRACT = "Custom OGC WMS services including Custom, Fiddle and Darkness layers";

    private final WmsServletRequestProcessor processor;

    public WmsServlet() {
        System.setProperty("org.geotools.referencing.forceXY", "true");

        // instantiate the layers
        Layer layer = new DriftingLayer();

        // setup the capabilities of the service which will extract features
        // from the layers to fill in defaults for the layer fields in generated
        // capabilities.xml
        Capabilities cap = Capabilities.builder()
        // set service name
                .serviceName(SERVICE_NAME)
                // set service title
                .serviceTitle(SERVICE_TITLE)
                // set service abstract
                .serviceAbstract(SERVICE_ABSTRACT)
                // add image format
                .imageFormat("image/png")
                // add info format
                .infoFormat("text/html")
                // add custom layer
                .layer(layer)
                // build caps
                .build();

        // initialize the request processor
        processor = WmsServletRequestProcessor.builder()
        // capabilities
                .capabilities(cap)
                // or use
                // .capabilitiesFromClasspath("/wms-capabilities.xml")
                // set image cache size
                .imageCache(200)
                // add custom layer as cached
                .addCachedLayer("Analyze", layer)
                // build it up
                .build();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {

        // use the processor to handle requests
        processor.doGet(req, resp);
    }

}