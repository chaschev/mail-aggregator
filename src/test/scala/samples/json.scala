package samples

import java.util

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail._
import com.google.common.collect.Lists
import org.joda.time.DateTime
import org.json4s.native.Serialization
import scala.collection.convert.decorateAsJava._


import org.json4s.native.Serialization.{writePretty, read, write}

import com.chaschev.mail.MailApp.GlobalContext.jsonFormats
import org.junit.{Assert, Test}

import scala.collection.mutable

/**
  * Created by andrey on 2/2/16.
  */
class json {
  @Test
  def testJsonConfSer(): Unit = {
    val ser = writePretty(JsonConfiguration(
      GlobalConfiguration(),
      List(
        MailServer("mail.ru", "addr", mailboxes = Nil )
      )
    ))

    println(ser)

    val deser: JsonConfiguration = read[JsonConfiguration](ser)
    println(deser)

    Assert.assertTrue("mail servers should not be empty", deser.mailServers.nonEmpty)
  }



  @Test
  def testMailboxSer(): Unit = {
    List("a", "b").asJava

    var ser = writePretty(Lists.newArrayList("a", "b"))

    println(ser)

    println(read[util.ArrayList[String]](ser))
  }
}
