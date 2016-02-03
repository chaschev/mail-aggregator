package com.chaschev.mail

import java.io.File
import java.util.concurrent.{ExecutorService, Executors}
import javax.mail._

import org.apache.commons.io.FileUtils
import org.json4s.native.Serialization._

/**
  * Created by andrey on 2/2/16.
  */
object MailApp {
  class FetchMode {
    val supplierServerPool: ExecutorService = Executors.newFixedThreadPool(GlobalContext.conf.mailServers.size)

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        supplierServerPool.shutdown()
      }
    }) )
  }

  object GlobalContext {
    lazy val conf: JsonConfiguration = {
      val conf = read[JsonConfiguration](FileUtils.readFileToString(new File("conf.json")))

      conf
    }

    lazy val cacheManager: CacheManager = {
      val manager = new CacheManager()

      manager.initFolders
      manager.initMailFolders(conf.mailServers)
    }

    var fetchMode: Option[FetchMode] = None

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
    // each mailbox {
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