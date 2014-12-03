package au.gov.amsa.util.nmea;

/**
 * Decodes the first two characters of the first field of an NMEA string (after
 * the ! or $).
 * 
 * @author dxm
 * 
 */
public enum Talker {

	AB("Independent AIS Base Station"), AD("Dependent AIS Base Station"), AG(
			"Heading Track Controller (Autopilot) - General"), AP(
			"Heading Track Controller (Autopilot) - Magnetic"), AI(
			"Mobile Class A or B AIS Station"), AN(
			"AIS Aids to Navigation Station"), AR("AIS Receiving Station"), AS(
			"AIS Station (ITU_R M1371 (Limited Base Station) "), AT(
			"AIS Transmitting Station"), AX("AIS Simplex Repeater Station"), BI(
			"Bilge Systems"), CD("Communications - Digital Selective Calling"), CR(
			"Communications - Data Receiver"), CS("Communications - Satellite"), CT(
			"Communications - Radio-Telephone (MH/HF)"), CV(
			"Communications - Radio-Telephone (VHF)"), CX("Scanning Receiver"), DE(
			"Decca Navigator"), DF("Direction Finder"), DU(
			"Duplex Repeater Station"), EC("Electronic Chraft System (ECS)"), EI(
			"Electronic Chart Display & Information System (ECDIS)"), EP(
			"Electronic Position Indicating Beacon (EPIRB)"), ER(
			"Engine room Monitoring Systems"), FD(
			"Fire Door Controller/Monitoring Point"), FE(
			"Fire Extinguisher System"), FR("Fire Sprinkler System"), GA(
			"Galileo Positioning System"), GL("GLONASS Receiver"), GN(
			"Global Navigation Satellite System (GNSS)"), GP(
			"Global Positioning System"), HC(
			"Heading sensor - compass, magnetic"), HE(
			"Heading sensor - gyro, north seeking"), HF(
			"Heading sensor - fluxgate"), HN(
			"Heading sensor - gyro, non-north seeking"), HD(
			"Hull Door Controller/Monitoring Panel"), HS(
			"Hull Stress Monitoring"), II("Integrated Instrumentation"), IN(
			"Integrated Navigation"), LC("Loran C"), P("Proprietary Code"), RA(
			"Radar and/or Radar Plotting"), RC(
			"Propulsion Machinery including Remote Control"), SA(
			"Physical Shore AIS Station"), SD("Sounder, depth"), SG(
			"Steering Gear/Stearing Engine"), SN(
			"Electronic Positioning System, other/general"), SS(
			"Sounder, scanning"), TI("Turn Rate Indicator"), UP(
			"Microprocessor Controller"), U0("User configured 0"), U1(
			"User configured 1"), U2("User configured 2"), U3(
			"User configured 3"), U4("User configured 4"), U5(
			"User configured 5"), U6("User configured 6"), U7(
			"User configured 7"), U8("User configured 8"), U9(
			"User configured 9"), VD("Velocity Sensor - Dopple, other/general"), VM(
			"Velocity Sensor - Speed Log, Water, Magnetic"), VW(
			"Velocity Sensor - Speed Log, Water, Mechanical"), VR(
			"Voyage Data Recorder"), WD(
			"Watertight Door Controller/Monitoring Panel"), WI(
			"Weather Instruments"), WL("Water Level Detection Systems"), YX(
			"Transducer"), ZA("Timekeepers, Time/Date - Atomic Clock"), ZC(
			"Timekeepers, Time/Date - Chronometer"), ZQ(
			"Timekeepers, Time/Date - Quartz"), ZV(
			"Timekeepers, Time/Date - Radio Update"), UNKNOWN("unknown");

	private final String description;

	private Talker(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
