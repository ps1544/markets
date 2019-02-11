package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataDailyMarketImpl extends DataNasdaqFilings{
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataDailyMarketImpl.class);

	// The number of columns (or data points) returned by URL 
	private static final int MAX_LENGTH = 15;

	// Maps to hold Name and Link for Stocks, ETFs, and Preferred Stocks
	private HashMap<String, String> mapStocks;
	private HashMap<String, String> mapPreferredStocks; 
	private HashMap<String, String> mapEtfs;

	// Base HTML Element from which we will get the required text information 
	private final String outerTag = "<tr>";

	// List containing symbols w/ highest net change. To be used by external classes for further data generation. 
	public List<String> listHighestNetChange;

	// Instantiate external and helper classes
	UserAgent userAgent = new UserAgent();      
	Utils utils = new Utils();

	double minStartValue = Config.MIN_OPEN_VALUE_THRESHOLD;
	int volume = Config.MIN_VOLUME_THRESHOLD;
	int number = Config.NUMBER_STOCK_SYMBOLS;

	/*
	 * Entry function for the class 
	 */

	public String  run(){

		logger.info("Generating Market Data for Stocks from NASDAQ and NYSE Exchanges.");
		String fileDailyMarketData = ""; 
		try{
			fileDailyMarketData = generateStockData();
		}
		catch(Exception e){
			e.getMessage();
		}
		/*
		logger.info("Generating Market Data for Preferred Stocks.");
		generatePreferredStockData();
		logger.info("Generating Market Data for ETFs.");
		generateEtfsData();
		 */
		return fileDailyMarketData;
	}

	/**
	 *  Generates data for stocks 
	 *  listed on NASDAQ and NYSE
	 */
	public String generateStockData() {
		boolean dataForStocks = true;
		List<String> listInput = new ArrayList<String>();

		// Output List that will contain cumulative listing of all data for CSV write
		List<String> cumulativeOutput = new ArrayList<String>();

		//  TODO: Let file name be governed by the criteria and not hard-coded, in case criteria changes for data collection.  
		String fileName = "Stocks_SortedPcntChange".concat(utils.getDateAndTime().concat(".csv"));
		String date = utils.getDate();
		String timeOfDay = utils.getTimeOfDay();
		fileName = fileName.replace(":", "-");

		try {
			// First, put the exchanges names and their respective URLs in a map

			mapStocks =  new LinkedHashMap<String, String>();
			if (Config.GENERATE_DAILY_DATA_NASDAQ_GLOBAL)
				mapStocks.putAll(getNasdaqGlobalWebLinkMap());
			if (Config.GENERATE_DAILY_DATA_NYSE)
				mapStocks.putAll(getNyseWebLinkMap());
			if (Config.GENERATE_DAILY_DATA_NASDAQ_CAPITAL)
				mapStocks.putAll(getNasdaqCapitalWebLinkMap());
			if (Config.GENERATE_DAILY_DATA_NYSE_AMERICAN)
				mapStocks.putAll(getNyseAmericanWebLinkMap());


			// Now, generate the data based on web scrawling
			try{
				listInput = generateData(mapStocks, fileName, dataForStocks);
			}
			catch (Exception e){
				e.getMessage();
			}

			/*
			 * Raw unsorted data received, now sort that data. Sort criteria is established in DailyMarketData class itself.
			 * TODO: Allow for the sort criteria to be a runtime decision made by caller function here with a given field under "columnName".  
			 */
			String columnName = "Volume";
			ArrayList<DataDailyMarket> dataDailyMarket = sortInputCsv(listInput, columnName);
			// Put in headers in the CSV
			cumulativeOutput.add("Name,Symbol,Open,High,Low,Close,NetChg,PcntChg,Volume,High52Wk,Low52Wk,Div,Yield,PERatio,YTDPcntChg,Exchange,Date,Time");

			for(DataDailyMarket temp: dataDailyMarket){
				/*
			logger.debug("DailyMarketData ["+  ++i +"] "+ 
				   temp.getSymbol() +
				   ", Name: " +temp.getName() + ", Open: " +temp.getOpen() + ", High: " +temp.getHigh() + ", Low: " +temp.getLow() + 
				   ", Close: " +temp.getClose() + ", Net Change: " +temp.getNetChange() + ", Percent Change: " +temp.getPcntChange() + ", Volume: " +temp.getVolume() +
				   ", 52 Wk High: " +temp.getHigh52Week() + ", 52 Wk Low: " +temp.getLow52Week() + ", Dividend: " +temp.getDividend() + ", Yield: " +temp.getYield() +
				   ", P/E: " +temp.getPeRatio() + ", YTD Pcnt Change: " +temp.getYtdChange() + ", Exchange: " +temp.getExchange());
				 */
				cumulativeOutput.add( temp.getName() +
						", " +temp.getSymbol() + ", " +temp.getOpen() + ", " +temp.getHigh() + ", " +temp.getLow() +
						", " +temp.getClose() + ", " +temp.getNetChange() + ", " +temp.getPcntChange() + ", " +temp.getVolume() + ", " +temp.getHigh52Week() +
						", " +temp.getLow52Week() + ", " +temp.getDividend() + ", " +temp.getYield() + ", " +temp.getPeRatio() + ", " +temp.getYtdChange() + ", " +temp.getExchange() +
						","+date+","+timeOfDay);			 
			}

			// Data all sorted, now figure out the major "n" movers by net change within the day and given certain baselines for starting value and trading volume 
			listHighestNetChange = OrderSymbols(cumulativeOutput, minStartValue, volume, number);
			int n = 0;
			logger.info("Symbols w/ Highest Net Change given minimum starting value and traded volumes of '"+minStartValue+"' & '"+volume+"' are ..."); 
			for (String str : listHighestNetChange){
				++n;
				logger.info("		["+n+"]"+ str);
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		modOutCSV outCSV = new modOutCSV(fileName);
		outCSV.writeList(cumulativeOutput);
		return fileName;
	}


	/**
	 *  Generates data for preferred stocks 
	 *  listed on NASDAQ and NYSE
	 */

	public void generatePreferredStockData() {
		boolean dataForStocks = false;

		mapPreferredStocks =  new HashMap<String, String>();
		mapPreferredStocks.putAll(getPreferredStocksWebLinkMap());

		String fileName = "PreferredStocks_".concat(utils.getDateAndTime().concat(".csv"));
		fileName = fileName.replace(":", "-");
		generateData(mapPreferredStocks, fileName, dataForStocks);
	}


	/**
	 *  Generates data for ETFs 
	 */

	public void generateEtfsData() {
		boolean dataForStocks = false;

		mapEtfs =  new HashMap<String, String>();
		mapEtfs.putAll(getEtfWebLinkMap());

		String fileName = "ETFs_".concat(utils.getDateAndTime().concat(".csv"));
		fileName = fileName.replace(":", "-");
		generateData(mapEtfs, fileName, dataForStocks);
	}

	/**
	 * Generates data for regular and preferred stocks along with ETFs
	 * Calls returnInnerHTMLsForSymbols() to do the actual parsing 
	 * 
	 * @param inputMap
	 * @param outFileName
	 * @param dataForStocks
	 * @return cumulativeOutput Unsorted List of Strings containing data for all input securities
	 */
	public List<String> generateData(HashMap<String, String> inputMap, String outFileName, boolean dataForStocks) {

		// List that will contain cumulative listing of all data for CSV write
		List<String> cumulativeOutput = new ArrayList<String>();
		UserAgentSettings uaSettings;

		try{
			// The data for ETFs and Preferred Stocks doesn't have PE information, therefore a lesser column and separate handling
			/*
			if (dataForStocks)
				cumulativeOutput.add("Name,Symbol,Open,High,Low,Close,Net Chg,% Chg,Volume,52 Wk High,52 Wk Low,Div,Yield,P/E,YTD % Chg, Exchange");
			else
				cumulativeOutput.add("Name,Symbol,Open,High,Low,Close,Net Chg,% Chg,Volume,52 Wk High,52 Wk Low,Div,Yield, YTD % Chg");
			 */
			// Now, take the keys for the input set. 
			Set<String> keySet = null;
			if ((inputMap !=null) && !(inputMap.isEmpty()))
				keySet = inputMap.keySet();
			else {
				logger.error("Empty Input Map received for Exchanges and their respective URLs.");
				System.exit(1);
			}

			for (String exchange : keySet){
				String url = inputMap.get(exchange);
				if (dataForStocks)
					//logger.debug("Retrieved URL \""+url+"\" for Exchange: "+exchange);
				
				userAgent.settings.autoRedirect = true;
				userAgent.settings.showWarnings = true;
				userAgent.settings.showTravel = true;
				userAgent.settings.responseTimeout = 30000;
				userAgent.setCacheEnabled(false);
				Document document = userAgent.visit(url);
				Thread.sleep(5000);
				//logger.debug("The Document URL is: "+document.getUrl()); 
				List<String> tempOutput = returnInnerHTMLsForSymbols(document, outerTag, exchange);
				cumulativeOutput.addAll(tempOutput);
			}
		}

		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			System.out.print("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}

		return cumulativeOutput;
	}

	/**
	 * Parses web page and returns a list of Strings each containing daily stock data for tabulated financial information
	 * @param doc
	 * @param outerTag
	 * @param exchange
	 * @return
	 */
	private List<String> returnInnerHTMLsForSymbols(Document doc, String outerTag, String exchange){
		List<String> listOutput = new ArrayList<String>();
		try {
			//logger.debug(doc.innerHTML(1));
			Elements trElements = doc.findEvery(outerTag);
			logger.info("Collecting data for Symbols listed on Exchange: "+exchange);


			for (Element trElement : trElements) {
				String tempSingleSymbolDetails = "";
				List<Element> tdElements = trElement.getChildElements();
				int size = tdElements.size();
				//logger.debug("The number of columns is "+size);

				if ((size == MAX_LENGTH)||(size == (MAX_LENGTH-1))){
					//logger.debug("__________________________");
					// Now go through each cell within the table row and add it's data 
					for (Element tdElement : tdElements){
						String textElement = tdElement.innerText().replaceAll("&nbsp;", "").replaceAll("&amp;", "&").replaceAll("&#039;", "'").replaceAll(",", "");
						if (textElement.contains("..."))
							textElement = "0.0";
						//logger.debug(textElement); 
						tempSingleSymbolDetails = tempSingleSymbolDetails.concat(textElement).concat(",");
					}
					// Concat EXCHANGE (NYSE, Nasdaq...) information as well 
					tempSingleSymbolDetails = tempSingleSymbolDetails.concat(exchange);
					//logger.debug(exchange);

					/*
					 * Exclude repeated header information.
					 * Crude using a column name for exclusion, but works for now.
					 */

					if (!tempSingleSymbolDetails.contains("52 WeekHigh"))
						listOutput.add(tempSingleSymbolDetails);
				}
			}
		}

		catch (Exception e){
			e.printStackTrace();
		}

		return listOutput;
	}

	/**
	 * Function to read a CSV like file as List<String> and sort based on given column
	 * 
	 * @param listInput List of Strings with a String representing a comma separated line within a CSV
	 * @param columnName Name of column used as the basis for sorting  
	 */

	public ArrayList<DataDailyMarket> sortInputCsv(List<String> listInput, String columnName){
		ArrayList<DataDailyMarket> dataDailyMarket = new ArrayList<DataDailyMarket>();
		boolean heading = true;
		for (String line : listInput){
			if (heading){
				heading = false;
				continue;
			}
			//logger.info(temp);
			String[] temp = line.split(",");

			String name = temp[0];
			String symbol = temp[1];
			double open = Double.parseDouble(temp[2].trim());
			double high = Double.parseDouble(temp[3].trim());
			double low = Double.parseDouble(temp[4].trim());
			double close = Double.parseDouble(temp[5].trim());
			double  netChange = Double.parseDouble(temp[6].trim());
			double  pcntChange = Double.parseDouble(temp[7].trim());
			int volume = Integer.parseInt(temp[8].trim());
			double  high52Week = Double.parseDouble(temp[9].trim());
			double  low52Week = Double.parseDouble(temp[10].trim());
			double  dividend = Double.parseDouble(temp[11].trim());
			double  yield = Double.parseDouble(temp[12].trim());
			double  peRatio = Double.parseDouble(temp[13].trim());
			double  ytdChange = Double.parseDouble(temp[14].trim());
			String exchange = temp[15];

			dataDailyMarket.add(new DataDailyMarket
					( name, symbol, open, high, low, close, netChange, pcntChange, volume, high52Week, low52Week, dividend, yield, peRatio, ytdChange, exchange));
		}

		Collections.sort(dataDailyMarket);
		//returnList = dailyMarketData.toArray();
		//logger.info(returnList.toString());
		return dataDailyMarket;
	}

	public List<String> OrderSymbols(List<String> listInput, double valueOpenBaseline, int valueVolumeBaseline, int count){
		int i = 0;
		boolean heading = true;
		listHighestNetChange = new LinkedList<String>();
		List<String> returnStockSymbolList = new LinkedList<String>();

		try{
			/*
			 * TODO: Change all this to use getFieldName() like functions 
			 * from DailyMarketData to avoid hard-coding.   
			 */
			for (String strLine : listInput){
				// Ignore the first line 
				if (heading){
					heading = false;
					continue;
				}
				// Now, split the comma separated string of data for a single stock and push it into individual fields
				// TODO: Need to clean this. This should't be based on position within array rather use proper get() calls 
				String[] temp = strLine.split(",");
				String symbol = temp[1];
				Double open = Double.parseDouble(temp[2].trim());
				int volume = Integer.parseInt(temp[8].trim());

				// Select based on criteria where openValue and day trading volume for symbol are higher than given thresholds. 
				// TODO: Technically, this should be doable for any value 
				if ((open >= valueOpenBaseline) && (volume >= valueVolumeBaseline)){
					++i;
					// In case the count is zero, break immediately. 
					if (i > count)
						break;
					returnStockSymbolList.add(symbol.trim().toUpperCase());
				}
			}
		}
		catch (NumberFormatException e) {e.printStackTrace();}
		catch (Exception e) {e.printStackTrace();}

		return returnStockSymbolList;
	}

	public List<String> getListSymbolsPrioritized() {
		return listHighestNetChange;
	} 
}