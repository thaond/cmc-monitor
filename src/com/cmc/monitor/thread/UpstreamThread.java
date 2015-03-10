package com.cmc.monitor.thread;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableListener;

import com.cmc.monitor.entity.Cmts;
import com.cmc.monitor.entity.UpstreamChannel;
import com.cmc.monitor.util.DbUtil;
import com.cmc.monitor.util.OIdConstants;
import com.cmc.monitor.util.SnmpUtils;

public class UpstreamThread extends AbstractCmtsThread {

	private static final Logger _LOGGER = Logger.getLogger(UpstreamThread.class);

	protected String sqlGetUpstream = "SELECT * FROM CMTS_MONITOR_UpstreamChannel us WHERE us.ifIndex = ? AND us.cmtsId = ?";
	protected String sqlGetAvgPowers = "SELECT AVG(cm.microRef) avgMicRef, AVG(cm.rxPower) avgRxPower, AVG(cm.txPower) avgTxPower, AVG(cm.usPower) avgUsPower, AVG(cm.dsPower) avgDsPower, AVG(cm.dsSNR) avgDsSNR FROM CMTS_MONITOR_CableModem cm WHERE cm.cmtsId = ? AND cm.ucIfIndex = ? AND cm.status = 6";
	protected String sqlUpdateUpstreamChannel = "UPDATE CMTS_MONITOR_UpstreamChannel SET modifiedDate = NOW(), qam = ?, avgOnlineCmDsPower = ?, avgOnlineCmUsPower = ?, avgOnlineCmDsSNR = ?, avgOnlineCmTxPower = ?, avgOnlineCmRxPower = ?, fecUncorrectable = ?, fecCorrected = ?, upChannelCmTotal = ?, upChannelCmRegistered = ?, upChannelCmActive = ?, upChannelModProfile = ?, upChannelWidth  = ?, upChannelFrequency  = ?, ifSigQUncorrectables = ?, ifSigQCorrecteds = ?, ifSigQUnerroreds = ?, ifSigQSNR = ?, ifAlias = ?, ifDesc = ? WHERE ifIndex = ? AND cmtsId = ?";
	protected String sqlInsertUnstreamChannel = "INSERT INTO CMTS_MONITOR_UpstreamChannel (ifIndex, cmtsId, createDate , modifiedDate, qam, avgOnlineCmDsPower, avgOnlineCmUsPower, avgOnlineCmDsSNR, avgOnlineCmTxPower, avgOnlineCmRxPower, fecUncorrectable, fecCorrected, upChannelCmTotal, upChannelCmRegistered, upChannelCmActive, upChannelModProfile, upChannelWidth , upChannelFrequency , ifSigQUncorrectables, ifSigQCorrecteds, ifSigQUnerroreds, ifSigQSNR, ifAlias, ifDesc) VALUES (?, ?, NOW(), NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	protected String sqlInsertUpstreamChannelHistory = "INSERT INTO CMTS_MONITOR_UpstreamChannelHistory (ifIndex, cmtsId, createDate, qam, avgOnlineCmDsPower, avgOnlineCmUsPower, avgOnlineCmDsSNR, avgOnlineCmTxPower, avgOnlineCmRxPower, fecUncorrectable, fecCorrected, upChannelCmTotal, upChannelCmRegistered, upChannelCmActive, upChannelModProfile, upChannelWidth , upChannelFrequency , ifSigQUncorrectables, ifSigQCorrecteds, ifSigQUnerroreds, ifSigQSNR, ifAlias, ifDesc) VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	protected int batchSize = 1000;

	private Queue<UpstreamChannel> updateUpstreamChannelQueue = new ConcurrentLinkedQueue<UpstreamChannel>();
	private Queue<UpstreamChannel> insertUpstreamChannelQueue =  new ConcurrentLinkedQueue<UpstreamChannel>();
	private Queue<UpstreamChannel> insertUpstreamChannelHistoryQueue =  new ConcurrentLinkedQueue<UpstreamChannel>();

