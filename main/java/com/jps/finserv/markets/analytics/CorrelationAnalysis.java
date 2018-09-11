package com.jps.finserv.markets.analytics;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.util.Utils;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

//import java.util.function.Function;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.stat.Statistics;

public class CorrelationAnalysis {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(CorrelationAnalysis.class);
	/** The name of the MySQL account to use (or empty for anonymous) */
	private final String userName = "psharma";
	/** The password for the MySQL account (or empty for anonymous) */
	private final String password = "$ys8dmin";
	/** The name of the computer running MySQL */
	private final String serverName = "localhost";
	/** The port of the MySQL server (default is 3306) */
	private final int portNumber = 3306;
	/** The name of the database we are testing with (this default is installed with MySQL) */
	private final String dbName = "markets";
	/** The name of the table we are using to maintain correlation data */
	private final String tblName = "correlation";
	Utils utils = new Utils();

	public void run() {
		SparkConf conf = new SparkConf().setMaster("local").setAppName("CorrelationTest");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SparkSession spark = SparkSession.builder().master("local").appName("CorrelationTest").getOrCreate();

		List<String> listSymbols = new LinkedList<String>();
		List<String> listCommodities = new LinkedList<String>();
		listSymbols = generateListEntities(spark, "equities_historic_data", "symbol", 1, false);
		listCommodities= generateListEntities(spark, "dailyindicators_historic_data", "entity", 1, false);
		List<String> listCorrelationData = new ArrayList<String>();
		//listCorrelationData.add("RF,symbol,Dollar,commodity,2018-01-12,2018-01-22,-0.2174061541029069");

		for (String symbol : listSymbols){
			for (String symbolTgt : listCommodities){
				if (!symbol.matches(symbolTgt)){
					String returnedValue = computeCorrelationBetweenSeries(spark, "symbol", symbol, "commodity", symbolTgt);
					if (returnedValue != "")
						listCorrelationData.add(returnedValue);
				}
			}
		}
		// All correlation computation done, we now insert this data into our catalog. 
		if (listCorrelationData.size() > 0)
			insertCorrelationData(listCorrelationData, dbName, tblName);

		jsc.stop();
		spark.stop();
	}

