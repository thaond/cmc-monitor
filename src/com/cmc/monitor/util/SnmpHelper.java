package com.cmc.monitor.util;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Session;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
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
import org.snmp4j.util.TableListener;
import org.snmp4j.util.TableUtils;

public class SnmpHelper {
	
	private String host;
	private String community;
	
	private Session session;
	
	public SnmpHelper(String host, String community) {
		this.host = host;
		this.community = community;
	}
	
	public Session getSession() throws IOException {
		
		if (session == null) {
			TransportMapping<UdpAddress> transportMapping = new DefaultUdpTransportMapping();
			session = new Snmp(transportMapping);
			transportMapping.listen();
		}
		return session;
	}
	
	public ResponseEvent get(OID[] oids) throws IOException {
    	PDU requestPDU = new PDU();
    	for(OID oid : oids) {
    		requestPDU.add(new VariableBinding(oid));
    	}
    	requestPDU.setType(PDU.GET);
    	
    	ResponseEvent responseEvent = getSession().send(requestPDU, getTarget()); 
    	
    	return responseEvent;
    }
	
	public void getTables(OID[] oidColumns, TableListener listener) throws IOException {
		TableUtils tableUtils = new TableUtils(getSession(), new DefaultPDUFactory());
		tableUtils.getTable(getTarget(), oidColumns, listener, null, null, null);
	}
	
	public void close() throws IOException {
		if (session != null) {
			session.close();
		}
	}
	
	/**
	 * This method return a Target, which contains information about where the data should be fetched and how.
	 * @return
	 */
	private Target getTarget() {
		CommunityTarget target = new CommunityTarget();
		Address address = GenericAddress.parse("udp:" + host + "/161");
		
		target.setCommunity(new OctetString(community));
		target.setAddress(address);
		target.setRetries(2);
		target.setTimeout(2000);
		target.setVersion(SnmpConstants.version2c);
		
		return target;
	}
	
	
}
