package com.chaschev.mail

import java.io._
import java.nio.file.{Files, Path, Paths}

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.MailStatus._
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.{Duration, DateTime}
import org.json4s.native.Serialization._

import scala.collection.mutable
import GlobalContext.jsonFormats
import scala.collection.mutable.MutableList

/**
  * Created by andrey on 2/3/16.
  */
case class CacheManager(){
  def updateStoredMailbox(mailServer: MailServer, mailboxCached: MailboxCached): Unit = {
    mailboxCached.save(CacheManager.getPath(mailServer, mailboxCached).toFile)
  }

  def loadMailbox(mailServer: MailServer, mailbox: Mailbox): MailboxCached = {
    CacheManager.loadMailboxFromFile(mailServer, mailbox)
  }

  def init(): Unit = {
    initMailFolders(GlobalContext.conf.mailServers)
  }

  // creates folders for the caches
  def initMailFolders(folders: List[MailServer]): Unit = {
    CacheManager.logger.info("initializing cache folders...")

    Files.createDirectory(CacheManager.CACHE_PATH)
  }

  def unload(mailbox: Mailbox): Unit = {
    ???
  }
}

object MailboxCached {
  val logger: Logger = LogManager.getLogger(MailboxCached)

  def fromFile(file: File): MailboxCached =
    read[MailboxCached](new BufferedReader(new FileReader(file)))
}

case class MailboxCached(email: EmailAddress, folders: mutable.MutableList[MailFolderCached]) extends MailboxTrait[MailFolderCached] {
  import MailboxCached.logger

  def findFirstNotFetched: Option[MailFolderCached] =
    folders.find(f => f.status != fetched)

  def status: MailStatus = if(findFirstNotFetched.isDefined) error else fetched

  /*def mergeFolders(otherFolders: List[MailFolderTrait]): MailboxCached = {
    copy(folders = mergeFoldersHelper(otherFolders))
  }*/

  lazy val lastUpdate: Option[DateTime] = {
    folders.lastOption map {
      folder => folder.lastUpdate
    }
  }

  def save(file: File): Unit = {
    logger.info(s"updating $file")
    write(this, new BufferedWriter(new FileWriter(file)))
  }
}



class MailFolderCached(
  val name: String,
  var status: MailStatus,
  val messages: mutable.MutableList[MailMessage]
) extends MailFolderTrait {

  def findFirstNotFetched: Option[MailMessage] =
    messages.find(m => m.status != fetched)

  def findFirstNotFetchedIndex: Int =
    messages.indexWhere(f => f.status != fetched)

  def findAllNotFetched: mutable.MutableList[MailMessage] = {
    messages.filter(_.notFetched)
  }

  def lastUpdate: DateTime = {
    messages.lastOption match {
      case Some(msg) => msg.date
      case None => new DateTime(0)
    }
  }

  def lastMessageNumber: Int = {
    messages.lastOption match {
      case Some(msg) => msg.num
      case None => 0
    }

  }

  def updateStatus(): Unit = {
    findFirstNotFetched match {
      case Some(msg) => status = msg.status
      case None => status = fetched
    }
  }

}

case object CacheManager {
  val logger: Logger = LogManager.getLogger(CacheManager)

  val CACHE_PATH = Paths.get(".cache")

  def makeFsSafe(s: String): String = s.replaceAll("[\\W^.]+", "")

  def getPath[T <: MailFolderTrait](mailServer: MailServer, mailbox: MailboxTrait[T]): Path = {
    getPath(mailServer.address, mailbox.email.name)
  }

  def getPath(server: String, mailbox: String): Path = {
    import CacheManager.{makeFsSafe => safe}

    CacheManager.CACHE_PATH.resolve(Paths.get(safe(server), safe(mailbox) + ".json"))
  }

  // initializes with an empty cache if doesn't exist
  def loadMailboxFromFile(mailServer: MailServer, mailbox: Mailbox): MailboxCached = {
    val file = getPath(mailServer, mailbox).toFile

    if(file.exists()) return MailboxCached.fromFile(file)

    val parent = file.getParentFile

    if(!parent.exists()){
      if(!parent.mkdirs()) throw new RuntimeException("unable to make a parent folder: " + parent.getAbsolutePath)
    }

    MailboxCached(mailbox.email, new mutable.MutableList[MailFolderCached])
  }


}