	/**
	 * 	
	 * @param spark Spark session instance
	 * @return First series for correlation calculation from DailyIndicators table
	 */
	String computeCorrelationBetweenSeries(SparkSession spark, String inputTypeA, String entityA, String inputTypeB, String entityB){
		String returnValue = "";
		// Spark SQL DB connection 
		Dataset<Row> dsSQLEquities = spark.read()
				.format("jdbc")
				.option("url", "jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName)
				.option("dbtable", "equities_historic_data")
				.option("useSSL", "false")
				.option("user", userName)
				.option("password", password)
				.load();

		Dataset<Row> dsSQLCommodities = spark.read()
				.format("jdbc")
				.option("url", "jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName)
				.option("dbtable", "dailyindicators_historic_data")
				.option("useSSL", "false")
				.option("user", userName)
				.option("password", password)
				.load();

		Dataset<Row> datasetA = null;
		Dataset<Row> datasetB = null;

		/*
		 * This function allows for correlation to be computed between two stocks, two commodities, or between a stock and a commodity
		 * Depending on what we have calcu
		 */
		if (inputTypeA.matches("symbol")){
			dsSQLEquities.createOrReplaceTempView("equities_historic_data");
			datasetA = spark.sql("select symbol, pcntChange as entityAPcntChange, date as dateSetA from equities_historic_data where symbol like '"+entityA+"' order by date asc");
		}
		else if (inputTypeA.equalsIgnoreCase("Commodity")){
			dsSQLCommodities.createOrReplaceTempView("dailyindicators_historic_data");
			datasetA = spark.sql("select entity, pcntChange as entityAPcntChange, date as dateSetA from dailyindicators_historic_Data where entity like '"+entityA+"' order by date asc");
		}
		if (inputTypeB.matches("symbol")){
			dsSQLEquities.createOrReplaceTempView("equities_historic_data");
			datasetB = spark.sql("select symbol, pcntChange as entityBPcntChange, date as dateSetB from equities_historic_data where symbol like '"+entityB+"' order by date asc");
		}
		else if (inputTypeB.equalsIgnoreCase("Commodity")){
			dsSQLCommodities.createOrReplaceTempView("dailyindicators_historic_data");
			datasetB = spark.sql("select entity, pcntChange as entityBPcntChange, date as dateSetB from dailyindicators_historic_Data where entity like '"+entityB+"' order by date asc");
		}

		// Now join the two to ensure that we have data for the exact same dates
		Dataset<Row> dsJoined = datasetA.join(datasetB, datasetA.col("dateSetA").equalTo(datasetB.col("dateSetB")), "inner");

		/*
		List<Row> listJoined =  datasetA.collectAsList();
		for (Row row : listJoined){
			logger.info(row.toString());
		}
		*/
		
		Dataset<Row> dsDate = dsJoined.select("dateSetA");
		List<Row> listDates = dsDate.collectAsList();
		Row lastRow = listDates.get(0);
		Row firstRow = listDates.get(listDates.size()-1);

		String startDate = firstRow.toString().replace("[", "").replace("]","");
		String endDate = lastRow.toString().replace("[", "").replace("]","");
		logger.debug("Start Date: "+ startDate+". End Date: "+endDate);

		// Now, check whether this record already exists in our catalog. Proceed only if it doesn't
		if (existsRecord(entityA, entityB,  startDate, endDate, dbName, tblName))
			logger.warn("Catalog entry already exists for quadruple: "+entityA, entityB, startDate, endDate); 
		else {
			// Now, pick up the pcntChange values for both entities
			Dataset<Row> dsEntityAPcntChange = dsJoined.select("entityAPcntChange");
			Dataset<Row> dsEntityBPcntChange = dsJoined.select("entityBPcntChange");

			JavaRDD<Double> seriesA = dsEntityAPcntChange.toJavaRDD().map(
					row -> {
						// The RDD Rows contain [ and ] which need to be removed
						String temp = row.toString().replace("[", "").replace("]","");
						logger.debug("The String value is "+temp);
						double v = Double.parseDouble(temp.trim());
						return v; 
					} 
					);

			JavaRDD<Double> seriesB = dsEntityBPcntChange.toJavaRDD().map(
					row -> {
						// The RDD Rows contain [ and ] which need to be removed
						String temp = row.toString().replace("[", "").replace("]","");
						logger.debug("The String value is "+temp);
						double v = Double.parseDouble(temp.trim());
						return v; 
					} 
					);
			double correlation = Statistics.corr(seriesA, seriesB, "pearson");
			logger.warn("The Correlation between "+entityA +" and "+entityB+" is: " + correlation);
			returnValue = entityA+","+inputTypeA+","+entityB+","+inputTypeB+","+startDate+","+endDate+","+correlation;
		} // if (!existRecord(entityA, entityB,  startDate, endDate, dbName, tblName)){
		return returnValue;
	}

	/**
	 * Generates a String list of all stock or commodities symbols in the given table 
	 * The list can be limited to max of 'numSublist'   
	 * @param spark Spark session
	 * @param tableName SQL Table from which unique entities are to be returned
	 * @param type Either "symbol" for stocks or "entity" for commodities like Gold, CrudeOil etc...  
	 * @param numSublist Limit list to this number if full listing is not desired
	 * @param manualSelection Only for type "symbol" if a manual listing of stocks is desired
	 * @return
	 */

	public List<String> generateListEntities(SparkSession spark, String tableName, String type, int numSublist, boolean manualSelection){
		List<String> listAllSymbols = new LinkedList<String>();

		Dataset<Row> dsSQLConnect = spark.read()
				.format("jdbc")
				.option("url", "jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName)
				.option("dbtable", tableName)
				.option("useSSL", "false")
				.option("user", userName)
				.option("password", password)
				.load();

		dsSQLConnect.createOrReplaceTempView(tableName);

		if (!manualSelection){
			// Select all distinct symbols from table
			String queryAllSymbols = "select distinct "+type+" from "+tableName;
			Dataset<Row> dsListAllSymbols = spark.sql(queryAllSymbols);
			// Collect all in the dataset and then limit the number to a subset unless you want to get data for all 5000 stocks
			List<Row> listRowSymbols = dsListAllSymbols.toJavaRDD().collect();
			List<Row> listToppedRowSymbol = listRowSymbols.subList(0, numSublist);

			for (Row singleRow : listToppedRowSymbol){
				String strSingleRow= singleRow.toString();
				String symbol = "";
				if ((strSingleRow.contains("[")) || ((strSingleRow.contains("]"))))
					symbol = strSingleRow.replace("[", "").replace("]","");
				else
					symbol = strSingleRow;
				listAllSymbols.add(symbol);
			}
		}
		else { 
			// put in desired listing of stocks here manually. Must set 'manualSelection' argument to TRUE in the caller function for this to be read. 
			listAllSymbols.add("GE");
			listAllSymbols.add("MSFT");
			listAllSymbols.add("AAPL");
			listAllSymbols.add("INTC");
			listAllSymbols.add("CMCSA");
			listAllSymbols.add("CSCO");
			listAllSymbols.add("XOM");
			listAllSymbols.add("T");
			listAllSymbols.add("FB");
			listAllSymbols.add("PFE");
			listAllSymbols.add("WFC");
			listAllSymbols.add("ORCL");
			listAllSymbols.add("C");
			listAllSymbols.add("ABEV");
			listAllSymbols.add("VZ");
			listAllSymbols.add("NFLX");
		}
		return listAllSymbols;
	}

	/**
	 * Inserts the correlation data into SQL table
	 * @param inputList a CSV list with each row capturing details
	 * @param dbName
	 * @param tblName
	 */
	public void insertCorrelationData(List<String> inputList, String dbName, String tblName){
		Connection conn = null;
		String lineError = "";
		Properties properties = new Properties();
		int countSkipped = 0;
		int countProcessed = 0;
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");
		properties.setProperty("autoReconnect", "true");

		int size = inputList.size();
		logger.debug("The size of the input list is:" + (size));
		if ((inputList == null) || (size == 0)){
			logger.error("Input list does not have any rows to be inserted into the database.");
		}
		// Else file has good data for us to process and insert into DB table. 
		else
			try {
				// TODO: Add exception handling for invalid server / port / db names. 
				// TODO: Handle case where the table doesn't exist. Create one in that case and then insert. 
				conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);

				boolean heading = false;
				for (String line : inputList){ 
					if (heading){
						heading = false;
						continue;
					}
					if (line == "")
						continue;
					//logger.debug(line);

					String[] temp = line.split(",");
					lineError = line;
					String entityA = temp[0].trim();
					String inputTypeA = temp[1].trim();
					String entityB = temp[2].trim();
					String inputTypeB = temp[3].trim();
					Date startDate = Date.valueOf(temp[4].trim());
					Date endDate = Date.valueOf(temp[5].trim());
					double correlation = Double.parseDouble(temp[6].trim());

					String statement = "INSERT INTO "+tblName+" VALUES (?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement pstmt = conn.prepareStatement(statement);
					pstmt.setString(1, entityA);
					pstmt.setString(2, inputTypeA);
					pstmt.setString(3, entityB);
					pstmt.setString(4, inputTypeB);

					// Finally the date
					pstmt.setDate(5, startDate);
					pstmt.setDate(6, endDate);
					// Now the double element
					pstmt.setDouble(7, correlation); 

					try{
						pstmt.execute();
						++countProcessed;
					}
					catch (SQLIntegrityConstraintViolationException e) {
						logger.info("Caught SQLIntegrityConstraintViolationException.");
						logger.info("Making assumption that data is already in catalog for row: '"+line+"'");
						++countSkipped;
					}
					pstmt.close();
				} // for (String line : returnList){ ...
				// All queries done, now close out the connection
				conn.close();
				logger.info("Inserted historical dataset into database.");
				logger.info("Total # of records inserted: "+countProcessed);
				logger.info("Total # of records skipped (were already present) for insert: "+countSkipped);
			}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage()+ " >> occurred for following line item:\""+lineError+"\"");
			//logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage()+ " >> while traversing following line item:\""+lineError+"\"");
			e.printStackTrace();
		}
	}

