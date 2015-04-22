package com.cmc.monitor.thread;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableListener;

import com.cmc.monitor.entity.CableModem;
import com.cmc.monitor.entity.Cmts;
import com.cmc.monitor.util.DbUtil;
import com.cmc.monitor.util.OIdConstants;
import com.cmc.monitor.util.SnmpHelper;
import com.cmc.monitor.util.SnmpUtils;
import com.crm.thread.util.ThreadUtil;
import com.fss.util.AppException;

public class CableModemThread extends AbstractCmtsThread {

	private static final Logger _LOGGER = Logger.getLogger(CableModemThread.class);
	
	private static final String PARAM_SQL_GET_CABLE_MODEM = "sqlGetCableModem";
	private static final String PARAM_SQL_GET_ALL_CABLE_MODEMS = "sqlGetAllCableModem";
	private static final String PARAM_SQL_UPDATE_CABLE_MODEM = "sqlUpdateCableModem";
	private static final String PARAM_SQL_INSERT_CABLE_MODEM = "sqlInsertCableModem";
	private static final String PARAM_SQL_INSERT_CABLE_MODEM_HISTORY = "sqlInsertCableModemHistory";
	private static final String PARAM_SQL_BATCH_SIZE = "batchSize";
	private static final String PARAM_USING_CACHE = "usingCache";

//	protected String sqlGetCableModem = "SELECT * FROM CMTS_MONITOR_CableModem cm WHERE cm.macAddress = ?";
//	protected String sqlGetAllCableModem = "SELECT * FROM CMTS_MONITOR_CableModem";
//	protected String sqlUpdateCableModem = "UPDATE CMTS_MONITOR_CableModem SET modifiedDate = NOW(), fecUncorrectable = ?, fecCorrected = ?, microRef = ?, rxPower = ?, txPower = ?, usPower = ?, dsPower = ?, uncorrectables = ?, correcteds = ?, unerroreds = ?, dsSNR = ?, usSNR = ?, ucIfIndex = ?, dcIfIndex = ?, cmSubIndex = ?, cmtsId = ?, cmIndex = ?, status = ? WHERE macAddress = ?";
//	protected String sqlInsertCableModem = "INSERT INTO CMTS_MONITOR_CableModem (macAddress, createDate, modifiedDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, NOW(), NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
//	protected String sqlInsertCableModemHistory = "INSERT INTO CMTS_MONITOR_CableModemHistory (macAddress, createDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
	
	protected String sqlGetCableModem = "SELECT * FROM CMTS_MONITOR_CableModem cm WHERE cm.macAddress = ?";
	protected String sqlGetAllCableModem = "SELECT * FROM CMTS_MONITOR_CableModem";
	protected String sqlUpdateCableModem = "UPDATE CMTS_MONITOR_CableModem SET modifiedDate = NOW(), fecUncorrectable = ?, fecCorrected = ?, microRef = ?, rxPower = ?, txPower = ?, usPower = ?, dsPower = ?, uncorrectables = ?, correcteds = ?, unerroreds = ?, dsSNR = ?, usSNR = ?, ucIfIndex = ?, dcIfIndex = ?, cmSubIndex = ?, cmtsId = ?, cmIndex = ?, status = ? WHERE macAddress = ?";
	protected String sqlInsertCableModem = "INSERT INTO CMTS_MONITOR_CableModem (macAddress, createDate, modifiedDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, SYSDATE, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
	protected String sqlInsertCableModemHistory = "INSERT INTO CMTS_MONITOR_CableModemHistory (macAddress, createDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

	protected int batchSize = 1000;
	protected boolean usingCache = true;

	private Queue<CableModem> updateCableModemQueue = new ConcurrentLinkedQueue<CableModem>();
	private Queue<CableModem> insertCableModemQueue = new ConcurrentLinkedQueue<CableModem>();
	private Queue<CableModem> insertCableModemHistoryQueue = new ConcurrentLinkedQueue<CableModem>();

	private Map<String, CableModem> cableModemMap;

	private static OID[] OID_COLUMNS = { new OID(OIdConstants.CmMacAddress), new OID(OIdConstants.CmDcIfIndex),
			new OID(OIdConstants.CmUcIfIndex), new OID(OIdConstants.CmUsSnr), new OID(OIdConstants.CmUnerroreds),
			new OID(OIdConstants.CmCorrecteds), new OID(OIdConstants.CmUncorrectables), new OID(OIdConstants.CmIpAddress),
			new OID(OIdConstants.CmStatus), new OID(OIdConstants.CmRxPower) };

