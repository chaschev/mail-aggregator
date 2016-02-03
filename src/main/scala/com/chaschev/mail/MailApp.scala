package com.chaschev.mail

import javax.mail._

/**
  * Created by andrey on 2/2/16.
  */
object MailApp {
  def main(args: Array[String]) {

    val props = System.getProperties()
    props.setProperty("mail.store.protocol", "imaps")
    val session = Session.getDefaultInstance(props, null)
    val store = session.getStore("imaps")
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
