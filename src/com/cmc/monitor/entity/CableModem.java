package com.cmc.monitor.entity;

import java.io.Serializable;
import java.util.Date;

public class CableModem implements Serializable {
	
	public static final int STATUS_OTHER = 1;
	public static final int STATUS_RANGING = 2;
	public static final int STATUS_RANGING_ABORTED = 3;
	public static final int STATUS_RANGING_COMPLETE = 4;
	public static final int STATUS_IP_COMPLETE = 5;
	public static final int STATUS_REGISTRATION_COMPLETE = 6;
	public static final int STATUS_ACCESS_DENIED = 7;

	// serialVersionUID
	private static final long serialVersionUID = 1791219725695974183L;
	
	private Date createDate;
	private Date modifiedDate;
	private long cmtsId;
	private int cmIndex;
	private double fecUncorrectable;
	private double fecCorrected;
	private long mircroRef; 
	private int rxPower;
	private int txPower;
	private int usPower;
	private int dsPower;
	private long uncorrectables;
	private long correcteds;
	private long unerroreds;
	private int dsSNR;
	private int usSNR;
	private int ucIfIndex;
	private int dcIfIndex;
	private String macAddress;
	private String cmSubIndex;
	private int status;
	private String ipAddress;
	
	public CableModem() {
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public long getCmtsId() {
		return cmtsId;
	}

	public void setCmtsId(long cmtsId) {
		this.cmtsId = cmtsId;
	}

	public int getCmIndex() {
		return cmIndex;
	}

	public void setCmIndex(int cmIndex) {
		this.cmIndex = cmIndex;
	}

	public double getFecUncorrectable() {
		return fecUncorrectable;
	}

	public void setFecUncorrectable(double fecuncorrectable) {
		this.fecUncorrectable = fecuncorrectable;
	}

	public double getFecCorrected() {
		return fecCorrected;
	}

	public void setFecCorrected(double fecCorrected) {
		this.fecCorrected = fecCorrected;
	}

	public long getMircroRef() {
		return mircroRef;
	}

	public void setMircroRef(long mircroRef) {
		this.mircroRef = mircroRef;
	}

	public int getRxPower() {
		return rxPower;
	}

	public void setRxPower(int rxPower) {
		this.rxPower = rxPower;
	}

	public int getTxPower() {
		return txPower;
	}

	public void setTxPower(int txPower) {
		this.txPower = txPower;
	}

	public int getUsPower() {
		return usPower;
	}

	public void setUsPower(int usPower) {
		this.usPower = usPower;
	}

	public int getDsPower() {
		return dsPower;
	}

	public void setDsPower(int dsPower) {
		this.dsPower = dsPower;
	}

	public long getUncorrectables() {
		return uncorrectables;
	}

	public void setUncorrectables(long uncorrectables) {
		this.uncorrectables = uncorrectables;
	}

	public long getCorrecteds() {
		return correcteds;
	}

	public void setCorrecteds(long correcteds) {
		this.correcteds = correcteds;
	}

	public long getUnerroreds() {
		return unerroreds;
	}

	public void setUnerroreds(long unerroreds) {
		this.unerroreds = unerroreds;
	}

	public int getDsSNR() {
		return dsSNR;
	}

	public void setDsSNR(int dsSNR) {
		this.dsSNR = dsSNR;
	}

	public int getUsSNR() {
		return usSNR;
	}

	public void setUsSNR(int usSNR) {
		this.usSNR = usSNR;
	}

	public int getUcIfIndex() {
		return ucIfIndex;
	}

	public void setUcIfIndex(int ucInIndex) {
		this.ucIfIndex = ucInIndex;
	}

	public int getDcIfIndex() {
		return dcIfIndex;
	}

	public void setDcIfIndex(int dcIfIndex) {
		this.dcIfIndex = dcIfIndex;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getCmSubIndex() {
		return cmSubIndex;
	}

	public void setCmSubIndex(String cmSubIndex) {
		this.cmSubIndex = cmSubIndex;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
}
