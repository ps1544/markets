package com.jps.finserv.markets.equities;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaunt.*;
import com.jaunt.component.*;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataNasdaqFilingsImpl {//extends CompanyFinancials{
	private final Logger logger = (Logger) LoggerFactory.getLogger(DataDailyMarketImpl.class);

	// Create an instance of the Utils class
	Utils utils = new Utils();
	// Instantiate an instance of Daily Market Data
	DataDailyMarketImpl dataDailyMarketImpl = new DataDailyMarketImpl();
	// Instantiate EDGAR Data class file as well
	DataSecEdgarCompanyFilings edgarData = new DataSecEdgarCompanyFilings();
	// The number of columns (or data points) returned by URL 
	private final int MAX_LENGTH = 6;
	// Flag to determine whether to retrieve quarterly or just annual financial data
	private boolean quarterlyData = Config.NASDAQ_CHECK_QUARTERLY_DATA;
	// Pattern to be added to baseURL that otherwise only collects annual data 
	private static final String quarterly = "&data=quarterly";
	// Base HTML Element from which we will get the required text information 
	String outerTag = "<tr>";
	// 10-Q or 10-K or some other financial data type
	String dataType = Config.FINANCIAL_DATATYPE;
	// Initiate file name strings for different types of statements we are covering
	String fileIncomeStmt  = "IncomeStmt"; 
	String fileBalanceSheet  = "BalanceSheet"; 
	String fileCashFlows  = "CashFlows"; 
	String fileRatios  = "Ratios"; 
	// List that contains the full dataset retrieved in this run
	

	/*
	 * Entry function for the class 
	 */
	public HashMap <String, String> run(){
		List<String> listInput = new LinkedList<String>();
		HashMap <String, String> returnMap = new HashMap<String, String>();
		List<String> cumulativeOutput = new ArrayList<String>();
		
		if (Config.INSERT_DAILY_MARKET_DATA_INTO_CATALOG){
			String fileDailyMarketData = dataDailyMarketImpl.run();
			returnMap.put("DailyMarketData", fileDailyMarketData);

			listInput = dataDailyMarketImpl.getListSymbolsPrioritized();
			String fileCompanyFinancialData = "FinancialStmts".concat("_").concat(utils.getDateAndTime().concat(".csv"));
			fileCompanyFinancialData = fileCompanyFinancialData.replace(":", "-");
			logger.info("Retrieving Quarterly and Annual Financial Records.");
			cumulativeOutput = generateFinancialsData(listInput, fileCompanyFinancialData, quarterlyData);

			// All desired data retrieved, now we write to the out File.  
			if ((cumulativeOutput != null) && (!cumulativeOutput.isEmpty())){
				modOutCSV outCSV = new modOutCSV(fileCompanyFinancialData);
				outCSV.writeList(cumulativeOutput);
			}
			else
				logger.error("Problems in generating Company Financials data from Exchanges.");

			returnMap.put("CompanyFinancialData", fileCompanyFinancialData);
			return returnMap;
		}
		// Not the best practice but handled in calling function. 
		return null;
	} 

	/**
	 * Makes the call to generate actual data 
	 */
	public List<String> generateFinancialsData(List<String> inputList, String fileName, boolean quarterly) {
		List<String> cumulativeOutput = new ArrayList<String>();
		int runSECData = Config.COUNT_SEC_NUM_DOCS;
		
		try{
			if ((inputList != null) && (!inputList.isEmpty()))
				for (String symbol : inputList){
					// First, get CSVs from NASDAQ website
					List<String> listInput = getDataFromHTMLLinks(symbol, quarterly, fileName);
					cumulativeOutput.addAll(listInput);
					
					// Also, for given symbol call EDGAR class instance to download data from SEC EDGAR as well. See "DataSecEdgarCompanyFilings.java" for implementation. 
					if (runSECData > 0)
						edgarData.run(symbol, dataType);
					else 
						logger.debug("Option disabled to retrieve 'Interactive Data' from SEC / EDGAR.");
				}
			else 
				logger.warn("Empty or Null input stock symbol list received while generating finacial datasets. No data will be generated on quarterly and annual filings. ");
		}
		catch (Exception e) {e.printStackTrace();}
		return cumulativeOutput;
	}


	/**
	 *  Recursively calls another function to generate financial data 
	 *  for a given symbol and 	writes that data to CSV output
	 * @param symbol
	 * @param quarterlyData
	 * @param outFileName
	 */
	public List<String> getDataFromHTMLLinks(String symbol, boolean quarterlyData, String outFileName) {

		UserAgent userAgent = new UserAgent();
		userAgent.settings.checkSSLCerts = false;
		String url = "http://www.nasdaq.com/symbol/"+symbol+"/financials"; // Set base URL given stock symbol

		/*
		 * The pattern for HTML links <a> contains attribute class with value "tab-header-a" as in: 
		 * 		<a id="tab1" title="" href="/symbol/pfe/financials?query=income-statement" class="tab-header-a">
		 * Creating a matching string to confirm that this is the right <a> link for the dataset we are interested in. 
		 */
		String matchStr = "tab-header-a"; 
		List<String> tempOutput = new ArrayList<String>();
		List<String> cumulativeOutput = new ArrayList<String>();
		try{
			userAgent.visit(url); // Visit the base URL with symbol
			Elements urls = userAgent.doc.findEvery("<a>"); // Find all links in HTML doc
			for (Element element : urls) {
				if (element.hasAttribute("class")){ // Does the link have a "class" attribute so we can do pattern search against value thereof
					String temp = element.getAt("class");
					String typeStmt = "";
					if (temp.matches(matchStr)) { // Check if "class" attribute has matching value of matchStr 
						String tempHref = element.getAt("href"); // if yes, this is our URL of interest
						// If quarterly, append string to generate quarterly data. 
						// Note that there is no quarterly data available for Financial Ratios
						if (quarterlyData) 					
							tempHref = tempHref.concat(quarterly);
						// Now, focus on getting the type of the financial document whether it is balance sheet, income stmt etc... 
						Element spanElement = element.findFirst("<span>");
						if (spanElement != null)
							typeStmt = spanElement.innerText();

						logger.debug("Retrieving "+typeStmt+ " for stock symbol "+ symbol.toUpperCase()+" with URL: "+tempHref);
						UserAgent ua = new UserAgent(); // Now, create new user agent instance and visit this URL
						ua.settings.checkSSLCerts = false;
						ua.visit(tempHref);
						// Now, get into more details to actually retrieve the data
						Elements trElements = ua.doc.findEvery(outerTag);
						tempOutput = returnInnerHTMLsForSymbols(trElements); // Call another function to actually traverse through the HTML of doc with tempHref

						if ((tempOutput != null) && (!(tempOutput.isEmpty()))){
							cumulativeOutput.add((typeStmt).concat(",").concat(symbol.toUpperCase()));
							cumulativeOutput.addAll(tempOutput);
							tempOutput.clear();
						}
						
					}
				}
			}
		}

		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return 	cumulativeOutput;
	}

	/**
	 * Given a parent HTML element, returns text of all it's child elements.
	 * @param elements
	 * @return
	 */

	private List<String> returnInnerHTMLsForSymbols(Elements elements){
		List<String> listOutput = new ArrayList<String>();
		try {
			for (Element trElement : elements) {
				String tempSingleSymbolDetails = "";
				List<Element> childElements = trElement.getChildElements();
				int size = childElements.size();
				//logger.debug("The number of columns is "+size);

				if (size == MAX_LENGTH){
					logger.debug("__________________________");
					// Now go through each cell within the table row and add it's data 
					for (Element element : childElements){
						String textElement = element.innerText().replaceAll("&nbsp;", "").replaceAll("&amp;", "&").replaceAll("&#039;", "'").replaceAll(",", "");
						logger.debug(textElement); 
						tempSingleSymbolDetails = tempSingleSymbolDetails.concat(textElement).concat(",");
					}

					// Concat EXCHANGE (NYSE, Nasdaq...) information as well 
					// tempSingleSymbolDetails = tempSingleSymbolDetails.concat(exchange);
					// logger.info(exchange);

					listOutput.add(tempSingleSymbolDetails);
				}
			} 
		}

		catch (Exception e){
			e.printStackTrace();
		}

		return listOutput;
	}
}