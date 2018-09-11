package com.jps.finserv.markets.util;

import java.io.BufferedReader;
import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import scala.collection.mutable.HashSet;

/*
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
 */
public class Utils {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Utils.class);

	public String getDate(){
		Date date = new Date();
		String strTime = null;

		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			strTime = format.format(date);
			//System.out.println("Current Date Time : " + strTime);
			if (strTime == null){
				strTime = "-";
			}
		}

		catch(Exception e){
			e.printStackTrace();
		}

		return strTime;
	}

	/**
	 * Function to return quarter given an input date
	 * @param strDate Given date in form MM/DD/YYYY 
	 * @return quarter in the form nQYYYY (n in 1,2,3,4 and YYYY is year) 
	 */
	public String generateQuarterGivenDate(String strDate){
		// Assuming that if date comes in for March, June, September, December then it is for that quarter.
		// Otherwise, for other months, the quarter is one before. For example, June 2017, will be considered
		// a 2Q2017 release, but May 2017 will be considered a 1Q2017 release. 

		// This is messed up partially because Java returns Jan as 0 rather than 1. 

		String returnQuarter = "";
		HashSet<Integer> intSet = new HashSet<Integer>();
		intSet.add(Calendar.MARCH+1);
		intSet.add(Calendar.APRIL+1);
		intSet.add(Calendar.JUNE+1);
		intSet.add(Calendar.JULY+1);
		intSet.add(Calendar.SEPTEMBER+1);
		intSet.add(Calendar.OCTOBER+1);
		intSet.add(Calendar.DECEMBER+1);
		intSet.add(Calendar.MAY+1);
		intSet.add(Calendar.AUGUST+1);
		intSet.add(Calendar.NOVEMBER+1);

		HashSet<Integer> intSetB = new HashSet<Integer>();
		intSetB.add(Calendar.JANUARY+1);
		intSetB.add(Calendar.FEBRUARY+1);

		try {
			String[] temp = strDate.split("/");
			int inputMonth = Integer.parseInt(temp[0]);
			int inputDate = Integer.parseInt(temp[1]);
			int inputYear = Integer.parseInt(temp[2]);
			//logger.info("Date: "+strDate+ ". 	Year: "+inputYear+", Month: "+(inputMonth-1)+", Date: "+inputDate);

			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.MONTH, inputMonth-1);
			calendar.set(Calendar.DATE, inputDate);
			calendar.set(Calendar.YEAR, inputYear);

			int quarter = (calendar.get(Calendar.MONTH)+1)/ 3;
			if (intSet.contains(calendar.get(Calendar.MONTH)+1)){
				returnQuarter = String.valueOf(quarter).concat("Q");
				returnQuarter = returnQuarter.concat(String.valueOf(calendar.get(Calendar.YEAR)));
				logger.debug("Setting Quarter as:"+ returnQuarter+" for date:"+strDate);
			}
			else if (intSetB.contains(calendar.get(Calendar.MONTH)+1)){
				returnQuarter = "4Q".concat(String.valueOf((calendar.get(Calendar.YEAR)-1)));
				logger.debug("Setting Quarter as:"+ returnQuarter+" for date:"+strDate);
			}
			else // Should never really occur. Remove this later.  
				logger.warn("Encountered DATE string not explicitly handled: "+strDate);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		/*
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final format.MONTH_FIELD = month;
			DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
			Date date = format.parse(strDate);
			logger.info(format.format(date));

		 */


		return returnQuarter;
	}

	/**
	 * Converts date format from M/dd/yyyy format to yyyy-MM-dd format
	 * @param strDate
	 * @return
	 */
	public String adjustDateFormat(String strDate){
		String returnDate = "";
		String[] temp;
		int inputMonth, inputDate, inputYear;
		inputMonth = inputDate = inputYear = 0;

		
		// Supports format MM/dd/yyyy
		if (strDate.matches("../../....")){
			temp = strDate.split("/");
			inputMonth = Integer.parseInt(temp[0]);
			inputDate = Integer.parseInt(temp[1]);
			inputYear = Integer.parseInt(temp[2]);
			Calendar calendar = Calendar.getInstance();

			calendar.set(Calendar.DATE, inputDate);
			calendar.set(Calendar.YEAR, inputYear);
			// Dates and years are as-is, but months start with January set to 0 and not to 1, so account for it here. 
			calendar.set(Calendar.MONTH, inputMonth-1);
			Date date = calendar.getTime();
			calendar.get(Calendar.MONTH);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			returnDate = sdf.format(date);
		}
		// Supports format MM/dd/yy
		else if (strDate.matches("../../..")){
			temp = strDate.split("/");
			inputMonth = Integer.parseInt(temp[0]);
			inputDate = Integer.parseInt(temp[1]);
			inputYear = Integer.parseInt(temp[2]);
			// TODO: This is admittedly poor, but will serve the purpose for now reasonably well. 
			if (inputYear < 50)
				inputYear = 2000+inputYear;
			else 
				inputYear = 1900+inputYear;
			Calendar calendar = Calendar.getInstance();

			calendar.set(Calendar.DATE, inputDate);
			calendar.set(Calendar.YEAR, inputYear);
			// Dates and years are as-is, but months start with January set to 0 and not to 1, so account for it here. 
			calendar.set(Calendar.MONTH, inputMonth-1);
			Date date = calendar.getTime();
			calendar.get(Calendar.MONTH);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			returnDate = sdf.format(date);
		} 

		// Supports format dd-MMM-yy 
		else if (strDate.contains("-")){
			/*
			temp = strDate.split("-");
			inputMonth = Integer.parseInt(temp[1]);
			inputDate = Integer.parseInt(temp[0]);
			inputYear = Integer.parseInt(temp[2]);
			 */
			SimpleDateFormat srcFormat = new SimpleDateFormat("dd-MMM-yy");
			SimpleDateFormat tgtFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date date = srcFormat.parse(strDate);
				returnDate = tgtFormat.format(date);
			}
			catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return returnDate;
	}

	public String getTimeOfDay(){
		Date date = new Date();
		String strTime = null;

		try {
			DateFormat format = new SimpleDateFormat("HH:mm:ss");
			strTime = format.format(date);
			//System.out.println("Current Date Time : " + strTime);
			if (strTime == null){
				strTime = "-";
			}
		}

		catch(Exception e){
			e.printStackTrace();
		}

		return strTime;
	}


	public String getDateAndTime(){
		Date date = new Date();
		String strTime = null;

		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd' T 'HH:mm:ss a z");
			strTime = format.format(date);
			//System.out.println("Current Date Time : " + strTime);
			if (strTime == null){
				strTime = "-";
			}
		}

		catch(Exception e){
			e.printStackTrace();
		}

		return strTime;
	}

	/**
	 * Returns interval between two timestamps 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public String returnTimeDifference(String startTime, String endTime){
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		long difference = 0;
		String dateFormatted = "";
		//Date date1;
		try {
			Date date1 = format.parse(startTime);
			Date date2 = format.parse(endTime);
			difference  = date2.getTime() - date1.getTime(); 

			Date date = new Date(difference);
			DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
			dateFormatted = formatter.format(date);
		} 
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dateFormatted;
	}



	public String convertTime(long TS){

		Date date = new Date(TS*1000);
		String strTime = "";

		try {
			DateFormat format =
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			//new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			strTime = format.format(date);
			//System.out.println("	Converted Date Time : " + strTime);
		}

		catch(Exception e){
			e.printStackTrace();
		}

		return strTime;
	}

	/*
	 * Commeting this out as the logger class 
	 * here clashes with the base logger class
	 * 
	public Logger  nsbLog () {

		Logger logger = Logger.getLogger("com.syncsort.bex.ps.AppNSB.BasicLogging");

		try {
			Level level;
			FileHandler handler = new FileHandler("AppNSB.log", false);
			SimpleFormatter formatterTxt = new SimpleFormatter();
			handler.setFormatter(formatterTxt);
			logger.addHandler(handler);
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return logger;
	}
	 */

	public String removeAllCharInstances(String str, CharSequence ch){
		String retString = str; 

		try{
			if (retString.contains(ch))
			{
				retString = retString.replaceAll(ch.toString(), "");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return retString;

	}

	public Hashtable convertListIntoHashTable (List<String> input, String pattern ){
		Hashtable<String, List<String>> hashTable = new Hashtable<String, List<String>>();

		String key = "";
		List<String> value = new ArrayList<String>();

		for (Iterator iter = input.iterator(); iter.hasNext(); ) {
			String s = (String)iter.next();

			if (s.contains(pattern)){
				key = s;
			}
			else {
				value.add(s);
			}
		}
		hashTable.put(key, value);
		return hashTable;
	}

	/**
	 * Program to run external programs / utilities
	 * @param params
	 */
	// https://stackoverflow.com/questions/13991007/execute-external-program-in-java?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
	public void runExternalExec (String cmd, String param){
		try{
			// Reference: http://www.rgagnon.com/javadetails/java-0014.html
			Process process = new ProcessBuilder(cmd, param).start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			//System.out.printf("Output of running %s is:", Arrays.toString(args));

			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		}
		catch (IOException e){
			e.getMessage();
			e.printStackTrace();
		}
		catch (Exception e){
			e.getMessage();
			e.printStackTrace();
		}

	}

	public List<String[]> readCSV(String fileName){

		List<String[]> listStock = new ArrayList<String[]>();

		try {
			FileReader fin = new FileReader(fileName);
			BufferedReader in = new BufferedReader(fin);
			String s = "";
			String[] singleStock = new String[20];
			String[] strArray = new String[20];

			// while the entire file is not read...
			while ((s = (in.readLine()))!= null){
				singleStock = s.split(",");
				listStock.add(singleStock);
			}  

			for (int i = 0; i<listStock.size(); i++){
				strArray = listStock.get(i);
				System.out.print("ListStock["+i+"] : ");
				for (int n = 0; n < strArray.length; n++){
					if (n ==0)
						System.out.print(strArray[n]);
					else
						System.out.print(", "+strArray[n]);
				}
				System.out.println();
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return listStock;
	}

	/*
	 * Downloads a file from given URL giving it localFileName
	 */
	public static void downloadFromUrl(String urlPath, String localFilename) throws IOException {
		InputStream is = null;
		FileOutputStream fos = null;


		try {
			URL url = new URL(urlPath);
			URLConnection urlConn = url.openConnection();//connect

			is = urlConn.getInputStream();               //get connection inputstream
			fos = new FileOutputStream(localFilename);   //open outputstream to local file

			byte[] buffer = new byte[4096];              //declare 4KB buffer
			int len;

			// Download and write locally, while input stream has data
			while ((len = is.read(buffer)) > 0) {  
				fos.write(buffer, 0, len);
			}

		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		}
	}
}




















