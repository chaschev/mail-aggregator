package com.chaschev.mail


import chaschev.lang.OpenBean
import chaschev.util.JOptOptions
import joptsimple.OptionParser
import scala.collection.convert.decorateAsJava._

object AppOptions {
  val parser = OpenBean.getStaticFieldValue(classOf[chaschev.util.JOptOptions], "parser").asInstanceOf[OptionParser]

  def init: Unit = {
    FORCE_FETCH
    FETCH_MODE
    PRINT_GRAPH_MODE
  }

  val FORCE_FETCH = parser
    .accepts("force", "forces mailboxes to update")

  val FETCH_MODE = parser
    .acceptsAll(List("f", "fetch").asJava, "fetch mode. Fetches e-mails to local cache")

  val PRINT_GRAPH_MODE = parser
    .acceptsAll(List("p", "print-graph").asJava, "print graph mode - scan the caches")
}

/**
  * Created by andrey on 2/3/16.
  */
class AppOptions(args: Array[String]) extends JOptOptions(args) {

}