	@Override
	protected void processSession() throws Exception {
		log("Load all cmts entry - time: " + System.currentTimeMillis());
		List<Cmts> cmtses = getCmtses();
		if (usingCache) {
			log("Load all cable modem for caching! - Time: " + System.currentTimeMillis());
			cableModemMap = getAllCableModem();
		}
		
		synchronized (this.getClass()) {
			// Resource here ... fuck you for reading this.
			int proccessingCmts = 0;
			List<SnmpHelper> snmpHelpers = new ArrayList<SnmpHelper>();
			long startTime = System.currentTimeMillis();
			
			for (Cmts cmts : cmtses) {
				if (cmts.isEnable()) {
					try {
						proccessingCmts++;
						SnmpHelper helper = new SnmpHelper(cmts.getHost(), cmts.getCommunity());
						snmpHelpers.add(helper);
						
						log("Start process cmts \'" + cmts.getTitle() + "\' - time: " + System.currentTimeMillis());
						SnmpUtils.getTables(helper.getTarget(), helper.getSession(), OID_COLUMNS, new CableModelListener(cmts), this);
					} catch (Exception e) {
						_LOGGER.error("Error when start process cmts" + cmts.getTitle(), e);
						log("Error when start process cmts, check log file for detail, errorMessage: " + e.getMessage());
					}
				}
			}
			
			// waiting for all crowler finish
			while (proccessingCmts > 0) {
				wait();
				proccessingCmts--;
			}
			
			// release helpers
			for (SnmpHelper helper : snmpHelpers) {
				try {
					helper.close();
				} catch (IOException e) {
					_LOGGER.error("Error when close main helper " + helper.toString(), e);
					log("Error when close main helper " + helper.toString());
				}
			}
			long finishTime = System.currentTimeMillis();
			log("Finish getting all cable model info in " + (finishTime - startTime) + " ms");
			
		}
	
	}

	protected void updateCableModem(TableEvent event, Cmts cmts, SnmpHelper snmpHelper, boolean finished) {
		CableModem cm = new CableModem();

		cm.setCmtsId(cmts.getCmtsId());
		cm.setCmIndex(event.getIndex().last());
		cm.setCmSubIndex(event.getColumns()[0].getVariable().toSubIndex(true).toString());
		cm.setMacAddress(event.getColumns()[0].getVariable().toString());
		cm.setDcIfIndex(event.getColumns()[1].getVariable().toInt());
		cm.setUcIfIndex(event.getColumns()[2].getVariable().toInt());
		cm.setUsSNR(event.getColumns()[3].getVariable().toInt());
		cm.setUnerroreds(event.getColumns()[4].getVariable().toLong());
		cm.setCorrecteds(event.getColumns()[5].getVariable().toLong());
		cm.setUncorrectables(event.getColumns()[6].getVariable().toLong());
		cm.setIpAddress(event.getColumns()[7].getVariable().toString());
		cm.setStatus(event.getColumns()[8].getVariable().toInt());
		cm.setRxPower(event.getColumns()[9].getVariable().toInt());

		// update Ds/Us power, dssnr/ micref
		OID[] oids = { (new OID(OIdConstants.CmDsPower)).append(cm.getCmSubIndex()),
				(new OID(OIdConstants.CmUsPower)).append(cm.getCmSubIndex()), (new OID(OIdConstants.CmDsSnr)).append(cm.getCmSubIndex()),
				(new OID(OIdConstants.CmMicroreflections).append(cm.getCmSubIndex())) };

		try {
			ResponseEvent resEvent = snmpHelper.get(oids);
			if (resEvent != null) {
				cm.setDsPower(resEvent.getResponse().get(0).getVariable().toInt());
				cm.setUsPower(resEvent.getResponse().get(1).getVariable().toInt());
				cm.setTxPower(resEvent.getResponse().get(1).getVariable().toInt());
				cm.setDsSNR(resEvent.getResponse().get(2).getVariable().toInt());
				cm.setMircroRef(resEvent.getResponse().get(3).getVariable().toLong());
			}
		} catch (IOException e) {
			_LOGGER.error("Error when get usPower, dsPower, dsSNR, MicroRef for cable modem " + cm.getMacAddress(), e);
			log("Error when get usPower, dsPower, dsSNR, MicroRef for cable modem " + cm.getMacAddress());
		}

		// Caculate FECs

		CableModem lastCm = null;

		if (usingCache) {
			lastCm = cableModemMap.get(cm.getMacAddress());
			if (lastCm == null) lastCm = getCableModem(cm.getMacAddress());
		} else {
			lastCm = getCableModem(cm.getMacAddress());
		}

		if (lastCm == null)
			lastCm = new CableModem();

		double unerroreds = (double) (cm.getUnerroreds() - lastCm.getUnerroreds());
		double correcteds = (double) (cm.getCorrecteds() - lastCm.getCorrecteds());
		double uncorrectables = (double) (cm.getUncorrectables() - lastCm.getUncorrectables());

		double fecCorrected = (correcteds / (unerroreds + correcteds + uncorrectables)) * 100;
		double fecUncorrectable = (uncorrectables / (unerroreds + correcteds + uncorrectables)) * 100;
		
		if (fecCorrected > 1000000 || fecUncorrectable > 1000000) {
			log("What the fuck: cm=" + cm.toString() + " lastCm = " + lastCm.toString());
		}

		// validate double
		if (Double.isNaN(fecCorrected) || Double.isInfinite(fecCorrected))
			fecCorrected = 0;
		if (Double.isNaN(fecUncorrectable) || Double.isInfinite(fecUncorrectable))
			fecUncorrectable = 0;

		cm.setFecCorrected(fecCorrected);
		cm.setFecUncorrectable(fecUncorrectable);

		// update to database
		upsertCableModemToDb(cm, finished, cmts);
	}

