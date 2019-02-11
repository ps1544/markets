package com.jps.finserv.markets.equities;

import com.jaunt.*;
import com.jps.finserv.markets.util.Utils;
import com.jps.finserv.markets.util.modOutCSV;
import com.jps.finserv.markets.util.modReadFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
public class DataCompanyIndustryInfo {

	//private final String url = "jdbc:postgresql://localhost/postgres";
	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "root";

	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "Iso885912@";

	/** The name of the computer running MySQL */
	private final String serverName = "localhost";

	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;

	private static final Logger logger = (Logger) LoggerFactory.getLogger(DataCompanyIndustryInfo.class);

	// List containing symbols w/ highest net change. To be used by external classes for further data generation. 
	public List<String> listHighestNetChange;

	// Instantiate external and helper classes
	UserAgent userAgent = new UserAgent();      
	Utils utils = new Utils();

	/*
	 * Entry function for the class 
	 */

	public void  run(String dbName, String tblName){
		UserAgent userAgent = new UserAgent();
		userAgent.settings.checkSSLCerts = false;
		String url = "https://www.nasdaq.com/screening/company-list.aspx";
		List<String> returnListOutput = new LinkedList<String>();
		/*
		 * Downloads data from concerned URL hrefs into local CSV files. 
		 * Then merges those file removing duplicate headers and finally 
		 * pushes that cumulative output to a file.   
		 */
		
		try{
			userAgent.visit(url); // Visit the base URL with symbol
			Element divsDownload = userAgent.doc.findFirst("<div id=\"companyListDownloads\">"); // Find all links in HTML doc
			Elements links = divsDownload .findEvery("<a>");
			int count = 0;
			for (Element link: links){
				String tempStr = link.getAt("href"); 
				if (tempStr.contains("download")){
					logger.info("Reference link for Industry Background: "+tempStr);
					++count;
					String localFilename = "IndustryInfo"+"_"+count+utils.getDateAndTime()+".csv";
					localFilename = localFilename.replace(":", "-");
					Utils.downloadFromUrl(tempStr, localFilename);
					List<String> tempList  = modReadFile.readFileIntoListOfStrings(localFilename);
					returnListOutput.addAll(tempList);
				}
			}
			/*
			 * We have headers duplicated, remove duplicates by using a set as the middle-man.
			 * Need to also use Linked Set and Lists, otherwise, table header will land up somewhere in the middle.  
			 */
			Set<String> set = new LinkedHashSet<>();
			set.addAll(returnListOutput);
			returnListOutput.clear();
			returnListOutput.addAll(set);
			// Duplicate headers being out, we now write output into a CSV.
			modOutCSV outCSV = new modOutCSV(("IndustryInfoCumulative"+"_"+count+utils.getDateAndTime()+".csv").replace(":", "-"));
			outCSV.writeList(returnListOutput);
			prepDataForInsert(returnListOutput, dbName, tblName);
		}
		catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
			logger.error("JauntException: "+e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	/*
	 * Simple function to capture listing of S&P500 companies from a flat CSV file. 
	 */
	private List<String> generateSnP500List(String dirPath){
		List<String> listSymbols = new ArrayList<String>();
		File dir = new File(dirPath);
		//listSymbols = modReadFile.readFileIntoListOfStrings(file);
		String pattern = "SnP500.csv";
		Collection<File> files = FileUtils.listFiles(dir, new WildcardFileFilter(pattern), null);
		if (files != null){
			logger.info("Found "+files.size()+" files with matching pattern.");
			for (File file: files){
				listSymbols = modReadFile.readFileIntoListOfStrings(file.getAbsolutePath());
			}
		}
		else
			logger.warn("S&P500: No files found to catalog!");
		
		return listSymbols;
	}
	
	private void prepDataForInsert(List<String> inputList, String dbName, String tblName){
		boolean heading = true;
		int noObjects = 0;
		List<List<Object>> sendList = new ArrayList<List<Object>>();
		String errorLine = "";

		// First get listing of S&P500 stocks
		//String dirPathSnP = "C:\\Users\\pshar\\Dropbox\\Programming";
		String dirPathSnP = System.getProperty("user.dir");

		List<String> listSnp500 = generateSnP500List(dirPathSnP);

		// Now, prep the data
		try{
			for (String line : inputList){ 
				// Miscellaneous tasks before we get to actual data: handle extra double quote, account for heading, keep reference to line in case of exception 
				errorLine = line;
				if (heading){
					heading = false;
					continue;
				}
				if (line == "")
					continue;

				/*
				 * All that done, now split the line by double quotes
				 * Cannot split by commas. There are many commas 
				 * in the company name string which will require specific handling otherwise
				 */
				String[] temp = line.split("\"");
				String symbol = temp[1].trim();
				String name = (temp[3].trim());

				// Need to account for n/a since "double" value will cause exception otherwise 
				double lastSale =0.0;
				if (!(temp[5].trim()).equals("n/a"))
					lastSale = Double.parseDouble(temp[5].trim());
				// Other variables are STRING class, so n/a will be acceptable. 
				String marketCap = temp[7].trim();
				String ipoYear = temp[9].trim();
				String sector = temp[11].trim();
				String industry= temp[13].trim();
				
				int SnP = 0;
				if (listSnp500.contains(symbol))
					SnP = 1;
				
				List<Object> tempList = new LinkedList<Object>();
				tempList.add(symbol);
				tempList.add(name);
				tempList.add(lastSale);
				tempList.add(marketCap);
				tempList.add(ipoYear);
				tempList.add(sector);
				tempList.add(industry);
				tempList.add(SnP);
				//noObjects = tempList.size();
				sendList.add(tempList);
			}
			insertDB(sendList, dbName, tblName);
		}
		catch(Exception e){
			logger.error("Exception <<"+e.getMessage()+">> at input data:"+errorLine);
			e.printStackTrace();
		}
	}
	/*
	 * 
	 */
	private void insertDB(List<List<Object>> inputList, String dbName, String tblName ){
		try {
			Connection conn = null;
			Properties properties = new Properties();
			properties.put("user", userName);
			properties.put("password", password);
			properties.put("useSSL", "false");
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement pstmt = conn.prepareStatement(statement);

			for (List<Object> tempList : inputList){
				// Set all String elements
				pstmt.setObject(1, tempList.get(0), JDBCType.VARCHAR);
				pstmt.setObject(2, tempList.get(1), JDBCType.VARCHAR);
				pstmt.setObject(3, tempList.get(2), JDBCType.DOUBLE);
				pstmt.setObject(4, tempList.get(3), JDBCType.VARCHAR);
				pstmt.setObject(5, tempList.get(4), JDBCType.VARCHAR);
				pstmt.setObject(6, tempList.get(5), JDBCType.VARCHAR);
				pstmt.setObject(7, tempList.get(6), JDBCType.VARCHAR);
				pstmt.setObject(8, tempList.get(7), JDBCType.TINYINT);
				try{
					pstmt.execute();
				}
				catch (SQLIntegrityConstraintViolationException e) {
					logger.warn("SQLIntegrityConstraintViolationException caught... Truncating table and refreshing dataset.");
					/*
					 * TODO: Handle this in generic fashion.
					 * If data need not be preserved, then clean up table. 
					 */
					if ((tblName.trim()).equals("industrybackground")){
						String delRowStmt = "DELETE FROM "+tblName;
						PreparedStatement pstmtDelete = conn.prepareStatement(delRowStmt);
						int countDelete = pstmtDelete.executeUpdate();
						if (countDelete > 0){ 
							pstmt.execute();
						}
						else{
							logger.error("Failed to cleanup existing dataset in table : "+tblName);
							logger.error("The latest data will NOT be inserted.");
						}
						pstmtDelete.close();
					}
				}
			}
			logger.info("Inserted Company Background dataset into DB Table.");
			pstmt.close();
			conn.close();
		} 
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage()+" >> while refreshing Company Background information.");
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage()+" >> while refreshing Company Background information.");
			e.printStackTrace();
		}

	}
}