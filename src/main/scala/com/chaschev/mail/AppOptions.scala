package com.chaschev.mail

import java.util

import chaschev.util.JOptOptions
import chaschev.util.JOptOptions.parser
import joptsimple.util.KeyValuePair
import scala.collection.convert.decorateAsJava._


/**
  * Created by andrey on 2/3/16.
  */
class AppOptions(args: Array[String]) extends JOptOptions(args) {
  val FORCE_FETCH = parser
    .accepts("force", "forces mailboxes to update")

  val FETCH_MODE = parser
    .acceptsAll(List("f", "fetch").asJava, "fetch mode. Fetches e-mails to local cache")

  val PRINT_GRAPH_MODE = parser
    .acceptsAll(List("p", "print-graph").asJava, "print graph mode - scan the caches")
}
