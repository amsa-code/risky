package au.gov.amsa.ais.router;

import au.gov.amsa.ais.router.model.Connection;
import au.gov.amsa.ais.router.model.Group;
import au.gov.amsa.ais.router.model.Port;

public class RouterMain {

    public static void main(String[] args) throws InterruptedException {

        // setup connections
        Connection terrestrial = Connection.builder().id("terrestrial").host("mariweb.amsa.gov.au")
                .port(9010).readTimeoutMs(10000).retryIntervalMs(1000).build();

        Connection satellite = Connection.builder().id("satellite").host("mariweb.amsa.gov.au")
                .port(9100).readTimeoutMs(300000).retryIntervalMs(10000).build();

        // set up groups
        Group groupAll = Group.builder().id("all").member(terrestrial).member(satellite).build();

        Group kembla = Group.builder().id("Port Kembla").member(terrestrial)
                .filterPattern(".*Kembla.*").build();

        // set up ports
        Port portAll = Port.builder().group(groupAll).port(9000).build();

        Port portTerrestrial = Port.builder().group(terrestrial).port(9001).build();

        Port portKembla = Port.builder().group(kembla).port(9002).build();

        // start

        Router.start(portAll, portTerrestrial, portKembla);

        Thread.sleep(Long.MAX_VALUE);

    }
}
