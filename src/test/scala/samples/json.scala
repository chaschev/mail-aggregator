package samples

import com.chaschev.mail._
import org.joda.time.DateTime
import org.json4s.native.Serialization

import org.json4s.native.Serialization.{read, write}

import com.chaschev.mail.MailApp.GlobalContext.jsonFormats

import scala.collection.mutable

/**
  * Created by andrey on 2/2/16.
  */
object json {
  def main(args: Array[String]): Unit = {
    val ser = Serialization.writePretty(JsonConfiguration(
      GlobalConfiguration(),
      List(
        MailServer("mail.ru", "addr", mailboxes =  Mailbox(EmailAddress("chaschev@mail.ru"), folders = mutable.MutableList(
          MailFolder("Inbox", DateTime.now, MailStatus.fetched)
        )) :: Nil)
      )
    ))

    println(ser)

    println(read[JsonConfiguration](ser))
  }
}
