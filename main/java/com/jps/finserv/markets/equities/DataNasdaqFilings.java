package com.jps.finserv.markets.equities;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataNasdaqFilings{
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataNasdaqFilings.class);

	private final static String nasdaqGlobal = "NASDAQ Global";
	private final static String nasdaqCapital = "NASDAQ Capital";
	private final static String nyse = "NYSE";
	private final static String nyseAmerican = "NYSE American";
	private final static String etfs = "ETFs";
	private final static String preferredStocks = "Preferred Stocks";

	//TODO: Move following to Config class
	private final static String urlNasdaqGlobalMarket = "http://www.wsj.com/mdc/public/page/2_3024-Nasdaq.html?mod=mdc_uss_pglnk";
	private final static String urlNasdaqCapitalMarket = "http://www.wsj.com/mdc/public/page/2_3024-SCAP.html?mod=topnav_2_3024";
	private final static String urlNyse = "http://www.wsj.com/mdc/public/page/2_3024-NYSE.html?mod=topnav_2_3024";
	private final static String urlNyseAmerican = "http://www.wsj.com/mdc/public/page/2_3024-AMEX.html?mod=topnav_2_3024";
	private final static String urlEtfs = "http://www.wsj.com/mdc/public/page/2_3024-USETFs.html?mod=mdc_uss_pglnk";
	private final static String urlPreferredStocks = "http://www.wsj.com/mdc/public/page/2_3024-Preferreds.html?mod=mdc_uss_pglnk";

	private HashMap<String, String> mapNasdaqGlobal =  new HashMap<String, String>();
	private HashMap<String, String> mapNasdaqCapital =  new HashMap<String, String>();
	private HashMap<String, String> mapNyse =  new HashMap<String, String>();
	private HashMap<String, String> mapNyseAmerican =  new HashMap<String, String>();
	private HashMap<String, String> mapEtfs =  new HashMap<String, String>();
	private HashMap<String, String> mapPreferredStocks =  new HashMap<String, String>();
	
	protected HashMap<String, String> getNasdaqGlobalWebLinkMap() {
		mapNasdaqGlobal.put(nasdaqGlobal, urlNasdaqGlobalMarket);
		return mapNasdaqGlobal;
	}
	
	protected HashMap<String, String> getNasdaqCapitalWebLinkMap() {
		mapNasdaqCapital.put(nasdaqCapital, urlNasdaqCapitalMarket);
		return mapNasdaqCapital;
	}
	
	protected HashMap<String, String> getNyseWebLinkMap() {
		mapNyse.put(nyse, urlNyse);
		return mapNyse;
	}
	
	protected HashMap<String, String> getNyseAmericanWebLinkMap() {
		mapNyseAmerican.put(nyseAmerican, urlNyseAmerican);
		return mapNyseAmerican;
	}
	
	protected HashMap<String, String> getEtfWebLinkMap() {
		mapEtfs.put(etfs, urlEtfs);
		return mapEtfs;
	}
	
	protected HashMap<String, String> getPreferredStocksWebLinkMap() {
		mapPreferredStocks.put(preferredStocks, urlPreferredStocks);
		return mapPreferredStocks;
	}
	
}