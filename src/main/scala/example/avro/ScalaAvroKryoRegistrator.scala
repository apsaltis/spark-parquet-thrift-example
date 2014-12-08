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

package example.avro

import example.avro.{Message, User}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import it.unimi.dsi.fastutil.io.{FastByteArrayInputStream, FastByteArrayOutputStream}
import org.apache.avro.io.{BinaryDecoder, BinaryEncoder, DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecord}
import org.apache.spark.serializer.KryoRegistrator

import scala.reflect.ClassTag
import com.esotericsoftware.kryo.Kryo
import com.twitter.chill.avro.AvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.spark.serializer.KryoRegistrator



// This file is based (and mostly copied from):
// https://github.com/bigdatagenomics/adam/blob/master/adam-core/src/main/scala/org/bdgenomics/adam/serialization/ADAMKryoRegistrator.scala
// Thanks to Matt Massie (@massie) and the ADAM project

case class InputStreamWithDecoder(size: Int) {
  val buffer = new Array[Byte](size)
  val stream = new FastByteArrayInputStream(buffer)
  val decoder = DecoderFactory.get().directBinaryDecoder(stream, null.asInstanceOf[BinaryDecoder])
}

// NOTE: This class is not thread-safe; however, Spark guarantees that only a single thread
// will access it.
class AvroSerializer[T <: SpecificRecord](implicit tag: ClassTag[T]) extends Serializer[T] {
  val reader = new SpecificDatumReader[T](tag.runtimeClass.asInstanceOf[Class[T]])
  val writer = new SpecificDatumWriter[T](tag.runtimeClass.asInstanceOf[Class[T]])
  var in = InputStreamWithDecoder(1024)
  val outstream = new FastByteArrayOutputStream()
  val encoder = EncoderFactory.get().directBinaryEncoder(outstream, null.asInstanceOf[BinaryEncoder])

  setAcceptsNull(false)

  def write(kryo: Kryo, kryoOut: Output, record: T) = {
    outstream.reset()
    writer.write(record, encoder)
    kryoOut.writeInt(outstream.array.length, true)
    kryoOut.write(outstream.array)
  }

  def read(kryo: Kryo, kryoIn: Input, klazz: Class[T]): T = this.synchronized {
    val len = kryoIn.readInt(true)
    if (len > in.size) {
      in = InputStreamWithDecoder(len + 1024)
    }
    in.stream.reset()
    // Read Kryo bytes into input buffer
    kryoIn.readBytes(in.buffer, 0, len)
    // Read the Avro object from the buffer
    reader.read(null.asInstanceOf[T], in.decoder)
  }
}

class SparkAvroKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    //kryo.register(classOf[User], new AvroSerializer[User]())
    //kryo.register(classOf[Message], new AvroSerializer[Message]())
    kryo.register(classOf[GenericRecord], AvroSerializer.GenericRecordSerializer[GenericRecord]())
    kryo.register(classOf[User], AvroSerializer.SpecificRecordSerializer[User])
    kryo.register(classOf[Message], AvroSerializer.SpecificRecordSerializer[Message])

  }
}