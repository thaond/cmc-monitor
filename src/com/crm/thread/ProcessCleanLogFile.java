package com.crm.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import sun.net.ftp.impl.FtpClient;

import com.crm.kernel.io.FileUtil;
import com.fss.thread.ManageableThread;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.util.AppException;
import com.fss.util.WildcardFilter;



public class ProcessCleanLogFile extends ManageableThread
{
	@SuppressWarnings("rawtypes")
	protected Vector vtDir = new Vector();
	protected int mDays = 2;
	protected String mstrWildcard = ".log";
	protected String mstrWildcardZip = ".gz";
	protected SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	protected String hostStoreFileZip =""; 
	protected String user ="";
	protected String pass ="";
	protected int port = 22;
	protected String destFolder ="";
	protected boolean deleteAfterMove = false;
	protected boolean fileTransferMode = false;
	
	public ProcessCleanLogFile()
	{
		sdf.setLenient(false);
	}
	@SuppressWarnings("rawtypes")
	public void fillParameter() throws AppException
	{
		super.fillParameter();
		mDays = this.loadInteger("BeforeToday");
		mstrWildcard = this.loadMandatory("WildCard");
		mstrWildcardZip = this.loadMandatory("WildCardZip");
		Object obj = getParameter("Log-Dir");
		if(obj != null && obj instanceof Vector)
		{
			vtDir = (Vector)obj;
		}
		hostStoreFileZip = this.loadMandatory("HostStoreFileZip");
		user = this.loadMandatory("User");
		pass = this.loadMandatory("Pass");
		port = this.loadInteger("Port");
		deleteAfterMove = this.loadMandatory("DeleteAfterMove").toUpperCase().equals("YES") ? true : false;
		fileTransferMode = this.loadMandatory("FileTransferMode").toUpperCase().equals("YES") ? true : false;
		destFolder = this.loadMandatory("DestinationFolder");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();
		Vector vtParam = new Vector();
		vtParam.addElement(ParameterUtil.createParameterDefinition("Path","",ParameterType.PARAM_TEXTBOX_MAX,"50","Path dir","0"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("Log-Dir","",ParameterType.PARAM_TABLE,vtParam));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("BeforeToday","",ParameterType.PARAM_TEXTBOX_MASK,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("WildCard","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("WildCardZip","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("HostStoreFileZip","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("User","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("Pass","",ParameterType.PARAM_PASSWORD,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("Port","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("DeleteAfterMove","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("DestinationFolder","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("FileTransferMode","",ParameterType.PARAM_TEXTBOX_MAX,"99"));
		
		vtReturn.addAll(super.getParameterDefinition());
		
		return vtReturn;
	}

	public void processSession() throws Exception
	{
		try
		{	
			fillLogFile();
			for(int i = 0;i < vtDir.size();i++)
			{
				@SuppressWarnings("rawtypes")
				String strPath = (String)((Vector)vtDir.elementAt(i)).elementAt(0);
				if(strPath == null || strPath.length() == 0)
				{
					continue;
				}
				
				File f = new File(strPath);
				if(f.exists())
				{
					if(f.isFile())
					{	
						processGzipFile(f);
					}
					else if(f.isDirectory())
					{
						processGzipDirectory(f);
					}
				}
				if(fileTransferMode)
				{
					copyFileToServer(strPath, destFolder + strPath);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * processGzipDirectory
	 *
	 * @param f File
	 */
	private void processGzipDirectory(File f)
	{
		String[] lFile = f.list(new WildcardFilter(mstrWildcard));
		
		for(int i = 0;i < lFile.length;i++)
		{
			String vstrFile = f.getAbsolutePath() + File.separator + lFile[i];
			
			File vf = new File(vstrFile);
			if(!vf.isFile() || vstrFile.length() < 8)
			{
				logMonitor("Igoned file " + vstrFile);
				continue;
			}

			File tempFile = new File(vstrFile);
			if (tempFile.exists())
			{
				Date dte = new Date(tempFile.lastModified());
				logMonitor(dte.toString());
				Date dteNow = new Date();
				dte = com.fss.util.DateUtil.addDay(dte,mDays);
				if(dte.after(dteNow))
				{
					logMonitor("Igoned file " + vstrFile);
					continue;
				}
				processGzipFile(vf);
			}
		}
	}

	/**
	 * processGzipFile
	 *
	 * @param f File
	 */
	private void processGzipFile(File f)
	{
		try
		{
			logMonitor("Gzip file:" + f.getAbsolutePath());
			com.fss.util.SmartZip.GZip(f.getAbsolutePath(),f.getAbsolutePath() + ".gz");
			FileUtil.deleteFile(f.getAbsolutePath());
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
			logMonitor("Could not Gzip file " + f.getAbsolutePath());
			logMonitor(ex.getMessage());
		}
	}
	
	public void copyFileToServer(String sourceFolder, String destFolder)
	{
		File folder = new File(sourceFolder);
		if (folder.exists())
		{
			String [] listFile = folder.list(new  WildcardFilter(mstrWildcardZip));
			for (int i = 0; i < listFile.length; i++)
			{
				FileUtil.copyFile(sourceFolder + listFile[i], destFolder + listFile[i]);
				if(deleteAfterMove)
				{
					FileUtil.deleteFile(sourceFolder + "/" + listFile[i]);
				}
			}
		}
	}
	public void sendFileToServer(String sourceFolder, String destFolder,
			String host, String username, String pass, int port) throws IOException
	{
		
	}
	
}
