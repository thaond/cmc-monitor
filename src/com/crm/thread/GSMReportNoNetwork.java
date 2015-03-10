package com.crm.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.crm.kernel.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;

public class GSMReportNoNetwork extends MailThread
{
	protected String SQL = "";
	protected String content = "";

	protected PreparedStatement _stmtSelect = null;
	protected PreparedStatement _stmtUpdate = null;
	private Connection connection = null;
	{

	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(createParameterDefinition("SQL", "",
				ParameterType.PARAM_TEXTAREA_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("MailFormat", "",
				ParameterType.PARAM_TEXTAREA_MAX, "100", ""));
		
		vtReturn.addAll(super.getParameterDefinition());
		
		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();
	
			// Fill parameter
			SQL = loadMandatory("SQL");
			content = loadMandatory("MailFormat");
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			connection = Database.getConnection();
			_stmtSelect = connection.prepareStatement(SQL);
			
			String strSQL = "Update CellularReport set sendFlag = 1 where reportId = ?";
			_stmtUpdate = connection.prepareStatement(strSQL);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		try
		{
			Database.closeObject(_stmtSelect);
			Database.closeObject(_stmtUpdate);
			Database.closeObject(connection);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			super.afterProcessSession();
		}
	}
	
	public void doProcessSession() throws Exception
	{
		ResultSet rs = null;
		try
		{
			logMonitor("Start send mail report");
			
			rs = _stmtSelect.executeQuery();
			StringBuilder body = new StringBuilder();
			
			int count = 0;
			while (rs.next())
			{
				body.append("<tr>");
				body.append("<td>" + rs.getString("isdn") + "</td>");
				body.append("<td>" + rs.getString("cellId") + "</td>");
				body.append("<td>" + rs.getString("latitude") + "</td>");
				body.append("<td>" + rs.getString("longitude") + "</td>");
				body.append("<td>" + rs.getString("reportDate") + "</td>");
				body.append("<td>" + (rs.getInt("status") == 1 ? "Ok" : "Not Ok") + "</td>");
				body.append("</tr>");
				
				_stmtUpdate.setLong(1, rs.getLong("reportId"));
				_stmtUpdate.addBatch();
				
				count++;
			}
			
			body.append("<tr>");
			body.append("<td>&nbsp;</td>");
			body.append("<td>&nbsp;</td>");
			body.append("<td>&nbsp;</td>");
			body.append("<td>&nbsp;</td>");
			body.append("<td>&nbsp;</td>");
			body.append("<td>&nbsp;</td>");
			body.append("</tr>");
			
			content = content.replaceAll("<=%Content=%>", body.toString());
			
			Date date = new Date();
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH");
			
			if (count > 0)
			{
				sendEmail(getSubject() + df.format(date), content, null);
				
				_stmtUpdate.executeBatch();
				connection.commit();
			}
			else
			{
				logMonitor("No data to report");
			}
			
			logMonitor("Finish send mail report");
			
			storeConfig();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			Database.closeObject(rs);
		}
	}
}
