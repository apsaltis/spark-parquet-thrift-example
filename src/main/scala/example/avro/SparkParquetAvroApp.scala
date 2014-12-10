package example.avro

import java.io.File

import example.avro.UserOperations._
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.{AvroKey, AvroJob, AvroValue}
import org.apache.avro.mapreduce.{AvroKeyInputFormat, AvroKeyOutputFormat}
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
  --jars "avro-1.7.7.jar,parquet-avro-1.5.0.jar" \
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
//    val sampleData = Range(1,10).toSeq.map{ v: Int =>
//      new SampleAvroObject("a"+v,"b"+v,"c"+v)
//    }
//    println(sampleData.map("  - " + _).mkString("\n"))


    val parquetStore = "file:///tmp/sample_store/avro"
    println("Writing sample data to Parquet.")
    println("  - ParquetStore: " + parquetStore)

    val job = new Job(sc.hadoopConfiguration)

    def factory(v: Int): SampleAvroObject =
      SampleAvroObject.newBuilder().setColA("a"+v).setColB("b"+v).setColC("c"+v).build()

    val sampleAvroFile = getTempFilePath("sample", ".avro")
    val parquetFile = new Path(Files.createTempDir().toString, "sample.parquet")
    UserOperations.writeAvroFile[SampleAvroObject](sampleAvroFile, factory, 20)

    convertAvroToParquetAvroFile(
      new Path(sampleAvroFile.toString),
      new Path(parquetFile.toString),
      SampleAvroObject.getClassSchema,
      sc.hadoopConfiguration)

    ParquetOutputFormat.setWriteSupportClass(job,classOf[AvroWriteSupport])
    AvroParquetOutputFormat.setSchema(job, SampleAvroObject.SCHEMA$)

    val avroRdd = sc.newAPIHadoopFile(sampleAvroFile.toString(),
      classOf[AvroKeyInputFormat[SampleAvroObject]],
      classOf[AvroKey[SampleAvroObject]],
      classOf[NullWritable])

    avroRdd.foreach(i => println(" AVRO: " + i))

    val parquetData = readParquetRDD[SampleAvroObject](sc, parquetFile.toString)

    parquetData.foreach(i => println(" PARQUET: " + i))

    avroRdd.map(i => i._1).map(u => u.datum().col_a).foreach(println)

  }
}
