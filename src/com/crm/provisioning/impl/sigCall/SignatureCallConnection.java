package com.crm.provisioning.impl.sigCall;

import java.net.URL;

import com.crm.provisioning.cache.ProvisioningConnection;
import com.crm.provisioning.message.CommandMessage;
import com.mct.provisioning.MCTProvisioningLocator;
import com.mct.provisioning.RequestHeader;
import com.mct.provisioning.ResponseHeader;
import com.mct.provisioning.MCTProvisioningBindingStub;

public class SignatureCallConnection extends ProvisioningConnection
{
	private MCTProvisioningBindingStub		service = null;
	private ResponseHeader					response = null;
	
	public SignatureCallConnection()
	{
		setHost("192.168.194.4");
		setPort(3300);
		setTimeout(15000);
	}

	private URL getURL(String host) throws Exception
	{
		URL url = new URL(host);
		return url;
	}

	@Override
	public boolean openConnection() throws Exception
	{
		URL url = getURL(getHost());
		
		MCTProvisioningLocator locator = new MCTProvisioningLocator();
		service = new MCTProvisioningBindingStub(url, locator);
		
		return super.openConnection();
	}

	public int register(CommandMessage request, int channel, int subscriberType, int subscriberStatus, int chargingType, int extensionDays, int sessionid) throws Exception
	{
		int responseCode = 0;
		try
		{
			RequestHeader header = new RequestHeader();
			header.setChannel(channel);
			header.setTransactionID("" + sessionid);
			header.setMSISDN(request.getIsdn());

			response = service.MCTSubscribe(header, subscriberType, subscriberStatus, chargingType, extensionDays, (float)request.getAmount(), "");
			responseCode = response.getResponseCode();
		}
		catch (Exception e)
		{
			throw e;
		}
		return responseCode;
	}

	public int unregister(CommandMessage request, int channel, int sessionid) throws Exception
	{
		int responseCode = 0;
		
		try
		{
			RequestHeader header = new RequestHeader();
			header.setChannel(channel);
			header.setTransactionID("" + sessionid);
			header.setMSISDN(request.getIsdn());

			response = service.MCTTerminate(header, "");
			responseCode = response.getResponseCode();
		}
		catch (Exception e)
		{
			throw e;
		}
		return responseCode;
	}

	public int renewal(CommandMessage request, int channel, int subscriberStatus, int chargingType, int extensionDays, int sessionid) throws Exception
	{
		int responseCode = 0;
		
		try
		{
			RequestHeader header = new RequestHeader();
			header.setChannel(channel);
			header.setTransactionID("" + sessionid);
			header.setMSISDN(request.getIsdn());

			response = service.MCTRenewal(header, subscriberStatus, chargingType, extensionDays, (float)request.getAmount(), "");
			responseCode = response.getResponseCode();
		}
		catch (Exception e)
		{
			throw e;
		}
		
		return responseCode;
	}


	public static void main(String args[])
	{
		CommandMessage request = new CommandMessage();
		request.setIsdn("84928110987");
		request.setChannel("SMS");
		request.setAmount(6000);
		try
		{
			SignatureCallConnection connection = new SignatureCallConnection();
			connection.setHost("http://10.8.39.106:80/mct/provisionapi/wsprovisioning.php");
			connection.setPort(80);
			connection.openConnection();

			int response = connection.register(request, 1, 1, 1, 1, 29, 123456789);
			// response = connection.unregister(request);
			// response = connection.reactive(request);
			// response = connection.renewal(request, 601);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
