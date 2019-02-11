package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataDailyMarketDataHistoricalJournal extends DataNasdaqFilings{
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataDailyMarketDataHistoricalJournal.class);

	// Instantiate external and helper classes
	UserAgent userAgent = new UserAgent();      
	Utils utils = new Utils();
	private final String baseUrl = "https://quotes.wsj.com/";

	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "psharma";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "$ys8dmin";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	/** The name of the database we are testing with*/
	private final String dbName = "markets";

	/** The name of the table we are inserting historical stock market data */
	private final String tblName = "equities_historic_data";

	/*
	 * Entry function for the class 
	 */

	public boolean run(){
		boolean success = false;
		logger.info("Generating historical daily returns for stocks.");
		List<String> listAllSymbols = new LinkedList<String>();
		//String query = "select distinct symbol, volume from equities_2018 where symbol not in (SELECT distinct symbol from equities_historic_data group by symbol having count('close')>61) "
			//	+ " and exchange like 'NYSE%' and date like '2018-05-09' order by volume desc limit 0,200";
		//String query = "select distinct symbol from equities_2018 where symbol in (select distinct symbol from industrybackground where SnP500 = 1 and symbol "
		//		+ "not in (SELECT distinct symbol from equities_historic_data group by symbol having count('close')>61)) and exchange like 'Nasdaq%' limit 0,50";
		
		/* Must adjust NASDAQ or NYSE in downstream function as well */
			listAllSymbols.add("NVO");
			
		//listAllSymbols = generateListSymbols("equities_2018", query);
		
		for (String symbol : listAllSymbols){
				List<String> returnedList = generateData(symbol);

				if (returnedList != null){
					logger.info("Inserting data for symbol:"+symbol);	
					insertDailyMarketData(returnedList, dbName, tblName);

					modOutCSV outCSV = new modOutCSV((symbol+"_"+".csv").replace("%", ""));
					outCSV.writeList(returnedList); 
					File file = new File(outCSV.toString());
					boolean deleted = false;
					if (file.exists())
						deleted = file.delete();
				}
				else
					logger.warn("Historic returns not found for symbol:" +symbol);
			}
		

		return success;
	}

	/*
	 * Returns list of symbols from the concerned table
	 */
	public List<String> generateListSymbols(String tableName, String query){
		List<String> listAllSymbols = new LinkedList<String>();
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		try {
			// TODO: Add exception handling for invalid server / port / db names. 
			// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String statement = query;//"select distinct symbol from "+tableName;
			PreparedStatement pstmt = conn.prepareStatement(statement);
			//pstmt.setString(1, tableName);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()){
				// 2018-04-14: Have noticed quite a few times that % is showing up in symbol names returned by SQL. Taking care of that here. 
				listAllSymbols.add((rs.getString(1)).replaceAll("%", ""));
			}
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage());
			//logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage());
			e.printStackTrace();
		}

		return listAllSymbols;
	}

	/**
	 * Parses the WSJ webpage to retrieve historical dataset for given symbol and saves output in a list of lines each line representing return for a day
	 * @param symbol
	 * @return
	 */
	public List<String> generateData(String symbol) {
		List<String> returnList = new ArrayList<String>();
		List<String> listOutput = new ArrayList<String>();

		//String url = baseUrl.concat(symbol).concat("/historical-prices");
		// XNAS for nasdaq, XNYS for NYSE
		String url = "https://quotes.wsj.com/ajax/historicalprices/4/SYMBOL?MOD_VIEW=page&ticker=SYMBOL&country=US&exchange=XNYS&instrumentType=STOCK&num_rows=7419.958333333333&range_days=7419.958333333333&startDate=01%2F01%2F1998&endDate=04%2F26%2F2018&_=1524758101851";
		url = url.replaceAll("SYMBOL", symbol);
		try {
			Document document = userAgent.visit(url);

			/*
			Element divContainer = document.findFirst("<div class=\"historical_topnav\">");
			//Element divContainer = document.findFirst("<div id=\"quotes_content_left_pnlAJAX\">");
			Element divDateRange = divContainer.findFirst("<div class=\"dateRange_nav\">");
			Element spanStartDate = divDateRange.findFirst("<input type=\"text\" class=\"datePicker\" id=\"selectDateFrom\">"); 
			spanStartDate.setAttribute("value", "01/01/1998");
			//Form form = document.getFormByButton("<input type=\"button\" value=\"go\" id=\"datPickerButton\">");
			Form form = userAgent.doc.getForm(0);   
			Document docData = form.submit(); 
			 */

			Element divHistoricalData = document.findFirst("<div id=\"historical_data_table\">");
			Element tblBody = divHistoricalData.findFirst("<tbody>");
			Elements tblRecordElements = tblBody.findEvery("<tr>");

			String tempSingleLine = "";
			String date = "";
			String open = "";
			String high = "";
			String low = "";
			String close = "";
			String volume = "";
			for (Element singleTblRecordElement : tblRecordElements){
				Elements tblDataElements = singleTblRecordElement.findEvery("<td>");
				int count = 0;
				for (Element singleDataElement : tblDataElements){
					String textElement = singleDataElement.innerText();
					textElement  = textElement.replaceAll(",", "").replaceAll("\n", "").replaceAll("\r", "").trim();
					if ((textElement != null) && (!textElement.isEmpty())){
						if (count == 0){
							date = textElement;
						}
						else if (count == 1)
							open = textElement;
						else if (count == 2)
							high = textElement;
						else if (count == 3)
							low = textElement;
						else if (count == 4)
							close = textElement;
						else if (count == 5)
							volume = textElement; 
						count++;
					}
				}
				if (!(date.isEmpty()) && (date.contains("/")) && (date != "")){	// Symbol and Date are PKs for SQL tables, can't be empty
					tempSingleLine = symbol.concat(",").concat(open).concat(",").concat(high).concat(",").
							concat(low).concat(",").concat(close).concat(",").concat(volume).concat(",").concat(date);
					listOutput.add(tempSingleLine);
					//List<String> tempList = listOutput;
					returnList.addAll(listOutput);
					listOutput.clear();
					date = open = high = low = close = volume = "";
				}
				else{ 
					logger.info("Details insufficient to insert row into catalog. Skipping row for symbol: "+symbol);
					date = open = high = low = close = volume = "";
				}	
			}
		} 
		catch(NotFound e){         //if an HTTP/connection error occurs, handle JauntException.
			System.out.print("Jaunt Exception: "+e.getMessage()+" while traversing HTML pages for symbol: "+symbol+"\n");
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			System.out.print("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}

		List <String> listWithPcntChangeValues = addNetChangePcntChange(returnList);
		return listWithPcntChangeValues;
	}

	/**
	 * Adds netChange and pcntChange details for a list that has data on closing quotes for symbls
	 * @param inputList 
	 * @return
	 */
	private List<String> addNetChangePcntChange(List<String> inputList){
		List<String>  returnList = new ArrayList <>();

		for (String currentLine : inputList){
			double netChange = 0.0;
			double pcntChange = 0.0;

			String[] temp = currentLine.split(",");
			String todaysClose = temp[4];
			int offset = inputList.indexOf(currentLine);
			if (offset < (inputList.size()-1)){
				int yesterdaysOffset = offset+1;
				String yesterdaysLine = inputList.get(yesterdaysOffset);
				String[] temp2 = yesterdaysLine.split(",");
				String yesterdaysClose = temp2[4];
				netChange = Double.parseDouble(todaysClose) - Double.parseDouble(yesterdaysClose);
				pcntChange = (netChange * 100) / Double.parseDouble(yesterdaysClose);
				String returnTempLine = currentLine+","+netChange+","+pcntChange;
				returnList.add(returnTempLine);
			}
		}
		return returnList;
	}

	/**
	 * Inserts a list of strings with symbol, open, high, low, close, volume, date, netchange, pcntChange into SQL
	 * @param inputList List that has the data to be inserted
	 * @param dbName
	 * @param tblName
	 */

	public void insertDailyMarketData(List<String> inputList, String dbName, String tblName){
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countInserted = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		int size = inputList.size();
		logger.debug("The size of the returned list is:" + (size-1));
		if ((inputList == null) || (inputList.size() == 1)){
			logger.error("Input list does not have any rows to be inserted into the database.");
			logger.error("Check if the input file only has headers and no data.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
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

					String[] temp = line.split(",");
					lineError = line;
					//String name = temp[0].trim();
					String symbol = temp[0].trim();
					double open = Double.parseDouble(temp[1].trim());
					double high = Double.parseDouble(temp[2].trim());
					double low = Double.parseDouble(temp[3].trim());
					double close = Double.parseDouble(temp[4].trim());
					String strVolume = temp[5].trim();
					int volume = 0;
					if (strVolume.contains(" ")){
						String[] listVolume = strVolume.split(" ");
						if (listVolume[1].matches("M")){
							Double dblVolume = (Double.parseDouble(listVolume[0])) * 1000000;
							volume = dblVolume.intValue();
						}
						else 
							logger.warn("Daily stock txaction volume representated in unexpected format."); 
					}
					else 
						volume = Integer.parseInt(temp[5].trim());

					Date date = Date.valueOf(utils.adjustDateFormat(temp[6].trim()));
					double netChange = Double.parseDouble(temp[7].trim());
					double pcntChange = Double.parseDouble(temp[8].trim());

					String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement pstmt = conn.prepareStatement(statement);
					// Set symbol, the only Stirng element
					pstmt.setString(1, symbol);
					// Set the only integer element
					pstmt.setInt(8, volume);
					// Now all the double elements
					pstmt.setDouble(2, open); pstmt.setDouble(3, high); pstmt.setDouble(4, low); pstmt.setDouble(5, close); 
					pstmt.setDouble(6, netChange); pstmt.setDouble(7, pcntChange);
					// Finally the date
					pstmt.setDate(9, date);

					try{
						pstmt.execute();
						++countInserted;
					}
					catch (SQLIntegrityConstraintViolationException e) {
						logger.debug("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Data already in catalog for tuple: '"+symbol+"', '" +date+"'");
						++countSkipped;
					}
					pstmt.close();
				} // for (String line : returnList){ ...
				// All queries done, now close out the connection
				conn.close();
				logger.info("Inserted historical dataset into database.");
				logger.info("Total # of records inserted: "+countInserted);
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