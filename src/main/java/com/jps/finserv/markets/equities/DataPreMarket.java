package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.analytics.DailyMarketPrediction;
import com.jps.finserv.markets.util.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Look into CNN Money for this kind of pre-market data as well
// http://money.cnn.com/quote/quote.html?symb=AAPL
public class DataPreMarket {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataPreMarket.class);

	// Instantiate external and helper classes
	UserAgent userAgent = new UserAgent();      
	Utils utils = new Utils();
	String baseUrl = "https://www.nasdaq.com/symbol/";
	String baseUrlCNN = "http://money.cnn.com/quote/quote.html?symb=";

	DataDailyMarketHistorical dailyMarket = new DataDailyMarketHistorical();
	//DailyMarketPrediction dailyPrediction = new DailyMarketPrediction();
	
	public Map<String, String>  run(String query, String date){

		Map<String, String> mapSymbolPreMktPrice = new HashMap<String, String>();
		logger.info("Generating pre market expections for list of stocks.");
		List<String> listAllSymbols = new LinkedList<String>();
		 
		
		if ((query == "") || (query.isEmpty()))
			//TODO: Get the last trading day accounting for weekends / holidays, and the pass that instead of the hard-coded date below
			
			query = "select distinct symbol from equities_2018 where date like '"+date+"' order by volume desc limit 1,300;";
		// query = "select distinct symbol from industrybackground where sector like 'Technology' and industry like 'Computer Software%'";
		//String query = "select distinct symbol from industrybackground where sector like 'Health Care' and industry like 'Major Pharma%' and (marketcap like '$___.%B' OR marketcap like '$__.%B') order by MarketCap desc";

		listAllSymbols = dailyMarket.generateListSymbols("equities_2018", query);

		for (String symbol : listAllSymbols){
			String temp = generateDataUsingNasdaq(symbol);
			if (temp.matches(",")){
				logger.warn("Data not found for symbol: "+symbol+" on primary target site.");
				temp = generateDataUsingCNNMondey(symbol);
				if (temp.matches(","))	
					logger.warn("Data not found for symbol: "+symbol+" on secondary target site either.");
			}
			mapSymbolPreMktPrice.put(symbol, temp);
		}
		Set<String> keySet = mapSymbolPreMktPrice.keySet();

		for (String key : keySet){
			String value = mapSymbolPreMktPrice.get(key);
			if (!(value.matches(",")))
				logger.info("Symbol: "+key+", PreMarket Data: LastSale, PcntChange "+value);
		}
		return mapSymbolPreMktPrice;
	}

	/**
	 * 
	 * @param symbol
	 * @return
	 */
	public String generateDataUsingCNNMondey(String symbol) {
		String pcntChange = "";
		String lastSale = "";
		String url = baseUrlCNN.concat(symbol.trim());

		try {
			Document document = userAgent.visit(url);

			Element tblDataLastSale = document.findFirst("<td class=\"wsod_last\"");
			if (tblDataLastSale  != null){
				Element spanLastSale = tblDataLastSale.findFirst("<span stream=\"last_\\*\"");
				if (spanLastSale != null)
					lastSale = spanLastSale.innerText().replace(",", "");
			}
			else
				logger.error("No pre-market data available for any stock symbol.");

			Element tblDataPcntChange = document.findFirst("td class=\"wsod_change\"");
			if (tblDataPcntChange != null){
				Element spanPcntChange = tblDataPcntChange.findFirst("<span stream=\"change_\\*\"");
				if (spanPcntChange != null)
					pcntChange = spanPcntChange.innerText().replace("+", "").replace(",", ""); 
			}
			else 
				logger.warn("No pre-market pcnt change data available for symbol: "+symbol);
		}
		catch(NotFound e){         
			e.getMessage();
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return (lastSale+","+pcntChange).trim();
	}


	/**
	 * 
	 * @param symbol
	 * @return
	 */
	public String generateDataUsingNasdaq(String symbol) {
		String pcntChange = "";
		String lastSale = "";
		String url = baseUrl.concat(symbol).concat("/premarket");

		try {
			Document document = userAgent.visit(url);
			Element divContainer = document.findFirst("<div id=\"qwidget_quote\">");
			if (divContainer  == null)
				logger.error("No pre-market data available for any stock symbol.");

			Element divPcntChange = divContainer.findFirst("div id=\"qwidget_percent\"");
			if (divPcntChange != null)
				pcntChange = divPcntChange.innerText().replace("%", "").replace(",", "");
			else 
				logger.warn("No pre-market pcnt change data available for symbol: "+symbol);

			Elements divLastSale = divContainer.findEvery("<div id=\"qwidget_lastsale\" class=\"qwidget-dollar\">");
			if (divLastSale != null)
				lastSale = divLastSale.innerText().replace("$", "").replace(",", "");
			else 
				logger.warn("No pre-market last sale data available for symbol: "+symbol);

			Elements divReds = divContainer.findEvery("div class=\"marginLR10px arrow-red\"");
			Elements divGreens = divContainer.findEvery("div class=\"marginLR10px arrow-green\"");
			// Have to be careful if we can't retrieve the +ve or -ve movement in pcnt values 
			if ((divReds == null) && (divGreens == null)){
				logger.error("Could not retrieve information on up or down movement in pre-market data retrieval.");
				return "";
			}
			// If the value is -ve, append the "-" sign
			else if ((divReds != null) && (divReds.size() > 0)){
				pcntChange = "-".concat(pcntChange);
				return lastSale+","+pcntChange;
			}
			// Otherwise, the value is +ve and no adjustment required

		}
		catch(NotFound e){         
			e.getMessage();
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		//		if (((lastSale.isEmpty()) && (pcntChange.isEmpty())) || ((lastSale == "") && (pcntChange == "")))

		return (lastSale+","+pcntChange).trim();
	}
}

