package com.jps.finserv.markets.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.util.modOutCSV;
import com.jps.finserv.markets.util.modReadFile;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DBInsertMovingAverages extends DBConnectMySql {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBInsertMovingAverages.class);

	//private final String url = "jdbc:postgresql://localhost/postgres";
	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "psharma";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "$ys8dmin";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	/** The name of the database we are testing with (this default is installed with MySQL) */
	private final String dbName = "markets";

	/** The name of the database we are testing with (this default is installed with MySQL) */
	private final String tblName = "equities_metadata6";

	Utils utils = new Utils();

	public void run(String dirPath){
		File dir = new File(dirPath);
		String pattern = "zMovingAverages*.csv";
		Collection<File> files = FileUtils.listFiles(dir, new WildcardFileFilter(pattern), null);
		if (files != null){
			logger.info("Found "+files.size()+" files with matching pattern.");

			for (File file: files){
				logger.info("Retrieving and inserting data from file: "+file.getName());
				String fileMovingAverages = file.getAbsolutePath();
				insertData (fileMovingAverages, dbName, tblName);
			}
		}
		else
			logger.warn("Moving Averages: No files found to catalog!");

	}
	
	/**
	 * Inserts the Moving Average data into SQL 
	 * @param fileMovingAverages
	 * @param dbName
	 * @param tblName
	 */
	private void insertData (String fileMovingAverages, String dbName, String tblName){
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countProcessed = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		List<String> inputList  = modReadFile.readFileIntoListOfStrings(fileMovingAverages);
		int size = inputList.size();
		logger.debug("The size of the input list is:" + (size));
		if ((inputList == null) || (size == 0)){
			logger.error("Input list does not have any rows to be inserted into the database.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
				int n = 0;
				boolean heading = true;
				for (String line : inputList){
					n++;
					if (heading){
						heading = false;
						continue;
					}
					if (line == "")
						continue;
					//logger.debug(line);

					String[] temp = line.split(",");
					lineError = line;
					// TODO: First and last lines dont have clean data right now, skip them to avoid Exception for now. Correct them later as well.  
					if ((n == 2) || (n == size) || (temp[0].trim() == "-1"))
						continue;
					String symbol = temp[1].trim();
					Date date = Date.valueOf(temp[2].trim());
					double open = Double.parseDouble(temp[3].trim());
					double high = Double.parseDouble(temp[4].trim());
					double low = Double.parseDouble(temp[5].trim());
					double close = Double.parseDouble(temp[6].trim());
					double netChange = Double.parseDouble(temp[7].trim());
					double pcntChange = Double.parseDouble(temp[8].trim());
					int volume = (int)Math.round(Double.parseDouble(temp[9].trim()));
					int moveStochastic = (int)Math.round(Double.parseDouble(temp[10].trim()));
					int moveSigma = (int)Math.round(Double.parseDouble(temp[11].trim()));
					
					double avgBW = Double.parseDouble(temp[12].trim());
					double stddevBW = Double.parseDouble(temp[13].trim());
					double meanStdDevBW = Double.parseDouble(temp[14].trim());
					double pcntlStockBW = Double.parseDouble(temp[15].trim());
					double pcntlVolBW = Double.parseDouble(temp[16].trim());
					double emaBW = Double.parseDouble(temp[17].trim());
					double oscillatorBW = Double.parseDouble(temp[18].trim());
					double highBlngrBW = Double.parseDouble(temp[19].trim());
					double lowBlngrBW  = Double.parseDouble(temp[20].trim());
					double avgGainsBW = Double.parseDouble(temp[21].trim());
					double avgLossesBW = Double.parseDouble(temp[22].trim());
					double rsiBW = Double.parseDouble(temp[23].trim());
					double mfmBW = Double.parseDouble(temp[24].trim());
					double adlBW = Double.parseDouble(temp[25].trim());
					
					double avgM = Double.parseDouble(temp[26].trim());
					double stddevM = Double.parseDouble(temp[27].trim());
					double pcntlStockM = Double.parseDouble(temp[28].trim());
					double pcntlVolM = Double.parseDouble(temp[29].trim());
					double emaM = Double.parseDouble(temp[30].trim());
					double oscillatorM = Double.parseDouble(temp[31].trim());
					double highBlngrM = Double.parseDouble(temp[32].trim());
					double lowBlngrM  = Double.parseDouble(temp[33].trim());
					double avgGainsM = Double.parseDouble(temp[34].trim());
					double avgLossesM = Double.parseDouble(temp[35].trim());
					double rsiM = Double.parseDouble(temp[36].trim());
					double adlM = Double.parseDouble(temp[37].trim());
					
					double avgQ = Double.parseDouble(temp[38].trim());
					double stddevQ = Double.parseDouble(temp[39].trim());
					double pcntlStockQ = Double.parseDouble(temp[40].trim());
					double pcntlVolQ = Double.parseDouble(temp[41].trim());
					double emaQ = Double.parseDouble(temp[42].trim());
					double oscillatorQ = Double.parseDouble(temp[43].trim());
					double highBlngrQ = Double.parseDouble(temp[44].trim());
					double lowBlngrQ  = Double.parseDouble(temp[45].trim());
					double avgGainsQ = Double.parseDouble(temp[46].trim());
					double avgLossesQ = Double.parseDouble(temp[47].trim());
					double rsiQ = Double.parseDouble(temp[48].trim());
					double adlQ = Double.parseDouble(temp[49].trim());
					
					double avgA = Double.parseDouble(temp[50].trim());
					double stddevA = Double.parseDouble(temp[51].trim());
					double pcntlStockA = Double.parseDouble(temp[52].trim());
					double pcntlVolA = Double.parseDouble(temp[53].trim());
					double emaA = Double.parseDouble(temp[54].trim());
					double oscillatorA = Double.parseDouble(temp[55].trim());
					double highBlngrA = Double.parseDouble(temp[56].trim());
					double lowBlngrA  = Double.parseDouble(temp[57].trim());
					double avgGainsA = Double.parseDouble(temp[58].trim());
					double avgLossesA = Double.parseDouble(temp[59].trim());
					double rsiA = Double.parseDouble(temp[60].trim());
					double adlA = Double.parseDouble(temp[61].trim());
					
					String statement = "INSERT INTO "+tblName+" VALUES ("
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ "? )";
					PreparedStatement pstmt = conn.prepareStatement(statement);

					pstmt.setString(1, symbol);
					pstmt.setDate(2, date);
					pstmt.setDouble(3, open);
					pstmt.setDouble(4, high);
					pstmt.setDouble(5, low);
					pstmt.setDouble(6, close);
					pstmt.setDouble(7, netChange);
					pstmt.setDouble(8, pcntChange);
					pstmt.setInt(9, volume);
					pstmt.setInt(10, moveStochastic);
					pstmt.setInt(11, moveSigma);
					
					pstmt.setDouble(12, avgBW);
					pstmt.setDouble(13, stddevBW);
					pstmt.setDouble(14, meanStdDevBW);
					pstmt.setDouble(15, pcntlStockBW);
					pstmt.setDouble(16, pcntlVolBW);
					pstmt.setDouble(17, emaBW);
					pstmt.setDouble(18, oscillatorBW);
					pstmt.setDouble(19, highBlngrBW);
					pstmt.setDouble(20, lowBlngrBW);
					pstmt.setDouble(21, avgGainsBW);
					pstmt.setDouble(22, avgLossesBW);
					pstmt.setDouble(23, rsiBW);
					pstmt.setDouble(24, mfmBW);
					pstmt.setDouble(25, adlBW);
					
					pstmt.setDouble(26, avgM);
					pstmt.setDouble(27, stddevM);
					pstmt.setDouble(28, pcntlStockM);
					pstmt.setDouble(29, pcntlVolM);
					pstmt.setDouble(30, emaM);
					pstmt.setDouble(31, oscillatorM);
					pstmt.setDouble(32, highBlngrM);
					pstmt.setDouble(33, lowBlngrM);
					pstmt.setDouble(34, avgGainsM);
					pstmt.setDouble(35, avgLossesM);
					pstmt.setDouble(36, rsiM);
					pstmt.setDouble(37, adlM);
					
					pstmt.setDouble(38, avgQ);
					pstmt.setDouble(39, stddevQ);
					pstmt.setDouble(40, pcntlStockQ);
					pstmt.setDouble(41, pcntlVolQ);
					pstmt.setDouble(42, emaQ);
					pstmt.setDouble(43, oscillatorQ);
					pstmt.setDouble(44, highBlngrQ);
					pstmt.setDouble(45, lowBlngrQ);
					pstmt.setDouble(46, avgGainsQ);
					pstmt.setDouble(47, avgLossesQ);
					pstmt.setDouble(48, rsiQ);
					pstmt.setDouble(49, adlQ);
					
					pstmt.setDouble(50, avgA);
					pstmt.setDouble(51, stddevA);
					pstmt.setDouble(52, pcntlStockA);
					pstmt.setDouble(53, pcntlVolA);
					pstmt.setDouble(54, emaA);
					pstmt.setDouble(55, oscillatorA);
					pstmt.setDouble(56, highBlngrA);
					pstmt.setDouble(57, lowBlngrA);
					pstmt.setDouble(58, avgGainsA);
					pstmt.setDouble(59, avgLossesA);
					pstmt.setDouble(60, rsiA);
					pstmt.setDouble(61, adlA);
					
					try{
						pstmt.execute();
						++countProcessed;
					}
					catch (SQLIntegrityConstraintViolationException e) {
						logger.info("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Making assumption that data is already in catalog for row: '"+line+"'");
						++countSkipped;
					}
					pstmt.close();
				} // for (String line : returnList){ ...
				// All queries done, now close out the connection
				conn.close();
				logger.info("Inserted moving averages into database.");
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
	}}


























