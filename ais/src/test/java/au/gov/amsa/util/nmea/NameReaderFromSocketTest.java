package au.gov.amsa.util.nmea;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class NameReaderFromSocketTest {

	@Test
	public void testConstructorForNonExistingHostThrowsUnknownHostException() {
		try {
			new NmeaReaderFromSocket("ZZZZAAGREFEFE09871091732409.amsa.gov.au",
					100);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof UnknownHostException);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorForNonExistingPortThrowsIllegalArgumentException() {
		new NmeaReaderFromSocket("ZZZZAAGREFEFE09871091732409.amsa.gov.au",
				Integer.MAX_VALUE);
	}

	@Test
	public void testConstructorForNonListeningSocketThrowsIOException()
			throws IOException {
		final ServerSocket server = new ServerSocket(0);
		int freePort = server.getLocalPort();
		assertTrue(freePort > 0);
		server.close();
		try {
			new NmeaReaderFromSocket(null, freePort);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof IOException);
		}
	}

	@Test
	public void testConstructorForListeningPort() throws IOException {
		final ServerSocket server = new ServerSocket(0);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Socket socket = server.accept();
					OutputStream os = socket.getOutputStream();
					os.write("hello".getBytes());
					os.close();
					server.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		t.start();
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(null,
				server.getLocalPort());
		String s = null;
		for (String line : r.read())
			s = line;
		assertEquals("hello", s);
	}

	@Test
	public void testNullSocket() {
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(null);
		r.close();
	}

	@Test
	public void testCloseWhenSocketDoesNotThrowException() throws IOException {
		Socket socket = createMock(Socket.class);
		socket.close();
		expectLastCall().once();
		replay(socket);
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(socket);
		r.close();
	}

	@Test
	public void testSocketCloseThrowsIOException() throws IOException {
		Socket socket = createMock(Socket.class);
		socket.close();
		expectLastCall().andThrow(
				new IOException("expected exception, please ignore")).once();
		replay(socket);
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(socket);
		r.close();
		verify(socket);
	}

	@Test
	public void testReadThrowsIOException() {
		Socket socket = createMock(Socket.class);
		try {
			expect(socket.getInputStream()).andThrow(
					new IOException("mocked exception")).once();
		} catch (IOException e) {
			fail();
		}
		replay(socket);
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(socket);
		try {
			r.read();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof IOException);
		}
		verify(socket);
	}

	@Test
	public void testNormalRead() {
		Socket socket = createMock(Socket.class);
		try {
			expect(socket.getInputStream()).andReturn(
					new ByteArrayInputStream("hello".getBytes())).once();
		} catch (IOException e) {
			fail();
		}
		replay(socket);
		NmeaReaderFromSocket r = new NmeaReaderFromSocket(socket);
		String s = null;
		for (String line : r.read())
			s = line;
		assertEquals("hello", s);
		verify(socket);
	}
}
