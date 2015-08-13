package au.gov.amsa.ihs.reader;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Optional;

import au.gov.amsa.ihs.model.Ship;
import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorIhsReader implements Operator<Ship, InputStream> {

    private static final Logger log = Logger.getLogger(OperatorIhsReader.class);

    protected static final String ELEMENT_SHIP_DATA = "ShipData";

    @Override
    public Subscriber<? super InputStream> call(final Subscriber<? super Ship> child) {
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

    private static DefaultHandler createHandler(final Subscriber<? super Ship> subscriber) {
        return new DefaultHandler() {

            int count = 0;

            Map<String, String> values = new HashMap<String, String>();
            Optional<String> currentElement = Optional.absent();

            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                checkSubscription(subscriber);
                if (ELEMENT_SHIP_DATA.equals(qName)) {
                    values.clear();
                    count++;
                    if (count % 1000 == 0)
                        log.info(count + " ships read");
                }
                currentElement = Optional.of(qName);
            }

            private void checkSubscription(final Subscriber<? super Ship> subscriber)
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
                        subscriber.onNext(buildShip());
                    } catch (RuntimeException e) {
                        throw new RuntimeException("error building Ship from " + values, e);
                    }
                }
            }

            private Ship buildShip() {
                return Ship.builder().classificationSocietyCode(value("ClassificationSocietyCode"))
                        .countryOfBuildCode(value("CountryOfBuildCode"))
                        .yearOfBuild(toYearOfBuild(value("DateOfBuild")))
                        .monthOfBuild(toMonthOfBuild(value("DateOfBuild")))
                        .flagCode(value("FlagCode")).grossTonnage(toLong(value("GrossTonnage")))
                        .groupBeneficialOwnerCompanyCode(value("GroupBeneficialOwnerCompanyCode"))
                        .groupBeneficialOwnerCountryOfDomicileCode(
                                value("GroupBeneficialOwnerCountryofDomicileCode"))
                        .imo(value("LRIMOShipNo").get())
                        .mmsi(value("MaritimeMobileServiceIdentityMMSINumber"))
                        .type2(value("ShiptypeLevel2")).type3(value("ShiptypeLevel3"))
                        .type4(value("ShiptypeLevel4")).type5(value("ShiptypeLevel5"))
                        .deadweightTonnage(toFloat(value("FormulaDWT")))
                        .statCode5(value("StatCode5"))
                        .lengthOverallMetres(toFloat(value("LengthOverallLOA")))
                        .breadthMetres(toFloat(value("Breadth")))
                        .displacementTonnage(toFloat(value("Displacement")))
                        .draughtMetres(toFloat(value("Draught")))
                        .speedKnots(toFloat(value("Speed")))
                        .lastUpdateTime(toDateTime(value("LastUpdateDate"))).name(value("ShipName"))
                        .shipBuilderCompanyCode(value("ShipbuilderCompanyCode"))
                        // build it
                        .build();
            }

            private Optional<String> value(String name) {
                String val = values.get(name);
                if (val == null)
                    return absent();
                else if (val.trim().length() == 0)
                    return absent();
                else
                    return Optional.fromNullable(val.trim());
            }

        };
    }

    private static Optional<DateTime> toDateTime(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            try {
                return of(DateTime.parse(value.get()));
            } catch (RuntimeException e) {
                return absent();
            }
        }
    }

    private static Optional<Integer> toYearOfBuild(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            try {
                return of(Integer.parseInt(value.get().substring(0, 4)));
            } catch (RuntimeException e) {
                return absent();
            }
        }
    }

    private static Optional<Integer> toMonthOfBuild(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            int month = Integer.parseInt(value.get().substring(4, 6));
            if (month > 0)
                return of(month);
            else
                return absent();
        }
    }

    private static Optional<Long> toLong(Optional<String> value) {
        if (!value.isPresent())
            return Optional.absent();
        else {
            return Optional.of(Long.parseLong(value.get()));
        }
    }

    private static Optional<Float> toFloat(Optional<String> value) {
        if (!value.isPresent())
            return Optional.absent();
        else {
            return Optional.of(Float.parseFloat(value.get()));
        }
    }

}
