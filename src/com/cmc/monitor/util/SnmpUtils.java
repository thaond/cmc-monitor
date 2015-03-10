/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cmc.monitor.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableListener;
import org.snmp4j.util.TableUtils;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 *
 * @author Daniel
 */
public class SnmpUtils {

	private static final Logger _LOGGER = Logger.getLogger(SnmpUtils.class);
	
    public static List getSubtree(String strAddress, String community, String strOID, String strPort) {
        List list = null;
        List retorno = null;
        try {
            OctetString octetCommunity = new OctetString(community);
            strAddress = strAddress + "/" + strPort;
            Address targetaddress = new UdpAddress(strAddress);
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();
            CommunityTarget comtarget = new CommunityTarget(targetaddress, octetCommunity);
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);
            PDUFactory pduf = new DefaultPDUFactory();
            Snmp snmp = new Snmp(transport);
            TreeUtils subtree = new TreeUtils(snmp, pduf);
            list = subtree.getSubtree(comtarget, new OID(strOID));
            if (list != null) {
                retorno = new ArrayList<String>();
                for (int i = 0; i < list.size(); i++) {
                    TreeEvent tevent = (TreeEvent) list.get(i);
                    VariableBinding[] vbs = tevent.getVariableBindings();
                    for (int x = 0; x < vbs.length; x++) {
                        retorno.add(vbs[x].getVariable().toString());
                    }
                }
            }
            snmp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retorno;
    }

     public static List getSubtree(String strAddress, String community, String strOID, String strPort, Boolean ignoreLast3Chars) {
        List list = null;
        List retorno = null;
        try {
            OctetString octetCommunity = new OctetString(community);
            strAddress = strAddress + "/" + strPort;
            Address targetaddress = new UdpAddress(strAddress);
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();
            CommunityTarget comtarget = new CommunityTarget(targetaddress, octetCommunity);
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);
            PDUFactory pduf = new DefaultPDUFactory();
            Snmp snmp = new Snmp(transport);
            TreeUtils subtree = new TreeUtils(snmp, pduf);
            list = subtree.getSubtree(comtarget, new OID(strOID));
            if (list != null) {
                retorno = new ArrayList<String>();
                for (int i = 0; i < list.size(); i++) {
                    TreeEvent tevent = (TreeEvent) list.get(i);
                    VariableBinding[] vbs = tevent.getVariableBindings();
                    for (int x = 0; x < vbs.length; x++) {
                        String aux = vbs[x].getVariable().toString();
                        int end = aux.length();
                        retorno.add(aux.substring(0, end-3));
                    }
                }
            }
            snmp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retorno;
    }

    public static String getNext(String strAddress, String community, String strOID, String strPort) {
        String str = "nada";
        try {
            OctetString octetCommunity = new OctetString(community);
            strAddress = strAddress + "/" + strPort;
            Address targetaddress = new UdpAddress(strAddress);
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();
            CommunityTarget comtarget = new CommunityTarget(targetaddress, octetCommunity);
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(strOID)));
            pdu.setType(PDU.GETNEXT);
            Snmp snmp = new Snmp(transport);
            ResponseEvent response = snmp.getNext(pdu, comtarget);
            if (response != null) {
                if (response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
                    PDU pduresponse = response.getResponse();
                    str = pduresponse.getVariableBindings().firstElement().toString();
                /*(if (str.contains("=")) {
                int len = str.indexOf("=");
                str = str.substring(len + 1, str.length());
                }*/
                }
            } else {
                return "Tempo de busca esgotado!";
            }
            snmp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String get(String strAddress, String community, String strOID, String strPort) {
        String str = "nada";
        try {
            OctetString octetCommunity = new OctetString(community);
            strAddress = strAddress + "/" + strPort;
            Address targetaddress = new UdpAddress(strAddress);
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();
            CommunityTarget comtarget = new CommunityTarget(targetaddress, octetCommunity);
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(strOID)));
            pdu.setType(PDU.GET);
            Snmp snmp = new Snmp(transport);
            ResponseEvent response = snmp.get(pdu, comtarget);
            if (response != null) {
                if (response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
                    PDU pduresponse = response.getResponse();
                    str = pduresponse.getVariableBindings().firstElement().toString();
                /*(if (str.contains("=")) {
                int len = str.indexOf("=");
                str = str.substring(len + 1, str.length());
                }*/
                }
            } else {
                return "Tempo de busca esgotado!";
            }
            snmp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }
    
    public static void getTables(String address, String community, OID[] columns, TableListener listener) throws IOException {
    	// Prepare target
    	CommunityTarget target = new CommunityTarget();
    	Address targetAddress = GenericAddress.parse(address);
    	
    	target.setAddress(targetAddress);
    	target.setCommunity(new OctetString(community));
    	target.setVersion(SnmpConstants.version2c);
    	target.setRetries(2);
    	target.setTimeout(2000);
    	
    	// prepare session
    	TransportMapping<UdpAddress> transportMapping = new DefaultUdpTransportMapping();
    	Snmp snmp = new Snmp(transportMapping);
    	transportMapping.listen();
    	
    	TableUtils tableUtils = new TableUtils(snmp, new DefaultPDUFactory());
    	tableUtils.getTable(target, columns, listener, null, null, null);
    }
    
    public static List<TableEvent> getTables(String address, String community, OID[] columns) throws IOException {
    	// Prepare target
    	CommunityTarget target = new CommunityTarget();
    	Address targetAddress = GenericAddress.parse(address);
    	
    	target.setAddress(targetAddress);
    	target.setCommunity(new OctetString(community));
    	target.setVersion(SnmpConstants.version2c);
    	target.setRetries(2);
    	target.setTimeout(2000);
    	
    	// prepare session
    	TransportMapping<UdpAddress> transportMapping = new DefaultUdpTransportMapping();
    	Snmp snmp = new Snmp(transportMapping);
    	transportMapping.listen();
    	
    	TableUtils tableUtils = new TableUtils(snmp, new DefaultPDUFactory());
    	return tableUtils.getTable(target, columns, null, null);
    }
    
    public static ResponseEvent get(String address, String community, OID[] oids) throws IOException {
    	CommunityTarget target = new CommunityTarget();
    	Address targetAddress = GenericAddress.parse(address);
    	
    	target.setAddress(targetAddress);
    	target.setCommunity(new OctetString(community));
    	target.setVersion(SnmpConstants.version2c);
    	target.setRetries(2);
    	target.setTimeout(2000);
    	
    	// prepare session
    	TransportMapping<UdpAddress> transportMapping = new DefaultUdpTransportMapping();
    	Snmp snmp = new Snmp(transportMapping);
    	transportMapping.listen();
    	
    	// prepare request
    	PDU requestPDU = new PDU();
    	for(OID oid : oids) {
    		requestPDU.add(new VariableBinding(oid));
    	}
    	
    	ResponseEvent responseEvent = snmp.get(requestPDU, target); 
    	
    	snmp.close();
    	
    	return responseEvent;
    }
}
