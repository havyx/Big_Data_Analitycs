package com.evertonsavio;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

import static org.apache.spark.sql.functions.*;


public class UDF_UserDefinedFunctionsSpark {

    @SuppressWarnings("resource")
    public static void main(String[] args)
    {
        System.setProperty("hadoop.home.dir", "c:/hadoop");
        Logger.getLogger("org.apache").setLevel(Level.WARN);

        SparkSession spark = SparkSession.builder().appName("testingSql").master("local[*]")
                .config("spark.sql.warehouse.dir","file:///c:/tmp/")
                .getOrCreate();

        Dataset<Row> dataset = spark.read()
                .option("header", true)
                //.option("inferSchema", true) //Performance Issues, prefer CAST
                .csv("src/main/resources/exams/students.csv");

        //dataset = dataset.withColumn("pass", lit("Yes"));
        //dataset = dataset.withColumn("pass", lit(col("grade").equalTo("A+")));
        //USING UDF User Defined Function
        spark.udf().register("hasPassed", (String grade) -> grade.equals("A+"), DataTypes.BooleanType);

        dataset = dataset.withColumn("pass", callUDF("hasPassed", col("grade")));

        dataset.show();
        spark.close();

    }

}