package com.jps.finserv.markets.equities;

import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modReadFile;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.Properties;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Alternate sources of this information are here: 
 * 	https://www.cnbc.com/commodities/
 * 	https://www.investing.com/commodities/gold-historical-data
 * 		For investing.com, just get to the price of any commodity and then append "historical-data" at the end to gather old market data. 
 * 		Then change the period within calendar and then programmatically click the button "Download Data".  
 */
public class DataDailyIndicatorHistorical {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataDailyIndicatorHistorical.class);
	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "psharma";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "$ys8dmin";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	/** The name of the database */
	private final String dbName = "markets";

	/** The name of the database */
	private final String tblName = "dailyindicators_historic_data";

	Utils utils = new Utils();

	// Retrieving data here: https://www.investing.com/indices/volatility-s-p-500-historical-data
	public void run(){
		//TODO: Create a map here with entity as key and filename as value
		String entity = "CrudeOil";
		//entity = "Dollar";
		//entity = "Gold";
		//prepDataForInsert ("GoldHistorical.csv", entity, dbName, tblName);// Separates using double quotes. Use for Gold only and there also expect exceptions since old data is using comma and not double quotes  
		prepDataForInsertUsingCommaSeparator("CrudeOilHistorical.csv", entity, dbName, tblName); // Separates using comma. Use for CrudeOil, VIX, Dollar
		entity = "VIX";
		prepDataForInsertUsingCommaSeparator("VixHistorical.csv", entity, dbName, tblName); // Separates using comma. Use for CrudeOil, VIX, Dollar
	}

	/** 
	 * DONT use this function for CrudeOil and VIX
	 * Prepares and inserts data into SQL
	 * @param fileCompanyFinancialData
	 * @param entity
	 * @param dbName
	 * @param tblName
	 */
	public void prepDataForInsert (String fileCompanyFinancialData, String entity, String dbName, String tblName){
		List<String> inputList  = modReadFile.readFileIntoListOfStrings(fileCompanyFinancialData);
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countProcessed = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		logger.debug("The size of the returned list is:" + inputList .size());
		if ((inputList == null) || (inputList .size() == 1)){
			logger.error("File \""+fileCompanyFinancialData+"\" does not have any rows to be inserted into the database.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
		{
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);

				boolean heading = false;
				for (String line : inputList){ 
					if (heading){
						heading = false;
						continue;
					}
					if (line == "")
						continue;
					//logger.debug(line);
					++countProcessed;
					// Need to split it against double quotes going by the nature of the data
					// Example data instance: 15-Mar-18,"1,317.80","1,324.70","1,328.10","1,314.90",257.89K,-0.59%
					String[] temp = line.split("\"");
					lineError = line;
					//String name = temp[0].trim();
					int volume = 0;
					Date date = Date.valueOf(utils.adjustDateFormat(temp[0].trim()));
					double price = Double.parseDouble(temp[1].replaceAll(",", "").trim());
					double open = Double.parseDouble(temp[3].replaceAll(",", "").trim());
					double high = Double.parseDouble(temp[5].replaceAll(",", "").trim());
					double low = Double.parseDouble(temp[7].replaceAll(",", "").trim());
					/*
					 * Since we had to parse this based on double quotes, need to 
					 * take in the last value and further parse it based on comma.  
					 */
					String[] tupleVolPcntchange = (temp[8]).trim().split(",");

					// volume comes in the form of either "-" if unavailable or like 257.89K. Need to account for all that here. 
					String tempVolString = (tupleVolPcntchange[1]).trim();
					if (tempVolString.contains("-"))
						volume = 0;
					else if (tempVolString.contains("K")){
						tempVolString  = tempVolString .replace("K","");
						double dblVolume = Double.parseDouble(tempVolString) * 1000;
						volume = (int)(Math.round(dblVolume));
					}
					else {// We haven't seen any other format, so can't account for it nor should assume anything. 
						logger.warn("The daily volume not reported in multiples of thousands. Setting volume to NULL.");
						logger.warn(lineError);
					}

					double pcntChange = Double.parseDouble(tupleVolPcntchange[2].replaceAll("%", "").trim());

					String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement pstmt = conn.prepareStatement(statement);
					// Set symbol, the only Stirng element
					pstmt.setString(1, entity);
					// Set the only integer element
					if (volume ==  0)
						pstmt.setNull(7, Types.INTEGER);
					else
						pstmt.setInt(7, volume);
					// Now all the double elements
					pstmt.setDouble(2, price); pstmt.setDouble(3, open); pstmt.setDouble(4, high); pstmt.setDouble(5, low); 
					pstmt.setDouble(6, pcntChange);
					// Finally the date
					pstmt.setDate(8, date);

					try{
						pstmt.execute();
					}
					catch (SQLIntegrityConstraintViolationException e) {
						logger.info("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Making assumption that data is already in catalog for entity: '"+entity+"' for date:"+date);
						++countSkipped;
					}
					pstmt.close();

				}
				conn.close();
				logger.info("Inserted historical dataset into database for entity: "+entity);
				logger.info("Total # of records inserted: "+countProcessed);
				logger.info("Total # of records skipped (were already present) for insert: "+countSkipped);
			}
			catch (SQLException e) {
				logger.error("SQLException: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
				//logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
				e.printStackTrace();}
			catch (Exception e) {
				logger.error("Non-SQL Exception: << "+e.getMessage()+ " >> while traversing following line item:\""+lineError+"\"");
				e.printStackTrace();
			}
		}
	}

	/*
	 * 5/10: USe this function to insert values for CrudeOil, VIX
	 * TODO: Take this function out after generalizing the insert using either comma or double quotes
	 */
	public void prepDataForInsertUsingCommaSeparator (String fileCompanyFinancialData, String entity, String dbName, String tblName){
		List<String> inputList  = modReadFile.readFileIntoListOfStrings(fileCompanyFinancialData);
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countProcessed = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		logger.debug("The size of the returned list is:" + inputList .size());
		if ((inputList == null) || (inputList .size() == 1)){
			logger.error("File \""+fileCompanyFinancialData+"\" does not have any rows to be inserted into the database.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
		{
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);

				boolean heading = false;
				for (String line : inputList){ 
					if (heading){
						heading = false;
						continue;
					}
					if (line == "")
						continue;
					//logger.debug(line);
					++countProcessed;
					String[] temp = line.split(",");
					lineError = line;
					//String name = temp[0].trim();
					int volume = 0;
					Date date = Date.valueOf(utils.adjustDateFormat(temp[0].trim()));
					double price = Double.parseDouble(temp[1].replaceAll(",", "").trim());
					double open = Double.parseDouble(temp[2].replaceAll(",", "").trim());
					double high = Double.parseDouble(temp[3].replaceAll(",", "").trim());
					double low = Double.parseDouble(temp[4].replaceAll(",", "").trim());
					// volume comes in the form of either "-" if unavailable or like 257.89K. Need to account for all that here. 
					String tempVolString = temp[5].replaceAll(",", "").trim();
					if (tempVolString.contains("-"))
						volume = 0;
					else if (tempVolString.contains("K")){
						tempVolString  = tempVolString .replace("K","");
						double dblVolume = Double.parseDouble(tempVolString) * 1000;
						volume = (int)(Math.round(dblVolume));
					}
					else {// We haven't seen any other format, so can't account for it nor should assume anything. 
						logger.warn("The daily volume not reported in multiples of thousands. Setting volume to NULL.");
						logger.warn(lineError);
					}

					double pcntChange = Double.parseDouble(temp[6].replaceAll("%", "").trim());

					String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement pstmt = conn.prepareStatement(statement);
					// Set symbol, the only Stirng element
					pstmt.setString(1, entity);
					// Set the only integer element
					if (volume ==  0)
						pstmt.setNull(7, Types.INTEGER);
					else
						pstmt.setInt(7, volume);
					// Now all the double elements
					pstmt.setDouble(2, price); pstmt.setDouble(3, open); pstmt.setDouble(4, high); pstmt.setDouble(5, low); 
					pstmt.setDouble(6, pcntChange);
					// Finally the date
					pstmt.setDate(8, date);

					try{
						pstmt.execute();
					}
					catch (SQLIntegrityConstraintViolationException e) {
						logger.info("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Making assumption that data is already in catalog for entity: '"+entity+"' for date:"+date);
						++countSkipped;
					}
					pstmt.close();

				}
				conn.close();
				logger.info("Inserted historical dataset into database for entity: "+entity);
				logger.info("Total # of records inserted: "+countProcessed);
				logger.info("Total # of records skipped (were already present) for insert: "+countSkipped);
			}
			catch (SQLException e) {
				logger.error("SQLException: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
				//logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
				e.printStackTrace();}
			catch (Exception e) {
				logger.error("Non-SQL Exception: << "+e.getMessage()+ " >> while traversing following line item:\""+lineError+"\"");
				e.printStackTrace();
			}
		}
	}

}
