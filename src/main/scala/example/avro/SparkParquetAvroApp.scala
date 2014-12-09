package example.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.{AvroJob, AvroValue }
import org.apache.avro.mapreduce.{ AvroKeyOutputFormat }
import org.apache.avro.specific.SpecificRecord
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.SparkContext._
import org.apache.spark.{SparkConf, SparkContext}
import parquet.avro.{AvroParquetOutputFormat, AvroWriteSupport, AvroReadSupport}

import scala.reflect.ClassTag

// Map Reduce.
import org.apache.hadoop.mapreduce.Job

// Parquet and Thrift support.
import parquet.hadoop.{ParquetInputFormat, ParquetOutputFormat}

import com.google.common.io.Files
import org.apache.hadoop.fs.Path
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.util.getTempFilePath

// our own classes generated from user.avdl by Avro tools
import example.avro.spark_parquet_avro.SampleAvroObject
import com.twitter.bijection.avro.SpecificAvroCodecs


/*
rm -rf /tmp/sample_store/avro && sbt assembly && ~SPARK_HOME/bin/spark-submit \
  --class "example.avro.SparkParquetAvroApp" \
  --jars avro-1.7.7.jar \
  --master "local[*]" \
  target/scala-2.10/SparkParquetAvroThrift.jar
 */

object SparkParquetAvroApp {
  def setKryoProperties(conf: SparkConf) = {
    conf.set("spark.kryo.registrator", classOf[SparkAvroKryoRegistrator].getName)
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryoserializer.buffer.mb", 4.toString)
    conf.set("spark.kryo.referenceTracking", "false")
  }

  def readParquetRDD[T <% SpecificRecord](sc: SparkContext, parquetFile: String)(implicit tag: ClassTag[T]): RDD[T] = {
    val jobConf= new JobConf(sc.hadoopConfiguration)
    ParquetInputFormat.setReadSupportClass(jobConf, classOf[AvroReadSupport[T]])
    sc.newAPIHadoopFile(
      parquetFile,
      classOf[ParquetInputFormat[T]],
      classOf[Void],
      tag.runtimeClass.asInstanceOf[Class[T]],
      jobConf)
      .map(_._2.asInstanceOf[T])
  }

  def main(args: Array[String]) {
    val mem = "3g"
    println("Initializing Spark context.")
    println("  Memory: " + mem)
    val sparkConf = setKryoProperties(new SparkConf())
      .setAppName("SparkParquetThrift")
      .setMaster("local[*]")
      .setSparkHome("~/Apps/spark-1.1.1-bin-hadoop2.4")
      .setJars(Seq())
      .set("spark.executor.memory", mem)
    val sc = new SparkContext(sparkConf)

    println("Creating sample Avro data.")
    val sampleData = Range(1,10).toSeq.map{ v: Int =>
      new SampleAvroObject("a"+v,"b"+v,"c"+v)
    }
    println(sampleData.map("  - " + _).mkString("\n"))


    AvroJob.setOutputSchema(new JobConf(), SampleAvroObject.SCHEMA$)

    val parquetStore = "file:///tmp/sample_store/avro"
    println("Writing sample data to Parquet.")
    println("  - ParquetStore: " + parquetStore)

    val job = new Job(sc.hadoopConfiguration)

    ParquetOutputFormat.setWriteSupportClass(job, classOf[AvroWriteSupport])
    AvroParquetOutputFormat.setSchema(job, SampleAvroObject.getClassSchema)

    sc.parallelize(sampleData)
      .map(obj => (null, obj))
      .saveAsNewAPIHadoopFile(s"$parquetStore/parquet",
        classOf[Void],
        classOf[SampleAvroObject],
        classOf[ParquetOutputFormat[SampleAvroObject]],
        job.getConfiguration
    )

    ParquetInputFormat.setReadSupportClass(job, classOf[AvroReadSupport[SampleAvroObject]])

//    implicit val specificAvroBinaryInjectionForSampleAvroObject = SpecificAvroCodecs.toBinary[SampleAvroObject]
//
//    ParquetOutputFormat.setWriteSupportClass(job, classOf[AvroWriteSupport])
//
//    AvroParquetOutputFormat.setSchema(job, SampleAvroObject.SCHEMA$)

//    sc.parallelize(sampleData)
//      .map(obj => (null, obj))
//      .saveAsNewAPIHadoopFile(
//        parquetStore,
//        classOf[Void],
//        classOf[SampleAvroObject],
//        classOf[ParquetOutputFormat[SampleAvroObject]],
//        job.getConfiguration
//      )
//
//    println("Reading 'col_a' and 'col_b' from Parquet data store.")
//
//    ParquetInputFormat.setReadSupportClass(
//      job,
//      classOf[AvroReadSupport[SampleAvroObject]]
//    )
//
////    job.getConfiguration.set("parquet.thrift.column.filter", "col_a;col_b")
//
//    val converter = sc.broadcast(specificAvroBinaryInjectionForSampleAvroObject)
//
//    val parquetData = readParquetRDD[SampleAvroObject](sc, parquetStore)
////    val parquetData = sc.newAPIHadoopFile(
////      parquetStore,
////      classOf[ParquetInputFormat[SampleAvroObject]],
////      classOf[Void],
////      classOf[SampleAvroObject],
////      job.getConfiguration
////    )
//    println(parquetData.collect().map("  - " + _).mkString("\n"))
  }
}
