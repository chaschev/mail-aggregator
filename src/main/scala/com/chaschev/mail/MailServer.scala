package com.chaschev.mail

import java.io.File
import java.nio.file.{Files, Path, Paths}
import javax.mail.Message

import com.chaschev.mail.MailStatus.MailStatus
import org.joda.time.DateTime

import scala.collection.mutable
import scala.collection.mutable.MutableList

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
) extends MailFolderTrait


case class MailMessage(
  num: Int = 0,
  date: DateTime,
  status: MailStatus
) {
  def notFetched: Boolean = num == 0 || status != MailStatus.fetched
}

object MailMessage {
  def from(msg: Message): MailMessage = {
    MailMessage(msg.getMessageNumber, new DateTime(msg.getSentDate), MailStatus.fetched)
  }
}


case class Mailbox(
    email: EmailAddress,
    folders: mutable.MutableList[MailFolder],
    lastUpdate: Option[DateTime] = Some(new DateTime(0))
) extends MailboxTrait[MailFolder]

case class MailServer (
  name: String,
  address: String,
  port: Int = 993,
  mailboxes: List[Mailbox]
  ) {

  def findMailbox(email: EmailAddress): Mailbox = {
    mailboxes.find(_.email.equals(email)).get
  }
}
