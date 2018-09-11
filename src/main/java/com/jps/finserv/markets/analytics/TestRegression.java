/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jps.finserv.markets.analytics;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

// $example on$
import scala.Tuple2;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.LinearRegressionModel;
import org.apache.spark.mllib.regression.LinearRegressionWithSGD;
import org.apache.spark.sql.SparkSession;
// $example off$
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for LinearRegressionWithSGD.
 */
public class TestRegression {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(TestRegression.class);

	public void run() {
		SparkConf conf = new SparkConf().setMaster("local").setAppName("TestRegression");
		JavaSparkContext sc = new JavaSparkContext(conf);
		SparkSession spark = SparkSession.builder().master("local").appName("CorrelationTest").getOrCreate();

		//SparkConf conf = new SparkConf().setAppName("JavaLinearRegressionWithSGDExample");
		//JavaSparkContext sc = new JavaSparkContext(conf);

		// $example on$
		// Load and parse the data
		String path = "C:/Program Files/spark-2.3.0-bin-hadoop2.7/data/mllib/ridge-data/lpsaModified.data";
		JavaRDD<String> data = sc.textFile(path);
		JavaRDD<LabeledPoint> parsedData = data.map(line -> {
			String[] parts = line.split(",");
			String[] features = parts[1].split(" ");
			double[] v = new double[features.length];
			for (int i = 0; i < features.length - 1; i++) {
				v[i] = Double.parseDouble(features[i]);
			}
			return new LabeledPoint(Double.parseDouble(parts[0]), Vectors.dense(v));
		});
		parsedData.cache();

		// Building the model
		int numIterations = 100;
		double stepSize = 0.00000001;
		LinearRegressionModel model =
				LinearRegressionWithSGD.train(JavaRDD.toRDD(parsedData), numIterations, stepSize);

		// Evaluate model on training examples and compute training error
		JavaPairRDD<Double, Double> valuesAndPreds = parsedData.mapToPair(point ->
		new Tuple2<>(model.predict(point.features()), point.label()));

		double MSE = valuesAndPreds.mapToDouble(pair -> {
			double diff = pair._1() - pair._2();
			return diff * diff;
		}).mean();
		System.out.println("training Mean Squared Error = " + MSE);

		// Save and load model
		model.save(sc.sc(), "target/tmp/javaLinearRegressionWithSGDModel");
		LinearRegressionModel sameModel = LinearRegressionModel.load(sc.sc(),
				"target/tmp/javaLinearRegressionWithSGDModel");
		// $example off$

		sc.stop();
	}
}