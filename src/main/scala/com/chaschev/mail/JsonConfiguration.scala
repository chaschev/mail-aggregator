package com.chaschev.mail

import java.io.File
import java.time.Duration

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
    updateInterval: String = "1d"
)

case class JsonConfiguration(
    global: GlobalConfiguration,
    mailServers: List[MailServer] = Nil
    ) {

    def saveToFile(file: File): Unit = synchronized {
        FileUtils.writeStringToFile(file, writePretty(this))
    }

    def findServer(mail: Mailbox): MailServer = {
        mailServers.find(_.mailboxes.contains(mail)).get
    }
}
