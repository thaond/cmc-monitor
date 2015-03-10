/**
 * MCTProvisioningLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mct.provisioning;

public class MCTProvisioningLocator extends org.apache.axis.client.Service implements com.mct.provisioning.MCTProvisioning {

    public MCTProvisioningLocator() {
    }


    public MCTProvisioningLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public MCTProvisioningLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for MCTProvisioningPort
    private java.lang.String MCTProvisioningPort_address = "http://10.8.39.106/mct/provisionapi/wsprovisioning.php";

    public java.lang.String getMCTProvisioningPortAddress() {
        return MCTProvisioningPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String MCTProvisioningPortWSDDServiceName = "MCTProvisioningPort";

    public java.lang.String getMCTProvisioningPortWSDDServiceName() {
        return MCTProvisioningPortWSDDServiceName;
    }

    public void setMCTProvisioningPortWSDDServiceName(java.lang.String name) {
        MCTProvisioningPortWSDDServiceName = name;
    }

    public com.mct.provisioning.MCTProvisioningPortType getMCTProvisioningPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(MCTProvisioningPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getMCTProvisioningPort(endpoint);
    }

    public com.mct.provisioning.MCTProvisioningPortType getMCTProvisioningPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.mct.provisioning.MCTProvisioningBindingStub _stub = new com.mct.provisioning.MCTProvisioningBindingStub(portAddress, this);
            _stub.setPortName(getMCTProvisioningPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setMCTProvisioningPortEndpointAddress(java.lang.String address) {
        MCTProvisioningPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.mct.provisioning.MCTProvisioningPortType.class.isAssignableFrom(serviceEndpointInterface)) {
                com.mct.provisioning.MCTProvisioningBindingStub _stub = new com.mct.provisioning.MCTProvisioningBindingStub(new java.net.URL(MCTProvisioningPort_address), this);
                _stub.setPortName(getMCTProvisioningPortWSDDServiceName());
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
        if ("MCTProvisioningPort".equals(inputPortName)) {
            return getMCTProvisioningPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://10.8.39.106/mct/provisionapi/wsprovisioning.php?wsdl", "MCTProvisioning");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://10.8.39.106/mct/provisionapi/wsprovisioning.php?wsdl", "MCTProvisioningPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("MCTProvisioningPort".equals(portName)) {
            setMCTProvisioningPortEndpointAddress(address);
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