	private static OID[] OID_COLUMNS = { new OID(OIdConstants.IfSigQSnr), new OID(OIdConstants.IfSigQUnerroreds),
			new OID(OIdConstants.IfSigQCorrecteds), new OID(OIdConstants.IfSigQUncorrectables), new OID(OIdConstants.UpChannelFrequency),
			new OID(OIdConstants.UpChannelWidth), new OID(OIdConstants.UpChannelModulationProfile),
			new OID(OIdConstants.UpChannelCmActive), new OID(OIdConstants.UpChannelCmRegistered), new OID(OIdConstants.UpChannelCmTotal) };

	@Override
	protected void processSession() throws Exception {
		log("processing session...");
		// Get all Cmts
		List<Cmts> cmtses = getCmtses();
		log("Count cmts: " + cmtses.size());
		cmtses.forEach(cmts -> {
			if (cmts.isEnable()) {
				try {
					SnmpUtils.getTables("udp:" + cmts.getHost() + "/161", cmts.getCommunity(), OID_COLUMNS, new UpstreamListener(cmts));
				} catch (Exception e) {
					_LOGGER.error("Error when get upstream information for cmts: " + cmts.getHost(), e);
					log(e.getMessage());
				}
			}
		});

	}

	protected void updateUpstreamChannel(TableEvent event, Cmts cmts, boolean finished) {
		if (event.getStatus() == TableEvent.STATUS_OK) {
			UpstreamChannel us = new UpstreamChannel();

			us.setCmtsId(cmts.getCmtsId());
			us.setIfIndex(event.getIndex().last());
			us.setIfSigQSNR(event.getColumns()[0].getVariable().toLong());
			us.setIfSigQUnerroreds(event.getColumns()[1].getVariable().toLong());
			us.setIfSigQCorrecteds(event.getColumns()[2].getVariable().toLong());
			us.setIfSigQUncorrectables(event.getColumns()[3].getVariable().toLong());
			us.setUpChannelFrequency(event.getColumns()[4].getVariable().toLong());
			us.setUpChannelWidth(event.getColumns()[5].getVariable().toLong());
			us.setUpChannelModProfile(event.getColumns()[6].getVariable().toInt());
			us.setUpChannelCmActive(event.getColumns()[7].getVariable().toInt());
			us.setUpChannelCmRegisterd(event.getColumns()[8].getVariable().toInt());
			us.setUpChannelCmTotal(event.getColumns()[9].getVariable().toInt());

			// update IfAlias and IfDesc
			OID[] oids = { (new OID(OIdConstants.Ifalias)).append(us.getIfIndex()), (new OID(OIdConstants.Ifdescr)).append(us.getIfIndex()) };

			try {
				ResponseEvent resEvent = SnmpUtils.get("udp:" + cmts.getHost() + "/161", cmts.getCommunity(), oids);
				if (resEvent != null) {
					us.setIfAlias(resEvent.getResponse().get(0).getVariable().toString());
					us.setIfDesc(resEvent.getResponse().get(1).getVariable().toString());
				}
			} catch (IOException e) {
				_LOGGER.error("Error when get if Alias and ifDesc for " + us.getIfIndex(), e);
				log("ERROR: " + e.getMessage());
			}

			// Calculate FEC
			UpstreamChannel lastUs = getUpstreamChannel(us.getIfIndex(), cmts.getCmtsId());
			if (lastUs == null) lastUs = new UpstreamChannel();
			double unerroreds = (double) (us.getIfSigQUnerroreds() - lastUs.getIfSigQUnerroreds());
			double correcteds = (double) (us.getIfSigQCorrecteds() - lastUs.getIfSigQCorrecteds());
			double uncorrectables = (double) (us.getIfSigQUncorrectables() - lastUs.getIfSigQUncorrectables());

			double fecCorrected = (correcteds / unerroreds) * 100;
			double fecUncorrectable = (uncorrectables / unerroreds) * 100;
			
			// validate doubles
			if (Double.isNaN(fecCorrected) || Double.isInfinite(fecCorrected)) fecCorrected = 0;
			if (Double.isNaN(fecUncorrectable) || Double.isInfinite(fecUncorrectable)) fecUncorrectable = 0;

			us.setFecCorrected(fecCorrected);
			us.setFecUncorrectable(fecUncorrectable);

			// Calculate AVG Powers
			double[] avgValues = getAvgOnlineCmPowers(us.getIfIndex(), cmts.getCmtsId());
			us.setAvgOnlineCmDsSNR(avgValues[6]);
			us.setAvgOnlineCmRxPower(avgValues[1]);
			us.setAvgOnlineCmTxPower(avgValues[2]);
			us.setAvgOnlineCmUsPower(avgValues[3]);
			us.setAvgOnlineCmDsPower(avgValues[4]);
			us.setAvgOnlineCmMicRef(avgValues[0]);

			// update to database
			upsertUpstreamChannelToDb(us, finished, cmts);
		}
	}

