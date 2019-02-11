package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Alternate sources of this information are here: 
 * 	https://www.cnbc.com/commodities/
 * 	https://www.investing.com/commodities/gold-historical-data
 * 		For investing.com, just get to the price of any commodity and then append "historical-data" at the end to gather old market data. 
 * 		Then change the period within calendar and then programmatically click the button "Download Data".  
 */
public class DataDailyIndicators {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataDailyIndicators.class);
	// TODO: Push this to Config class and read this from there
	private final String url = "http://markets.wsj.com/?mod=Markets_MDW_MDC";
	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "psharma";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "$ys8dmin";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	Utils utils = new Utils();

	// Look into DataCompanyIndustryInfo and DBConnectMySql and find a way to insert the daily data on economic indicators.
	// Push the data into a list where each line represents input for an insert statement
	public void run(){
		UserAgent userAgent = new UserAgent();
		userAgent.settings.checkSSLCerts = false;
		Map<String, LinkedHashMap<String, String>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();

		Set<String> tgtMatchSetA = new HashSet<String>();
		Set<String> tgtMatchSetB = new HashSet<String>();

		// TODO: Externalize this set and retrieve values from a different file
		tgtMatchSetA.add("Crude Oil"); 
		tgtMatchSetA.add("Gold");
		tgtMatchSetA.add("XX:BUXX"); // symbol for WSJ dollar index on the concerned page 
		tgtMatchSetA.add("US:DJIA"); // DJIA
		tgtMatchSetA.add("US:COMP"); // Nasdaq Composite
		tgtMatchSetA.add("US:SPX"); // S&P 500
		tgtMatchSetA.add("US:RUT"); // Russell 2000

		// The following values don't show price; just the yield and that too under variable pcntChange
		// Need to handle them in a different set
		tgtMatchSetB.add("BX:TMUBMUSD03M"); // symbol for US 3 month bill on the concerned page
		tgtMatchSetB.add("BX:TMUBMUSD02Y"); // symbol for US 2 year bill on the concerned page
		tgtMatchSetB.add("BX:TMUBMUSD05Y"); // symbol for US 5 year note
		tgtMatchSetB.add("BX:TMUBMUSD10Y"); // symbol for US 10 year bond
		tgtMatchSetB.add("BX:TMUBMUSD30Y"); // symbol for US 10 year bond

		try{
			userAgent.visit(url); // Visit the base URL with symbol
			Elements tblRecords = userAgent.doc.findEvery("<tr data-ticker-id"); // Find all <tr> tags in the HTML doc

			for (Element tableRecord : tblRecords){
				String entity = tableRecord.getAt("data-ticker-id");
				Set<String> returnMapKeySet = returnMap.keySet();

				/*Traverse the data for ENTITY if 
				 * 1) we are on that entity in our input list 
				 * AND 
				 * 2) we haven't already traversed it already  
				 */
				if ((tgtMatchSetA.contains(entity)) && (!returnMapKeySet.contains(entity.replaceAll(" ", "")))){
					logger.info("Generating data for:"+entity); 
					Map<String, LinkedHashMap<String, String>> mapSeriesA = retrieveDataForSetA(tableRecord, entity);
					if ((mapSeriesA != null) && (!mapSeriesA.isEmpty()))
						returnMap.putAll(mapSeriesA);
				}
				else if ((tgtMatchSetB.contains(entity)) && (!returnMapKeySet.contains(entity.replaceAll(" ", "")))){
					logger.info("Generating data for:"+entity);
					Map<String, LinkedHashMap<String, String>> mapSeriesB = retrieveDataForSetB(tableRecord, entity);
					if ((mapSeriesB != null) && (!mapSeriesB.isEmpty()))
						returnMap.putAll(mapSeriesB);
				}
			}
			logger.info("The economic indicator map has "+returnMap.size()+ " values.");
			if (returnMap.size() > 0)
				prepDataForInsert(returnMap, "markets", "DailyIndicators");
			else
				logger.error("No information available to insert data for Daily Indicators.");
		} // try
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Generates data for economic entities like Crude Oil, Gold from WSJ Markets page. 
	 * Not compatible to retrieve values for entities like US 2/5/10 year bonds. 
	 * @param tableRecord
	 * @param entity
	 * @return
	 */
	Map<String, LinkedHashMap<String, String>> retrieveDataForSetA(Element tableRecord, String entity){
		Map<String, LinkedHashMap<String, String>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
		String name = "";
		String price = "";
		String change = "";
		String pcntChange = ""; 
		boolean priceChecked = false;
		boolean changeChecked = false;
		boolean pcntChangeChecked = false;
		//logger.info(tableRecord.innerHTML());
		entity = entity.replaceAll(" ", "");
		//logger.debug("Entity: "+entity);
		Elements dataListElem = tableRecord.findEvery("<td class>");
		int count = 0;
		try{
			for (Element dataSingleElem : dataListElem){
				//logger.debug("In the for loop.");
				String tdAttrValue = dataSingleElem.getAt("class").trim();
				if (tdAttrValue.equals("firstCol")){
					Element href =  dataSingleElem.findFirst("<a>");
					if (href != null){
						name = href.innerText();
						name = name.replaceAll(" ","");
						name = name.replaceAll("\\.",""); // Need to replace dot, if present in entity, as in "U.S. 5 Year"
						//logger.info("Name Value: "+name);
					}
				}
				else if (tdAttrValue.equals("dataCol")){
					priceChecked = true;
					if ((count == 0) || (count == 1)){ // The first or second (only if first was empty) iteration has the right value 
						String temp = dataSingleElem.innerText(); // get the data and if it is not null or more importantly, not empty then that's our data
						//count = 0;
						if ((temp !=null)&&(temp != ""))
							tempMap.put("price", temp);
						else // put in empty string because downstream expects exact number of inputs
							tempMap.put("price", price);
					} 
					++count;
				}
				else if ((tdAttrValue.equals("dataCol priceUp")) || (tdAttrValue.equals("dataCol priceDown"))){
					changeChecked = true;
					change = dataSingleElem.innerText();
					tempMap.put("change", change); 
					//logger.debug("Change: "+change);
				}
				else if ((tdAttrValue.equals("dataCol last priceUp")) || (tdAttrValue.equals("dataCol last priceDown")) || (tdAttrValue.equals("dataCol last"))){
					pcntChangeChecked = true;
					pcntChange = (dataSingleElem.innerText()).replaceAll("%", "");
					tempMap.put("pcntChange", pcntChange);
					//logger.debug("%Change: "+pcntChange); 
				}
			} // for (Element dataSingleElem : dataListElem){ 
			if (name != ""){
				if (priceChecked && changeChecked && pcntChangeChecked)
					returnMap.put(entity.replaceAll(" ", ""), tempMap);
				else
					logger.warn("Not all expected values returned. Skipping adding this iteration to map.");
			}
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return returnMap;
	}

	/**
	 * Generates data for economic entities like US 2/5/10 year bonds from WSJ Markets page. 
	 * Not compatible to retrieve values for entities like Crude Oil, Gold etc...  
	 * @param tableRecord
	 * @param entity
	 * @return
	 */
	Map<String, LinkedHashMap<String, String>> retrieveDataForSetB(Element tableRecord, String entity){
		Map<String, LinkedHashMap<String, String>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
		String name = "";
		String price = "";
		String change = "";
		String pcntChange = ""; 
		boolean priceChecked = false;
		boolean changeChecked = false;
		boolean pcntChangeChecked = false;
		//logger.info(tableRecord.innerHTML());
		entity = entity.replaceAll(" ", "");
		//logger.debug("Entity: "+entity);
		Elements dataListElem = tableRecord.findEvery("<td class>");
		int count = 0;
		try{
			for (Element dataSingleElem : dataListElem){
				//logger.debug("In the for loop.");
				String tdAttrValue = dataSingleElem.getAt("class").trim();
				if (tdAttrValue.equals("firstCol")){
					Element href =  dataSingleElem.findFirst("<a>");
					if (href != null){
						name = href.innerText();
						name = name.replaceAll(" ","");
						name = name.replaceAll("\\.",""); // Need to replace dot, if present in entity, as in "U.S. 5 Year"
						//logger.info("Name Value: "+name);
					}
				}
				else if (tdAttrValue.equals("dataCol")){
					priceChecked = true;
					/*
					 * This will always be empty, which is fine, 
					 * since we are currently (in downstream code) 
					 * labeling yield (pcntChange) as price for bonds.  
					 */
					tempMap.put("price", price);
					/*
					 * Now during off hours, the change is showing up as "0/32"
					 * but it is coming under "dataCol " and not under "dataCol priceUp/down". 
					 * Therefore, if there are 3 empty datacols then take in change as empty.
					 * And yes, this is cluegy, welcome to html parsing :) 
					 */
					++count;
					if (count == 2) 
						tempMap.put("change", change);
				}
				else if ((tdAttrValue.equals("dataCol priceUp")) || (tdAttrValue.equals("dataCol priceDown"))){
					changeChecked = true;
					change = dataSingleElem.innerText();
					tempMap.put("change", change); 
					//logger.debug("Change: "+change);
				}
				else if ((tdAttrValue.equals("dataCol last priceUp")) || (tdAttrValue.equals("dataCol last priceDown")) || (tdAttrValue.equals("dataCol last"))){
					pcntChangeChecked = true;
					pcntChange = (dataSingleElem.innerText()).replaceAll("%", "");
					tempMap.put("pcntChange", pcntChange);
					//logger.debug("%Change: "+pcntChange); 
				}
			} // for (Element dataSingleElem : dataListElem){ 

			if (priceChecked && changeChecked && pcntChangeChecked)
			//if (priceChecked && pcntChangeChecked) // For rates, the HTML attribute for "change" may not always be present. Therefore, not require this as a must-have 
				returnMap.put(entity.replaceAll(" ", ""), tempMap);
			else
				logger.warn("Not all expected values returned. Skipping adding this iteration to map.");

		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return returnMap;
	}


	/**
	 * Prepares the data for SQL DB insert operation
	 * @param inputMap
	 * @param dbName
	 * @param tblName
	 */
	private void prepDataForInsert(Map<String, LinkedHashMap<String, String>>  inputMap, String dbName, String tblName){

		List<String> returnListOutput = new LinkedList<String>();
		String date = utils.getDate();
		String timeOfDay = utils.getTimeOfDay();
		String lineSingleDataPoint = "";
		returnListOutput.add("Entity,Price,Change,PcntChange,Date,Time");
		Set<String> keysEntityNames = inputMap.keySet();
		// Read data from input Map and convert it into a list where each line has single data point including data and time separated by commas
		for (String keySingleEntity: keysEntityNames){
			//logger.debug("Key Value is:" +keySingleEntity);
			lineSingleDataPoint = keySingleEntity.concat(",");
			Map<String, String> innerMap = inputMap.get(keySingleEntity);
			Set<String> innerMapKeys = innerMap.keySet();
			for (String innerKey: innerMapKeys){
				//logger.debug(		innerKey+":"+innerMap.get(innerKey));
				lineSingleDataPoint = lineSingleDataPoint.concat(innerMap.get(innerKey));
				lineSingleDataPoint = lineSingleDataPoint.concat(",");
			}
			lineSingleDataPoint = lineSingleDataPoint.concat(date).concat(",").concat(timeOfDay);
			logger.info("Data to be inserted into database:"+lineSingleDataPoint);
			returnListOutput.add(lineSingleDataPoint); 
		}
		if ((returnListOutput == null) || (returnListOutput.isEmpty())){
			logger.error("Received null / empty list for database insert operation.");
			logger.error("Program will exit");
			System.exit(1);
		}

		/*
		 * Now, we have data properly in a list where each line corresponds to a single SQL insert operation.
		 * Take in that data, separate it by comma, and then adjust it for double values where applicable. 
		 */

		boolean heading = true;
		List<List<Object>> sendList = new ArrayList<List<Object>>();
		String errorLine = "";

		try{
			for (String line : returnListOutput){ 
				// Miscellaneous tasks before we get to actual data: handle extra double quote, account for heading, keep reference to line in case of exception 
				errorLine = line;
				if (heading){
					heading = false;
					continue;
				}
				if (line == "")
					continue;

				/*
				 * All that done, now split the line by double quotes
				 * Cannot split by commas. There are many commas 
				 * in the company name string which will require specific handling otherwise
				 */
				String[] temp = line.split(",");
				String entity = temp[0].trim();

				// TODO: Handle these as NULL rather than 0.0 which is actually a value
				double price = 0.0;
				double change = 0.0;
				double pcntChange = 0.0;
				/*
				 *  For interest rates, the price comes out as empty and pcntChange actually
				 *  has the yield information. So, if price string is empty, make pcntChange the 
				 * price and make pcntChange empty.
				 * 0=entityName; 1=price; 2=change; 3=pcntChange; 4=date; 5=time 
				 */

				// Handle PRICE first
				// For set B (interest rates), remember that price is empty and yield is in temp[3] rather
				if (temp[1].isEmpty()){
					if (!temp[3].isEmpty()) {
						price = Double.parseDouble(temp[3].trim());
					}
				}
				else {// price not empty (set A), then take in price value from temp[1] and pcntChange from temp[3]
					price =  Double.parseDouble(temp[1].trim());
					if (!temp[3].isEmpty()) 
						pcntChange =  Double.parseDouble(temp[3].trim());
				}

				// Values for CHANGE can come in string form as 3/32, 0/32, 5/32 etc.. Convert them in real double value 
				if (!temp[2].isEmpty()){
					if (temp[2].contains("/")){
						String[] tempStrList = (temp[2].split("/"));
						double numerator = Double.parseDouble(tempStrList[0]);
						double denominator= Double.parseDouble(tempStrList[1]);
						change =  numerator / denominator;
						//logger.debug("The CHANGE value is:"+change);
					}
					else change =  Double.parseDouble(temp[2].trim());
				}

				List<Object> tempList = new LinkedList<Object>();
				tempList.add(entity); tempList.add(price); tempList.add(change); 
				tempList.add(pcntChange); tempList.add(date); tempList.add(timeOfDay);

				sendList.add(tempList);
			}// for (String line : returnListOutput){ 
			insertDB(sendList, dbName, tblName);
		}
		catch(Exception e){
			logger.error("Exception <<"+e.getMessage()+">> at input data:"+errorLine);
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves values as a list and inserts items therein into database
	 * @param inputList List of objects where each line is one entry into database
	 * @param dbName DB Name
	 * @param tblName Table Name
	 */
	private void insertDB(List<List<Object>> inputList, String dbName, String tblName ){
		try {
			Connection conn = null;
			Properties properties = new Properties();
			properties.put("user", userName);
			properties.put("password", password);
			properties.put("useSSL", "false");
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?)";
			PreparedStatement pstmt = conn.prepareStatement(statement);
			//int countUpdated = 0;
			//int countProcessed = 0;


			for (List<Object> tempList : inputList){
				// Set all String elements
				String entity = tempList.get(0).toString();
				pstmt.setObject(1, tempList.get(0), JDBCType.VARCHAR);
				pstmt.setObject(2, tempList.get(1), JDBCType.DOUBLE);

				if (Double.parseDouble(tempList.get(2).toString()) == 0.0)
					pstmt.setNull(3, Types.DOUBLE);
				else
					pstmt.setObject(3, tempList.get(2), JDBCType.DOUBLE);

				if (Double.parseDouble(tempList.get(3).toString()) == 0.0)
					pstmt.setNull(4, Types.DOUBLE);
				else
					pstmt.setObject(4, tempList.get(3), JDBCType.DOUBLE);

				Date date = Date.valueOf(tempList.get(4).toString());
				pstmt.setObject(5, tempList.get(4), JDBCType.DATE);
				pstmt.setObject(6, tempList.get(5), JDBCType.TIME);

				try{
					pstmt.execute();
				}
				catch (SQLIntegrityConstraintViolationException e) {
					//logger.debug("SQLIntegrityConstraintViolationException occurred likely because of Primary Key restriction for following line item:\""+lineError+"\"");
					//logger.debug("Latest market data now available for symbol: \""+symbol+"\".");
					//logger.debug("Replacing older data from the same day.");
					String delRowStmt = "DELETE  FROM "+tblName+ " WHERE entity like ? and date like ?";
					PreparedStatement pstmtDelete = conn.prepareStatement(delRowStmt);
					pstmtDelete.setString(1, entity);
					pstmtDelete.setDate(2, date);
					/*
					 * For the given symbol, delete the existing older row from the same day. 
					 * If that succeeds then, and only then, attempt the current insert again.  
					 */
					int countDelete = pstmtDelete.executeUpdate();
					if (countDelete > 0){ 
						pstmt.execute();
						//++countUpdated;
					}
					else{
						logger.error("Failed to delete existing row for entity: "+entity);
						logger.error("The latest data will NOT be inserted for this entity.");
					}
					pstmtDelete.close();
				}
				//pstmt.close();
			}
			logger.info("Inserted Company Background dataset into DB Table.");
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

	}
}