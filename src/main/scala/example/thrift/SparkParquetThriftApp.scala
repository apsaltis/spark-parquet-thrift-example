///////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
///////////////////////////////////////////////////////////////////////////

package example.thrift

// Scala collections.

// Spark.

import example.thrift.spark_parquet_thrift.SampleThriftObject
import org.apache.spark
import org.apache.spark.SparkContext._
import org.apache.spark.{SparkConf, SparkContext}

// Map Reduce.
import org.apache.hadoop.mapreduce
import org.apache.hadoop.mapreduce.Job

// File.

// Parquet and Thrift support.
import parquet.hadoop.thrift.{ParquetThriftInputFormat, ParquetThriftOutputFormat, ThriftReadSupport}
import parquet.hadoop.{ParquetInputFormat, ParquetOutputFormat}


/*
sbt assembly && ~SPARK_HOME/bin/spark-submit \
  --class "example.thrift.SparkParquetThriftApp" \
  --master "local[*]" \
  target/scala-2.10/SparkParquetAvroThrift.jar
 */

object SparkParquetThriftApp {
  def main(args: Array[String]) {
    val mem = "3g"
    println("Initializing Spark context.")
    println("  Memory: " + mem)
    val sparkConf = new SparkConf()
      .setAppName("SparkParquetThrift")
      .setMaster("local[*]")
      .setSparkHome("~/Apps/spark-1.1.1-bin-hadoop2.4")
      .setJars(Seq())
      .set("spark.executor.memory", mem)
    val sc = new SparkContext(sparkConf)

    println("Creating sample Thrift data.")
    val sampleData = Range(1,10).toSeq.map{ v: Int =>
      new SampleThriftObject("a"+v,"b"+v,"c"+v)
    }
    println(sampleData.map("  - " + _).mkString("\n"))

    val job = new Job()
    val parquetStore = "file:///tmp/sample_store"
    println("Writing sample data to Parquet.")
    println("  - ParquetStore: " + parquetStore)
    ParquetThriftOutputFormat.setThriftClass(job, classOf[SampleThriftObject])
    ParquetOutputFormat.setWriteSupportClass(job, classOf[SampleThriftObject])
    sc.parallelize(sampleData)
      .map(obj => (null, obj))
      .saveAsNewAPIHadoopFile(
        parquetStore,
        classOf[Void],
        classOf[SampleThriftObject],
        classOf[ParquetThriftOutputFormat[SampleThriftObject]],
        job.getConfiguration
      )

    println("Reading 'col_a' and 'col_b' from Parquet data store.")
    ParquetInputFormat.setReadSupportClass(
      job,
      classOf[ThriftReadSupport[SampleThriftObject]]
    )
    job.getConfiguration.set("parquet.thrift.column.filter", "col_a;col_b")
    val parquetData = sc.newAPIHadoopFile(
      parquetStore,
      classOf[ParquetThriftInputFormat[SampleThriftObject]],
      classOf[Void],
      classOf[SampleThriftObject],
      job.getConfiguration
    ).map{case (void,obj) => obj}
    println(parquetData.collect().map("  - " + _).mkString("\n"))
  }
}