	protected void upsertUpstreamChannelToDb(UpstreamChannel upstreamChannel, boolean finished, Cmts cmts) {
		UpstreamChannel us = getUpstreamChannel(upstreamChannel.getIfIndex(), upstreamChannel.getCmtsId());

		if (us == null) {
			insertUpstreamChannelQueue.add(upstreamChannel);
		} else {
			updateUpstreamChannelQueue.add(upstreamChannel);
		}
		insertUpstreamChannelHistoryQueue.add(upstreamChannel);

		insertUpstreamChannelToDb(finished, cmts);
		updateUpstreamChannelToDb(finished, cmts);
		insertUpstreamChannelHistoryToDb(finished, cmts);
	}

	protected synchronized void insertUpstreamChannelToDb(boolean finished, Cmts cmts) {
		if (insertUpstreamChannelQueue.size() == batchSize || finished) {
			List<UpstreamChannel> ups = new ArrayList<UpstreamChannel>();

			for (int i = 0; i < batchSize; i++) {
				UpstreamChannel us = insertUpstreamChannelQueue.poll();
				if (us != null) {
					ups.add(us);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlInsertUnstreamChannel)) {
				for (UpstreamChannel us : ups) {

					pstmt.setInt(1, us.getIfIndex()); // ifIndex,
					pstmt.setLong(2, us.getCmtsId());// cmtsId,
					pstmt.setString(3, us.getQam()); // qam,
					pstmt.setDouble(4, us.getAvgOnlineCmDsPower()); // avgOnlineCmDsPower,
					pstmt.setDouble(5, us.getAvgOnlineCmUsPower()); // avgOnlineCmUsPower,
					pstmt.setDouble(6, us.getAvgOnlineCmDsSNR());// avgOnlineCmDsSNR,
					pstmt.setDouble(7, us.getAvgOnlineCmTxPower());// avgOnlineCmTxPower,
					pstmt.setDouble(8, us.getAvgOnlineCmRxPower());// avgOnlineCmRxPower,
					pstmt.setDouble(9, us.getFecUncorrectable());// fecUncorrectable,
					pstmt.setDouble(10, us.getFecCorrected());// fecCorrected,
					pstmt.setInt(11, us.getUpChannelCmTotal());// upChannelCmTotal,
					pstmt.setInt(12, us.getUpChannelCmRegisterd()); // upChannelCmRegistered,
					pstmt.setInt(13, us.getUpChannelCmActive());// upChannelCmActive,
					pstmt.setInt(14, us.getUpChannelModProfile()); // upChannelModProfile,
					pstmt.setLong(15, us.getUpChannelWidth());// upChannelWidth
					pstmt.setLong(16, us.getUpChannelFrequency());// upChannelFrequency
					pstmt.setLong(17, us.getIfSigQUncorrectables());// ifSigQUncorrectables,
					pstmt.setLong(18, us.getIfSigQCorrecteds());// ifSigQCorrecteds,
					pstmt.setLong(19, us.getIfSigQUnerroreds());// ifSigQUnerroreds,
					pstmt.setDouble(20, us.getIfSigQSNR());// ifSigQSNR,
					pstmt.setString(21, us.getIfAlias());// ifAlias,
					pstmt.setString(22, us.getIfDesc());// ifDesc
					
					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("Add UpstreamChannel of cmtsId " + cmts.getCmtsId() + " has been insert - time: " + System.currentTimeMillis());
				}
			} catch (SQLException e) {
				_LOGGER.error("Error when insert upstream channel", e);
				log("Error when insert upstream channel: " + e.getMessage());
			}
		}

	}

