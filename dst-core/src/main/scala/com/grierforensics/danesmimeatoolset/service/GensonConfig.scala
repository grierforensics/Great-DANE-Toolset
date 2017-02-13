// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.service

import javax.mail.internet.InternetAddress

import com.owlike.genson._
import com.owlike.genson.stream.{ObjectReader, ObjectWriter}

object GensonConfig {
  val genson = new GensonBuilder().
    useIndentation(true).
    useRuntimeType(true).
    useDateAsTimestamp(true).
    withConverters(new InternetAddressConverter).
    withBundle(ScalaBundle().useOnlyConstructorFields(false)).
    create()
}

class InternetAddressConverter extends Converter[InternetAddress] {
  override def serialize(ia: InternetAddress, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeValue(ia.toString)
  }

  override def deserialize(reader: ObjectReader, ctx: Context): InternetAddress = {
    new InternetAddress(reader.valueAsString())
  }
}
