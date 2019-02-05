package com.jps.finserv.markets.analytics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.sql.Date;
import java.time.LocalDate;
//import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.equities.DataPreMarket;
import com.jps.finserv.markets.util.Utils;

public class DailyMarketPrediction {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DailyMarketPrediction.class);

	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "root";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "Iso885912@";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	/** The name of the database we are testing with (this default is installed with MySQL) */
	private final String dbName = "markets";

	/** Create instance of Utils class */
	Utils utils = new Utils();

	/** Instantiate Pre-Market data retrieval class	 */
	DataPreMarket preMarket = new DataPreMarket();

	/**
	 * Entry program for the class
	 * @param dbName
	 * @param tblName
	 * @param queryTable
	 */

	public void run(){
		String tblName = createTable(dbName, "", "");
		boolean populated = populateTable(dbName, tblName, "");
		if (populated)
			insertPreMarketQuotes(tblName);
		//dropTable(dbName, tblName);

	}
	/**
	 * 
	 * @param query Query that will generate list of symbols
	 * @return A map of symbol as keys and their pre-market values as targets
	 */
	public Map<String, String> insertPreMarketQuotes(String tblName){
		Map<String, String> mapSymbolValue = new LinkedHashMap<String, String>();
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		try {
			String date = getLastTradingDate();
			mapSymbolValue = preMarket.run("", date);
			Set<String> setSymbols = mapSymbolValue.keySet();
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String qInsert = "UPDATE "+tblName+" SET preMarket=?, pcntChange=? WHERE symbol like ?";
			PreparedStatement pstmt = conn.prepareStatement(qInsert);

			for (String symbol : setSymbols){
				String strReturned = mapSymbolValue.get(symbol).trim();
				String[] temp = strReturned.split(",");

				if ((!strReturned.matches(",")) && (temp.length >= 2))	{
					String strLastClosing = temp[0];
					String strPcntChange = temp[1];

					if ((strLastClosing.isEmpty()) || (strLastClosing ==  ""))
						pstmt.setNull(1, Types.DOUBLE);
					else{
						double value = Double.parseDouble(strLastClosing);
						pstmt.setObject(1, value, JDBCType.DOUBLE);
					}

					if ((strPcntChange.isEmpty()) || (strPcntChange ==  ""))
						pstmt.setNull(2, Types.DOUBLE);
					else{
						double value = Double.parseDouble(strPcntChange);
						pstmt.setObject(2, value, JDBCType.DOUBLE);
					}

					//pstmt.setObject(1, tblName, JDBCType.VARCHAR);
					pstmt.setObject(3, symbol, JDBCType.VARCHAR);

					try{
						//logger.debug("Executing update: "+pstmt.toString()); 
						int n = pstmt.executeUpdate();
					}
					catch (SQLIntegrityConstraintViolationException e) {
						//logger.info("Data already in catalog for for symbol: '"+symbol+"' for the quarter:"+quarter[i]);
						logger.info("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Making assumption that data is already in catalog for symbol: '"+symbol+"'");
					}
				}
			}
			pstmt.close();
			conn.close();
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}

		return mapSymbolValue;
	}
	/**
	 * Generic function to create SQL table
	 * @param dbName
	 * @param tblName
	 * @param queryTable
	 * @return
	 */
	public String createTable(String dbName, String tblName, String queryTable){
		boolean tblCreated = false;
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		String date = utils.getDate().replaceAll("-", "");
		if ((tblName.isEmpty()) && (tblName == ""))
			tblName = "dailymarket_"+date;

		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			if ((queryTable.isEmpty()) && (queryTable == "")) 
				queryTable = 	"CREATE TABLE "+tblName+" (symbol VARCHAR(10) NOT NULL, lastClosing DOUBLE(9,3), preMarket DOUBLE(9,3), pcntChange DOUBLE(5,2), exchange VARCHAR (20), date DATE, time TIME(3), PRIMARY KEY(symbol))";
			PreparedStatement pstmt = conn.prepareStatement(queryTable);
			logger.info("Executing query: "+queryTable);
			pstmt.execute();
			ResultSet rs1 = pstmt.executeQuery("show tables");

			while (rs1.next()){
				String tblTemp = rs1.getString(1);
				//logger.debug(tblTemp);
				if (tblTemp.matches(tblName))
					tblCreated = 	true;
			}
			if (tblCreated )
				logger.info("Created table '"+tblName+"'");
			pstmt.close();
			conn.close();

		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}
		return tblName; 
	}

	/**
	 * Populates data in the Pre Market Opening table with closing values from last trading day 
	 * @param dbName
	 * @param tblName
	 * @param query
	 * @return
	 */
	public boolean populateTable(String dbName, String tblName, String query){
		boolean populated = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		// The table name will use today's date but data needs to come from last trading day
		String today = utils.getDate();
		if ((tblName.isEmpty()) && (tblName == ""))
			tblName = "dailymarket_"+today;

		String lastTradingDate = getLastTradingDate();
		
		
		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			if ((query.isEmpty()) && (query == "")) 
				query = 	"INSERT INTO "+tblName+ " (symbol, lastClosing, exchange)  select symbol, close, exchange from equities_2018 where date like '"+lastTradingDate+"' ";
			pstmt = conn.prepareStatement(query);
			logger.info("Executing query: "+query);
			int n = pstmt.executeUpdate();
			if (n > 0){
				logger.info("Inserted data from last trading day into table.");
				populated = true;
			}
			pstmt.close();
			conn.close();
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}
		return populated;
	}
	/**
	 * Drops the given table
	 * @param dbName
	 * @param tblName
	 * @return
	 */
	public boolean dropTable(String dbName, String tblName){

		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String stmt = "Drop Table "+tblName;
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			logger.info("Executing query: "+stmt);
			pstmt.execute();
			ResultSet rs2 = pstmt.executeQuery("show tables");
			while (rs2.next()){
				String tblTemp = rs2.getString(1);
				//logger.debug(tblTemp);
				if (tblTemp.matches(tblName)){
					pstmt.close();
					conn.close();
					return false;
				}
			}
			pstmt.close();
			conn.close();
			logger.info("Dropped Table '"+tblName+"'");
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}
		return true; 
	}

	/**
	 * Retrieves the date for the most recent trading day
	 * @param dbName
	 * @param tblName
	 * @return
	 */
	public String getLastTradingDate(){
		//String date = "";
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		Date sqlDate = Date.valueOf(utils.getDate());
		LocalDate date = sqlDate.toLocalDate();

		//Date date = Date.valueOf(utils.getDate());

		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			PreparedStatement pstmt = null;
			for (int i = 0; i < 5; i++){
				String stmt = "select * from equities_2018 where date like '"+date.toString()+"'";
				pstmt = conn.prepareStatement(stmt);
				logger.debug("Executing query: "+stmt);
				ResultSet rs2 = pstmt.executeQuery();
				if (rs2.next()){
					pstmt.close();
					conn.close();
					logger.info("Returning last trading date as: "+date.toString());
					return date.toString();
				}
				else{
					date = date.minusDays(1);
					logger.debug("No market records retrieved. Setting date to a day earlier: "+date); 
				}
			}
			pstmt.close();
			conn.close();
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}
		return date.toString(); 
	}


}