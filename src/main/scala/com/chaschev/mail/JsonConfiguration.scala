package com.chaschev.mail

import java.io.{BufferedWriter, FileWriter, File}
import java.time.Duration

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.MailStatus.MailStatus
import org.apache.commons.io.FileUtils
import org.json4s.native.Serialization._

import com.chaschev.mail.MailApp.GlobalContext.jsonFormats

/**
  * Created by andrey on 2/2/16.
  */

case class GlobalConfiguration(
    threadCount: Int = 64,
    connectionLimitPerServer: Int = 8,
    timeoutSec: Int = 60,
    retries: Int = 5,
    batchSize: Int = 200,
    updateIntervalHours: Int = 24
)

case class JsonConfiguration(
    global: GlobalConfiguration,
    mailServers: List[MailServer] = Nil
    ) {

    def saveToFile(file: File): Unit = synchronized {
      val out = new BufferedWriter(new FileWriter(file))
      try {
        writePretty(this, out)
      } finally {
        out.close()
      }
    }

    def findServer(email: String): MailServer = {
        mailServers.find(srv => srv.mailboxes.exists(mailbox => mailbox.email.name.equals(email))).get
    }

  def save(): JsonConfiguration = {
    saveToFile(GlobalContext.CONF_FILE)
    this
  }

  def updateEmailStatus(mailbox: MailboxTrait[MailFolderTrait], mailStatus: MailStatus): Unit ={
    findServer(mailbox.email.name)
  }

}
