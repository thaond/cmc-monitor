package com.mct.provisioning;

public class MCTProvisioningPortTypeProxy implements com.mct.provisioning.MCTProvisioningPortType {
  private String _endpoint = null;
  private com.mct.provisioning.MCTProvisioningPortType mCTProvisioningPortType = null;
  
  public MCTProvisioningPortTypeProxy() {
    _initMCTProvisioningPortTypeProxy();
  }
  
  public MCTProvisioningPortTypeProxy(String endpoint) {
    _endpoint = endpoint;
    _initMCTProvisioningPortTypeProxy();
  }
  
  private void _initMCTProvisioningPortTypeProxy() {
    try {
      mCTProvisioningPortType = (new com.mct.provisioning.MCTProvisioningLocator()).getMCTProvisioningPort();
      if (mCTProvisioningPortType != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)mCTProvisioningPortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)mCTProvisioningPortType)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (mCTProvisioningPortType != null)
      ((javax.xml.rpc.Stub)mCTProvisioningPortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public com.mct.provisioning.MCTProvisioningPortType getMCTProvisioningPortType() {
    if (mCTProvisioningPortType == null)
      _initMCTProvisioningPortTypeProxy();
    return mCTProvisioningPortType;
  }
  
  public com.mct.provisioning.ResponseHeader MCTSubscribe(com.mct.provisioning.RequestHeader requestHeader, int subscriberType, int subscriberStatus, int chargingType, int extensionDays, float chargeAmount, java.lang.String remarks) throws java.rmi.RemoteException{
    if (mCTProvisioningPortType == null)
      _initMCTProvisioningPortTypeProxy();
    return mCTProvisioningPortType.MCTSubscribe(requestHeader, subscriberType, subscriberStatus, chargingType, extensionDays, chargeAmount, remarks);
  }
  
  public com.mct.provisioning.ResponseHeader MCTTerminate(com.mct.provisioning.RequestHeader requestHeader, java.lang.String remarks) throws java.rmi.RemoteException{
    if (mCTProvisioningPortType == null)
      _initMCTProvisioningPortTypeProxy();
    return mCTProvisioningPortType.MCTTerminate(requestHeader, remarks);
  }
  
  public com.mct.provisioning.ResponseHeader MCTRenewal(com.mct.provisioning.RequestHeader requestHeader, int subscriberStatus, int chargingType, int extensionDays, float chargeAmount, java.lang.String remarks) throws java.rmi.RemoteException{
    if (mCTProvisioningPortType == null)
      _initMCTProvisioningPortTypeProxy();
    return mCTProvisioningPortType.MCTRenewal(requestHeader, subscriberStatus, chargingType, extensionDays, chargeAmount, remarks);
  }
  
  
}