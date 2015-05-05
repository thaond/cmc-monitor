package com.cmc.monitor.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbUtil {
	
	private static final Logger _LOGGER = Logger.getLogger(DbUtil.class);
	
	public static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				_LOGGER.error(e);
			}
		}
	}
	
	public static void closeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				_LOGGER.error(e);
			}
		}
	}
	
	public static void commitConnection(Connection conn) {
		try {
			conn.commit();
		} catch (SQLException e) {
			_LOGGER.error(e);
		}
	}
	
	public static void rollbackConnection(Connection conn) {
		try {
			conn.rollback();
		} catch (SQLException e) {
			_LOGGER.error(e);
		}
	}
	
	public static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				_LOGGER.error(e);
			}
		}
	}
}
