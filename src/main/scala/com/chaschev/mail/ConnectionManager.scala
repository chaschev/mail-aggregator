package com.chaschev.mail

import java.util.concurrent.{ConcurrentHashMap, Semaphore}
import javax.mail.{Folder, Session, Store}

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.MailApp.GlobalContext._
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.DateTime

import scala.collection.concurrent.Map
import scala.collection.convert.decorateAsJava._
import scala.collection.convert.decorateAsScala._
import scala.util.control.NonFatal

/**
  * Created by andrey on 2/3/16.
  */
case class ActiveStore(server: MailServer, mail: Mailbox, store: Store, startedAt: DateTime = new DateTime()) {
  def getServerFolder(folder: MailFolderCached): Folder = {
    val f = store.getFolder(folder.name)

    f.open(Folder.READ_ONLY)

    f
  }

  def getFolders: List[MailFolder] = getFoldersRec(store.getDefaultFolder)

  private def getFoldersRec(root: Folder): List[MailFolder] = {
    var folders: List[Folder] = root.list().toList.filter(x => (x.getType & javax.mail.Folder.HOLDS_MESSAGES) != 0)

    folders.flatMap { folder =>
      MailFolder(folder.getFullName, DateTime.now(), MailStatus.error) :: getFoldersRec(folder)
    }
  }


}

object ServerConnection {
  val logger: Logger = LogManager.getLogger(ServerConnection)

  val session: Session = {
    val props = System.getProperties()

    props.setProperty("mail.store.protocol", "imaps")

    Session.getDefaultInstance(props, null)
  }
}

class ServerConnection(val server: MailServer) {

  import ServerConnection.logger

  private val semaphore = new Semaphore(GlobalContext.conf.global.connectionLimitPerServer)

  private var activeStores: Map[String, ActiveStore] = new ConcurrentHashMap[String, ActiveStore]().asScala

  def tryAcquire(mail: Mailbox): Option[ActiveStore] = {
    logger.debug(s"trying to acquire connection for $mail")

    if (!semaphore.tryAcquire(1)) return None

    val store = ServerConnection.session.getStore("imaps")

    store.connect(server.address, server.port, mail.email.name, PasswordManager.getPassword(mail))

    val activeStore = ActiveStore(GlobalContext.conf.findServer(mail.email.name), mail, store)

    activeStores.putIfAbsent(mail.email.name, activeStore)

    Some(activeStore)
  }

  def release(mail: Mailbox, activeStore: ActiveStore): Unit = {
    try {
      logger.debug(s"releasing connection for $mail")

      activeStores.remove(mail.email.name)
      activeStore.store.close()
    } finally {
      semaphore.release(1)
    }
  }
}

object ConnectionManager {
  val logger: Logger = LogManager.getLogger(ConnectionManager)
}


class ConnectionManager {

  import ConnectionManager.logger

  private var connections: Map[String, ServerConnection] = new ConcurrentHashMap[String, ServerConnection]().asScala

  def getConnection(mailServer: MailServer): ServerConnection = synchronized {
    var r: ServerConnection = null

    connections.putIfAbsent(mailServer.name, {
      r = new ServerConnection(mailServer)
      r
    }) match {
      case Some(x) => x
      case None => r
    }
  }


  def updateMessages(mailServer: MailServer, mailbox: Mailbox, activeStore: ActiveStore): Unit = {
    logger.info(s"updating messages for $mailbox")

    val currentFolders: List[MailFolder] = activeStore.getFolders

    logger.info(s"found ${currentFolders.size} server folders: ${currentFolders.mkString("\n  ")}")

    //merge folders from servers

    val mailboxCached = {
      logger.info("loading folders from cache")
      val tempMailboxCached = cacheManager.loadMailbox(mailServer, mailbox)

      logger.info("updating folders in cache")
      tempMailboxCached.mergeFolders(currentFolders.map{_.castTo(classOf[MailFolderCached])})

      cacheManager.updateStoredMailbox(mailServer, tempMailboxCached)

      mailServer.findMailbox(mailbox.email).mergeFolders(currentFolders)

      //not good
      logger.info("updating folders in conf file")
      GlobalContext.saveConf()

      tempMailboxCached

    }

    for (folder <- mailboxCached.foldersAsScala) {
      logger.info(s"working with folder: $folder")

      val notFetched = folder.findAllNotFetched

      val serverFolder = activeStore.getServerFolder(folder)

      try {
        val fetchFromNum = folder.lastMessageNumber + 1

        val maxMessageNumber = serverFolder.getMessageCount

        var i = fetchFromNum

        logger.info(s"trying to fetch ${maxMessageNumber - i + 1} messages $fetchFromNum..$maxMessageNumber, new messages count: ${serverFolder.getNewMessageCount}")

        while (i <= maxMessageNumber && !Thread.interrupted()) {
          val fetchUpto = Math.min(maxMessageNumber, i + GlobalContext.conf.global.batchSize - 1)

          if(i <= fetchUpto) {
            logger.info(s"${mailbox.email.name}/$folder fetching from $i to $fetchUpto")

            val messages = serverFolder.getMessages(i, fetchUpto)

            logger.debug(s"fetched ${messages.length} messages")

            var j = 0

            val fetchedMessages = messages.map({ msg => MailMessage.from(msg) })
            val filteredMessages: Array[MailMessage] = fetchedMessages.filter(_.isDefined).map(_.get)

            if(filteredMessages.size < fetchedMessages.size) {
              logger.info(s"could not fetch ${fetchedMessages.size - filteredMessages.size} messages")
            }

            folder.messages.addAll(filteredMessages.toList.asJava)
            folder.updateStatus()

            cacheManager.updateStoredMailbox(mailServer, mailboxCached)

            GlobalContext.saveConf()

            i = fetchUpto + 1
          }
        }
      }
      catch {
        case NonFatal(e) =>
          logger.warn(s"error with folder $folder", e)
      }
      finally {
        serverFolder.close(true)

        mailbox.updateStats(mailboxCached)
      }

    }
  }
}

