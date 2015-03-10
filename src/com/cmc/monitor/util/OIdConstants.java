package com.cmc.monitor.util;

/**
 * Constants OID for CMC monitor
 * @author richard
 *
 */
public interface OIdConstants {
	
    public static final String IfIndex 						= "1.3.6.1.2.1.2.2.1.1";	// IfIndex - Unique value for each interface
    public static final String Ifdescr 						= "1.3.6.1.2.1.2.2.1.2";
    public static final String Ifalias 						= "1.3.6.1.2.1.31.1.1.1.18";
    
    public static final String IfSigQSnr 						= "1.3.6.1.2.1.10.127.1.1.4.1.5"; // Current signal-to-noise ratio (SNR), in tenths of dB, for this downstream channel (CM) or upstream channel (CMTS).
    public static final String IfSigQUnerroreds 				= "1.3.6.1.2.1.10.127.1.1.4.1.2"; // Number of codewords received on this channel without errors.
    public static final String IfSigQCorrecteds 				= "1.3.6.1.2.1.10.127.1.1.4.1.3"; // Number of codewords received on this channel with correctable errors
    public static final String IfSigQUncorrectables			= "1.3.6.1.2.1.10.127.1.1.4.1.4"; // Number of codewords received on this channel with uncorrectable errors.
    
    public static final String UpChannelFrequency			= "1.3.6.1.2.1.10.127.1.1.2.1.2"; //The center of the frequency band associated with this upstream interface.
    public static final String UpChannelWidth				= "1.3.6.1.2.1.10.127.1.1.2.1.3"; // The bandwidth of this upstream interface
    public static final String UpChannelModulationProfile	= "1.3.6.1.2.1.10.127.1.1.2.1.4"; // An entry identical to the docsIfModIndex in the  docsIfCmtsModulationTable that describes this channel.
    
    public static final String UpChannelCmActive				= "1.3.6.1.4.1.9.9.116.1.4.1.1.4";
    public static final String UpChannelCmRegistered		= "1.3.6.1.4.1.9.9.116.1.4.1.1.5";
    public static final String UpChannelCmTotal				= "1.3.6.1.4.1.9.9.116.1.4.1.1.3";

    public static final String CmMacAddress 					= "1.3.6.1.2.1.10.127.1.3.3.1.2"; // MAC address for the CM’s cable interface.
    public static final String CmDcIfIndex 					= "1.3.6.1.2.1.10.127.1.3.3.1.4"; // If index of downstream channel 
    public static final String CmUcIfIndex 					= "1.3.6.1.2.1.10.127.1.3.3.1.5"; // If index of upstream channel	
    public static final String CmUsSnr 						= "1.3.6.1.2.1.10.127.1.3.3.1.13"; // Signal/Noise ratio as perceived for upstream data from this Cable Modem
    public static final String CmUnerroreds 					= "1.3.6.1.2.1.10.127.1.3.3.1.10"; // Codewords received without error from this Cable Modem.
    public static final String CmCorrecteds 					= "1.3.6.1.2.1.10.127.1.3.3.1.11"; // Codewords received with correctable errors from this 	Cable Modem
    public static final String CmUncorrectables 				= "1.3.6.1.2.1.10.127.1.3.3.1.12"; // Codewords received with uncorrectable errors from this Cable Modem
    public static final String CmIpAddress 					= "1.3.6.1.2.1.10.127.1.3.3.1.3"; // If Address for this cable model
    public static final String CmStatus 						= "1.3.6.1.2.1.10.127.1.3.3.1.9"; // Current Cable Modem connectivity state
    public static final String CmRxPower 						= "1.3.6.1.2.1.10.127.1.3.3.1.6"; // Receive power level, in tenths of dBmV, for this CM, as perceived by the CMTS on the upstream.
    
    public static final String CmDsPower 						= "1.3.6.1.4.1.9.10.59.1.2.1.1.1"; // Received power level, in tenths of dBmV, of the cable modem
    public static final String CmUsPower 						= "1.3.6.1.4.1.9.10.59.1.2.1.1.2"; // Operational transmit power level, in tenths of dBmV, for the upstream of the cable modem.
    public static final String CmDsSnr 						= "1.3.6.1.4.1.9.10.59.1.2.1.1.4"; // Signal-to-noise ratio (SNR), in tenths of dB, as perceived by the CMTS, for the downstream of the cable modem.
    public static final String CmMicroreflections 			= "1.3.6.1.4.1.9.10.59.1.2.1.1.5"; // Total microreflections, expressed as dBc below the signal level, as perceived by the CMTS, for the cable modem’s downstream.This object is only a rough indication of microflections, including in-channel response, and not an absolute measurement.

}
