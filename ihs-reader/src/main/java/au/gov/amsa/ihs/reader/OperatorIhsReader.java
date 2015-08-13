package au.gov.amsa.ihs.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Optional;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorIhsReader implements Operator<Map<String, String>, InputStream> {

    private static final Logger log = Logger.getLogger(OperatorIhsReader.class);

    protected static final String ELEMENT_SHIP_DATA = "ShipData";

    @Override
    public Subscriber<? super InputStream> call(
            final Subscriber<? super Map<String, String>> child) {
        return new Subscriber<InputStream>() {

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(InputStream is) {
                try {
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    DefaultHandler handler = createHandler(child);
                    parser.parse(is, handler);
                } catch (UnsubscribedSAXException e) {
                    child.onCompleted();
                } catch (Exception e) {
                    onError(e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        };
    }

    public static class UnsubscribedSAXException extends SAXException {

        private static final long serialVersionUID = 1L;

    }

    public static class MyDefaultHandler extends DefaultHandler {

        private int count = 0;
        private final Map<String, String> values = new HashMap<String, String>();
        private Optional<String> currentElement = Optional.absent();
        private final Subscriber<? super Map<String, String>> subscriber;

        public MyDefaultHandler(Subscriber<? super Map<String, String>> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            checkSubscription(subscriber);
            if (ELEMENT_SHIP_DATA.equals(qName)) {
                values.clear();
                count++;
                if (count % 1000 == 0)
                    log.info(count + " ships read");
            }
            currentElement = Optional.of(qName);
        }

        private void checkSubscription(final Subscriber<? super Map<String, String>> subscriber)
                throws UnsubscribedSAXException {
            if (subscriber.isUnsubscribed())
                throw new UnsubscribedSAXException();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            checkSubscription(subscriber);
            String val = new String(ch, start, length).trim();
            if (val.length() > 0) {
                String currentValue = values.get(currentElement.get());
                if (currentValue == null)
                    values.put(currentElement.get(), val);
                else
                    values.put(currentElement.get(), currentValue + val);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            checkSubscription(subscriber);
            if (ELEMENT_SHIP_DATA.equals(qName)) {
                try {
                    subscriber.onNext(new HashMap<>(values));
                } catch (RuntimeException e) {
                    throw new RuntimeException("error building Ship from " + values, e);
                }
            }
        }

    }

    private static DefaultHandler createHandler(
            final Subscriber<? super Map<String, String>> subscriber) {
        return new MyDefaultHandler(subscriber);
    }

}