	protected synchronized void updateUpstreamChannelToDb(boolean finished, Cmts cmts) {
		if (updateUpstreamChannelQueue.size() == batchSize || finished) {
			List<UpstreamChannel> ups = new ArrayList<UpstreamChannel>();
			
			for (int i = 0; i < batchSize; i++) {
				UpstreamChannel us = updateUpstreamChannelQueue.poll();
				if (us != null) {
					ups.add(us);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlUpdateUpstreamChannel)) {
				for (UpstreamChannel us : ups) {

					pstmt.setString(1, us.getQam()); // qam,
					pstmt.setDouble(2, us.getAvgOnlineCmDsPower()); // avgOnlineCmDsPower,
					pstmt.setDouble(3, us.getAvgOnlineCmUsPower()); // avgOnlineCmUsPower,
					pstmt.setDouble(4, us.getAvgOnlineCmDsSNR());// avgOnlineCmDsSNR,
					pstmt.setDouble(5, us.getAvgOnlineCmTxPower());// avgOnlineCmTxPower,
					pstmt.setDouble(6, us.getAvgOnlineCmRxPower());// avgOnlineCmRxPower,
					pstmt.setDouble(7, us.getFecUncorrectable());// fecUncorrectable,
					pstmt.setDouble(8, us.getFecCorrected());// fecCorrected,
					pstmt.setInt(9, us.getUpChannelCmTotal());// upChannelCmTotal,
					pstmt.setInt(10, us.getUpChannelCmRegisterd()); // upChannelCmRegistered,
					pstmt.setInt(11, us.getUpChannelCmActive());// upChannelCmActive,
					pstmt.setInt(12, us.getUpChannelModProfile()); // upChannelModProfile,
					pstmt.setLong(13, us.getUpChannelWidth());// upChannelWidth
					pstmt.setLong(14, us.getUpChannelFrequency());// upChannelFrequency
					pstmt.setLong(15, us.getIfSigQUncorrectables());// ifSigQUncorrectables,
					pstmt.setLong(16, us.getIfSigQCorrecteds());// ifSigQCorrecteds,
					pstmt.setLong(17, us.getIfSigQUnerroreds());// ifSigQUnerroreds,
					pstmt.setDouble(18, us.getIfSigQSNR());// ifSigQSNR,
					pstmt.setString(19, us.getIfAlias());// ifAlias,
					pstmt.setString(20, us.getIfDesc());// ifDesc
					pstmt.setInt(21, us.getIfIndex()); // ifIndex,
					pstmt.setLong(22, us.getCmtsId());// cmtsId,

					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("All UpstreamChannel of cmtsId " + cmts.getCmtsId() + " has been update - time: " + System.currentTimeMillis());
				}
			} catch (SQLException e) {
				_LOGGER.error("Error when update upstream channe", e);
				log("Error when update upstream channel: " + e.getMessage());
			}
		}
	}

