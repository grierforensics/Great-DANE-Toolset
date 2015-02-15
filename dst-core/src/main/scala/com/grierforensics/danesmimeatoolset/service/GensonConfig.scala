package com.grierforensics.danesmimeatoolset.service

import com.owlike.genson.{ScalaBundle, GensonBuilder}

object GensonConfig {
  val genson = new GensonBuilder().
    useIndentation(true).
    useRuntimeType(true).
    useDateAsTimestamp(true).
    withBundle(ScalaBundle().useOnlyConstructorFields(false)).
    create()
}