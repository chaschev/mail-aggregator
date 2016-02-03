package samples

import com.chaschev._
import com.chaschev.mail._

import org.json4s.native.Serialization.{read, write}

/**
  * Created by andrey on 2/2/16.
  */
object json {
  def main(args: Array[String]): Unit = {
    implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all +
      new org.json4s.ext.EnumNameSerializer(MailStatus)

    val ser = write(JsonConfiguration(
      GlobalConfiguration(),
      List(
        MailServer("mail.ru", "addr", folders = List(
          new MailFolder("Inbox", MailStatus.fetched)
        ))
      )
    ))

    println(ser)

    println(read[JsonConfiguration](ser))
  }
}
