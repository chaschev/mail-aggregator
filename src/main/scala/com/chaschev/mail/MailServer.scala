package com.chaschev.mail

import java.io.File
import java.nio.file.{Files, Path, Paths}

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
   status: MailStatus
)


case class MailFolderEntryCached(
    name: String,
    status: MailStatus,
    messages: List[MailMessage]
)


object MailFolderCache {
  def fromFile(file: File): MailFolderCache ={
    ???
  }
}

case class MailFolderCache(
   file: File,
   entries: MutableList[MailFolderEntryCached]
) {

}



case class MailMessage(
  date: DateTime,
  status: MailStatus
)

case class Mailbox(
                  email: EmailAddress,
                  folders: List[MailFolder] = Nil
                  ) {
  //to update loaded configuration from server
  def mergeFolders(folders: List[MailFolder]): Mailbox = {
    val map1 = this.folders.groupBy(_.name).mapValues(_.head)
    val map2 = folders.groupBy(_.name).mapValues(_.head)

    val result: MutableList[MailFolder] = MutableList()

    for(f <- this.folders){
      result += (if (map2.contains(f.name)) {
        map2(f.name)
      } else {
        f
      })
    }

    for(f <- folders){
      if(!map1.contains(f.name)){
        result += f
      }
    }

    this.copy(folders = result.toList)
  }

}

case class MailServer (
  name: String,
  address: String,
  port: Int = 993,
  mailboxes: List[Mailbox]
  ) {


}
