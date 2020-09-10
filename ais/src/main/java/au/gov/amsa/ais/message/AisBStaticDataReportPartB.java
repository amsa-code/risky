package au.gov.amsa.ais.message;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.Util;

public class AisBStaticDataReportPartB extends AbstractAisBStaticDataReport {
	
	private final static int MESSAGE_LENGTH = 168;

	private final static String CALL_SIGN_NOT_AVAILABLE = "@@@@@@@";
	private final static int DIMENSION_ZERO = 0;

    private final int shipType;
    private final String vendorManufacturerId;
    private final int vendorUnitModelCode;
    private final int vendorUnitSerialNumber;
    private final Optional<String> callsign;
    private final Optional<Integer> dimensionA;
    private final Optional<Integer> dimensionB;
    private final Optional<Integer> dimensionC;
    private final Optional<Integer> dimensionD;

    public AisBStaticDataReportPartB(String message, int padBits) {
        this(message, null, padBits);
    }

    public AisBStaticDataReportPartB(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    public AisBStaticDataReportPartB(AisExtractorFactory factory, String message, String source,
            int padBits) {
    	super(PART_NUMBER_B,
    		  factory, 
    		  source, 
    		  factory.create(message, MESSAGE_LENGTH, padBits));
    	
    	shipType = extractShipType(getExtractor());
    	vendorManufacturerId = extractVendorManufacturerId(getExtractor());
    	vendorUnitModelCode = extractVendorUnitModelCode(getExtractor());
    	vendorUnitSerialNumber = extractVendorUnitSerialNumber(getExtractor());
    	callsign = extractCallSign(getExtractor());
        dimensionA = extractDimensionA(getExtractor());
        dimensionB = extractDimensionB(getExtractor());
        dimensionC = extractDimensionC(getExtractor());
        dimensionD = extractDimensionD(getExtractor());
    }
    
    @VisibleForTesting
    static Integer extractShipType(AisExtractor extractor) {
    	return extractor.getValue(40, 48);
    }
    
    @VisibleForTesting
    static String extractVendorManufacturerId(AisExtractor extractor) {
    	return extractor.getString(48, 66);
    }
    
    @VisibleForTesting
    static Integer extractVendorUnitModelCode(AisExtractor extractor) {
    	return extractor.getValue(66, 70);
    }
    
    @VisibleForTesting
    static Integer extractVendorUnitSerialNumber(AisExtractor extractor) {
    	return extractor.getValue(70, 90);
    }
    
    @VisibleForTesting
    static Optional<String> extractCallSign(AisExtractor extractor) {

    	String value = extractor.getString(90, 132);
    	if(CALL_SIGN_NOT_AVAILABLE.contentEquals(value)) {
    		return Optional.absent();
    	} else {
    		return Optional.of(value);
    	}
    }
    
    @VisibleForTesting
    static Optional<Integer> extractDimensionA(AisExtractor extractor) {
    	int value = extractor.getValue(132, 141);
    	
    	return value != DIMENSION_ZERO ? Optional.of(value) : Optional.absent();
    }
    
    @VisibleForTesting
    static Optional<Integer> extractDimensionB(AisExtractor extractor) {
    	int value = extractor.getValue(141, 150);
    	
    	return value != DIMENSION_ZERO ? Optional.of(value) : Optional.absent();
    }
    
    @VisibleForTesting
    static Optional<Integer> extractDimensionC(AisExtractor extractor) {
    	int value = extractor.getValue(150, 156);
    	
    	return value != DIMENSION_ZERO ? Optional.of(value) : Optional.absent();
    }
    
    @VisibleForTesting
    static Optional<Integer> extractDimensionD(AisExtractor extractor) {
    	int value = extractor.getValue(156, 162);
    	
    	return value != DIMENSION_ZERO ? Optional.of(value) : Optional.absent();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisPositionBStaticPartB [source=");
        builder.append(getSource());
        builder.append(", messageId=");
        builder.append(getMessageId());
        builder.append(", mmsi=");
        builder.append(getMmsi());
        builder.append(", repeatIndicator=");
        builder.append(getRepeatIndicator());
        builder.append(", partNumber=");
        builder.append(getPartNumber());
        builder.append(", typeOfShipAndCargoType=");
        builder.append(getShipType());
        builder.append(", vendorManufacturerId=");
        builder.append(getVendorManufacturerId());
        builder.append(", vendorUnitModelCode=");
        builder.append(getVendorUnitModelCode());
        builder.append(", vendorUnitSerialNumber=");
        builder.append(getVendorUnitSerialNumber());
        builder.append(", callSign=");
        builder.append(getCallsign());
        builder.append(", dimensionA=");
        builder.append(getDimensionA());
        builder.append(", dimensionB=");
        builder.append(getDimensionB());
        builder.append(", dimensionC=");
        builder.append(getDimensionC());
        builder.append(", dimensionD=");
        builder.append(getDimensionD());
        builder.append("]");
        return builder.toString();
    }

	public int getShipType() {
		return shipType;
	}

	public Optional<String> getCallsign() {
		return callsign;
	}

	public Optional<Integer> getDimensionA() {
		return dimensionA;
	}

	public Optional<Integer> getDimensionB() {
		return dimensionB;
	}

	public Optional<Integer> getDimensionC() {
		return dimensionC;
	}

	public Optional<Integer> getDimensionD() {
		return dimensionD;
	}

    public Optional<Integer> getLengthMetres() {
        Optional<Integer> a = getDimensionA();
        if (!a.isPresent()) {
            return Optional.absent();
        }
        Optional<Integer> b = getDimensionB();
        if (!b.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(a.get() + b.get());
    }

    public Optional<Integer> getWidthMetres() {
        Optional<Integer> c = getDimensionC();
        if (!c.isPresent()) {
            return Optional.absent();
        }
        Optional<Integer> d = getDimensionD();
        if (!d.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(c.get() + d.get());
    }

	public String getVendorManufacturerId() {
		return vendorManufacturerId;
	}

	public Integer getVendorUnitModelCode() {
		return vendorUnitModelCode;
	}

	public Integer getVendorUnitSerialNumber() {
		return vendorUnitSerialNumber;
	}
}
