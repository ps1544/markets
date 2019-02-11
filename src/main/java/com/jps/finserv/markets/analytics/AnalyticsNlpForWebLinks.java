package com.jps.finserv.markets.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.service.exception.BadRequestException;

import com.jps.finserv.markets.catalog.DBConnectMySql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyticsNlpForWebLinks extends LanguageAnalysis{

	private final Logger logger = (Logger) LoggerFactory.getLogger(AnalyticsNlpForWebLinks.class);
	// List of companies that we will compare for either symbol or name	
	private List<String> companies = new ArrayList<String>();
	// Table upon which the query needs to be run
	private String tblName = "equities_2018";

	private Set<String> linksNoteworthy = new HashSet<String>();
	// Keep only one of these set to TRUE. Both TRUE won't work as expected
	private final boolean EntityKeywords = true;
	private final boolean SentimentAnalysis = false;  

	/**
	 * Downloads news events form baseUrl based on search pattern and pushes 
	 * them to Watson NLP for processing. Retrieves JsonNode results and 
	 * returns that as a  list of JsonNodes. 
	 * @return Map of URL, JsonObject where url is the link to news item and JsonNode object is the NLP output from Watson
	 */

	private Map<String, JsonNode> collectNewsNLPOutput (String urlBase, String pattern, int max){
		UserAgent userAgent = new UserAgent();
		userAgent.settings.checkSSLCerts = false;

		ObjectMapper mapper = new ObjectMapper();
		Map<String, JsonNode> mapUrlNlpOutput = new HashMap<String, JsonNode>();

		try{
			userAgent.visit(urlBase); // Visit the base URL with symbol

			Elements urls = userAgent.doc.findEvery(pattern);
			int count = 0;
			Set<String> uniqueLinks = new HashSet<String>();

			if (urls.size() != 0){
				for (Element url: urls){
					if (count < max){
						String tempStr = url.getAt("href"); 
						// Had to add this check because some urls had duplicate links 
						if (!uniqueLinks.contains(tempStr)){
							uniqueLinks.add(tempStr);
							//String response = "";
							String response = generateNLP(tempStr);
							if (response != ""){
								logger.debug(response);
								JsonNode jNode = mapper.readTree(response);
								mapUrlNlpOutput.put(url.getAt("href"), jNode);
							}
						}
					}
				}
			}
			else
				logger.warn("Did not find search pattern:\""+pattern+"\" in URL:"+ urlBase);
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return mapUrlNlpOutput;
	}	

	/**
	 * Runs IBM Watson NLP on the given link
	 * @param url URL on which Watson NLP is to be run
	 * @return 
	 */
	private String generateNLP(String url){
		int count = 0;
		logger.info("Processing natural language for link:	"+url);
		// Create a list of JsonObjects and take the input therein. Then pass it on to find the company names
		++count;
		String response = "";
		try{
			if (EntityKeywords)
				response = analyzeLanguage(url);

			if (SentimentAnalysis){
				List<String> targets = buildTargets();
				response = analyzeLanguageSentiment(url, targets);
			}
		}
		catch(BadRequestException e){         // catch exception from Watson 
			if (e.getMessage().equals("cannot locate keyphrase")){
				--count; // Need to continue on with more pages, as this one doesn't really count
				logger.warn("No input phrases found in the concerned article");
			}
			else
				logger.error("Watson API failed to retrieve URL: "+e.getMessage());
		}
		return response;
	}

	/**
	 * Parses the JSON returned by NLP
	 * @param inputUrlNlpOutputMap Map containing URL of the web link and JSON for NLP analysis of that link
	 */
	private void reviewCompanyInformation(Map<String, JsonNode> inputUrlNlpOutputMap){

		if ((inputUrlNlpOutputMap == null) || (inputUrlNlpOutputMap.isEmpty())) {
			logger.error("Empty symbol list received after news analysis. "
					+ "Cannot relay news events back to company symbols and names.");
			return;
		}
		else{
			traverseEntityResponse(inputUrlNlpOutputMap);
			traverseKeywordResponse(inputUrlNlpOutputMap);
		}
	}

	/**
	 * Traverses ENTITY response returned by Watson NLP for input URLs
	 * Links output with the stock symbols in the catalog
	 * @param inputUrlNlpOutputMap Map of URLs as keys and their Watson NLP output in JSON format as values 
	 */
	private void traverseEntityResponse(Map<String, JsonNode> inputUrlNlpOutputMap){

		String query = "";
		Set<String > urlSet = inputUrlNlpOutputMap.keySet();
		try{
			for (String url : urlSet){
				JsonNode jNode = inputUrlNlpOutputMap.get(url);
				if (jNode.has("entities")){
					JsonNode jsonEntitiesArray = jNode.get("entities");
					if ((jsonEntitiesArray == null) || (jsonEntitiesArray.isEmpty(null)))  {
						logger.warn("Information for \"entity\" not received during NLP for URL:"+url);
						continue;
					}
					for (JsonNode jNodeSingleEntity : jsonEntitiesArray){
						if (jNodeSingleEntity.has("type")){
							String type = jNodeSingleEntity.get("type").asText();
							if (type.matches("Company")){
								logger.info("_________________________");
								String companyName = jNodeSingleEntity.get("text").asText();
								logger.info("URL: "+url);  
								logger.info("Company name / symbol: "+companyName);

								String relevance = "";
								String sentiment = "";
								if (jNodeSingleEntity.has("relevance")){
									relevance = jNodeSingleEntity.get("relevance").asText(); 
									logger.info("		Relevance: "+relevance);
								}
								if (jNodeSingleEntity.has("sentiment")){
									JsonNode jNodeSentiment = 	jNodeSingleEntity.get("sentiment");
									sentiment = jNodeSentiment.get("score").asText();
									logger.info("		Sentiment: "+jNodeSentiment.get("score"));
								}
								// Consider link noteworthy if relevance > 0.9 AND (sentiment < 0.5 OR sentiment > 0.6)
								if ((Double.parseDouble(relevance.trim())) > 0.9){
									if (			((Double.parseDouble(sentiment.trim())	) >  0.6) 		|| 			((Double.parseDouble(sentiment.trim())	) < -0.5)			)	{
										logger.info("Entity: "+companyName+" Relevance: "+relevance+" Sentiment: "+sentiment);
										logger.info("Adding URL in shortlist:"+url);
										linksNoteworthy.add(url);
									}
								}
								if (jNodeSingleEntity.has("emotion")){
									JsonNode jNodeEmotion = 	jNodeSingleEntity.get("emotion");
									String anger = jNodeEmotion .get("anger").asText();
									String disgust = jNodeEmotion .get("disgust").asText();
									String fear = jNodeEmotion .get("fear").asText();
									String joy = jNodeEmotion .get("joy").asText();
									String sadness = jNodeEmotion .get("sadness").asText();

									logger.info("		Anger: "+anger);
									logger.info("		Disgust: "+disgust);
									logger.info("		Fear: "+fear);
									logger.info("		Joy: "+joy);
									logger.info("		Sadness: "+sadness);

									// Considering "joy" numeric value as a criteria to affect market condition
									if ((Double.parseDouble(joy.trim())) > 0.5){
										logger.info("Joy: "+joy);
										logger.info("Adding URL in shortlist:"+url);
										linksNoteworthy.add(url);
									}
								} 

								companies.add(companyName);
								// Now, check whether we have the company name in our catalog, if so, get their daily market returns for the month
								query = "SELECT * FROM "+tblName+ " WHERE name like '%"+companyName+"%' OR symbol like '%"+companyName+"%'";
								//logger.debug("Query: "+query);
								//logger.info("Recent daily returns for '%"+companyName+"%'");
								//ResultSet rs = mySqlInstance.executeQuery(query);
							}
							else if (type.matches("Person")){
								logger.info("_________________________");
								String personName = jNodeSingleEntity.get("text").asText();
								logger.info("URL: "+url);  
								logger.info("Name: "+personName);

								if (jNodeSingleEntity.has("relevance")){
									logger.info("		Relevance: "+jNodeSingleEntity.get("relevance"));
								}
								if (jNodeSingleEntity.has("emotion")){
									JsonNode jNodeEmotion = 	jNodeSingleEntity.get("emotion");
									String anger = jNodeEmotion .get("anger").asText();
									String disgust = jNodeEmotion .get("disgust").asText();
									String fear = jNodeEmotion .get("fear").asText();
									String joy = jNodeEmotion .get("joy").asText();
									String sadness = jNodeEmotion .get("sadness").asText();

									logger.info("		Anger: "+anger);
									logger.info("		Disgust: "+disgust);
									logger.info("		Fear: "+fear);
									logger.info("		Joy: "+joy);
									logger.info("		Sadness: "+sadness);
								} 
							}
						}
						//logger.info(jsonEntitiesArray.toString());
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Traverses KEYWORD response returned by Watson NLP for input URLs
	 * Links output with the stock symbols in the catalog
	 * @param inputUrlNlpOutputMap Map of URLs as keys and their Watson NLP output in JSON format as values 
	 */
	private void traverseKeywordResponse(Map<String, JsonNode> inputUrlNlpOutputMap){

		Set<String > urlSet = inputUrlNlpOutputMap.keySet();

		try{
			for (String url : urlSet){
				JsonNode jNode = inputUrlNlpOutputMap.get(url);
				if (jNode.has("keywords")){
					JsonNode jsonKeywordsArray = jNode.get("keywords");
					if ((jsonKeywordsArray == null) || (jsonKeywordsArray.isEmpty(null)))  {
						logger.warn("Information for \"entity\" not received during NLP for URL:"+url);
						continue;
					}
					for (JsonNode jNodeSingleEntity : jsonKeywordsArray){
						if (jNodeSingleEntity.has("text")){
							logger.info("_________________________");
							String keyword = jNodeSingleEntity.get("text").asText();
							logger.info("URL: "+url);  
							logger.info("Keyword referred: "+keyword);

							String relevance = "";
							String sentiment = "";

							if (jNodeSingleEntity.has("relevance")){
								relevance = jNodeSingleEntity.get("relevance").asText(); 
								logger.info("		Relevance: "+relevance);
							}
							if (jNodeSingleEntity.has("sentiment")){
								JsonNode jNodeSentiment = 	jNodeSingleEntity.get("sentiment");
								sentiment = jNodeSentiment.get("score").asText();
								logger.info("		Sentiment: "+jNodeSentiment.get("score"));
							}
							// Consider link noteworthy if relevance > 0.9 AND (sentiment < -0.5 OR sentiment > 0.6)
							if ((Double.parseDouble(relevance.trim())) > 0.9){
								if (			((Double.parseDouble(sentiment.trim())	) >  0.6) 		|| 			((Double.parseDouble(sentiment.trim())	) < -0.5)			)	{
									logger.info("Keyword: "+keyword+" Relevance: "+relevance+" Sentiment: "+sentiment);
									logger.info("Adding URL in shortlist:"+url);
									linksNoteworthy.add(url);
								}
							}
							if (jNodeSingleEntity.has("emotion")){
								JsonNode jNodeEmotion = 	jNodeSingleEntity.get("emotion");
								String anger = jNodeEmotion .get("anger").asText();
								String disgust = jNodeEmotion .get("disgust").asText();
								String fear = jNodeEmotion .get("fear").asText();
								String joy = jNodeEmotion .get("joy").asText();
								String sadness = jNodeEmotion .get("sadness").asText();

								logger.info("		Anger: "+anger);
								logger.info("		Disgust: "+disgust);
								logger.info("		Fear: "+fear);
								logger.info("		Joy: "+joy);
								logger.info("		Sadness: "+sadness);

								// Considering "joy" numeric value as a criteria to affect market condition
								if ((Double.parseDouble(joy.trim())) > 0.5){
									//logger.info("Joy: "+joy);
									//logger.info("Adding URL in shortlist:"+url);
									linksNoteworthy.add(url);
								}
								else if ((Double.parseDouble(fear.trim())) < -0.6){
									//logger.info("Fear: "+fear);
									//logger.info("Adding URL in shortlist:"+url);
									linksNoteworthy.add(url);
								}


							} 
						} 
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> buildTargets(){
		List<String> targets = new ArrayList<String>();

		targets.add("Dow Jones Industrial Average");
		targets.add("Dow Futures");
		targets.add("DJIA"); 
		targets.add("NASDAQ");
		targets.add("Nasdaq Composite");
		targets.add("S&P 500");
		targets.add("Stoxx");
		targets.add("Nikkei");
		targets.add("FTSE");
		targets.add("Volatility");
		targets.add("VIX");
		targets.add("stocks");
		targets.add("bonds");
		targets.add("interest rates");
		targets.add("crash");

		//targets.add("Federal Reserve");
		//targets.add("WSJ Dollar Index");

		/*Temp adjustments to focus on 10-K and 10-Q of Pfizer 
		targets.add("R&D");
		targets.add("profits");
		targets.add("losses");
		targets.add("Research & Development");
		targets.add("Pfizer Essential Health");
		targets.add("Pfizer Innovative Health");
		 */
		return targets;
	}

	/**
	 * Returns a Map of base URL to be traversed along with HREF link 
	 * patterns to be searched and number of such patterns to be traversed. 
	 * @return Map<BaseURL, Map<urlPattern, number of pages to be traversed>>
	 * //TODO: Need to move these constants in their own Java file. Also simplify the use of aggregates, way too complicated right now.
	 */
	private Map<String, Map<String, Integer>> mapBaseUrlWithHrefSearchPattern(){
		Map<String, Map<String, Integer>> returnMap = new HashMap<String, Map<String, Integer>>();

		String urlSeekingAlpha = "https://seekingalpha.com/market-news";
		int numSeekingAlphaLinksToTraverse = 0;
		String patternSA = "<a class=\"add-source-assigned\" >";
		Map<String, Integer> tempSAMap = new HashMap<String, Integer>();
		tempSAMap.put(patternSA, numSeekingAlphaLinksToTraverse);
		returnMap.put(urlSeekingAlpha, tempSAMap);

		String urlSeekingAlphaAll = "https://seekingalpha.com/market-news/all";
		int numSeekingAlphaAllLinksToTraverse = 0;
		String patternSAAll = "<a sasource=\"title_mc_quote\">";
		Map<String, Integer> tempSAAllMap = new HashMap<String, Integer>();
		tempSAAllMap.put(patternSAAll, numSeekingAlphaAllLinksToTraverse);
		returnMap.put(urlSeekingAlphaAll, tempSAAllMap);

		String urlWSJEconomy = "https://www.wsj.com/news/economy";
		int numWSJEconomyLinksToTraverse = 0;
		String patternWSJEconomy = "<a data-referer=\"/news/economy\" >";
		Map<String, Integer> tempWSJEconomyMap = new HashMap<String, Integer>();
		tempWSJEconomyMap.put(patternWSJEconomy, numWSJEconomyLinksToTraverse);
		returnMap.put(urlWSJEconomy, tempWSJEconomyMap);

		String urlWSJMarkets = "https://www.wsj.com/news/markets";
		int numWSJMarketLinksToTraverse = 5;
		String patternWSJMarkets = "<a class=\"wsj-headline-link\" >";
		Map<String, Integer> tempWSJMarketMap = new HashMap<String, Integer>();
		tempWSJMarketMap.put(patternWSJMarkets, numWSJMarketLinksToTraverse);
		returnMap.put(urlWSJMarkets, tempWSJMarketMap);

		return returnMap;
	}

	/**
	 * Adds ability to run Watson NLP on a listing manually added 
	 * @return
	 */
	private Map<String, JsonNode>  acctManualURLList() {
		List<String> listManualURLs = new LinkedList();
		
		listManualURLs .add("https://www.wsj.com/articles/how-wells-fargos-1-billion-settlement-affects-its-earnings-1524250158");
		listManualURLs .add("https://www.bloomberg.com/gadfly/articles/2018-04-13/wells-fargo-earnings-bank-still-hasn-t-dealt-with-its-scandal");
		listManualURLs .add("https://www.bloombergquint.com/markets/2018/04/13/wells-fargo-s-revenue-beats-estimates-despite-punishment-by-fed");
		
		/*
		listManualURLs .add("https://www.wsj.com/articles/no-bad-news-counts-as-good-news-at-ge-1524237214");
		listManualURLs .add("https://www.wsj.com/articles/ge-takes-hit-from-old-mortgage-unit-but-says-turnaround-on-track-1524222453");
		listManualURLs .add("https://www.nytimes.com/2018/04/20/technology/ge-earnings.html");
		listManualURLs .add("https://www.bloomberg.com/gadfly/articles/2018-04-20/ge-earnings-a-polite-clap-for-clearing-a-lowered-bar");
		listManualURLs .add("https://www.bloomberg.com/news/articles/2018-04-20/ge-has-something-for-everyone-so-bull-bear-debate-to-rage-on");	
		*/
		ObjectMapper mapper = new ObjectMapper();
		Map<String, JsonNode> mapUrlNlpOutput = new HashMap<String, JsonNode>();
		
		try{
			for (String tempStr : listManualURLs){
				String response = generateNLP(tempStr);
				if (response != ""){
					logger.debug(response);
					JsonNode jNode = mapper.readTree(response);
					mapUrlNlpOutput.put(tempStr, jNode);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return mapUrlNlpOutput;
	}
	
	/**
	 * Entry function for the class. Retrieves a map of URLs and pattern to be
	 * searched. Then calls the function to feed this text output into Watson
	 * based NLP feeds.  
	 */
	public void run(){

		/* Temp adjustments to focus on 10-K and 10-Q of Pfizer 
		String temp10KStr = "https://www.sec.gov/Archives/edgar/data/78003/000007800318000027/pfe-12312017x10kshell.htm";
		List<String> targets = buildTargets();
		//analyzeLanguage(temp10KStr);
		analyzeLanguageSentiment(temp10KStr, targets);
		 */

		Map<String, Map<String, Integer>> inputMap = mapBaseUrlWithHrefSearchPattern();
		Map<String, JsonNode> outputMap = new HashMap<String, JsonNode>();

		if ((inputMap == null) || (inputMap.isEmpty())){
			logger.error("Empty input map received for URL Natural Language Processing");
		}
		else{
			// If input map not empty or null, we continue on retrieving the keys which are a set of base URLs
			Set<String> keysBaseUrls = inputMap.keySet();
			for (String baseUrl : keysBaseUrls){
				Map<String, Integer> mapPatternAndCount = inputMap.get(baseUrl);
				Set<String> setPatterns = mapPatternAndCount.keySet();
				for (String pattern: setPatterns){
					int count = mapPatternAndCount.get(pattern);
					Map<String, JsonNode> tempMap; 
					if (count > 0){
						tempMap = collectNewsNLPOutput (baseUrl, pattern, count);
						// Null and empty checks are done downstream, simply call the function with the list. 
						outputMap.putAll(tempMap);
					}
				}
			}

			/*
			 *  Uncomment following couple of lines if anything is to be checked manually 
			 */
			Map<String, JsonNode> mapUrlJson = acctManualURLList();
			outputMap.putAll(mapUrlJson);
			
			// Everything traversed, now time to analyze the NLP output
			reviewCompanyInformation(outputMap);

			// Finally, see if we have noteworthy links to check
			// This will potentially decide the criteria whether this news can affect the market or not
			if (!linksNoteworthy.isEmpty())
			{
				logger.info("Following links were considered to have potential impact value above designated threshold:");
				for (String link: linksNoteworthy){
					logger.info(link);
				}
			}
		}
	} // run()
} // Class AnalyticsNlpForWebLinks{...}


