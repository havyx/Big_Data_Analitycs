package com.evertonsavio;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.evaluation.ClusteringEvaluator;
import org.apache.spark.ml.feature.OneHotEncoderEstimator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import javax.xml.crypto.Data;

public class GymCompetitorsClusteringSolution {

public static void main(String[] args) {
		
		System.setProperty("hadoop.home.dir", "c:/hadoop");
		Logger.getLogger("org.apache").setLevel(Level.WARN);

		SparkSession spark = SparkSession.builder()
				.appName("Gym Competitors")
				.config("spark.sql.warehouse.dir","file:///c:/tmp/")
				.master("local[*]").getOrCreate();
		
		Dataset<Row> csvData = spark.read()
				.option("header", true)
				.option("inferSchema", true)
				.csv("src/main/resources/GymCompetition.csv");

		//csvData.printSchema();
		
		StringIndexer genderIndexer = new StringIndexer();
		genderIndexer.setInputCol("Gender");
		genderIndexer.setOutputCol("GenderIndex");
		csvData = genderIndexer.fit(csvData).transform(csvData);
				
		OneHotEncoderEstimator genderEncoder = new OneHotEncoderEstimator();
		genderEncoder.setInputCols(new String[] {"GenderIndex"});
		genderEncoder.setOutputCols(new String[] {"GenderVector"});
		csvData = genderEncoder.fit(csvData).transform(csvData);
		csvData.show();

		VectorAssembler vectorAssembler = new VectorAssembler();
		Dataset<Row> inputData = vectorAssembler.setInputCols(new String[]{"GenderVector", "Age", "Height", "Weight", "NoOfReps"})
				.setOutputCol("features").transform(csvData).select("features");

		inputData.show();

		KMeans kMeans = new KMeans();

		for (int noOfClusters = 2; noOfClusters <= 8; noOfClusters++){

			kMeans.setK(noOfClusters);
			System.out.println("No of clusters: "+ noOfClusters);

			KMeansModel model = kMeans.fit(inputData);
			Dataset<Row> predictions = model.transform(inputData);
			predictions.show();

			Vector[] clusterCentrs = model.clusterCenters();
			for(Vector v : clusterCentrs){
				System.out.println(v);
			}

			predictions.groupBy("prediction").count().show();
			System.out.println("sse is " + model.computeCost(inputData));
			ClusteringEvaluator evaluator = new ClusteringEvaluator();
			System.out.println("Slihouette with squared euclidian distance is " + evaluator.evaluate(predictions));

		}

	}
}
