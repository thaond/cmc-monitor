package com.crm.subscriber.bean;

import java.io.Serializable;
import java.util.Date;

import com.crm.kernel.message.Constants;


//create by trungnq

public class SubscriberBalance implements Serializable {

	private static final long	serialVersionUID	= 1L;

	private long				balanceId			= Constants.DEFAULT_ID;
	private long				userId				= Constants.DEFAULT_ID;
	private String				userName			= "";
	private Date				createDate			= null;
	private Date				modifiedDate		= null;
	private long				subscriberId		= Constants.DEFAULT_ID;
	private String				isdn				= "";
	private String				balanceType			= "";
	private int					balanceAmount		= 0 ;
	private Date				startDate			= null;
	private Date				expirationDate		= null;
	private int					status				= 0;
	private String				description			= "";
	private int					cumulationAmount	= 0;
	

	
	public long getBalanceId() {
		return balanceId;
	}
	public void setBalanceId(long balanceId) {
		this.balanceId = balanceId;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
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
	public long getSubscriberId() {
		return subscriberId;
	}
	public void setSubscriberId(long subscriberId) {
		this.subscriberId = subscriberId;
	}
	public String getIsdn() {
		return isdn;
	}
	public void setIsdn(String isdn) {
		this.isdn = isdn;
	}
	public String getBalanceType() {
		return balanceType;
	}
	public void setBalanceType(String balanceType) {
		this.balanceType = balanceType;
	}
	public int getBalanceAmount() {
		return balanceAmount;
	}
	public void setBalanceAmount(int balanceAmount) {
		this.balanceAmount = balanceAmount;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public int getCumulationAmount() {
		// TODO Auto-generated method stub
		return cumulationAmount;
	}
	public void setCumulationAmount(int cumulationAmount) {
		this.cumulationAmount = cumulationAmount;

	}
	
}
