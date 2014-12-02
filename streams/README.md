streams
=============

Features 
* reshare socket broadcasts of strings (for example AIS NMEA) on localhost

Republishing a busy stream from a remote host on a local host server socket can bring about a significant lowering of network traffic and reduce load on the remote host.

This project is a super example of the effectiveness of [RxJava](https://github.com/ReactiveX/RxJava) for processing streams of data.

Here is an example of reading a stream of AIS NMEA messages from a host and republishing it on a server port for multiple subscribers:

```java
String host = "mariweb.amsa.gov.au";
int port = 9010;
long quietTimeoutMs = 60000;
long reconnectDelayMs = 1000;
int serverSocketPort = 6564;

Observable<String> lines = Lines.from(host, port, quietTimeoutMs,
		reconnectDelayMs).map(Lines.TRIM);
StringServer.start(lines, serverSocketPort);
```

To test the above code:

```telnet localhost 6564```

and you should see the republished stream.

Multiple connections to *localhost:6564* will just reuse the one stream to the source and 
when all connections have disconnected the stream to source is disconnected as well.