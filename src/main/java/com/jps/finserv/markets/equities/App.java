package com.jps.finserv.markets.equities;

import com.jps.finserv.markets.analytics.*;
import com.jps.finserv.markets.callreport.DataBankCallReport;
import com.jps.finserv.markets.callreport.XbrlIntegrationImpl;
import com.jps.finserv.markets.callreport.ParserXSD;
import com.jps.finserv.markets.callreport.ParserJAXB;
import com.jps.finserv.markets.catalog.*;
import com.jps.finserv.markets.cloud.S3Upload;
import com.jps.finserv.markets.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(App.class);
	static Utils utils = new Utils();

	private static class StockData implements Runnable {
		public void run() { 
			/*
			HelloWatson watson = new HelloWatson();
			watson.run();

			DataBankCallReport callReport = new DataBankCallReport();
			callReport.run();

			DBConnectionXmlDb xmlDBConn = new DBConnectionXmlDb();
			try {
				xmlDBConn.testConn("", "C:/Users/pshar/Dropbox/workspace/StockData/Inventory.xml");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			XbrlIntegrationImpl xbrlValidator = new XbrlIntegrationImpl();
			xbrlValidator.LoadValidate();

			ParserJAXB parseJAXB = new ParserJAXB();
			parseJAXB.LoadValidate();
			 */

			// Only need to execute following. It calls the daily data capture as well.
			// TODO: Create a map for datatype (dailyData, qrtData) and respective fileNames. This will allow us to retrieve all fileNames in one go and then to insert into different tables.  
			DataNasdaqFilingsImpl dataFinancials = new DataNasdaqFilingsImpl();
			HashMap<String, String> inputMap = dataFinancials.run();

			if ((inputMap != null) && (!inputMap.isEmpty())){
				DBConnectMySql jConn = new DBConnectMySql();
				Set<String> keys = inputMap.keySet();

				for (String key : keys){
					if (key.equals("DailyMarketData")){
						jConn.insertDailyMarketData(inputMap.get(key), "markets", "equities_2018");
						//String fileDailyMarketData = "Stocks_SortedPcntChange2018-02-15 T 10-22-57 AM EST.csv";
					}
					else if (key.equals("CompanyFinancialData")){
						String fileCompanyFinancialData = inputMap.get(key);
						jConn.insertCompanyFinancialsData(fileCompanyFinancialData, "markets", "bsheet2018");
						//String fileDailyMarketData = "Stocks_SortedPcntChange2018-02-15 T 10-22-57 AM EST.csv";

						DBInsertIncomeStmts dbIncomeStmt = new DBInsertIncomeStmts();
						dbIncomeStmt.prepDataForInsert (fileCompanyFinancialData, "markets", "IncomeStmt2018");

						DBInsertCashFlows dbCashFlows = new DBInsertCashFlows ();
						dbCashFlows.prepDataForInsert (fileCompanyFinancialData, "markets", "CashFlows2018");
					}
				}
				DataCompanyIndustryInfo industryInfo = new DataCompanyIndustryInfo();
				industryInfo.run("markets", "IndustryBackground");

				DataDailyIndicators indicators = new DataDailyIndicators();
				indicators.run();
			}

			else{
				logger.warn("Empty set received for Daily Market and Company Financial datasets.");
				logger.warn("Check whether Flag is appropriately set for data retrieval.");
			}

			//CorrelationTest spark = new CorrelationTest();
			//spark.run();

			//DataDailyIndicators indicators = new DataDailyIndicators();
			//indicators.run();

			//RegressionAnalysis regression = new RegressionAnalysis();
			//regression.run();

			/*
			DBConnectMySql jConn = new DBConnectMySql();
			String tableHeader = "name,symbol,LastSale,open,high,low,close,netChange,pcntChange,volume,high52Weeks,low52Weeks,dividend,yield,peRatio,ytdPcntChange,exchange,MarketCap,IPOYear,Sector,Industry,date,time";
			String query = "SELECT T1.name, T1.symbol, T2.LastSale, T1.open, T1.high, T1.low, T1.close, T1.netChange, T1.pcntChange, T1.volume, T1.high52Weeks, T1.low52Weeks, T1.dividend, T1.yield, T1.peRatio, T1.ytdPcntChange, T1.exchange, T2.MarketCap, T2.IPOYear, T2.Sector, T2.Industry, T1.date, T1.time FROM equities_2018_04 as T1 LEFT OUTER JOIN industrybackground as T2 on T1.symbol = T2.symbol WHERE T2.MarketCap like '$___.%B' and T2.Sector like 'Health Care' GROUP BY T1.symbol ORDER BY T1.volume desc";
			jConn.outSqlToCSV(query, tableHeader, 2);
			 */

			
			/*
			List<String> listSymbol = dataHistorical.generateListSymbols("equities_historic_data", "select distinct symbol from equities_historic_data");
			DataNasdaqFilingsImpl nasdaqRun = new DataNasdaqFilingsImpl();
			nasdaqRun.generateFinancialsData(listSymbol, "", true); 

			DBConnectMySql jConn = new DBConnectMySql();
			jConn.insertCompanyFinancialsData(fileCompanyFinancialData, "markets", "bsheet2018");

			DBInsertIncomeStmts dbIncomeStmt = new DBInsertIncomeStmts();
			dbIncomeStmt.prepDataForInsert (fileCompanyFinancialData, "markets", "IncomeStmt2018");

			DBInsertCashFlows dbCashFlows = new DBInsertCashFlows ();
			dbCashFlows.prepDataForInsert (fileCompanyFinancialData, "markets", "CashFlows2018");
			 */

			//CorrelationAnalysis lambda = new CorrelationAnalysis();
			//lambda.run();

			//TestRegression regression= new TestRegression();
			//regression.run();

			//DataPreMarket preMarket = new DataPreMarket();
			//preMarket.run("", "2018-04-12");
			//utils.runExternalExec("rscript", "C:\\Users\\pshar\\Dropbox\\workspace\\R Language Scripts\\Quant\\RegressionIndustryBased.R");

			//DailyMarketPrediction  predictor = new DailyMarketPrediction ();
			//predictor.run();

			//DataDailyMarketDataHistoricalJournal dataHistoricalJournal = new DataDailyMarketDataHistoricalJournal ();
			//dataHistoricalJournal.run();
			
			//DBQuarterlyData quarterlyData  = new DBQuarterlyData();
			//quarterlyData.prepDataForInsert();
			//S3Upload s3Upload = new S3Upload();
			//s3Upload.run("", false);
			
			//DataDailyIndicatorHistorical dailyHistoricalData = new DataDailyIndicatorHistorical();
			//dailyHistoricalData.run();
			
			//DBInsertMovingAverages movingAverages = new DBInsertMovingAverages();
			//movingAverages.run("C:\\Users\\pshar\\Dropbox\\workspace\\HelloPython");

			//AnalyticsNlpForWebLinks saAnalytics = new AnalyticsNlpForWebLinks();
			//saAnalytics.run();

		}
	}

	public static void main( String[] args )
	{
		//String startTimeProjectRun = utils.getTimeOfDay();
		long startTimeProjectRun = System.currentTimeMillis();
		logger.info( "Hello World From Stock Data!!!" );
		Thread thread = new Thread(new StockData());
		thread.run();
		//String endTimeProjectRun = utils.getTimeOfDay();
		long endTimeProjectRun = System.currentTimeMillis();
		//logger.info("Total duration of the run: "+utils.returnTimeDifference(startTimeProjectRun, endTimeProjectRun));
		logger.info("Total duration of the run: "+(endTimeProjectRun - startTimeProjectRun) / 1000+" seconds.");

		/*
		DateFormat formatter = new SimpleDateFormat("hh:mm:ss.SSS");
		String formattedTime = formatter.format((endTimeProjectRun - startTimeProjectRun)/1000);
		logger.info("Total duration of the run: "+formattedTime);
		 */
	}
}
















