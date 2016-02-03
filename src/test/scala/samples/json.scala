package samples

import com.chaschev.mail._
import org.json4s.native.Serialization

import org.json4s.native.Serialization.{read, write}

import com.chaschev.mail.MailApp.GlobalContext.jsonFormats

/**
  * Created by andrey on 2/2/16.
  */
object json {
  def main(args: Array[String]): Unit = {
    val ser = Serialization.writePretty(JsonConfiguration(
      GlobalConfiguration(),
      List(
        MailServer("mail.ru", "addr", mailboxes =  Mailbox(EmailAddress("chaschev@mail.ru"), folders = List(
          MailFolder("Inbox", MailStatus.fetched)
        )) :: Nil)
      )
    ))

    println(ser)

    println(read[JsonConfiguration](ser))
  }
}
