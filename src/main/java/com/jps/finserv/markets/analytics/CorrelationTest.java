package com.jps.finserv.markets.analytics;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jps.finserv.markets.equities.DataDailyIndicators;
import com.jps.finserv.markets.util.modReadFile;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.VoidFunction;

import static java.lang.Math.toIntExact;
import java.io.File;
import java.util.Arrays;
import java.util.List;
//import java.util.function.Function;


import org.apache.spark.api.java.JavaDoubleRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.stat.Statistics;
import org.apache.spark.rdd.RDD;


public class CorrelationTest {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(CorrelationTest.class);

	public void run() {
		SparkConf conf = new SparkConf().setMaster("local").setAppName("CorrelationTest");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SparkSession spark = SparkSession.builder().master("local").appName("CorrelationTest").getOrCreate();

		// Problem is because of ROW instead of DOUBLE. Get the CSV data single line just with the closing price and heading. 
		// Then correlate it with say Gold for say 3 or 4 days. 

		Dataset<Row> dsCsv34 = spark.read().format("csv")
				.option("sep", ",")
				.option("inferSchema", "true")
				.option("header", "true")
				.load("SQLOut_2018-03-06 T 08-42-32 AM EST.csv");
		Dataset<Row> dsCsv35 = spark.read().format("csv")
				.option("sep", ",")
				.option("inferSchema", "true")
				.option("header", "true")
				.load("SQLOut_2018-03-06 T 08-42-37 AM EST.csv");

		Dataset<Row> dsCsv34PcntChange = dsCsv34.select("pcntChange");
		Dataset<Row> dsCsv35PcntChange = dsCsv35.select("pcntChange");
		final long n =  dsCsv34PcntChange.count();
		final int sizeLimit = toIntExact(n);

		
		// Create a list and put in these indiviudla
		//JavaRDD<Double>[] rdd1;
		
		
		JavaRDD<Double> series1 = dsCsv34PcntChange.limit(sizeLimit).toJavaRDD().map(
				row -> {
					String temp = row.toString().replace("[", "").replace("]","");
					logger.debug("The String value is "+temp);
					double v = Double.parseDouble(temp.trim());
					return v; 
				} 
				);

		JavaRDD<Double> series2 = dsCsv35PcntChange.limit(sizeLimit).toJavaRDD().map(
				row -> {
					String temp = row.toString().replace("[", "").replace("]","");
					logger.debug("The String value is "+temp);
					double v = Double.parseDouble(temp.trim()); 
					return v; 
				} 
				);

		Double correlation = Statistics.corr(series1, series2, "pearson");
		logger.info("The Correlation between the two series is: " + correlation);
		
		jsc.stop();
		spark.stop();

		/*
		 * 
		 * 
		//JavaDoubleRDD javaDRDD = series1.flatMapToDouble(null);

		File f1 = new File("SQLOut_2018-03-06 T 08-42-32 AM EST.csv");
		List<String> tempList1 = modReadFile.readFileIntoListOfStrings(f1.getName());

		File f2 = new File("SQLOut_2018-03-06 T 08-42-37 AM EST.csv");
		List<String> tempList2 = modReadFile.readFileIntoListOfStrings(f2.getName());

		List<Double> numbers = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
		JavaRDD<Double> jRDD = jsc.parallelize(numbers, 1);

		JavaRDD<Double> series2 = dsCsv35PcntChange.toJavaRDD().map(
				returnV -> {
					Double v;// = new Double[tempList1.size() -1];
					int i = 0;
					for (String line :tempList2){
						if (i ==0)
							continue; // ignore header
						String[] parts = line.split(",");
						v = Double.parseDouble(parts[1]);
						i++;
						return v;
					}
					//return Vectors.dense(v);
				} 
				);
		class GetPcntChange implements Function<String, Integer> {
			public void apply(List<String> inputList) {
				double[] v = new double[inputList.size()];
				int i = 0;
				for (String line : inputList){
					String[] parts = line.split(",");
					v[i] = Double.parseDouble(parts[1]);
					i++;
				}
				//return Vectors.dense(v);
			}
		}

		//JavaDoubleRDD series1 = dsCsv34PcntChange.javaRDD().flatMapToDouble(new GetPcntChange());
		JavaRDD<Vector> series1 = dsCsv34PcntChange.map((new GetPcntChange()).apply(null));

		JavaDoubleRDD series2 = dsCsv35PcntChange .javaRDD().flatMapToDouble(null);

		Double correlation = Statistics.corr(series1.srdd(), series2.srdd(), "pearson");
		System.out.println("Correlation is: " + correlation);

		Dataset<Row> dfSql = spark.sql("select entity, pcntChange, date from DailyIndicators where entity like 'Gold'");
		//RDD<Row> jRDD_03_02 = dsCsv34PcntChange.rdd();
		//RDD<Row> jRDD_03_01 = dsCsv35PcntChange.rdd();

		//Double corr1 = Statistics.corr(jRDD_03_02, jRDD_03_01, "pearson");
		//Statistics.corr(jRDD_03_01, jRDD_03_02, "pearson");

		JavaDoubleRDD seriesX = jsc.parallelizeDoubles(
				Arrays.asList(1.0, 2.0, 3.0, 3.0, 5.0));  // a series

		// must have the same number of partitions and cardinality as seriesX
		JavaDoubleRDD seriesY = jsc.parallelizeDoubles(
				Arrays.asList(11.0, 22.0, 33.0, 33.0, 555.0));

		// compute the correlation using Pearson's method. Enter "spearman" for Spearman's method.
		// If a method is not specified, Pearson's method will be used by default.
		Double correlation = Statistics.corr(seriesX.srdd(), seriesY.srdd(), "pearson");
		System.out.println("Correlation is: " + correlation);

		// note that each Vector is a row and not a column
		JavaRDD<Vector> data = jsc.parallelize(
				Arrays.asList(
						Vectors.dense(1.0, 10.0, 100.0),
						Vectors.dense(2.0, 20.0, 200.0),
						Vectors.dense(5.0, 33.0, 366.0)
						)
				);

		// calculate the correlation matrix using Pearson's method.
		// Use "spearman" for Spearman's method.
		// If a method is not specified, Pearson's method will be used by default.
		Matrix correlMatrix = Statistics.corr(data.rdd(), "pearson");
		System.out.println(correlMatrix.toString());
		jsc.stop();
		spark.stop();
	}
		 */
	}
}