	protected CableModem getCableModem(String macAdress) {
		CableModem cm = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();

			pstmt = conn.prepareStatement(sqlGetCableModem);
			pstmt.setString(1, macAdress);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				cm = new CableModem();
				cm.setMacAddress(rs.getString("macAddress"));
				cm.setCreateDate(rs.getDate("createDate"));
				cm.setModifiedDate(rs.getDate("modifiedDate"));
				cm.setFecUncorrectable(rs.getDouble("fecUncorrectable"));
				cm.setFecCorrected(rs.getDouble("fecCorrected"));
				cm.setMircroRef(rs.getLong("microRef"));
				cm.setRxPower(rs.getInt("rxPower"));
				cm.setTxPower(rs.getInt("txPower"));
				cm.setUsPower(rs.getInt("usPower"));
				cm.setDsPower(rs.getInt("dsPower"));
				cm.setUncorrectables(rs.getLong("uncorrectables"));
				cm.setCorrecteds(rs.getLong("correcteds"));
				cm.setUnerroreds(rs.getLong("unerroreds"));
				cm.setDsSNR(rs.getInt("dsSNR"));
				cm.setUsSNR(rs.getInt("usSNR"));
				cm.setUcIfIndex(rs.getInt("ucIfIndex"));
				cm.setDcIfIndex(rs.getInt("dcIfIndex"));
				cm.setCmSubIndex(rs.getString("cmSubIndex"));
				cm.setCmtsId(rs.getLong("cmtsId"));
				cm.setCmIndex(rs.getInt("cmIndex"));
				cm.setStatus(rs.getInt("status"));
			}

		} catch (Exception e) {
			_LOGGER.error("Error when getting CableModem from db with macAdddress = " + macAdress, e);
			log("Error when getting CableModem from db with macAdddress = " + macAdress + ". Check log for detail. Message: "
					+ e.getMessage());
		} finally {
			DbUtil.closeConnection(conn);
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(pstmt);
		}

		return cm;
	}

	protected Map<String, CableModem> getAllCableModem() {
		Map<String, CableModem> mapCapbles = new HashMap<String, CableModem>();

		try (Connection conn = getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sqlGetAllCableModem)) {

			while (rs.next()) {
				CableModem cm = new CableModem();
				cm.setMacAddress(rs.getString("macAddress"));
				cm.setCreateDate(rs.getDate("createDate"));
				cm.setModifiedDate(rs.getDate("modifiedDate"));
				cm.setFecUncorrectable(rs.getDouble("fecUncorrectable"));
				cm.setFecCorrected(rs.getDouble("fecCorrected"));
				cm.setMircroRef(rs.getLong("microRef"));
				cm.setRxPower(rs.getInt("rxPower"));
				cm.setTxPower(rs.getInt("txPower"));
				cm.setUsPower(rs.getInt("usPower"));
				cm.setDsPower(rs.getInt("dsPower"));
				cm.setUncorrectables(rs.getLong("uncorrectables"));
				cm.setCorrecteds(rs.getLong("correcteds"));
				cm.setUnerroreds(rs.getLong("unerroreds"));
				cm.setDsSNR(rs.getInt("dsSNR"));
				cm.setUsSNR(rs.getInt("usSNR"));
				cm.setUcIfIndex(rs.getInt("ucIfIndex"));
				cm.setDcIfIndex(rs.getInt("dcIfIndex"));
				cm.setCmSubIndex(rs.getString("cmSubIndex"));
				cm.setCmtsId(rs.getLong("cmtsId"));
				cm.setCmIndex(rs.getInt("cmIndex"));
				cm.setStatus(rs.getInt("status"));

				mapCapbles.put(cm.getMacAddress(), cm);
			}

		} catch (Exception e) {
			_LOGGER.error("Error when get all CableModem from db", e);
			log("Error when get all CableModem from db. Check log for detail. Message: " + e.getMessage());
		}

		return mapCapbles;
	}

	protected void upsertCableModemToDb(CableModem cableModem, boolean finished, Cmts cmts) {
		CableModem cm = getCableModem(cableModem.getMacAddress());

		if (cm == null) {
			insertCableModemQueue.add(cableModem);
		} else {
			updateCableModemQueue.add(cableModem);
		}
		insertCableModemHistoryQueue.add(cableModem);

		insertCableModemToDB(cmts, finished);
		updateCableModemToDB(cmts, finished);
		insertCableModemHistoryToDB(cmts, finished);
	}

	protected void finishAll(Cmts cmts) {
		insertCableModemToDB(cmts, true);
		insertCableModemHistoryToDB(cmts, true);
		updateCableModemToDB(cmts, true);
	}

	protected void insertCableModemToDB(Cmts cmts, boolean finished) {
		if (insertCableModemQueue.size() == batchSize || finished) {
			int size = insertCableModemQueue.size() <= batchSize ? insertCableModemQueue.size() : batchSize;
			List<CableModem> cms = new ArrayList<CableModem>();

			for (int i = 0; i < size; i++) {
				CableModem cm = insertCableModemQueue.poll();
				if (cm != null) {
					cms.add(cm);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlInsertCableModem)) {
				for (CableModem cm : cms) {

					pstmt.setString(1, cm.getMacAddress());
					pstmt.setDouble(2, cm.getFecUncorrectable());
					pstmt.setDouble(3, cm.getFecCorrected());
					pstmt.setLong(4, cm.getMircroRef());
					pstmt.setInt(5, cm.getRxPower());
					pstmt.setInt(6, cm.getTxPower());
					pstmt.setInt(7, cm.getUsPower());
					pstmt.setInt(8, cm.getDsPower());
					pstmt.setLong(9, cm.getUncorrectables());
					pstmt.setLong(10, cm.getCorrecteds());
					pstmt.setLong(11, cm.getUnerroreds());
					pstmt.setInt(12, cm.getDsSNR());
					pstmt.setInt(13, cm.getUsSNR());
					pstmt.setInt(14, cm.getUcIfIndex());
					pstmt.setInt(15, cm.getDcIfIndex());
					pstmt.setString(16, cm.getCmSubIndex());
					pstmt.setLong(17, cm.getCmtsId());
					pstmt.setInt(18, cm.getCmIndex());
					pstmt.setInt(19, cm.getStatus());

					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("Inserting CableModem has been finished for cmts \'" + cmts.getTitle() + "\' - Time: " + System.currentTimeMillis());
				}
			} catch (Exception e) {
				_LOGGER.error("Error when trying insert CableModems for cmts \'" + cmts.getTitle() + "\'.", e);
				log("Error when trying insert CableModems for cmts \'" + cmts.getTitle() + "\'. Check log for detail - ErrorMessage: "
						+ e.getMessage());
			}
		}
	}

	protected void insertCableModemHistoryToDB(Cmts cmts, boolean finished) {
		if (insertCableModemHistoryQueue.size() == batchSize || finished) {
			int size = insertCableModemHistoryQueue.size() <= batchSize ? insertCableModemHistoryQueue.size() : batchSize;
			List<CableModem> cms = new ArrayList<CableModem>();

			for (int i = 0; i < size; i++) {
				CableModem cm = insertCableModemHistoryQueue.poll();
				if (cm != null) {
					cms.add(cm);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlInsertCableModemHistory)) {
				for (CableModem cm : cms) {

					pstmt.setString(1, cm.getMacAddress());
					pstmt.setDouble(2, cm.getFecUncorrectable());
					pstmt.setDouble(3, cm.getFecCorrected());
					pstmt.setLong(4, cm.getMircroRef());
					pstmt.setInt(5, cm.getRxPower());
					pstmt.setInt(6, cm.getTxPower());
					pstmt.setInt(7, cm.getUsPower());
					pstmt.setInt(8, cm.getDsPower());
					pstmt.setLong(9, cm.getUncorrectables());
					pstmt.setLong(10, cm.getCorrecteds());
					pstmt.setLong(11, cm.getUnerroreds());
					pstmt.setInt(12, cm.getDsSNR());
					pstmt.setInt(13, cm.getUsSNR());
					pstmt.setInt(14, cm.getUcIfIndex());
					pstmt.setInt(15, cm.getDcIfIndex());
					pstmt.setString(16, cm.getCmSubIndex());
					pstmt.setLong(17, cm.getCmtsId());
					pstmt.setInt(18, cm.getCmIndex());
					pstmt.setInt(19, cm.getStatus());

					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("Inserting CableModemHistory has been finished for cmts \'" + cmts.getTitle() + "\' - Time: "
							+ System.currentTimeMillis());
				}
			} catch (Exception e) {
				_LOGGER.error("Error when trying insert CableModemHistories for cmts \'" + cmts.getTitle() + "\'.", e);
				log("Error when trying insert CableModemHistories for cmts \'" + cmts.getTitle()
						+ "\'. Check log for detail - ErrorMessage: " + e.getMessage());
			}
		}
	}

	protected void updateCableModemToDB(Cmts cmts, boolean finished) {
		if (updateCableModemQueue.size() == batchSize || finished) {
			int size = updateCableModemQueue.size() <= batchSize ? updateCableModemQueue.size() : batchSize;
			List<CableModem> cms = new ArrayList<CableModem>();

			for (int i = 0; i < size; i++) {
				CableModem cm = updateCableModemQueue.poll();
				if (cm != null) {
					cms.add(cm);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlUpdateCableModem)) {
				for (CableModem cm : cms) {

					pstmt.setDouble(1, cm.getFecUncorrectable());
					pstmt.setDouble(2, cm.getFecCorrected());
					pstmt.setLong(3, cm.getMircroRef());
					pstmt.setInt(4, cm.getRxPower());
					pstmt.setInt(5, cm.getTxPower());
					pstmt.setInt(6, cm.getUsPower());
					pstmt.setInt(7, cm.getDsPower());
					pstmt.setLong(8, cm.getUncorrectables());
					pstmt.setLong(9, cm.getCorrecteds());
					pstmt.setLong(10, cm.getUnerroreds());
					pstmt.setInt(11, cm.getDsSNR());
					pstmt.setInt(12, cm.getUsSNR());
					pstmt.setInt(13, cm.getUcIfIndex());
					pstmt.setInt(14, cm.getDcIfIndex());
					pstmt.setString(15, cm.getCmSubIndex());
					pstmt.setLong(16, cm.getCmtsId());
					pstmt.setInt(17, cm.getCmIndex());
					pstmt.setInt(18, cm.getStatus());
					pstmt.setString(19, cm.getMacAddress());

					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("Updating CableModem has been finished for cmts \'" + cmts.getTitle() + "\' - Time: " + System.currentTimeMillis());
				}
			} catch (Exception e) {
				_LOGGER.error("Error when trying update CableModems for cmts \'" + cmts.getTitle() + "\'.", e);
				log("Error when trying update CableModems for cmts \'" + cmts.getTitle() + "\'. Check log for detail - ErrorMessage: "
						+ e.getMessage());
			}
		}
	}

	/**
	 * @author richard
	 */
	class CableModelListener implements TableListener {

		private final Cmts cmts;
		private volatile boolean finished = false;
		private SnmpHelper snmpHelper;

		public CableModelListener(Cmts cmts) {
			this.cmts = cmts;
			try {
				snmpHelper = new SnmpHelper(cmts.getHost(), cmts.getCommunity());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean next(TableEvent event) {
			try {
				updateCableModem(event, cmts, snmpHelper, false);
			} catch (Exception e) {
				_LOGGER.error("Error when next", e);
			}
			return true;
		}

		@Override
		public synchronized void finished(TableEvent event) {
			if ((event.getStatus() == TableEvent.STATUS_OK) && (event.getIndex() != null)) {
				updateCableModem(event, cmts, snmpHelper, true);
			} else {
				finishAll(cmts);
			}
			try {
				snmpHelper.close();
			} catch (IOException e) {
				//throw new RuntimeException(e);
			}
			finished = true;
			
			if (event.getUserObject() != null) {
				synchronized (event.getUserObject()) {
					event.getUserObject().notify();
				}
			}
		}

		@Override
		public boolean isFinished() {
			return finished;
		}
	}
	
	/* Thread parameters */
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getParameterDefinition() {
		Vector vector = super.getParameterDefinition();
		
		vector.addElement(ThreadUtil.createTextParameter(PARAM_SQL_GET_ALL_CABLE_MODEMS, 1000, "SQL Query - get all cable modem."));
		vector.addElement(ThreadUtil.createTextParameter(PARAM_SQL_GET_CABLE_MODEM, 1000, "SQL Query - get cable modem by mac address."));
		vector.addElement(ThreadUtil.createTextParameter(PARAM_SQL_INSERT_CABLE_MODEM, 1000, "SQL Query - insert cable modem"));
		vector.addElement(ThreadUtil.createTextParameter(PARAM_SQL_INSERT_CABLE_MODEM_HISTORY, 1000, "SQL Query - insert cable modem history"));
		vector.addElement(ThreadUtil.createTextParameter(PARAM_SQL_UPDATE_CABLE_MODEM, 1000, "SQL Query - update cable modem by mac address"));
		vector.addElement(ThreadUtil.createIntegerParameter(PARAM_SQL_BATCH_SIZE, "Batch size insert to db"));
		vector.addElement(ThreadUtil.createBooleanParameter(PARAM_USING_CACHE, "Cache cable modem to process or no"));
		
		return vector;
	}
	
	@Override
	public void fillParameter() throws AppException {
		super.fillParameter();
		
		this.sqlGetAllCableModem = ThreadUtil.getString(this, PARAM_SQL_GET_ALL_CABLE_MODEMS, true, "SELECT * FROM CMTS_MONITOR_CableModem");
		this.sqlGetCableModem = ThreadUtil.getString(this, PARAM_SQL_GET_CABLE_MODEM, true, "SELECT * FROM CMTS_MONITOR_CableModem cm WHERE cm.macAddress = ?");
		this.sqlInsertCableModem = ThreadUtil.getString(this, PARAM_SQL_INSERT_CABLE_MODEM, true, "INSERT INTO CMTS_MONITOR_CableModem (macAddress, createDate, modifiedDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, SYSDATE, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
		this.sqlInsertCableModemHistory = ThreadUtil.getString(this, PARAM_SQL_INSERT_CABLE_MODEM_HISTORY, true, "INSERT INTO CMTS_MONITOR_CableModemHistory (macAddress, createDate, fecUncorrectable, fecCorrected, microRef, rxPower, txPower, usPower, dsPower, uncorrectables, correcteds, unerroreds, dsSNR, usSNR, ucIfIndex, dcIfIndex, cmSubIndex, cmtsId, cmIndex, status ) VALUES (?, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
		this.sqlUpdateCableModem = ThreadUtil.getString(this, PARAM_SQL_UPDATE_CABLE_MODEM, true, "UPDATE CMTS_MONITOR_CableModem SET modifiedDate = NOW(), fecUncorrectable = ?, fecCorrected = ?, microRef = ?, rxPower = ?, txPower = ?, usPower = ?, dsPower = ?, uncorrectables = ?, correcteds = ?, unerroreds = ?, dsSNR = ?, usSNR = ?, ucIfIndex = ?, dcIfIndex = ?, cmSubIndex = ?, cmtsId = ?, cmIndex = ?, status = ? WHERE macAddress = ?");
		this.batchSize = ThreadUtil.getInt(this, PARAM_SQL_BATCH_SIZE, 1000);
		this.usingCache = ThreadUtil.getBoolean(this, PARAM_USING_CACHE, true);
	}

}
