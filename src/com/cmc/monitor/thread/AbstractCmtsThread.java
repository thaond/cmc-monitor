package com.cmc.monitor.thread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cmc.monitor.entity.Cmts;
import com.cmc.monitor.util.DbUtil;
import com.fss.thread.ManageableThread;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public abstract class AbstractCmtsThread extends ManageableThread {
	
	private static final Logger _LOGGER = Logger.getLogger(AbstractCmtsThread.class);
	
	protected ComboPooledDataSource cpds;
	//protected String driverClass = "com.mysql.jdbc.Driver";
	//protected String jdbcUrl = "jdbc:mysql://localhost:3306/lportal?useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false";
	//protected String dbUser = "lportal";
	//protected String dbPassword = "thanhlong";
	
	protected String driverClass = "oracle.jdbc.driver.OracleDriver";
	protected String jdbcUrl = "jdbc:oracle:thin:@172.20.24.76:1521:CTI";
	protected String dbUser = "cmts";
	protected String dbPassword = "cmts";
	
	protected int dbMinPoolSize = 5;
	protected int dbAcquireIncrement = 5;
	protected int dbMaxPoolSize = 100;
	protected int dbMaxStatements = 180;
	
	private String sqlGetCmtses = "SELECT * FROM CMTS_MONITOR_Cmts";
	
	@Override
	protected void beforeSession() throws Exception {
		super.beforeSession();
		
		if (cpds == null) {
			log("Prepare db datasource ... time: " + System.currentTimeMillis());
			cpds = new ComboPooledDataSource();
			cpds.setDriverClass(driverClass);
			cpds.setJdbcUrl(jdbcUrl);
			cpds.setUser(dbUser);
			cpds.setPassword(dbPassword);
			cpds.setMinPoolSize(dbMinPoolSize);
			cpds.setAcquireIncrement(dbAcquireIncrement);
			cpds.setMaxPoolSize(dbMaxPoolSize);
			cpds.setMaxStatements(dbMaxStatements);
		}
	}
	
	@Override
	public Connection getConnection() {
		try {
			return cpds.getConnection();
		} catch (SQLException e) {
			_LOGGER.error("Error when try getting database connection", e);
			log("Error when getConnection() - ErrorMessage: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		
		log("Destroying... ");
		if (cpds != null) {
			cpds.hardReset();
		}
	}
	
	protected List<Cmts> getCmtses() {
		List<Cmts> cmtses = new ArrayList<>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlGetCmtses);
			
			while (rs.next()) {
				Cmts cmts = new Cmts();
				
				cmts.setCmtsId(rs.getLong("cmtsId"));
				cmts.setTitle(rs.getString("title"));
				cmts.setHost(rs.getString("host"));
				cmts.setCommunity(rs.getString("community"));
				cmts.setEnable(rs.getBoolean("enable"));
				
				cmtses.add(cmts);
			}
		} catch (Exception e) {
			_LOGGER.error("Error when try getting all Cmts from database", e);
			log("Error when getAllCmts() - ErrorMessage: " + e.getMessage());
			try {
				log("Number connection: " + cpds.getNumConnections(dbUser, dbPassword));
				log("Number idle connection: " + cpds.getNumIdleConnections(dbUser, dbPassword));
				log("Number busy connection: " + cpds.getNumBusyConnections(dbUser, dbPassword));
				cpds.softReset(dbUser, dbPassword);
				log("Number unclose connection: " + cpds.getNumUnclosedOrphanedConnections(dbUser, dbPassword));
				
			} catch (SQLException ex) {
				_LOGGER.error("Error when monitor c3p0 conn pool", ex);
				log("Error when monitor c3p0 conn pool: " + ex.getMessage());
			}
			
		} finally {
			DbUtil.closeConnection(conn);
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(stmt);
		}
		
		return cmtses;
	}
}
