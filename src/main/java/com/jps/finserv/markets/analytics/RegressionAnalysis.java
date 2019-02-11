package com.jps.finserv.markets.analytics;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.ml.stat.ChiSquareTest;
import org.apache.spark.sql.types.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.LassoModel;
import org.apache.spark.mllib.regression.LassoWithSGD;
import scala.Tuple2;

import java.io.Serializable;

import static java.lang.Math.toIntExact;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.stat.Statistics;
import org.apache.spark.mllib.util.MLUtils;

public class RegressionAnalysis {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(RegressionAnalysis.class);
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
	/** The name of the database we are testing with (this default is installed with MySQL) */
	private final String sector = "Health Care";

	public void run() {
		SparkConf conf = new SparkConf().setMaster("local").setAppName("CorrelationTest");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SparkSession spark = SparkSession.builder().master("local").appName("CorrelationTest").getOrCreate();

		JavaRDD<String> lines = spark.read().textFile("RegressionAnalysis.txt").toJavaRDD();			

		// line is the input and Row as an instance of RowFactory is returned
		JavaRDD<Row> parsedData = lines.map(
				line -> {
					String[] sarray = line.trim().split(",");
					double[] values = new double[sarray.length];
					for (int i = 0; i < sarray.length; i++) {
						values[i] = Double.parseDouble(sarray[i]);
					}
					return RowFactory.create(values);
				}
				);
		//parsedData.foreach(System.out::println);

		// Do expect problems with two structfields since our data only has one label.
		// Read more on these particular StructField types and also on VectorUDT class
		StructType schema = new StructType(new StructField[]{
				new StructField("label", DataTypes.DoubleType, false, Metadata.empty()),
				new StructField("features", new VectorUDT(), false, Metadata.empty()),
		});

		Dataset<Row> training = spark.createDataFrame(parsedData, schema);

		/*
		Row r = ChiSquareTest.test(training, "features", "label").head();
		logger.info("pValues: " + r.get(0).toString());
		logger.info("degreesOfFreedom: " + r.getList(1).toString());
		logger.info("statistics: " + r.get(2).toString());
		 */

		LogisticRegression lr = new LogisticRegression()
				.setMaxIter(10)
				.setRegParam(0.3)
				.setElasticNetParam(0.8);

		// Fit the model
		LogisticRegressionModel lrModel = lr.fit(training);

		// Print the coefficients and intercept for logistic regression
		logger.info("Coefficients: "
		  + lrModel.coefficients() + " Intercept: " + lrModel.intercept());

		jsc.stop();
		spark.stop();

		/*
		// Spark SQL DB connection to table containing daily data on basic economic indicators
		Dataset<Row> dsSQLDailyIndicators = spark.read()
				.format("jdbc")
				.option("url", "jdbc:mysql://"+ serverName + ":" + portNumber + "/" + dbName)
				.option("dbtable", "industrybackground")
				.option("useSSL", "false")
				.option("user", userName)
				.option("password", password)
				.load();
		dsSQLDailyIndicators.createOrReplaceTempView("equities_2018_03");

		List<String> listAllSymbols = generateListAllSymbols(spark);

		List<JavaRDD<Double>> returnedList = new ArrayList<JavaRDD<Double>> ();
		for (String tgtSymbol : listAllSymbols){
			//String inputSymbol = "PFE";
			JavaRDD<Double> returnedPcntChangeDoubleRDD = retrieveCatalogData(spark, dsSQLDailyIndicators, tgtSymbol);
			returnedList.add(returnedPcntChangeDoubleRDD);
		}

		 */
	}

	/**
	 * Returns percent daily change rate for given stock symbol from the concerned SQL table 
	 * @param spark Spark session
	 * @param dsSQLDailyIndicators Dataset with SQL connection information
	 * @param inputSymbol Stock symbol whose daily closing quotes are to be returned
	 * @return
	 */
	private JavaRDD<Double> retrieveCatalogData(SparkSession spark, Dataset<Row> dsSQLDailyIndicators, String inputSymbol){
		Dataset<Row> returnDataset;
		//Dataset<Row> dsSymbolA = spark.sql("select * from industrybackground WHERE MarketCap like '$___.%B' and sector like 'Health Care'; '"+inputSymbol+"'");
		Dataset<Row> dsSymbolA = spark.sql("select * from equities_2018_03 WHERE symbol like '"+inputSymbol+"'");
		Dataset<Row> dsSymbolAPcntChange = dsSymbolA.select("pcntChange");
		// Here, the 'row' is input and the Functional Interface returns the double value within that row to form Dataset<Row>
		JavaRDD<Double> returnJavaRDDDouble = dsSymbolAPcntChange.toJavaRDD().map(
				row -> {
					String temp = row.toString().replace("[", "").replace("]","");
					logger.debug("The String value is "+temp);
					double v = Double.parseDouble(temp.trim()); 
					return v; 
				} 
				);

		return returnJavaRDDDouble;
	}

	
}