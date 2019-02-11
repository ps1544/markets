package com.jps.finserv.markets.callreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbrlapi.data.Store;

public class ParserXSD {

	private final Logger logger = (Logger) LoggerFactory.getLogger(ParserXSD.class);
	private static String file1 = "C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample/Call_123.xbrl";
	private static String schemaFile = "C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample/call-report031-2017-09-30-v154.xsd";

	public void LoadValidate(){

		// validate the SAX representation of XML against DTD
		try {
			validateXSDSax();
		} 
		catch (ParserConfigurationException | IOException e) {e.printStackTrace();}
	}


	private void validateXSDSax() throws ParserConfigurationException, IOException {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new File(schemaFile));
			Validator validator = schema.newValidator();
			validator.validate(new SAXSource(new InputSource(new FileInputStream(new File(file1)))));
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

}