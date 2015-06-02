package au.gov.amsa.streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public class RxJava {

    private static final Logger log = LoggerFactory.getLogger(RxJava.class);

    public static void setErrorHandler() {
        RxJavaErrorHandler handler = new RxJavaErrorHandler() {

            @Override
            public void handleError(Throwable e) {
                log.error(e.getMessage(), e);
            }
        };
        RxJavaPlugins.getInstance().registerErrorHandler(handler);
    }

}