	protected synchronized void insertUpstreamChannelHistoryToDb(boolean finished, Cmts cmts) {
		if (insertUpstreamChannelHistoryQueue.size() == batchSize || finished) {
			List<UpstreamChannel> ups = new ArrayList<UpstreamChannel>();
			
			for (int i = 0; i < batchSize; i++) {
				UpstreamChannel us = insertUpstreamChannelHistoryQueue.poll();
				if (us != null) {
					ups.add(us);
				}
			}

			try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlInsertUpstreamChannelHistory)) {
				for (UpstreamChannel us : ups) {
					pstmt.setInt(1, us.getIfIndex()); // ifIndex,
					pstmt.setLong(2, us.getCmtsId());// cmtsId,
					pstmt.setString(3, us.getQam()); // qam,
					pstmt.setDouble(4, us.getAvgOnlineCmDsPower()); // avgOnlineCmDsPower,
					pstmt.setDouble(5, us.getAvgOnlineCmUsPower()); // avgOnlineCmUsPower,
					pstmt.setDouble(6, us.getAvgOnlineCmDsSNR());// avgOnlineCmDsSNR,
					pstmt.setDouble(7, us.getAvgOnlineCmTxPower());// avgOnlineCmTxPower,
					pstmt.setDouble(8, us.getAvgOnlineCmRxPower());// avgOnlineCmRxPower,
					pstmt.setDouble(9, us.getFecUncorrectable());// fecUncorrectable,
					pstmt.setDouble(10, us.getFecCorrected());// fecCorrected,
					pstmt.setInt(11, us.getUpChannelCmTotal());// upChannelCmTotal,
					pstmt.setInt(12, us.getUpChannelCmRegisterd()); // upChannelCmRegistered,
					pstmt.setInt(13, us.getUpChannelCmActive());// upChannelCmActive,
					pstmt.setInt(14, us.getUpChannelModProfile()); // upChannelModProfile,
					pstmt.setLong(15, us.getUpChannelWidth());// upChannelWidth
					pstmt.setLong(16, us.getUpChannelFrequency());// upChannelFrequency
					pstmt.setLong(17, us.getIfSigQUncorrectables());// ifSigQUncorrectables,
					pstmt.setLong(18, us.getIfSigQCorrecteds());// ifSigQCorrecteds,
					pstmt.setLong(19, us.getIfSigQUnerroreds());// ifSigQUnerroreds,
					pstmt.setDouble(20, us.getIfSigQSNR());// ifSigQSNR,
					pstmt.setString(21, us.getIfAlias());// ifAlias,
					pstmt.setString(22, us.getIfDesc());// ifDesc

					pstmt.addBatch();
				}

				pstmt.executeBatch();

				if (finished) {
					log("Add UpstreamChannelHistory of cmtsId " + cmts.getCmtsId() + " has been insert _ time: " + System.currentTimeMillis());
				}
			} catch (SQLException e) {
				_LOGGER.error("Error when insert upstream channel history", e);
				log("Error when insert upstream channel history: " + e.getMessage());
			}
		}
	}
	
	protected void finishAll(Cmts cmts) {
		insertUpstreamChannelHistoryToDb(true, cmts);
		insertUpstreamChannelToDb(true, cmts);
		updateUpstreamChannelToDb(true, cmts);
	}

	protected double[] getAvgOnlineCmPowers(int upIfIndex, long cmtsId) {
		double[] results = new double[6];

		Connection conn = null;
		PreparedStatement ptst = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
			ptst = conn.prepareStatement(sqlGetAvgPowers);
			ptst.setLong(1, cmtsId);
			ptst.setInt(2, upIfIndex);
			rs = ptst.executeQuery();

			if (rs.next()) {
				results[0] = rs.getDouble("avgMicRef");
				results[1] = rs.getDouble("avgRxPower");
				results[2] = rs.getDouble("avgTxPower");
				results[3] = rs.getDouble("avgUsPower");
				results[4] = rs.getDouble("avgDsPower");
				results[6] = rs.getDouble("avgDsSNR");
			}

		} catch (SQLException e) {
			_LOGGER.error("Error UpstreamThread.getAvgOnlineCmPowers(" + upIfIndex + ", " + cmtsId +")", e);
			log("Error when getAvgOnlineCm" + e.getMessage());
		} finally {
			DbUtil.closeConnection(conn);
			DbUtil.closeStatement(ptst);
			DbUtil.closeResultSet(rs);
		}

		return results;
	}

	protected UpstreamChannel getUpstreamChannel(int ifIndex, long cmtsId) {
		UpstreamChannel us = null;

		Connection conn = null;
		PreparedStatement ptmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			ptmt = conn.prepareStatement(sqlGetUpstream);
			ptmt.setInt(1, ifIndex);
			ptmt.setLong(2, cmtsId);

			rs = ptmt.executeQuery();

			if (rs.next()) {
				us = new UpstreamChannel();

				us.setIfDesc(rs.getString("ifDesc"));
				us.setIfAlias(rs.getString("ifAlias"));
				us.setIfSigQSNR(rs.getDouble("ifSigQSNR"));
				us.setIfSigQUnerroreds(rs.getLong("ifSigQUnerroreds"));
				us.setIfSigQCorrecteds(rs.getLong("ifSigQCorrecteds"));
				us.setIfSigQUncorrectables(rs.getLong("ifSigQUncorrectables"));
				us.setUpChannelWidth(rs.getLong("upChannelWidth"));
				us.setUpChannelModProfile(rs.getInt("upChannelModProfile"));
				us.setUpChannelCmActive(rs.getInt("upChannelCmActive"));
				us.setUpChannelCmRegisterd(rs.getInt("upChannelCmRegistered"));
				us.setUpChannelCmTotal(rs.getInt("upChannelCmTotal"));
				us.setFecCorrected(rs.getDouble("fecCorrected"));
				us.setFecUncorrectable(rs.getDouble("fecUncorrectable"));
				us.setAvgOnlineCmDsSNR(rs.getDouble("avgOnlineCmDsSNR"));
				us.setAvgOnlineCmRxPower(rs.getDouble("avgOnlineCmRxPower"));
				us.setAvgOnlineCmTxPower(rs.getDouble("avgOnlineCmTxPower"));
				us.setAvgOnlineCmDsPower(rs.getDouble("avgOnlineCmDsPower"));
				us.setAvgOnlineCmUsPower(rs.getDouble("avgOnlineCmUsPower"));
				us.setQam(rs.getString("qam"));
				us.setModifiedDate(rs.getDate("modifiedDate"));
				us.setCreateDate(rs.getDate("createDate"));
				us.setCmtsId(rs.getLong("cmtsId"));
				us.setIfIndex(rs.getInt("ifIndex"));
			}

		} catch (SQLException e) {
			_LOGGER.error("Error UpstreamThread.getUpstreamChannel(" + ifIndex + ", " + cmtsId + ")", e);
			log("Error when get UpstreamChannel: " + e.getMessage());
		} finally {
			DbUtil.closeConnection(conn);
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(ptmt);
		}

		return us;
	}

	protected class UpstreamListener implements TableListener {

		private final Cmts cmts;
		private volatile boolean finished = false;

		public UpstreamListener(Cmts cmts) {
			this.cmts = cmts;
		}

		@Override
		public boolean next(TableEvent event) {
			try {
				updateUpstreamChannel(event, cmts, false);
			} catch (Exception e) {
				_LOGGER.error("Error when next", e);
			}
			return true;
		}

		@Override
		public synchronized void finished(TableEvent event) {
			if ((event.getStatus() == TableEvent.STATUS_OK) && (event.getIndex() != null)) {
				updateUpstreamChannel(event, cmts, true);
			} else {
				finishAll(cmts);
			}
			
			finished = true;
		}

		@Override
		public boolean isFinished() {
			return finished;
		}

	}
}
