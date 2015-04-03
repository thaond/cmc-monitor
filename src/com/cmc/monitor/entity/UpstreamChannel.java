package com.cmc.monitor.entity;

import java.io.Serializable;
import java.util.Date;

public class UpstreamChannel implements Serializable {

	private static final long serialVersionUID = 3791643050895787266L;

	private Date createDate;
	private Date modifiedDate;
	private String qam;
	private double avgOnlineCmDsSNR;
	private double avgOnlineCmTxPower;
	private double avgOnlineCmRxPower;
	private double avgOnlineCmDsPower;
	private double avgOnlineCmUsPower;
	private double avgOnlineCmMicRef;
	private double fecUncorrectable;
	private double fecCorrected;
	private int upChannelCmTotal;
	private int upChannelCmRegisterd;
	private int upChannelCmActive;
	private int upChannelModProfile;
	private long upChannelWidth;
	private long upChannelFrequency;
	private long ifSigQUncorrectables;
	private long ifSigQCorrecteds;
	private long ifSigQUnerroreds;
	private double ifSigQSNR;
	private String ifAlias;
	private int ifIndex;
	private String ifDesc;
	private long cmtsId;

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

	public String getQam() {
		return qam;
	}

	public void setQam(String qam) {
		this.qam = qam;
	}

	public double getAvgOnlineCmDsSNR() {
		return avgOnlineCmDsSNR;
	}

	public void setAvgOnlineCmDsSNR(double avgOnlineCmDsSNR) {
		this.avgOnlineCmDsSNR = avgOnlineCmDsSNR;
	}

	public double getAvgOnlineCmDsPower() {
		return avgOnlineCmDsPower;
	}

	public void setAvgOnlineCmDsPower(double avgOnlineCmDsPower) {
		this.avgOnlineCmDsPower = avgOnlineCmDsPower;
	}

	public double getAvgOnlineCmUsPower() {
		return avgOnlineCmUsPower;
	}

	public void setAvgOnlineCmUsPower(double avgOnlineCmUsPower) {
		this.avgOnlineCmUsPower = avgOnlineCmUsPower;
	}

	public double getAvgOnlineCmTxPower() {
		return avgOnlineCmTxPower;
	}

	public void setAvgOnlineCmTxPower(double avgOnlineCmTxPower) {
		this.avgOnlineCmTxPower = avgOnlineCmTxPower;
	}

	public double getAvgOnlineCmRxPower() {
		return avgOnlineCmRxPower;
	}

	public void setAvgOnlineCmRxPower(double avgOnlineCmRxPower) {
		this.avgOnlineCmRxPower = avgOnlineCmRxPower;
	}
	
	public double getAvgOnlineCmMicRef() {
		return avgOnlineCmMicRef;
	}

	public void setAvgOnlineCmMicRef(double avgOnlineCmMicRef) {
		this.avgOnlineCmMicRef = avgOnlineCmMicRef;
	}

	public double getFecUncorrectable() {
		return fecUncorrectable;
	}

	public void setFecUncorrectable(double fecUncorrectable) {
		this.fecUncorrectable = fecUncorrectable;
	}

	public double getFecCorrected() {
		return fecCorrected;
	}

	public void setFecCorrected(double fecCorrected) {
		this.fecCorrected = fecCorrected;
	}

	public int getUpChannelCmTotal() {
		return upChannelCmTotal;
	}

	public void setUpChannelCmTotal(int upChannelCmTotal) {
		this.upChannelCmTotal = upChannelCmTotal;
	}

	public int getUpChannelCmRegisterd() {
		return upChannelCmRegisterd;
	}

	public void setUpChannelCmRegisterd(int upChannelCmRegisterd) {
		this.upChannelCmRegisterd = upChannelCmRegisterd;
	}

	public int getUpChannelCmActive() {
		return upChannelCmActive;
	}

	public void setUpChannelCmActive(int upChannelCmActive) {
		this.upChannelCmActive = upChannelCmActive;
	}

	public int getUpChannelModProfile() {
		return upChannelModProfile;
	}

	public void setUpChannelModProfile(int upChannelModProfile) {
		this.upChannelModProfile = upChannelModProfile;
	}

	public long getUpChannelWidth() {
		return upChannelWidth;
	}

	public void setUpChannelWidth(long upChannelWidth) {
		this.upChannelWidth = upChannelWidth;
	}

	public long getUpChannelFrequency() {
		return upChannelFrequency;
	}

	public void setUpChannelFrequency(long upChannelFrequency) {
		this.upChannelFrequency = upChannelFrequency;
	}

	public long getIfSigQUncorrectables() {
		return ifSigQUncorrectables;
	}

	public void setIfSigQUncorrectables(long ifSigQUncorrectables) {
		this.ifSigQUncorrectables = ifSigQUncorrectables;
	}

	public long getIfSigQCorrecteds() {
		return ifSigQCorrecteds;
	}

	public void setIfSigQCorrecteds(long ifSigQCorrecteds) {
		this.ifSigQCorrecteds = ifSigQCorrecteds;
	}

	public long getIfSigQUnerroreds() {
		return ifSigQUnerroreds;
	}

	public void setIfSigQUnerroreds(long ifSigQUnerroreds) {
		this.ifSigQUnerroreds = ifSigQUnerroreds;
	}

	public double getIfSigQSNR() {
		return ifSigQSNR;
	}

	public void setIfSigQSNR(double ifSigQSNR) {
		this.ifSigQSNR = ifSigQSNR;
	}

	public String getIfAlias() {
		return ifAlias;
	}

	public void setIfAlias(String ifAlias) {
		this.ifAlias = ifAlias;
	}

	public int getIfIndex() {
		return ifIndex;
	}

	public void setIfIndex(int ifIndex) {
		this.ifIndex = ifIndex;
	}

	public String getIfDesc() {
		return ifDesc;
	}

	public void setIfDesc(String ifDesc) {
		this.ifDesc = ifDesc;
	}

	public long getCmtsId() {
		return cmtsId;
	}

	public void setCmtsId(long cmtsId) {
		this.cmtsId = cmtsId;
	}

	@Override
	public String toString() {
		return "UpstreamChannel [createDate=" + createDate + ", modifiedDate=" + modifiedDate + ", qam=" + qam + ", avgOnlineCmDsSNR="
				+ avgOnlineCmDsSNR + ", avgOnlineCmTxPower=" + avgOnlineCmTxPower + ", avgOnlineCmRxPower=" + avgOnlineCmRxPower
				+ ", avgOnlineCmDsPower=" + avgOnlineCmDsPower + ", avgOnlineCmUsPower=" + avgOnlineCmUsPower + ", avgOnlineCmMicRef="
				+ avgOnlineCmMicRef + ", fecUncorrectable=" + fecUncorrectable + ", fecCorrected=" + fecCorrected + ", upChannelCmTotal="
				+ upChannelCmTotal + ", upChannelCmRegisterd=" + upChannelCmRegisterd + ", upChannelCmActive=" + upChannelCmActive
				+ ", upChannelModProfile=" + upChannelModProfile + ", upChannelWidth=" + upChannelWidth + ", upChannelFrequency="
				+ upChannelFrequency + ", ifSigQUncorrectables=" + ifSigQUncorrectables + ", ifSigQCorrecteds=" + ifSigQCorrecteds
				+ ", ifSigQUnerroreds=" + ifSigQUnerroreds + ", ifSigQSNR=" + ifSigQSNR + ", ifAlias=" + ifAlias + ", ifIndex=" + ifIndex
				+ ", ifDesc=" + ifDesc + ", cmtsId=" + cmtsId + "]";
	}

	
}
