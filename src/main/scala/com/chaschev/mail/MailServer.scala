package com.chaschev.mail

import java.util.{Date, ArrayList}
import javax.mail.Message.RecipientType
import javax.mail.{Multipart, MessagingException, Address, Message}

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.MailStatus.MailStatus
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.DateTime

import scala.reflect.ClassTag
import scala.util.control.NonFatal

sealed abstract class ProgressStatus

case class notStarted() extends ProgressStatus

case class inProgress() extends ProgressStatus

case class timeouts() extends ProgressStatus

case class errors() extends ProgressStatus

case class finished() extends ProgressStatus

case class MailFolder(
  name: String,
  lastUpdate: DateTime,
  status: MailStatus
) extends MailFolderTrait {
  override def castTo[T <: MailFolderTrait](aClass: Class[T]): T = {
    if(aClass == classOf[MailFolder]) return this.asInstanceOf[T]

    if(aClass == classOf[MailFolderCached] ) {
      return new MailFolderCached(name, status, new ArrayList[MailMessage]()).asInstanceOf[T]
    }

    throw new RuntimeException("shit happened in conversion")
  }
}


case class MailMessage(
  num: Int = 0,
  date: DateTime,
  from: Array[String],
  to: Array[String],
  status: MailStatus,
  partCount: Int
) {
  def shortString() = s"$date ${fromEmails()} - ${toEmails()}"

  def extractEmail(s: String): String = {
    MailMessage.emailPattern.findFirstMatchIn(s).map { matcher =>
      matcher.group(1).toLowerCase
    }.getOrElse({
      if(s.contains("@")) {
        s.toLowerCase
      }else {
        "empty!"
      }
    })
  }

  def fromEmails(): Array[String] = from.map(extractEmail)
  def toEmails(): Array[String] = to.map(extractEmail)

  def notFetched: Boolean = num == 0 || status != MailStatus.fetched
}

object MailMessage {
  val emailPattern = "[<](.*[@].*)[>]".r

  val logger: Logger = LogManager.getLogger(MailMessage)

  def mapAddress(address: Address): String = address.toString

  def arrayFromNullAble[T: ClassTag](a: Array[T]): Array[T] =  if(a == null) Array.empty[T] else a

  def from(msg: Message): Option[MailMessage] = {
    try {
      val partCount = msg.getContent match {
        case multipart: Multipart =>
          multipart.getCount
        case s: String => 0
        case _ => -1
      }

      Some(MailMessage(
        msg.getMessageNumber,
        new DateTime(msg.getSentDate),
        msg.getFrom.map {mapAddress},
        (arrayFromNullAble(msg.getRecipients(RecipientType.TO))
          ++ arrayFromNullAble(msg.getRecipients(RecipientType.CC))
          ++ arrayFromNullAble(msg.getRecipients(RecipientType.BCC))
          ).map {mapAddress},
        MailStatus.fetched, partCount
      ))
    } catch {
      case NonFatal(e) =>
        logger.warn(s"error on message ${e.toString}")
        None
    }


  }
}

case class MailboxStats(
  totalMessages: Int
)

case class Mailbox(
  email: EmailAddress,
  folders: ArrayList[MailFolder] = new ArrayList(),
  lastUpdate: Option[DateTime] = Some(new DateTime(0)),
  var stats: MailboxStats = MailboxStats(0)
) extends MailboxTrait[MailFolder] {
  def updateStats(mailboxCached: MailboxCached): Unit = {
    stats = MailboxStats(
      mailboxCached.foldersAsScala.foldLeft(0)((sum, folder) => sum + folder.messages.size())
    )
    GlobalContext.saveConf()
  }

  override def toString: String = s"Mailbox(${email.name}, ${folders.size} folders)"

}

case class MailServer(
  name: String,
  address: String,
  mailboxes: List[Mailbox],
  var totalMessages:Int = 0,
  port: Int = 993
) {
  def updateStatsOnDisks(): Unit = {
    totalMessages = mailboxes.foldLeft(0)((sum, box) => sum + box.stats.totalMessages)
    GlobalContext.saveConf()
  }


  def findMailbox(email: EmailAddress): Mailbox = {
    mailboxes.find(_.email.equals(email)).get
  }
}
