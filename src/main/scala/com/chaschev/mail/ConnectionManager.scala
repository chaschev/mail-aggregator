package com.chaschev.mail

import java.util.concurrent.{Semaphore, ConcurrentHashMap}
import javax.mail.search.{ComparisonTerm, ReceivedDateTerm}
import javax.mail.{Message, Folder, Session, Store}

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.MailApp.GlobalContext._
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.DateTime
import scala.collection.convert.decorateAsJava._


import scala.collection.concurrent.Map

import scala.collection.convert.decorateAsScala._

/**
  * Created by andrey on 2/3/16.
  */
case class ActiveStore(server: MailServer, mail: Mailbox, store: Store, startedAt: DateTime = new DateTime()) {
  def getServerFolder(folder: MailFolderCached): Folder = {
    val f = store.getFolder(folder.name)

    f.open(Folder.READ_ONLY)

    f
  }

  def getFolders: List[MailFolder] = {
    var folders: List[Folder] = store.getDefaultFolder.list().toList.filter(x => (x.getType & javax.mail.Folder.HOLDS_MESSAGES) != 0)

    folders.map { folder =>
      MailFolder(folder.getName, DateTime.now(), MailStatus.error)
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

    val activeStore = ActiveStore(GlobalContext.conf.findServer(mail), mail, store)

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

    //merge folders from servers

    val mailboxCached = {
      logger.info("updating folders in cache")
      val tempMailboxCached = cacheManager.loadMailbox(mailServer, mailbox)

      tempMailboxCached.mergeFolders(currentFolders)

      cacheManager.updateStoredMailbox(mailServer, tempMailboxCached)

      mailServer.findMailbox(mailbox.email).mergeFolders(currentFolders)

      //not good
      logger.info("updating folders in conf file")
      GlobalContext.saveConf()

      tempMailboxCached

    }

    for (folder <- mailboxCached.foldersAsScala) {
      val notFetched = folder.findAllNotFetched

      val serverFolder = activeStore.getServerFolder(folder)

      try {
        val fetchFromNum = folder.lastMessageNumber + 1

        val maxMessageNumber = serverFolder.getMessageCount

        var i = fetchFromNum

        while (i <= maxMessageNumber && !Thread.interrupted()) {
          val fetchUpto = Math.min(maxMessageNumber, i + GlobalContext.conf.global.batchSize)
          logger.info(s"${mailbox.email.name}/$folder fetching from $i to $fetchUpto")

          val messages = serverFolder.getMessages(1, maxMessageNumber)

          folder.messages.addAll(messages.map({ msg => MailMessage.from(msg) }).toList.asJava)

          cacheManager.updateStoredMailbox(mailServer, mailboxCached)

          i = fetchUpto + 1
        }
      } finally {
        serverFolder.close(true)
      }

    }
  }
}

