package au.gov.amsa.ais;

import static au.gov.amsa.ais.TstUtil.handleAisStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class LiveStreamer {

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("Testing live stream");
		int port = Integer.parseInt(System.getProperty("ais.port", "1236"));
		String host = System.getProperty("ais.host", "cbrais01.amsa.gov.au");
		System.out.println("connecting to port " + port);

		FileOutputStream raw = new FileOutputStream("target/raw-2.txt");
		FileOutputStream processed = new FileOutputStream(
				"target/processed-2.txt", true);
		while (true)
			try {
				Socket socket = new Socket(host, port);
				handleAisStream(socket.getInputStream(), new PrintStream(raw),
						new PrintStream(processed), System.out);
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// do nothing
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
