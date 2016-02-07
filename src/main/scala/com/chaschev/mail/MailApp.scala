package com.chaschev.mail

import java.io.{PrintStream, BufferedWriter, FileWriter, File}
import java.util.concurrent.{ExecutorService, Executors}
import javax.mail._

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.{Logger, LogManager}
import org.json4s.native.Serialization._

/**
  * Created by andrey on 2/2/16.
  */
object MailApp {
  val logger: Logger = LogManager.getLogger(MailApp)

  class FetchMode {
    val supplierServerPool: ExecutorService = Executors.newFixedThreadPool(GlobalContext.conf.mailServers.size)
    val connectionManager: ConnectionManager = new ConnectionManager()

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        logger.info(s"shutting down supplier pool")

        supplierServerPool.shutdown()
      }
    }) )
  }

  object GlobalContext {
    val CONF_FILE = new File("conf.json")

    lazy val conf: JsonConfiguration = {
      val conf = read[JsonConfiguration](FileUtils.readFileToString(CONF_FILE))

      conf
    }

    lazy val cacheManager: CacheManager = {
      val manager = new CacheManager()

      manager.init()

      manager
    }

    def iterateOverMessages(f: (MailServer, Mailbox, MailboxCached, MailFolderCached, MailMessage) => Unit): Unit = {
      for(srv <- conf.mailServers) {
        for(mailbox <- srv.mailboxes) {
          val mailboxCached = cacheManager.loadMailbox(srv, mailbox)

          for (folder <- mailboxCached.foldersAsScala) {
            for(message <- folder.messagesAsScala) {
              f(srv,mailbox, mailboxCached, folder, message)
            }
          }
        }
      }
    }

    var fetchMode: Option[FetchMode] = None

    def saveConf(): JsonConfiguration = conf.save()

    /*def saveConf(): Unit ={
      writePretty(conf.toJsonVersion(), new BufferedWriter(new FileWriter(CONF_FILE)))
    }*/

    implicit val jsonFormats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all +
      new org.json4s.ext.EnumNameSerializer(MailStatus)
  }

  def main(args: Array[String]) {
    val props = System.getProperties()

    props.setProperty("mail.store.protocol", "imaps")

    val session = Session.getDefaultInstance(props, null)
    val store = session.getStore("imaps")


    // read json
    // read mailboxes
    // connectionManager
    //
    //
    // group mailboxes per server
    // create produces threadpool per server
    //
    // + fetch mode
    // each mailbox thread {
    //   load messages from cache
    //    if force => force connection
    //    if less than 1days & all ok => next mailbox
    //   store = connectionManager.get(mailbox)
    //   if not available, wait 10 sec and redo
    //   if available, send store to mail fetcher
    //
    // mail fetcher:
    //   update folders, save configuration - centralize conf saving
    //   scan through mail, refetch problematic
    //   update for the dates left
    //   store to cache each 100 mails

    // + scan cache mode:
    //   load conf
    //   read caches


    try {
      // use imap.gmail.com for gmail
      store.connect("imap.mail.ru", "X", "X")

      val list2 = store.getDefaultFolder.list().toList.filter(x => (x.getType & javax.mail.Folder.HOLDS_MESSAGES) != 0)

      println(list2)

      val inbox = store.getFolder("Inbox")
      inbox.open(Folder.READ_ONLY)

      // limit this to 20 message during testing
      val messages = inbox.getMessages()
      val limit = 20
      var count = 0


      for (message <- messages) {
        val message1: Message = message
        count = count + 1
        if (count > limit) System.exit(0)
        println(message.getSubject())
      }
      inbox.close(true)
    } catch {
      case e: NoSuchProviderException =>  e.printStackTrace()
        System.exit(1)
      case me: MessagingException =>      me.printStackTrace()
        System.exit(2)
    } finally {
      store.close()
    }
  }
}
