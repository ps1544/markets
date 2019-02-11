package com.jps.finserv.markets.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;
import com.jps.finserv.markets.util.modReadFile;
import com.jps.finserv.markets.equities.Config;

import java.util.LinkedList;
import java.util.List;

/*
 * TODO: 3/1/18: Program redundant now by other classes.
 * Delete it later on.  
 */
public class AnalyticsWSJ extends LanguageAnalysis{

	private final Logger logger = (Logger) LoggerFactory.getLogger(AnalyticsWSJ.class);

	// Instantiate external and helper classes
	Utils utils = new Utils();
	UserAgent userAgent = new UserAgent();      

	/*
	 * Entry function for the class 
	 */

	public void  run(){
		UserAgent userAgent = new UserAgent();
		userAgent.settings.checkSSLCerts = false;
		String urlBase = "https://www.wsj.com/news/markets";
		List<String> returnListOutput = new LinkedList<String>();
		/*
		 * Downloads data from concerned URL hrefs into local CSV files. 
		 * Then merges those file removing duplicate headers and finally 
		 * pushes that cumulative output to a file.   
		 */

		try{
			userAgent.visit(urlBase); // Visit the base URL with symbol
			//Elements urls = userAgent.doc.findEvery("<a class=\"wsj-headline-link\" >");
			Elements urls = userAgent.doc.findEvery("<a data-referer=\"/news/economy\" >");
			int count = 0;
			int max =50;
			for (Element url: urls){
				if (count < max){
					String tempStr = url.getAt("href"); 
					logger.info("Running IBM Watson NLP against link:\n"+tempStr);
					analyzeLanguage(tempStr);
					++count;
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
	}
}