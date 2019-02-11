package com.jps.finserv.markets.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.util.modOutCSV;
import com.jps.finserv.markets.util.modReadFile;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.Utils;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DBConnectMySql {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBConnectMySql.class);

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

	/** The name of the table we are testing with */
	//private final String tableName = "JDBC_TEST";

	/** The number of columns or quarters to be covered */
	private final int COLUMNS = 4;

	/**	Flag to confirm whether SQL dump for company financials needs to be created */
	private final boolean SQL_DUMP_FINANCIALS = Config.GENERATE_SQL_DUMP_COMPANY_FINANCIALS;

	/**	Flag to confirm whether SQL dump for daily market data needs to be created */
	private final boolean SQL_DUMP_DAILY_MKT_DATA = Config.GENERATE_SQL_DUMP_DAILY_MARKET;

	Utils utils = new Utils();

	protected Set<String> StmtTypes;

	/**
	 * inserts company financial data into SQL
	 * @param fileCompanyFinancialData CSV file containing financial data 
	 * @param dbName SQL database for insert
	 * @param tblName SQL table for insert
	 */
	public void insertCompanyFinancialsData(String fileCompanyFinancialData, String dbName, String tblName){

		//dbIncomeStmt.prepDataForInsert (fileCompanyFinancialData, dbName, tblName)
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countInserted = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");
		Map<String, List<String>> mapBalanceSheetData = new HashMap<String, List<String>> ();

		StmtTypes = new HashSet<String>();
		StmtTypes.add("Balance Sheet");
		StmtTypes.add("Income Statement");
		StmtTypes.add("Cash Flow");
		StmtTypes.add("Financial Ratios");

		List<String> inputList  = modReadFile.readFileIntoListOfStrings(fileCompanyFinancialData);

		logger.debug("The size of the returned list is:" + inputList .size());
		if ((inputList == null) || (inputList .size() == 1)){
			logger.error("File \""+fileCompanyFinancialData+"\" does not have any rows to be inserted into the database.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the DB doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
				mapBalanceSheetData  = parseIndividualStmtData(inputList, "Balance Sheet");
				Set<String> keys = mapBalanceSheetData.keySet();

				String quarter[] = new String[COLUMNS];
				BigDecimal CashAndEquivalents[] = new BigDecimal[COLUMNS];
				BigDecimal ShortTermInvestments[] = new BigDecimal[COLUMNS];
				BigDecimal NetReceivables[] = new BigDecimal[COLUMNS];
				BigDecimal Inventory[] = new BigDecimal[COLUMNS];
				BigDecimal OtherCurrentAssets[] = new BigDecimal[COLUMNS];
				BigDecimal TotalCurrentAssets[] = new BigDecimal[COLUMNS];
				BigDecimal LongTermInvestments[] = new BigDecimal[COLUMNS];
				BigDecimal FixedAssets[] = new BigDecimal[COLUMNS];
				BigDecimal Goodwill[] = new BigDecimal[COLUMNS];
				BigDecimal IntangibleAssets[] = new BigDecimal[COLUMNS];
				BigDecimal OtherAssets[] = new BigDecimal[COLUMNS];
				BigDecimal DeferredAssetCharges[] = new BigDecimal[COLUMNS];
				BigDecimal TotalAssets[] = new BigDecimal[COLUMNS];
				BigDecimal AccountsPayable[] = new BigDecimal[COLUMNS];
				BigDecimal ShortTermDebt[] = new BigDecimal[COLUMNS];
				BigDecimal OtherCurrentLiabilities[] = new BigDecimal[COLUMNS];
				BigDecimal TotalCurrentLiabilities[] = new BigDecimal[COLUMNS];
				BigDecimal LongTermDebt[] = new BigDecimal[COLUMNS];
				BigDecimal OtherLiabilities[] = new BigDecimal[COLUMNS];
				BigDecimal DeferredLiabilityCharges[] = new BigDecimal[COLUMNS];
				BigDecimal MiscStocks[] = new BigDecimal[COLUMNS];
				BigDecimal MinorityInterest[] = new BigDecimal[COLUMNS];
				BigDecimal TotalLiabilities[] = new BigDecimal[COLUMNS];
				BigDecimal CommonStocks[] = new BigDecimal[COLUMNS];
				BigDecimal CapitalSurplus[] = new BigDecimal[COLUMNS];
				BigDecimal RetainedEarnings[] = new BigDecimal[COLUMNS];
				BigDecimal TreasuryStock[] = new BigDecimal[COLUMNS];
				BigDecimal OtherEquity[] = new BigDecimal[COLUMNS];
				BigDecimal TotalEquity[] = new BigDecimal[COLUMNS];
				BigDecimal TotalLiabilitiesAndEquity[] = new BigDecimal[COLUMNS];

				for (String symbol: keys){
					
					boolean skipWithInsufficientData = false;
					List<String> listData = mapBalanceSheetData.get(symbol);
					for (String line: listData){
						try{
							lineError = line;
							String[] temp = line.split(",");
							if (temp[0].equals(("Quarter Ending:").trim())){
								for (int i = 2; i <=5; i++){
									quarter[i-2] = utils.generateQuarterGivenDate(temp[i].trim());
									/*
								if (temp[i].equals("3/31/2018"))
									quarter[i-2] = "1Q2018";
								else if (temp[i].equals("9/30/2017")){
									quarter[i-2] = "3Q2017";
									//logger.info("Set quarter to:"+"3Q2017");
								}
								else if (temp[i].equals("6/30/2017")){
									quarter[i-2] = "2Q2017";
									//logger.info("Set quarter to:"+"2Q2017");
								}
								else if (temp[i].equals("3/31/2017")){
									quarter[i-2] = "1Q2017";
									//logger.info("Set quarter to:"+"1Q2017");
								}
								else if (temp[i].equals("12/31/2016")){
									quarter[i-2] = "4Q2016";
									//logger.info("Set quarter to:"+"4Q2016");
								}
									 */
								}
							}
							else  if (temp[0].equals(("Cash and Cash Equivalents").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									CashAndEquivalents[i-2] = new BigDecimal(price);
									//logger.info("Added Cash information into the variable");
								}
							}
							else if (temp[0].equals(("Short-Term Investments").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									ShortTermInvestments[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Net Receivables").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									NetReceivables[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Inventory").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									Inventory[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Other Current Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OtherCurrentAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Total Current Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalCurrentAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Long-Term Investments").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									LongTermInvestments[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Fixed Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									FixedAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Goodwill").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									Goodwill[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Intangible Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									IntangibleAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Other Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OtherAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Deferred Asset Charges").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									DeferredAssetCharges[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Total Assets").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalAssets[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Accounts Payable").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									AccountsPayable[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Short-Term Debt / Current Portion of Long-Term Debt").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									ShortTermDebt[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Other Current Liabilities").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OtherCurrentLiabilities[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Total Current Liabilities").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalCurrentLiabilities[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Long-Term Debt").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									LongTermDebt[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Other Liabilities").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OtherLiabilities[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Deferred Liability Charges").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									DeferredLiabilityCharges[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Misc. Stocks").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									MiscStocks[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Minority Interest").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									MinorityInterest[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Total Liabilities").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalLiabilities[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Common Stocks").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									CommonStocks[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Capital Surplus").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									CapitalSurplus[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Retained Earnings").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									RetainedEarnings[i-2] = new BigDecimal(price);
								}
							}
							else if (temp[0].equals(("Treasury Stock").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", ""); 
									TreasuryStock[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Other Equity").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", ""); 
									OtherEquity[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Total Equity").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalEquity[i-2] = new BigDecimal(price);
								}
							}else if (temp[0].equals(("Total Liabilities & Equity").trim())){
								for (int i = 2; i <=5; i++){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalLiabilitiesAndEquity[i-2] = new BigDecimal(price);
								}
							}
						}
						/*
						 * For new companies, we may not have data for all 4 quarters or
						 * for the last 4 years if data is annual. Simply catch the exception
						 * and the list will automatically ensure that whatever data we do 
						 * have, gets inserted. 
						 */
						catch (ArrayIndexOutOfBoundsException e){
							/*
							 * For new companies, we may not have data for all 4 quarters or
							 * for the last 4 years if data is annual. Simply catch the exception
							 * and skip this round in the loop 
							 */
							skipWithInsufficientData = true;
							continue;
						}

						catch (Exception e){
							e.printStackTrace();
						}
					}
					// Now we have gotten through all the data for a given symbol, pick up the variables and insert into SQL.  
					for (int i = 0; i < COLUMNS; i++){

						String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						PreparedStatement pstmt = conn.prepareStatement(statement);

						pstmt.setString(1, symbol);
						pstmt.setString(2, quarter[i]);
						pstmt.setBigDecimal(3, CashAndEquivalents[i]);
						pstmt.setBigDecimal(4, ShortTermInvestments[i]);
						pstmt.setBigDecimal(5, NetReceivables[i]);
						pstmt.setBigDecimal(6, Inventory[i]);
						pstmt.setBigDecimal(7, OtherCurrentAssets[i]);
						pstmt.setBigDecimal(8, TotalCurrentAssets[i]);
						pstmt.setBigDecimal(9, LongTermInvestments[i]);
						pstmt.setBigDecimal(10, FixedAssets[i]);
						pstmt.setBigDecimal(11, Goodwill[i]);
						pstmt.setBigDecimal(12, IntangibleAssets[i]);
						pstmt.setBigDecimal(13, OtherAssets[i]);
						pstmt.setBigDecimal(14, DeferredAssetCharges[i]);
						pstmt.setBigDecimal(15, TotalAssets[i]);
						pstmt.setBigDecimal(16, AccountsPayable[i]);
						pstmt.setBigDecimal(17, ShortTermDebt[i]);
						pstmt.setBigDecimal(18, OtherCurrentLiabilities[i]);
						pstmt.setBigDecimal(19, TotalCurrentLiabilities[i]);
						pstmt.setBigDecimal(20, LongTermDebt[i]);
						pstmt.setBigDecimal(21, OtherLiabilities[i]);
						pstmt.setBigDecimal(22, DeferredLiabilityCharges[i]);
						pstmt.setBigDecimal(23, MiscStocks[i]);
						pstmt.setBigDecimal(24, MinorityInterest[i]);
						pstmt.setBigDecimal(25, TotalLiabilities[i]);
						pstmt.setBigDecimal(26, CommonStocks[i]);
						pstmt.setBigDecimal(27, CapitalSurplus[i]);
						pstmt.setBigDecimal(28, RetainedEarnings[i]);
						pstmt.setBigDecimal(29, TreasuryStock[i]);
						pstmt.setBigDecimal(30, OtherEquity[i]);
						pstmt.setBigDecimal(31, TotalEquity[i]);
						pstmt.setBigDecimal(32, TotalLiabilitiesAndEquity[i]);

						try{
							if (skipWithInsufficientData){
								skipWithInsufficientData = false;
								continue;
							}
							else {
								int n = pstmt.executeUpdate();
								countInserted = countInserted+n;
							}
						}
						catch (SQLIntegrityConstraintViolationException e) {
							logger.debug("Caught SQLIntegrityConstraintViolationException.");
							logger.info("Balance Sheet information already in catalog for symbol: '"+symbol+"' for the quarter:"+quarter[i]);
							++countSkipped;
						}
						catch (MysqlDataTruncation e) {
							logger.warn(e.getMessage());
							++countSkipped;
						}
						
						pstmt.close();
					}
				}
				logger.info("Total # of records inserted into table '"+tblName+"' : "+countInserted);
				logger.info("Total # of records skipped (were already present) for insert: "+countSkipped);

				// Export DB Table if Config flag is set
				if (SQL_DUMP_FINANCIALS){
					List<String> returnList = new LinkedList<String>();
					//if ((countInserted > 0) || (countSkipped > 0)){
					String strSelectAll = "SELECT * FROM "+tblName;
					PreparedStatement pstmtSelectAll = conn.prepareStatement(strSelectAll);
					ResultSet rs = pstmtSelectAll.executeQuery();
					returnList.add("Symbol,Quarter,CashAndEquivalents,ShortTermInvestments,NetReceivables,Inventory,OtherCurrentAssets,TotalCurrentAssets,LongTermInvestments,FixedAssets,"
							+ "Goodwill,IntangibleAssets,OtherAssets,DeferredAssetCharges,TotalAssets,AccountsPayable,ShortTermDebt,OtherCurrentLiabilities,TotalCurrentLiabilities,"
							+ "LongTermDebt,OtherLiabilities,DeferredLiabilityCharges,MiscStocks,MinorityInterest,TotalLiabilities,CommonStocks,CapitalSurplus,RetainedEarnings,"
							+ "TreasuryStock,OtherEquity,TotalEquity,TotalLiabilitiesAndEquity");
					while (rs.next()){
						String line = "";
						line = rs.getString(1).concat(",");
						line = line.concat(rs.getString(2).concat(","));
						for (int i = 3; i <=32; i++){
							line = line.concat((rs.getBigDecimal(i).toString()).concat(","));
						}
						//logger.debug(line);
						returnList.add(line);
					}
					pstmtSelectAll.close();
					//}
					String fileSQLOut = "SQLBalanceSheetData".concat("_").concat(utils.getDateAndTime().concat(".csv"));
					fileSQLOut = fileSQLOut.replace(":", "-");
					modOutCSV outCSV = new modOutCSV(fileSQLOut);
					outCSV.writeList(returnList);
				}
				// All queries done, now close out the connection
				conn.close();
			}

		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
			logger.error("For debugging check the referred data point in the input file: \""+fileCompanyFinancialData+"\"");
			e.printStackTrace();
		}
		catch (Exception e) {
			logger.error("Generic Exception: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
			logger.error("For debugging check the referred data point in the input file: \""+fileCompanyFinancialData+"\"");
			e.printStackTrace();
		}
	}


	/**
	 * 
	 * @param fileDailyMarketData
	 * @param dbName
	 * @param tblName
	 */

	public void insertDailyMarketData(String fileDailyMarketData, String dbName, String tblName){
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countUpdated = 0;
		int countProcessed = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		properties.setProperty("autoReconnect", "true");
		//properties.put("verifyServerCertificate", false);
		//properties.put("requireSSL", false);

		List<String> returnList  = modReadFile.readFileIntoListOfStrings(fileDailyMarketData);

		int size = returnList.size();
		logger.debug("The size of the returned list is:" + (size-1));
		if ((returnList == null) || (returnList.size() == 1)){
			logger.error("File \""+fileDailyMarketData+"\" does not have any rows to be inserted into the database.");
			logger.error("Check if the input file only has headers and no data.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);

				boolean heading = true;
				for (String line : returnList){ 
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
					String name = temp[0].trim();
					String symbol = temp[1].trim();
					double open = Double.parseDouble(temp[2].trim());
					double high = Double.parseDouble(temp[3].trim());
					double low = Double.parseDouble(temp[4].trim());
					double close = Double.parseDouble(temp[5].trim());
					double netChange = Double.parseDouble(temp[6].trim());
					double pcntChange = Double.parseDouble(temp[7].trim());
					int volume = Integer.parseInt(temp[8].trim());
					double high52Week = Double.parseDouble(temp[9].trim());
					double low52Week = Double.parseDouble(temp[10].trim());
					double dividend = Double.parseDouble(temp[11].trim());
					double yield = Double.parseDouble(temp[12].trim());
					double peRatio = Double.parseDouble(temp[13].trim());
					double ytdChange = Double.parseDouble(temp[14].trim());
					String  exchange = temp[15].trim();
					Date date = Date.valueOf(temp[16]);
					Time time = Time.valueOf(temp[17]);

					String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement pstmt = conn.prepareStatement(statement);
					// Set all String elements
					pstmt.setString(1, name); pstmt.setString(2, symbol); pstmt.setString(16, exchange);
					// Set the only integer element
					pstmt.setInt(9, volume);
					// Now all the double elements
					pstmt.setDouble(3, open); pstmt.setDouble(4, high); pstmt.setDouble(5, low);
					pstmt.setDouble(6, close); pstmt.setDouble(7, netChange); pstmt.setDouble(8, pcntChange);
					pstmt.setDouble(10, high52Week); pstmt.setDouble(11, low52Week); pstmt.setDouble(12, dividend);
					pstmt.setDouble(13, yield); pstmt.setDouble(14, peRatio); pstmt.setDouble(15, ytdChange);
					// Finally the date
					pstmt.setDate(17, date);
					pstmt.setTime(18, time);

					try{
						pstmt.execute();
					}
					catch (SQLIntegrityConstraintViolationException e) {
						//logger.debug("SQLIntegrityConstraintViolationException occurred likely because of Primary Key restriction for following line item:\""+lineError+"\"");
						//logger.debug("Latest market data now available for symbol: \""+symbol+"\".");
						//logger.debug("Replacing older data from the same day.");
						String delRowStmt = "DELETE  FROM "+tblName+ " WHERE symbol like ? and date like ?";
						PreparedStatement pstmtDelete = conn.prepareStatement(delRowStmt);
						pstmtDelete.setString(1, symbol);
						pstmtDelete.setDate(2, date);
						/*
						 * For the given symbol, delete the existing older row from the same day. 
						 * If that succeeds then, and only then, attempt the current insert again.  
						 */
						int countDelete = pstmtDelete.executeUpdate();
						if (countDelete > 0){ 
							pstmt.execute();
							++countUpdated;
						}
						else{
							logger.error("Failed to delete existing row for symbol: "+symbol);
							logger.error("The latest data will NOT be inserted for this symbol.");
						}
						pstmtDelete.close();
					}
					catch (com.mysql.cj.jdbc.exceptions.MysqlDataTruncation e) {
						e.printStackTrace();
						logger.error("Failed to insert daily market data row into catalog. ");
					}
					
					pstmt.close();
				} // for (String line : returnList){ ...

				// Export DB Table if Config flag is set
				if (SQL_DUMP_DAILY_MKT_DATA){
					List<String> outputList = new LinkedList<String>();
					//if ((countProcessed > 0) || (countUpdated > 0)){
					String strSelectAll = "SELECT * FROM "+tblName;
					PreparedStatement pstmtSelectAll = conn.prepareStatement(strSelectAll);
					ResultSet rs = pstmtSelectAll.executeQuery();
					outputList.add("Name,Symbol,Open,High,Low,Close,NetChange,PcntChange,Volume,High52Weeks,Low52Weeks,Dividend,Yield,PERatio,YtdPcntChange,Exchange,Date,Time");
					while (rs.next()){
						String lineStr = "";
						lineStr = rs.getString(1).concat(",");
						lineStr = lineStr.concat(rs.getString(2).concat(","));
						for (int i = 3; i <=8; i++){
							lineStr = lineStr.concat(Double.toString(rs.getDouble(i))).concat(",");
						}
						lineStr = lineStr.concat(Integer.toString(rs.getInt(9))).concat(",");
						for (int i = 10; i <=15; i++){
							lineStr = lineStr.concat(Double.toString(rs.getDouble(i))).concat(",");
						}
						lineStr = lineStr.concat(rs.getString(16).concat(","));
						DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
						DateFormat dfTime = new SimpleDateFormat("HH:mm:ss");
						lineStr = lineStr.concat(df.format(rs.getDate(17))).concat(",");
						lineStr = lineStr.concat(dfTime.format(rs.getTime(18)));
						//logger.debug(lineStr);
						outputList.add(lineStr);
					}
					pstmtSelectAll.close();
					//}
					String fileSQLOut = "SQLDailyMarketData".concat("_").concat(utils.getDateAndTime().concat(".csv"));
					fileSQLOut = fileSQLOut.replace(":", "-");
					modOutCSV outCSV = new modOutCSV(fileSQLOut);
					outCSV.writeList(outputList);
				}
				// All queries done, now close out the connection
				conn.close();				
			}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
			logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage()+ " >> while traversing following line item:\""+lineError+"\" in the input file: \""+fileDailyMarketData+"\"");
			e.printStackTrace();
		}
		logger.info("Total # of symbols processed for SQL DB insert: "+countProcessed);
		logger.info("Total # of symbols that had their data refreshed w/ a SQL DB update: "+countUpdated);
	}

	/**
	 * generates a map of symbols with a list of financial data
	 * @param inputList a string list retrieved from a CSV file containing symbol name and company financial data
	 * @param StmtType financial data type like balance sheet, income statement whose data will be returned by this function
	 * @return a map of symbols with data on their respective financial statement type 
	 */
	protected Map<String, List<String>> parseIndividualStmtData(List<String> inputList, String StmtType){
		Map<String, List<String>> returnMap = new HashMap<String, List<String>> ();
		String symbol = "";
		boolean started = false;
		List<String> tempStmtDataList = new ArrayList<String>();

		// Had to add this in, because the child class calls this function but not the other one where these values get added
		Set<String> StmtTypes = new HashSet<String>();
		StmtTypes.add("Balance Sheet");
		StmtTypes.add("Income Statement");
		StmtTypes.add("Cash Flow");
		StmtTypes.add("Financial Ratios");


		try{
			for (String line : inputList){ 
				String[] temp = line.split(",");
				String firstBase = temp[0].trim(); 
				// If the first column value matches the input Stmt type, then simply grab the symbol and start the counter
				if(firstBase.equals(StmtType)){
					symbol = temp[1].trim();
					started = true;
					//logger.debug("Setting clearStart flag at line: "+ line);
				}
				// Don't start until we have the starting point for the StmtType we are interested in... 
				else if (started){
					// If so, check whether this is the end of input sequence for that StmtType
					if (StmtTypes.contains(firstBase)){
						// If it is indeed the end of data for that symbol and for that StmtType, then put the symbol and data in map and clear the list
						List<String> list2 = new ArrayList<String>();
						list2.addAll(tempStmtDataList);
						tempStmtDataList.clear();
						returnMap.put(symbol, list2);
						//logger.debug("Breaking off the sequence at line: "+ line);
						//logger.info("Size of tempStmtDataList is:"+list2.size());
						started = false;
						symbol = "";
					}
					// else we are still in the middle of the current StmtType, keep on grabbing individual lines
					else
					{
						// Annuals start with "Period Ending", skip them for now...
						if (line.contains("Period Ending:")){
							tempStmtDataList.clear();
							started = false;
							symbol = "";
						}
						tempStmtDataList.add(line);
						//logger.debug("Inserting line: "+line);
					}
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return returnMap;
	}

	/**
	 * 
	 * @param query The SQL query to be run 
	 * @param strTableHeading A string containing all relevant column headers for the CSV with commas separating them
	 * @param numTableColumns Number of columns
	 */
	public ResultSet executeQuery(String query){

		Connection conn = null;
		Properties properties = new Properties();
		ResultSet rs = null;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		try{
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			Statement st = conn.createStatement();
			PreparedStatement pstmt= conn.prepareStatement(query);
			rs = pstmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			while (rs.next()){
				for (int i = 1; i <= columnsNumber; i++) {
					if (i > 1) 
						System.out.print(",  ");
					String columnValue = rs.getString(i);
					System.out.print(columnValue + " " + rsmd.getColumnName(i));
				}
				System.out.println("");
			}
			st.close();
			conn.close();
			return rs;
		}
		catch (SQLException e) {
			logger.error("SQLException: "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			logger.error("Run and validate input SQL query.");
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Pushes output of a SQL query to a CSV file
	 * @param query The SQL query to be run 
	 * @param strTableHeading A string containing all relevant column headers for the CSV with commas separating them
	 * @param numTableColumns Number of columns
	 */
	public void outSqlToCSV(String query, String strTableHeading, int numTableColumns){

		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");
		String errorLine ="";;

		try{
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			Statement st = conn.createStatement();

			List<String> returnList = new LinkedList<String>();
			PreparedStatement pstmtSelectAll = conn.prepareStatement(query);
			ResultSet rs = pstmtSelectAll.executeQuery();
			if (strTableHeading != "")
				returnList.add(strTableHeading);

			if (rs != null){
				while (rs.next()){
					String line = "";
					for (int i = 1; i <=numTableColumns; i++){
						//logger.debug(line);
						line = line.concat((rs.getString(i)).concat(","));
						errorLine = line;
					}
					returnList.add(line);
				}
			}
			else 
				logger.error("Empty or Null Result Set received. Validate the input SQL query.");
			pstmtSelectAll.close();
			String fileSQLOut = "SQLOut".concat("_").concat(utils.getDateAndTime().concat(".csv"));
			fileSQLOut = fileSQLOut.replace(":", "-");
			modOutCSV outCSV = new modOutCSV(fileSQLOut);
			outCSV.writeList(returnList);
			File f = new File(fileSQLOut);
			if (f.exists())
				logger.info("Exported SQL query results to file: "+fileSQLOut);

			conn.close();
		}
		catch (SQLException e) {
			logger.error("SQLException: "+e.getMessage());
			logger.error("Exception occurred at data input:  "+errorLine);
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			logger.error("Run and validate input SQL query.");
			logger.error("Exception occurred at data input:  "+errorLine);
			e.printStackTrace();
		}
	}

	/**
	 * Creates a map of Primary Key and a list of values for asked columns 
	 * @param dbName Name of the DB where execution needs to be made
	 * @param tableName Name of the concerned table
	 * @param pk Primary Key
	 * @param columns A list<String> of table columns that are to be returned in the map with PK as key
	 * @return
	 * 
	 * TODO: Enhance this to account for multiple columns rather than just one
	 */
	public Map<String, String> mapPKWithOtherColumns(String dbName, String tblName, String pk, String column){

		Map<String, String> returnMap = new HashMap<String, String> ();
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");
		//HashSet<String> hSet = new HashSet<String>();

		try{
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			Statement st = conn.createStatement();
			String query = "SELECT ?, ? FROM ?";
			//List<String> returnList = new LinkedList<String>();
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setObject(1, pk, JDBCType.VARCHAR);
			pstmt.setObject(2, column, JDBCType.VARCHAR);
			pstmt.setObject(3, tblName, JDBCType.VARCHAR);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()){
				returnMap.put(rs.getString(1), rs.getString(2));
			}
		}
		catch (SQLException e) {
			logger.error("SQLException: "+e.getMessage());
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			logger.error("Run and validate input SQL query.");
			e.printStackTrace();
		}
		return returnMap;
	}


}