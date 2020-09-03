package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.AisStaticDataReportPart;
import au.gov.amsa.ais.Util;

public abstract class AbstractAisBStaticDataReport implements AisStaticDataReport {
	
	private final AisExtractor extractor;
	
	private final String source;
    private final int messageId;
    private final int repeatIndicator;
    private final int mmsi;
    private final int partNumber;
    
    public static int extractPartNumber(AisExtractorFactory factory, String message, int padBits) {
    	AisExtractor extractor = factory.create(message, 50, padBits);
    	
    	return extractor.getValue(38, 40);
    }
    
    protected AbstractAisBStaticDataReport(AisStaticDataReportPart staticReportPart,
    									   AisExtractorFactory factory, 
    									   String source,
    									   AisExtractor extractor) {
        this.source = source;
        this.extractor = extractor;
        messageId = extractor.getMessageId();
        Util.checkMessageId(getMessageId(), AisMessageType.STATIC_DATA_REPORT);
        repeatIndicator = extractor.getValue(6, 8);
        mmsi = extractor.getValue(8, 38);
        partNumber = staticReportPart.getPartNumber();
        Util.checkStaticDataReportPartNumber(getPartNumber(), staticReportPart);
    }

	protected AisExtractor getExtractor() {
		return extractor;
	}
	
    @Override
    public int getMessageId() {
        return messageId;
    }

    @Override
    public int getRepeatIndicator() {
        return repeatIndicator;
    }

    @Override
    public int getMmsi() {
        return mmsi;
    }
    
	@Override
	public int getPartNumber() {
		return partNumber;
	}

	@Override
	public String getSource() {
		return source;
	}
}
