package au.gov.amsa.ais.message;

import com.google.common.base.Optional;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisStaticDataReportPart;
import au.gov.amsa.ais.Util;

public class AisBStaticDataReportPartB extends AbstractAisBStaticDataReport implements AisStaticDataReportPartB {
	
	private final static int MESSAGE_LENGTH = 168;

	private final static String CALL_SIGN_NOT_AVAILABLE = "@@@@@@@";
	private final static int TYPE_OF_SHIP_AND_CARGO_TYPE_NOT_AVAILABLE = 0;

    private Integer shipType;
    private String vendorManufacturerId;
    private Integer vendorUnitModelCode;
    private Integer vendorUnitSerialNumber;
    private String callsign;
    private final int dimensionA;
    private final int dimensionB;
    private final int dimensionC;
    private final int dimensionD;
    private int typeOfElectronicPosition;

    public AisBStaticDataReportPartB(String message, int padBits) {
        this(message, null, padBits);
    }

    public AisBStaticDataReportPartB(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    public AisBStaticDataReportPartB(AisExtractorFactory factory, String message, String source,
            int padBits) {
    	super(AisStaticDataReportPart.PART_B,
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
    	typeOfElectronicPosition = getExtractor().getValue(162, 4);
    }
    
    static Integer extractShipType(AisExtractor extractor) {
    	int value = extractor.getValue(40, 48);
    	if(TYPE_OF_SHIP_AND_CARGO_TYPE_NOT_AVAILABLE == value) {
    		return null;
    	} else {
    		return value;
    	}
    }
    
    static String extractVendorManufacturerId(AisExtractor extractor) {
    	return extractor.getString(48, 66);
    }
    
    static Integer extractVendorUnitModelCode(AisExtractor extractor) {
    	return extractor.getValue(66, 70);
    }
    
    static Integer extractVendorUnitSerialNumber(AisExtractor extractor) {
    	return extractor.getValue(70, 90);
    }
    
    static String extractCallSign(AisExtractor extractor) {

    	String value = extractor.getString(90, 132);
    	if(CALL_SIGN_NOT_AVAILABLE.contentEquals(value)) {
    		return null;
    	} else {
    		return value;
    	}
    }
    
    static int extractDimensionA(AisExtractor extractor) {
    	return extractor.getValue(132, 141);
    }
    
    static int extractDimensionB(AisExtractor extractor) {
    	return extractor.getValue(141, 150);
    }
    
    static int extractDimensionC(AisExtractor extractor) {
    	return extractor.getValue(150, 156);
    }
    
    static int extractDimensionD(AisExtractor extractor) {
    	return extractor.getValue(156, 162);
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
        builder.append(", typeOfElectronicPosition=");
        builder.append(getTypeOfElectronicPosition());
        builder.append("]");
        return builder.toString();
    }

	@Override
	public int getShipType() {
		return shipType;
	}

	@Override
	public String getCallsign() {
		return callsign;
	}

	@Override
	public int getTypeOfElectronicPosition() {
		return typeOfElectronicPosition;
	}

	@Override
	public Optional<Integer> getDimensionA() {
        if (dimensionA == 0) {
            return Optional.absent();
        } else {
            return Optional.of(dimensionA);
        }
	}

	@Override
	public Optional<Integer> getDimensionB() {
		if (dimensionB == 0) {
            return Optional.absent();
        } else {
            return Optional.of(dimensionB);
        }
	}

	@Override
	public Optional<Integer> getDimensionC() {
		if (dimensionC == 0) {
            return Optional.absent();
        } else {
            return Optional.of(dimensionC);
        }
	}

	@Override
	public Optional<Integer> getDimensionD() {
		if (dimensionD == 0) {
            return Optional.absent();
        } else {
            return Optional.of(dimensionD);
        }
	}

	@Override
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

    @Override
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

	@Override
	public String getVendorManufacturerId() {
		return vendorManufacturerId;
	}

	@Override
	public Integer getVendorUnitModelCode() {
		return vendorUnitModelCode;
	}

	@Override
	public Integer getVendorUnitSerialNumber() {
		return vendorUnitSerialNumber;
	}
}
