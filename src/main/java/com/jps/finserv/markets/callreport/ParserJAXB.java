package com.jps.finserv.markets.callreport;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

//import hello.jaxb.Collection;
import javax.xml.bind.*;
import javax.xml.bind.util.*;
import javax.xml.bind.helpers.*;
import javax.xml.parsers.ParserConfigurationException;
//import test.jaxb.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ParserJAXB {

	private final Logger logger = (Logger) LoggerFactory.getLogger(ParserJAXB.class);
	private static String file1 = "C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample/Call_123.xbrl";
	private static String schemaFile = "C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample/call-report031-2017-09-30-v154.xsd";

	public void LoadValidate(){

		// validate the SAX representation of XML against DTD
		try {
			validateXSDJaxb();
		} 
		catch (ParserConfigurationException | IOException e) {e.printStackTrace();}
	}


	private void validateXSDJaxb() throws ParserConfigurationException, IOException {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance("hello.jaxb");
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			// Collection collection= (Collection) unmarshaller.unmarshal(new File( file1));
			unmarshaller.unmarshal(new File( file1));
		} catch (JAXBException e) {e.printStackTrace();}
		
}
	
}