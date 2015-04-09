streams
=============

Features 
* read a socket broadcast of strings (for example [AIS](http://en.wikipedia.org/wiki/Automatic_Identification_System) [NMEA](http://en.wikipedia.org/wiki/NMEA_0183) feed) with appropriate timeouts and reconnect delays
* reshare an ```Observable<String>``` on a server socket 

Republishing a busy stream from a remote host on a local host server socket can bring about a significant lowering of network traffic and reduce load on the remote host.

This project is a super example of the effectiveness of [RxJava](https://github.com/ReactiveX/RxJava) for processing streams of data.

Here is an example of reading a stream of AIS NMEA messages from a host and republishing it on a server port for multiple subscribers:

```java
import au.gov.amsa.streams.StringSockets;
import au.gov.amsa.streams.StringServer;
import java.util.concurrent.TimeUnit;
import rx.Observable;

Observable<String> strings = 
		StringSockets.from("mariweb.amsa.gov.au")
               .port(9010)
               .quietTimeout(1, TimeUnit.MINUTES)
               .reconnectDelay(1, TimeUnit.SECONDS)
               .create();
int serverSocketPort = 6564;
StringServer.create(strings, serverSocketPort).start();
```

To test the above code:

```telnet localhost 6564```

and you should see the republished stream.

Multiple connections to *localhost:6564* will just reuse the one stream to the source and 
when all connections have disconnected the stream to source is disconnected as well.
