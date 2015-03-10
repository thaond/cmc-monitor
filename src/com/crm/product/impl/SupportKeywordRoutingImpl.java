package com.crm.product.impl;

import java.util.ArrayList;
import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.OrderRoutingInstance;
import com.crm.subscriber.bean.SubscriberProduct;
import com.crm.subscriber.impl.SubscriberProductImpl;
import com.fss.util.AppException;

public class SupportKeywordRoutingImpl extends VNMOrderRoutingImpl {
	public CommandMessage checkCurrentServiceStatus(OrderRoutingInstance instance, ProductRoute orderRoute, CommandMessage order) throws Exception {
		CommandMessage result = super.parser(instance, orderRoute, order);

		if (result.getStatus() != Constants.ORDER_STATUS_DENIED) {
			ProductEntry product = ProductFactory.getCache().getProduct(orderRoute.getProductId());
			result.getParameters().setString("ListProductId", product.getParameters().getString("ListProductId", ""));
		}

		return result;
	}

	public CommandMessage checkDataPackageServiceStatus(OrderRoutingInstance instance, ProductRoute orderRoute, CommandMessage order) throws Exception {
		CommandMessage result = super.parser(instance, orderRoute, order);

		if (result.getStatus() != Constants.ORDER_STATUS_DENIED) {
			ProductEntry product = ProductFactory.getCache().getProduct(orderRoute.getProductId());
			result.getParameters().setString("ListDataPackageId", product.getParameters().getString("ListDataPackageId", ""));
		}

		return result;
	}

	public CommandMessage helpInforService(OrderRoutingInstance instance, ProductRoute orderRoute, CommandMessage order) throws Exception {
		CommandMessage result = super.parser(instance, orderRoute, order);
		if (result.getStatus() != Constants.ORDER_STATUS_DENIED) {
			String[] keyword = result.getKeyword().trim().split(" ");
			if(keyword.length == 1 ){
				result.setCause(result.getActionType() + ".success");
			}
			else if (keyword.length > 1) {
				result.setCause(keyword[1].trim().toUpperCase() + ".success");
			} else {
				result.setCause(result.getActionType() + Constants.ERROR_KEYWORD);
			}
		}

		return result;
	}

	public CommandMessage cancelMultiDataPackage(OrderRoutingInstance instance, ProductRoute orderRoute, CommandMessage order) throws Exception {

		CommandMessage result = super.parser(instance, orderRoute, order);
		/*super.smsParser(instance, orderRoute, order);
		VNMMessage result = CommandUtil.createVNMMessage(order);*/
		
		if (result.getStatus() != Constants.ORDER_STATUS_DENIED) {
			ProductEntry product = ProductFactory.getCache().getProduct(orderRoute.getProductId());
			result.getParameters().setString("ListDataPackageId", product.getParameters().getString("ListDataPackageId", ""));
		}

		return result;
	}

	public CommandMessage cancelMultiService(OrderRoutingInstance instance, ProductRoute orderRoute, CommandMessage order) throws Exception {
		CommandMessage result = super.parser(instance, orderRoute, order);

		if (result.getStatus() != Constants.ORDER_STATUS_DENIED) {
			ProductEntry product = ProductFactory.getCache().getProduct(orderRoute.getProductId());
			ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), product.getParameters().getString("ListServicesUnregister", ""));

			if (arrProduct.isEmpty()) {
				throw new AppException(Constants.ERROR_SUBSCRIBER_NOT_FOUND);
			}

			ProductEntry productEntry = null;
			String productName = "";
			String keyword = "";

			for (SubscriberProduct subProduct : arrProduct) {
				productEntry = ProductFactory.getCache().getProduct(subProduct.getProductId());
				productName = productEntry.getAlias().trim();
				keyword = "TERMINATE_" + productName;

				CommandMessage request = new CommandMessage();

				request.setRequestTime(new Date());
				request.setChannel("core");
				request.setServiceAddress(productName);
				request.setIsdn(result.getIsdn());
				request.setKeyword(keyword);
				request.setSubProductId(subProduct.getSubProductId());
				request.setTimeout(120000);

				QueueFactory.attachLocal(QueueFactory.ORDER_REQUEST_QUEUE, request);
			}
			// check error message
			// checkSuccessCancelService(instance, result, arrProduct);
		}

		return result;
	}
}
