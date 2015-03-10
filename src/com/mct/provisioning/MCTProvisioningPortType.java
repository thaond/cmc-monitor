/**
 * MCTProvisioningPortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mct.provisioning;

public interface MCTProvisioningPortType extends java.rmi.Remote {

    /**
     * MyCallerText Provisioning - Subscribe API service
     */
    public com.mct.provisioning.ResponseHeader MCTSubscribe(com.mct.provisioning.RequestHeader requestHeader, int subscriberType, int subscriberStatus, int chargingType, int extensionDays, float chargeAmount, java.lang.String remarks) throws java.rmi.RemoteException;

    /**
     * MyCallerText Provisioning - Terminate API service
     */
    public com.mct.provisioning.ResponseHeader MCTTerminate(com.mct.provisioning.RequestHeader requestHeader, java.lang.String remarks) throws java.rmi.RemoteException;

    /**
     * MyCallerText Provisioning - Renewal API service
     */
    public com.mct.provisioning.ResponseHeader MCTRenewal(com.mct.provisioning.RequestHeader requestHeader, int subscriberStatus, int chargingType, int extensionDays, float chargeAmount, java.lang.String remarks) throws java.rmi.RemoteException;
}
