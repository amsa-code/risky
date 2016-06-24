package au.gov.amsa.ais;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import au.gov.amsa.util.nmea.NmeaUtil;

public final class MariwebSslConnectMain {

    public static final String CONTEXT_SSL = "SSL";

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException,
            UnknownHostException, IOException, InterruptedException {

        SSLContext context = setAlwaysHappyTrustManager(false);
        SSLSocketFactory factory = context.getSocketFactory();
        // Socket socket = factory.createSocket("mariweb.amsa.gov.au", 9005);
        Socket socket = factory.createSocket("5.2.17.137", 9004);

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> read(socket, latch));

        System.out.println("sleeping");
        Thread.sleep(5000);

        OutputStream out = socket.getOutputStream();
        System.out.println("sending command");

        String password = System.getProperty("password");
        if (password == null)
            throw new IllegalArgumentException("you must set the 'password' system property");

        String command = "$PMWLSS," + System.currentTimeMillis() / 1000 + ",4,amsa," + password
                + ",1*";
        String checksum = NmeaUtil.getChecksum(command);
        command = command + checksum + "\r\n";

        out.write(command.getBytes(StandardCharsets.UTF_8));
        out.flush();
        System.out.println(command);

        latch.await();

    }

    private static void read(Socket socket, CountDownLatch latch) {
        try {
            System.out.println("starting reader");
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        latch.countDown();
    }

    private static SSLContext setAlwaysHappyTrustManager(boolean certificateMustBeValid)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance(CONTEXT_SSL);
        TrustManager[] trustManagers = null;
        if (!certificateMustBeValid) {
            TrustManager trustManager = new AlwaysHappyTrustManager();
            trustManagers = new TrustManager[] { trustManager };
        }
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        return sslContext;
    }

    private static class AlwaysHappyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // don't throw exception
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // don't throw exception
        }

        @Override
        public final X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
