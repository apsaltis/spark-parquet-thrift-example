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

class SparkAvroKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    //kryo.register(classOf[User], new AvroSerializer[User]())
    //kryo.register(classOf[Message], new AvroSerializer[Message]())
    kryo.register(classOf[GenericRecord], AvroSerializer.GenericRecordSerializer[GenericRecord]())
    kryo.register(classOf[User], AvroSerializer.SpecificRecordSerializer[User])
    kryo.register(classOf[Message], AvroSerializer.SpecificRecordSerializer[Message])

  }
}