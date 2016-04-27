package au.gov.amsa.ais.router;

import au.gov.amsa.ais.router.model.Connection;
import au.gov.amsa.ais.router.model.Group;
import au.gov.amsa.ais.router.model.Port;

public class RouterMain {
    public static void main(String[] args) throws InterruptedException {

        Connection terrestrial = Connection.builder().id("terrestrial").host("mariweb.amsa.gov.au")
                .port(9010).readTimeoutMs(10000).retryIntervalMs(1000).build();

        Connection satellite = Connection.builder().id("satellite").host("mariweb.amsa.gov.au")
                .port(9100).readTimeoutMs(300000).retryIntervalMs(10000).build();

        Group group = Group.builder().id("all").member(terrestrial).member(satellite).build();

        Port port = Port.builder().group(group).port(9000).build();

        port.start();

        Thread.sleep(Long.MAX_VALUE);
    }
}
