package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisStaticDataReportPart;
import au.gov.amsa.ais.Util;

public class AisBStaticDataReportPartA extends AbstractAisBStaticDataReport implements AisStaticDataReportPartA {

	private final static int MESSAGE_LENGTH = 160;
	
	private final static String NAME_NOT_AVAILABLE = "@@@@@@@@@@@@@@@@@@@@";
	
    private final String name;

    public AisBStaticDataReportPartA(String message, int padBits) {
        this(message, null, padBits);
    }

    public AisBStaticDataReportPartA(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    public AisBStaticDataReportPartA(AisExtractorFactory factory, String message, String source,
            int padBits) {
    	super(AisStaticDataReportPart.PART_A, 
    		  factory, 
    		  source, 
    		  factory.create(message, MESSAGE_LENGTH, padBits));
    	
        name = extractName(getExtractor());
    }
    
    static String extractName(AisExtractor extractor) {
    	String value = extractor.getString(40, 160);
    	if(NAME_NOT_AVAILABLE.contentEquals(value)) {
    		return null;
    	} else {
    		return value;
    	}
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisPositionBStaticPartA [source=");
        builder.append(getSource());
        builder.append(", messageId=");
        builder.append(getMessageId());
        builder.append(", mmsi=");
        builder.append(getMmsi());
        builder.append(", repeatIndicator=");
        builder.append(getRepeatIndicator());
        builder.append(", partNumber=");
        builder.append(getPartNumber());
        builder.append(", name=");
        builder.append(getName());
        builder.append("]");
        return builder.toString();
    }
}
