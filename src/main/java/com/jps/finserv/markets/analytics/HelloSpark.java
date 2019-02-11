package com.jps.finserv.markets.analytics;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;

public class HelloSpark {
  public void run() {
    String logFile = "C:/Program Files/spark-2.3.0-bin-hadoop2.7/README.md"; // Should be some file on your system
    SparkSession spark = SparkSession.builder().master("local").appName("Simple Application").getOrCreate();
    Dataset<String> logData = spark.read().textFile(logFile).cache();

    long numAs = logData.filter(s -> s.contains("a")).count();
    long numBs = logData.filter(s -> s.contains("b")).count();

    System.out.println("	" + numAs + ", lines with b: " + numBs);

    spark.stop();
  }
  
  
  
}