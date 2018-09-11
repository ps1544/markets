package com.jps.finserv.markets.equities;


/**
 * Required pre-set constants for overall FINSERV application
 * <p>
 *  
 */
public class Config {

	// IBM Watson account constants
	public static final String WATSON_API_VERSION_DATE		= "2017-11-07";
	public static final String WATSON_ENDPOINT = "https://gateway.watsonplatform.net/discovery/api/";
	public static final String WATSON_NEWS_ENV_ID = "system";
	public static final String WATSON_NEWS_ENV_COLLECTION = "news-en";

	// IBM Watson private account details
	public static final String WATSON_USERNAME = "a4dd1d34-1518-4729-acba-72782cb84e08";
	public static final String WATSON_PWD = "yZZ6aNABAPQz";

	// SEC Edgar details. See DataSecEdgarCompanyFilings.java
	public static final String SEC_EDGAR_BASE_URL = "https://www.sec.gov/edgar/searchedgar/companysearch.html";
	public static final int COUNT_SEC_NUM_DOCS= 0;
	/*
	 *  Whether to look for "10-Q" or "10-K" or for something else...
	 *  Must succeed when entered under "Filing Type" field on a 
	 *  page like: https://www.sec.gov/cgi-bin/browse-edgar?CIK=GS&owner=exclude&action=getcompany&Find=Search
	 */
	public static final String FINANCIAL_DATATYPE = "10-Q";

	// For filtering stocks based on daily data movement
	public static final boolean INSERT_DAILY_MARKET_DATA_INTO_CATALOG = true; // Controls the run for daily market capture
	public static final boolean GENERATE_DAILY_DATA_NASDAQ_GLOBAL = true;
	public static final boolean GENERATE_DAILY_DATA_NYSE = true;
	public static final boolean GENERATE_DAILY_DATA_NASDAQ_CAPITAL = true;
	public static final boolean GENERATE_DAILY_DATA_NYSE_AMERICAN = true;

	public static final boolean GENERATE_SQL_DUMP_DAILY_MARKET = false;
	
	
	// Company financal data but not from SEC/ EDGAR. See CompanyNasdaqFinancialsDataGeneration.java and GenerateDailyMarketData.java
	public static final String NASDAQ_BASE_URL = "http://www.nasdaq.com/symbol/";
	public static final boolean GENERATE_SQL_DUMP_COMPANY_FINANCIALS = false;
	public static final boolean NASDAQ_CHECK_QUARTERLY_DATA = true;
	public static final double MIN_OPEN_VALUE_THRESHOLD = 10.00;
	public static final int MIN_VOLUME_THRESHOLD = 5000000;
	public static final int NUMBER_STOCK_SYMBOLS = 0;

	// FDIC / FFIEC Call Reports
	public static final String FFIEC_CALL_REPORT_BASE_URL = "https://cdr.ffiec.gov/public/ManageFacsimiles.aspx";
	public static final String FFIEC_CALL_REPORT_VIEW_FACSIMILE_BASE_URL = "https://cdr.ffiec.gov/public/ViewFacsimileDirect.aspx?ds=";

}