	/**
	 * Checks whether the record already exists in our catalog. If yes, then avoid costly correlation calculation in Spark
	 * @param entityA
	 * @param entityB
	 * @param startDate
	 * @param endDate
	 * @param dbName
	 * @param tblName
	 * @return true if record exists
	 */
	private boolean existsRecord(String entityA, String entityB,  String startDate, String endDate, String dbName, String tblName){
		Connection conn = null;
		Properties properties = new Properties();
		properties.put("user", userName);
		properties.put("password", password);
		properties.put("useSSL", "false");

		PreparedStatement pstmt; 

		try{
			conn = DriverManager.getConnection("jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName, properties);
			String query = "select * from "+tblName+" where entity1 like '"+entityA+"' and entity2 like '"+entityB+"' and startDate = '"+startDate+"' and endDate like '"+endDate+"'";
			pstmt = conn.prepareStatement(query);
			ResultSet rs = pstmt.executeQuery();
			if (rs.first()){
				pstmt.close();
				conn.close();
				return true;
			}
			else {
				// The two entities could be reversed, so account for that as well here. 
				query = "select * from "+tblName+" where entity1 like '"+entityB+"' and entity2 like '"+entityA+"' and startDate like '"+startDate+"' and endDate like '"+endDate+"'";
				pstmt = conn.prepareStatement(query);
				rs = pstmt.executeQuery();
				if (rs.first()){
					pstmt.close();
					conn.close();
					return true;
				}
			}
			pstmt.close();
			conn.close();
		}
		catch (SQLException e) {
			logger.error("SQLException: << "+e.getMessage()+ " >>.");
			//logger.error("For debugging check the referred data point in the input file: \""+fileDailyMarketData+"\"");
			e.printStackTrace();}
		catch (Exception e) {
			logger.error("Non-SQL Exception: << "+e.getMessage()+ " >>.");
			e.printStackTrace();
		}
		return false;
	}

}