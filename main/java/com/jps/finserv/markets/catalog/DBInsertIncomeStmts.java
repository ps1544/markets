package com.jps.finserv.markets.catalog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.util.modOutCSV;
import com.jps.finserv.markets.util.modReadFile;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.Utils;

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

public class DBInsertIncomeStmts extends DBConnectMySql {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBInsertIncomeStmts.class);

	//private final String url = "jdbc:postgresql://localhost/postgres";
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

	/** The name of the table we are testing with */
	//private final String tableName = "JDBC_TEST";

	/** The number of columns or quarters to be covered */
	private final int COLUMNS = 4;

	Utils utils = new Utils();

	private Set<String> StmtTypes;

	/**
	 * 
	 * @param fileCompanyFinancialData
	 * @param dbName
	 * @param tblName
	 */
	public void prepDataForInsert (String fileCompanyFinancialData, String dbName, String tblName){
		Map<String, List<String>> mapIncomeStmtData = new HashMap<String, List<String>> ();
		List<List<Object>> sendList = new ArrayList<List<Object>>();
		String errorLine = "";
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
		{
			try{
				String quarter[] = new String[COLUMNS];
				BigDecimal TotalRevenue[] = new BigDecimal[COLUMNS];
				BigDecimal CostOfRevenue[] = new BigDecimal[COLUMNS];
				BigDecimal GrossProfit[] = new BigDecimal[COLUMNS];
				BigDecimal ResearchDevelopment[] = new BigDecimal[COLUMNS];
				BigDecimal SGA[] = new BigDecimal[COLUMNS];
				BigDecimal NonRecurringItems[] = new BigDecimal[COLUMNS];
				BigDecimal OtherOperatingItems[] = new BigDecimal[COLUMNS];
				BigDecimal OperatingIncome[] = new BigDecimal[COLUMNS];
				BigDecimal AdditionalIncomeExpenseItems[] = new BigDecimal[COLUMNS];
				BigDecimal EBIT[] = new BigDecimal[COLUMNS];
				BigDecimal InterestExpense[] = new BigDecimal[COLUMNS];
				BigDecimal EarningsBeforeTax[] = new BigDecimal[COLUMNS];
				BigDecimal IncomeTax[] = new BigDecimal[COLUMNS];
				BigDecimal MinorityInterest[] = new BigDecimal[COLUMNS];
				BigDecimal EquityNetUnconsolidatedSubsidiary[] = new BigDecimal[COLUMNS];
				BigDecimal NetIncomeContOperations[] = new BigDecimal[COLUMNS];
				BigDecimal NetIncome[] = new BigDecimal[COLUMNS];
				BigDecimal NetIncomeApplicableShareholders[] = new BigDecimal[COLUMNS];

				mapIncomeStmtData  = parseIndividualStmtData(inputList, "Income Statement");
				Set<String> keys = mapIncomeStmtData.keySet();

				/*
				for (String symbol1: keys){
					List<String> listData1 = mapIncomeStmtData.get(symbol1);
					for (String line: listData1){
						//logger.debug("Symbol: "+symbol1+": "+line);
					}
				}
				 */

				for (String symbol: keys){
					List<String> listData = mapIncomeStmtData.get(symbol);
					List<Object> tempList = new LinkedList<Object>();

					for (int i = 2; i <=5; i++){
						tempList.add(symbol); // symbol needs to be added only once per vertical line (per quarter that is)
						for (String line: listData){
							try{
								errorLine = line;
								String[] temp = line.split(",");
								if (temp[0].equals(("Quarter:").trim())){
									continue;
								}
								else if (temp[0].equals(("Quarter Ending:").trim())){
									quarter[i-2] = utils.generateQuarterGivenDate(temp[i]);
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
									tempList.add(quarter[i-2]);
								}
								else if (temp[0].equals(("Total Revenue").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									TotalRevenue[i-2] = new BigDecimal(price);
									tempList.add(TotalRevenue[i-2]);
								}
								else if (temp[0].equals(("Cost of Revenue").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									CostOfRevenue[i-2] = new BigDecimal(price);
									tempList.add(CostOfRevenue[i-2]);
								}
								else if (temp[0].equals(("Gross Profit").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									GrossProfit[i-2] = new BigDecimal(price);
									tempList.add(GrossProfit[i-2]);
								}
								else if (temp[0].equals(("Research and Development").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									ResearchDevelopment[i-2] = new BigDecimal(price);
									tempList.add(ResearchDevelopment[i-2]);
								}
								else if (temp[0].equals(("Sales General and Admin.").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									SGA[i-2] = new BigDecimal(price);
									tempList.add(SGA[i-2]);
								}
								else if (temp[0].equals(("Non-Recurring Items").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									NonRecurringItems[i-2] = new BigDecimal(price);
									tempList.add(NonRecurringItems[i-2]);
								}
								else if (temp[0].equals(("Other Operating Items").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OtherOperatingItems[i-2] = new BigDecimal(price);
									tempList.add(OtherOperatingItems[i-2]);
								}
								else if (temp[0].equals(("Operating Income").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									OperatingIncome[i-2] = new BigDecimal(price);
									tempList.add(OperatingIncome[i-2]);
								}
								else if (temp[0].equals(("Add'l income/expense items").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									AdditionalIncomeExpenseItems[i-2] = new BigDecimal(price);
									tempList.add(AdditionalIncomeExpenseItems[i-2]);
								}
								else if (temp[0].equals(("Earnings Before Interest and Tax").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									EBIT[i-2] = new BigDecimal(price);
									tempList.add(EBIT[i-2]);
								}
								else if (temp[0].equals(("Interest Expense").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									InterestExpense[i-2] = new BigDecimal(price);
									tempList.add(InterestExpense[i-2]);
								}
								else if (temp[0].equals(("Earnings Before Tax").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									EarningsBeforeTax[i-2] = new BigDecimal(price);
									tempList.add(EarningsBeforeTax[i-2]);
								}
								else if (temp[0].equals(("Income Tax").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									IncomeTax[i-2] = new BigDecimal(price);
									tempList.add(IncomeTax[i-2]);
								}
								else if (temp[0].equals(("Minority Interest").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									MinorityInterest[i-2] = new BigDecimal(price);
									tempList.add(MinorityInterest[i-2]);
								}
								else if (temp[0].equals(("Equity Earnings/Loss Unconsolidated Subsidiary").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									EquityNetUnconsolidatedSubsidiary[i-2] = new BigDecimal(price);
									tempList.add(EquityNetUnconsolidatedSubsidiary[i-2]);
								}
								else if (temp[0].equals(("Net Income-Cont. Operations").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									NetIncomeContOperations[i-2] = new BigDecimal(price);
									tempList.add(NetIncomeContOperations[i-2]);
								}
								else if (temp[0].equals(("Net Income").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									NetIncome[i-2] = new BigDecimal(price);
									tempList.add(NetIncome[i-2]);
								}
								else if (temp[0].equals(("Net Income Applicable to Common Shareholders").trim())){
									String price = temp[i].replace("$","").replace("(", "-").replace(")", "");
									NetIncomeApplicableShareholders[i-2] = new BigDecimal(price);
									tempList.add(NetIncomeApplicableShareholders[i-2]);
								}
							}
							/*
							 * For new companies, we may not have data for all 4 quarters or
							 * for the last 4 years if data is annual. Simply catch the exception
							 * and the list will automatically ensure that whatever data we do 
							 * have, gets inserted. 
							 */
							catch (ArrayIndexOutOfBoundsException e){
								tempList.clear();
							}

							catch (Exception e){
								e.printStackTrace();
							}

						} //for (String line: listData){
						List<Object> txferList = new LinkedList<Object>();
						if (!tempList.isEmpty()){
							txferList.addAll(tempList);
							sendList.add(txferList);
							tempList.clear();
						}

					} //for (int i = 2; i <=5; i++){
				}// for (String symbol: keys){
				insertDB(sendList, dbName, tblName);
			}
			catch(Exception e){
				logger.error("Exception <<"+e.getMessage()+">> at input data:"+errorLine);
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 
	 */
	private void insertDB(List<List<Object>> inputList, String dbName, String tblName ){
		int countSkipped = 0, countInserted = 0;
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		String symbol = "";

		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement pstmt = conn.prepareStatement(statement);

			for (List<Object> tempList : inputList){
				// Set all String elements
				symbol = tempList.get(0).toString();
				//quarter[i] = tempList.get(1).toString();
				pstmt.setObject(1, tempList.get(0), JDBCType.VARCHAR);
				pstmt.setObject(2, tempList.get(1), JDBCType.VARCHAR);
				pstmt.setObject(3, tempList.get(2), JDBCType.DOUBLE);
				pstmt.setObject(4, tempList.get(3), JDBCType.DOUBLE);
				pstmt.setObject(5, tempList.get(4), JDBCType.DOUBLE);
				pstmt.setObject(6, tempList.get(5), JDBCType.DOUBLE);
				pstmt.setObject(7, tempList.get(6), JDBCType.DOUBLE);
				pstmt.setObject(8, tempList.get(7), JDBCType.DOUBLE);
				pstmt.setObject(9, tempList.get(8), JDBCType.DOUBLE);
				pstmt.setObject(10, tempList.get(9), JDBCType.DOUBLE);
				pstmt.setObject(11, tempList.get(10), JDBCType.DOUBLE);
				pstmt.setObject(12, tempList.get(11), JDBCType.DOUBLE);
				pstmt.setObject(13, tempList.get(12), JDBCType.DOUBLE);
				pstmt.setObject(14, tempList.get(13), JDBCType.DOUBLE);
				pstmt.setObject(15, tempList.get(14), JDBCType.DOUBLE);
				pstmt.setObject(16, tempList.get(15), JDBCType.DOUBLE);
				pstmt.setObject(17, tempList.get(16), JDBCType.DOUBLE);
				pstmt.setObject(18, tempList.get(17), JDBCType.DOUBLE);
				pstmt.setObject(19, tempList.get(18), JDBCType.DOUBLE);
				pstmt.setObject(20, tempList.get(19), JDBCType.DOUBLE);

				try{
					int n = pstmt.executeUpdate();
					countInserted = countInserted+n;
				}
				catch (SQLIntegrityConstraintViolationException e) {
					//logger.info("Data already in catalog for for symbol: '"+symbol+"' for the quarter:"+quarter[i]);
					logger.debug("Caught SQLIntegrityConstraintViolationException.");
					//logger.info("Making assumption that data is already in catalog for symbol: '"+symbol+"'");
					logger.info("Income Statement information already in catalog for symbol: '"+symbol+"' for the quarter:"+tempList.get(1));
					++countSkipped;
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
		logger.info("Inserted Income Statements in database.");
		logger.info("Total # of records (one record per quarter each input symbol) inserted into table '"+tblName+"' : "+countInserted);
		logger.info("Total # of records skipped (were already present) for insert: "+countSkipped);
	}
}