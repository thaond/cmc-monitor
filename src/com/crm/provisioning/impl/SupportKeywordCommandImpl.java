package com.crm.provisioning.impl;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.crm.kernel.message.Constants;
import com.crm.kernel.sql.Database;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductMessage;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.message.VNMMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.provisioning.util.CommandUtil;
import com.crm.provisioning.util.ResponseUtil;
import com.crm.subscriber.bean.SubscriberProduct;
import com.crm.subscriber.impl.SubscriberProductImpl;

public class SupportKeywordCommandImpl extends CommandImpl
{
	public VNMMessage checkServiceStatus(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		try
		{
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			String listProductId = product.getParameters().getString("ListProductId", "");

			ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listProductId);

			if (arrProduct.isEmpty())
			{
				result.setCause(Constants.ERROR_SERVICE_LIST_NOT_FOUND);
				return result;
			}

			ProductMessage productMessage = null;

			ProductEntry productCache = null;
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			for (SubscriberProduct subProduct : arrProduct)
			{
				productCache = ProductFactory.getCache().getProduct(subProduct.getProductId());

				productMessage = product.getProductMessage(result.getActionType(), result.getCampaignId(), result.getLanguageId(),
						result.getChannel(), productCache.getAlias() + ".active");

				if (productMessage != null)
				{
					String content = productMessage.getContent();

					content = content.replaceAll("~SERVICE_START_DATE~", sdf.format(subProduct.getRegisterDate()));
					content = content.replaceAll("~SERVICE_PRICE~", "" + productCache.getPrice());
					content = content.replaceAll("~SERVICE_AMOUNT~", "" + productCache.getParameters().getLong(product.getAlias() + ".amount", 0));

					CommandUtil.sendSMS(instance, result, content);
					Thread.sleep(1000);
				}
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}

		return result;
	}

	public VNMMessage checkDataStatus(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception
	{

		VNMMessage result = CommandUtil.createVNMMessage(request);
		try
		{
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			String listDataPackageId = product.getParameters().getString("ListDataPackageId", "");

			ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listDataPackageId);

			if (arrProduct.isEmpty())
			{
				result.setCause(Constants.ERROR_PACKAGE_LIST_NOT_FOUND);
				return result;
			}

			ProductMessage productMessage = null;
			ProductEntry productCache = null;

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			for (SubscriberProduct subProduct : arrProduct)
			{
				productCache = ProductFactory.getCache().getProduct(subProduct.getProductId());

				productMessage = product.getProductMessage(result.getActionType(), result.getCampaignId(), result.getLanguageId(),
						result.getChannel(), productCache.getAlias() + ".active");

				if (productMessage != null)
				{
					String content = productMessage.getContent();

					result.setResponseValue(ResponseUtil.SERVICE_START_DATE, sdf.format(subProduct.getRegisterDate()));
					result.setResponseValue(ResponseUtil.SERVICE_PRICE, productCache.getPrice());
					result.setResponseValue(ResponseUtil.SERVICE_AMOUNT, product.getParameters().getLong(productCache.getAlias() + ".amount", 0));

					CommandUtil.sendSMS(instance, result, content);
					Thread.sleep(1000);
				}
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}

		return result;
	}

	public VNMMessage cancelMultiDataPackage(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);
		Connection connection = null;
		ArrayList<SubscriberProduct> arrProduct = null;

		try
		{
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			String listDataPackageId = product.getParameters().getString("ListDataPackageId", "");

			arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listDataPackageId);

			if (arrProduct == null || arrProduct.isEmpty())
			{
				result.setCause(Constants.ERROR_PACKAGE_LIST_NOT_FOUND);
				return result;
			}

			connection = Database.getConnection();
			String listActivePackage = getStrProductId(arrProduct);

			SubscriberProductImpl.unregisterMulti(connection, result.getIsdn(), listActivePackage);
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}
		finally
		{
			Database.closeObject(connection);
		}
		return result;
	}

	public String getStrProductId(ArrayList<SubscriberProduct> arrList)
	{
		String str = "";
		for (int i = 0; i < arrList.size(); i++)
		{
			if (i <= arrList.size() - 2)
			{
				str += (arrList.get(i).getProductId() + ", ");
			}
			else
				str += (arrList.get(i).getProductId());
		}
		return str;
	}

	public String getStrProductAlias(ArrayList<ProductEntry> arrList)
	{
		String str = "";
		for (int i = 0; i < arrList.size(); i++)
		{
			if (i <= arrList.size() - 2)
			{
				str += (arrList.get(i).getAlias().toString() + ", ");
			}
			else
				str += (arrList.get(i).getAlias().toString());
		}
		return str;
		// return arrList.toString().replaceAll("\\[|\\]", "");
	}

}
