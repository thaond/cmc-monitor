/**
 * APIServicesServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.fss.SMSUtility.services;

public class APIServicesServiceLocator extends org.apache.axis.client.Service implements com.fss.SMSUtility.services.APIServicesService {

    public APIServicesServiceLocator() {
    }


    public APIServicesServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public APIServicesServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for API
    private java.lang.String API_address = "http://10.8.13.61:7865/eload/services/API";

    public java.lang.String getAPIAddress() {
        return API_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String APIWSDDServiceName = "API";

    public java.lang.String getAPIWSDDServiceName() {
        return APIWSDDServiceName;
    }

    public void setAPIWSDDServiceName(java.lang.String name) {
        APIWSDDServiceName = name;
    }

    public com.fss.SMSUtility.services.APIServices getAPI() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(API_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getAPI(endpoint);
    }

    public com.fss.SMSUtility.services.APIServices getAPI(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.fss.SMSUtility.services.APISoapBindingStub _stub = new com.fss.SMSUtility.services.APISoapBindingStub(portAddress, this);
            _stub.setPortName(getAPIWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setAPIEndpointAddress(java.lang.String address) {
        API_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.fss.SMSUtility.services.APIServices.class.isAssignableFrom(serviceEndpointInterface)) {
                com.fss.SMSUtility.services.APISoapBindingStub _stub = new com.fss.SMSUtility.services.APISoapBindingStub(new java.net.URL(API_address), this);
                _stub.setPortName(getAPIWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("API".equals(inputPortName)) {
            return getAPI();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://services.SMSUtility.fss.com", "APIServicesService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://services.SMSUtility.fss.com", "API"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("API".equals(portName)) {
            setAPIEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
