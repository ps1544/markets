package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.util.*;
import com.jaunt.component.*;
import com.jps.finserv.markets.util.HttpGetExecutor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSecEdgarCompanyFilings {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DataSecEdgarCompanyFilings.class);

	private final String baseUrl = Config.SEC_EDGAR_BASE_URL;
	HttpGetExecutor httpGet = new HttpGetExecutor();

	/*
	 * Entry function for the class 
	 */
	public void run(String symbol, String testFinancialDocumentType){
		generateData(symbol, testFinancialDocumentType);
	}

	private void generateData(String symbol, String testFinancialDocumentType) {

		try{
			UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser)
			userAgent.settings.checkSSLCerts = false;

			/*
			 * In next few lines of code, we will:
			 * ... start with the base page where you can enter either company name or symbol
			 * ... locate the text field that asks for symbol name and fill it in with the symbol we are retrieving data for
			 * ... and finally locate the form that contains this text field and submit that form to get to next HTML page.  
			 */
			userAgent.visit(baseUrl);
			userAgent.doc.fillout("Ticker or CIK", symbol);
			String elementQuery = "<form id=\"fast-search\"";
			Form form = userAgent.doc.getForm(elementQuery);
			Document newDoc = form.submit();
			
			/*
			 * So, now we are on a page like this "https://www.sec.gov/cgi-bin/browse-edgar?CIK=GS&owner=exclude&action=getcompany". Here, we will
			 * ... locate text entry box with label "Filing Type:" and fill it in with desired form like 10-k, 10-q etc... and then submit the entire page with a button that has label "Search" 
			 */
			Document thirdDoc = null;
			int num10Qor10KInstances = Config.COUNT_SEC_NUM_DOCS;
			
				// TODO: Handle the case where symbol is incorrect so we don't end up failing later on. 
				logger.info("Retrieving financial documents for company symbol "+ symbol +" from: "+newDoc.getUrl());

				try {
					newDoc.fillout("Filing Type:", testFinancialDocumentType);
					thirdDoc = newDoc.submit("Search");
					if (thirdDoc != null) 
						logger.info("The "+ testFinancialDocumentType +" documents for company symbol "+ symbol +" are available at: "+thirdDoc.getUrl());
					else{
						logger.error("Could not retrieve financial documents for symbol \""+ symbol +"\"");
					}
				}
				catch(NotFound e){
					logger.error("Could not retrieve financial documents for symbol \""+ symbol +"\"");// with URL: \n"+thirdDoc.getUrl()+"\n");
					logger.error("Enter '"+symbol+"' under textbox 'Fast Search'  at URL 'https://www.sec.gov/edgar/searchedgar/companysearch.html' and "
							+ "validate whether SEC provides data for this company.");
					return;
				}
				/*
				 * Now, we are on a page that has HTML links for kind of data (say 10-k, 10-q)  that we requested. 
				 * We now look for URL links for the end HTML page we are interested in and call another function to do the job.
				 * Number of individual 10-k, 10-Q instances to return data for. 1 instance means data for one quarter 
				 * or one year will be returned.  These are sorted at SEC site and within this program, so latest is (are) guaranteed.  
				 */    

				String tag = "<a>";
				String searchPattern = "&nbsp;Interactive Data";

				List<String> list10Qor10KInstances = traverseInteractiveData(thirdDoc.getUrl(), symbol, tag, searchPattern, num10Qor10KInstances );

				/*
				 * Now recursively go through an individual 10-q or 10-k page like following: 
				 * "https://www.sec.gov/cgi-bin/viewer?action=view&cik=70858&accession_number=0000070858-17-000046&xbrl_type=v"
				 * Here, we can retrieve all data by simply visiting link pointed to by text "View Excel Document". Need to do this only once per page and therefore numInstances = 1. 
				 */
				if ((list10Qor10KInstances != null ) && (!list10Qor10KInstances.isEmpty())){
					for (String single10Qor10K : list10Qor10KInstances){
						int numInstances = 1;
						String viewExcelDocs = "View Excel Document"; // String to be searched on HTML page like: https://www.sec.gov/cgi-bin/viewer?action=view&cik=70858&accession_number=0000070858-17-000046&xbrl_type=v
						List<String> listString = traverseInteractiveData(single10Qor10K, symbol,  tag, viewExcelDocs, numInstances);
						for (String tempStr : listString){
							UserAgent newUA = new UserAgent();
							// Now, download the actual file. AFAIK Jaunt doesn't do this, so need to account for this ourselves. 
							String localFilename = testFinancialDocumentType+"_"+symbol+".xlsx";
							Utils.downloadFromUrl(tempStr, localFilename);
						}
					}
				}
				else {
					logger.error("Could not retrieve \"Interactive Data\" links for financial document type \""+testFinancialDocumentType+"\" for symbol: "+symbol);
				}
			
			
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.warn("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (IOException e) {e.getMessage();} 
	}

	/**
	 * 
	 * @param url
	 * @param tag
	 * @param searchPattern
	 */
	private List<String> traverseInteractiveData(String url, String symbol, String tag, String searchInnerTextPattern, int numInstances) {

		UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser)
		userAgent.settings.checkSSLCerts = false;
		List<String> listUrls = new LinkedList<String>();
		int num = 0; 

		try{
			userAgent.visit(url);
			Elements urls = userAgent.doc.findEvery(tag);

			for (Element element : urls) {
				if (((element.innerText()).contains(searchInnerTextPattern))) {
					String temp = element.getAt("href");
					if (num < numInstances){
						++num;
						//logger.info("Retrieving "+testFinancialDocumentType.toUpperCase()+ "document(s) for symbol "+symbol+ " with URL:  "+temp);
						logger.info("Visiting URL: "+temp);
						listUrls.add(temp);
					}
					else
						break; // Come out as soon as condition is satisfied. No need to loop further through the <a> links. 
				}
			}
		}
		catch(JauntException e) {
			logger.warn("JauntException: "+e.getMessage()); e.printStackTrace();
		}
		return listUrls;
	}

}
