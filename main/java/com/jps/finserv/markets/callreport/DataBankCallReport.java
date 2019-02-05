package com.jps.finserv.markets.callreport;

import com.jaunt.*;
import com.jaunt.component.*;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBankCallReport {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DataBankCallReport.class);

	private final String baseUrl = Config.FFIEC_CALL_REPORT_BASE_URL;
	private final String baseViewFacsimileUrl = Config.FFIEC_CALL_REPORT_VIEW_FACSIMILE_BASE_URL;

	
	/*
	 * Entry function for the class 
	 */
	public void run(){
		//	generateData();
		// TODO: Need to create proper lists and enums for these and take out hard-coding here. 
		String typeReport = "call";
		String typeId = "id_rssd";
		int id = 480228;
		String date = "09302017";

		retrieveDownloadLinks(typeReport, typeId, id, date); 
	}


	private void retrieveDownloadLinks(String typeReport, String typeId, int id, String date) {
		UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser)
		userAgent.settings.checkSSLCerts = false;

		String adjustedViewFaxUrl = baseViewFacsimileUrl.concat(typeReport).concat("&idtype=").concat(typeId).concat("&id=")+id+"&date=".concat(date);
		//logger.debug("Here is the constructed URL: "+adjustedViewFaxUrl);
		String urlXbrlDnld = "";
		try{
			userAgent.visit(adjustedViewFaxUrl);
			logger.info("The call form is available at: \n"+userAgent.doc.getUrl()+"\n");
			Document newDoc = userAgent.doc.submit("Download XBRL");
			urlXbrlDnld  = newDoc.getUrl();
			logger.info("The Original Download XBRL form is available at: \n"+urlXbrlDnld +"\n");
			
			String localFilename= id+date+".xml"; //needs to be replaced with local file path
	//        Utils.downloadFromUrl(urlXbrlDnld, localFilename);
			
		
		} 
		catch(ResponseException e){logger.error("Failed to open URL:"+e.getRequestUrlData());	e.getMessage();}
		catch(SearchException e){logger.error("Failed to find requested button:"+e.getMessage());} 
		//catch (UnsupportedEncodingException e) {logger.error("Failed to decode the URL: \""+urlXbrlDnldDecoded+" with exception:"+e.getMessage());} 
		//catch (IOException e) {e.getMessage();} 
	}


	private void generateData() {

		UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser)
		userAgent.settings.checkSSLCerts = false;

		try{
			userAgent.visit(baseUrl);
			Form form = userAgent.doc.getForm("<form id=form1>");
			form.setSelect("ctl00$MainContentHolder$FacsimileSearchControl1$reportTypeDropDownList", "CALL");
			form.set("ctl00$MainContentHolder$FacsimileSearchControl1$FromDateDropdownlist", "106");
			form.setRadio("ctl00$MainContentHolder$FacsimileSearchControl1$identifierRButton", "identifierRButton");
			form.setTextField("ctl00$MainContentHolder$FacsimileSearchControl1$uniqueIDTextBox", "817824");
			Document newDoc = userAgent.doc.submit("Search");

			logger.info("The call form is available at: \n"+newDoc.getUrl()+"\n");

		}
		catch (ResponseException e){
			logger.error(e.getMessage());
		}
		catch (SearchException e){
			logger.error(e.getMessage());
		}
	}




}