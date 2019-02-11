package com.jps.finserv.markets.catalog;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.analytics.DailyMarketPrediction;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.equities.DataDailyMarketHistorical;
import com.jps.finserv.markets.equities.DataNasdaqFilingsImpl;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;

/*
 * Adding this class much later and as you will see w/o much planning. 
 * The Q'ly data was being inserted as part of daily processing to ensure 
 * that Nasdaq site doesn't throw us out if hit by 100's of hits in no time. 
 * So, adding this in after a good 10 weeks or so by just pulling in existing 
 * functions from other classes. 
 */

public class DBQuarterlyData {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBQuarterlyData.class);

	// Flag to determine whether to retrieve quarterly or just annual financial data
	private boolean quarterlyData = Config.NASDAQ_CHECK_QUARTERLY_DATA;
	// Create an instance of the Utils class
	Utils utils = new Utils();
	DataDailyMarketHistorical dailyMarket = new DataDailyMarketHistorical();
	DailyMarketPrediction predictMarket = new DailyMarketPrediction();
	DataNasdaqFilingsImpl dataNasdaq = new DataNasdaqFilingsImpl();
	DBConnectMySql jConn = new DBConnectMySql();

	public void prepDataForInsert (){
		List<String> listInput = new LinkedList<String>();
		String fileCompanyFinancialData = "FinancialStmts".concat("_").concat(utils.getDateAndTime().concat(".csv"));
		fileCompanyFinancialData = fileCompanyFinancialData.replace(":", "-");
		String lastTradingDate = predictMarket.getLastTradingDate();
		//String query = "select distinct symbol from equities_2018 where symbol NOT in (select distinct symbol from bsheet2018) and date like '"+lastTradingDate+"' order by volume desc limit 0, 20;";
		String query = "select distinct symbol, volume from equities_2018 where date like '"+lastTradingDate+"' and symbol in (select distinct symbol from industrybackground "
				+ "where Snp500 like 1 and symbol not in (select distinct symbol from bsheet2018 where quarter like '1Q2018')) order by volume desc limit 0,50";
		listInput = dailyMarket.generateListSymbols("equities_2018", query);

		/*
		listInput.add("GS");
		listInput.add("MS");
		listInput.add("C");
		listInput.add("BAC");
		listInput.add("WFC");
		listInput.add("JPM");
		listInput.add("USB");
		listInput.add("UBS");
		listInput.add("DB");
		listInput.add("BLK");
		listInput.add("CS");
		listInput.add("ICE");
		listInput.add("SCHW");
		*/
		
		logger.info("The size of the list with symbols is: "+listInput.size()); 
		List<String> cumulativeOutput = dataNasdaq.generateFinancialsData(listInput, fileCompanyFinancialData, quarterlyData);
		if ((cumulativeOutput != null) && (!cumulativeOutput.isEmpty())){
			modOutCSV outCSV = new modOutCSV(fileCompanyFinancialData);
			outCSV.writeList(cumulativeOutput);
		}

		File file = new File(fileCompanyFinancialData);
		if (file.exists()){
			jConn.insertCompanyFinancialsData(fileCompanyFinancialData, "markets", "bsheet2018");
			//String fileDailyMarketData = "Stocks_SortedPcntChange2018-02-15 T 10-22-57 AM EST.csv";

			DBInsertIncomeStmts dbIncomeStmt = new DBInsertIncomeStmts();
			dbIncomeStmt.prepDataForInsert (fileCompanyFinancialData, "markets", "IncomeStmt2018");

			DBInsertCashFlows dbCashFlows = new DBInsertCashFlows ();
			dbCashFlows.prepDataForInsert (fileCompanyFinancialData, "markets", "CashFlows2018");
		}
	}
}
