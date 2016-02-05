package com.chaschev.mail

import java.io.File

import com.chaschev.mail.MailApp.GlobalContext.jsonFormats
import org.apache.commons.io.FileUtils
import org.json4s.native.Serialization._

/**
  * Created by andrey on 2/5/16.
  */
object PasswordManager {
  val passwords: Map[String, String] = {
    read[Map[String, String]](FileUtils.readFileToString(new File("pwd.json")))
  }

  def getPassword(mailbox: Mailbox): String = {
    passwords(mailbox.email.name)
  }
}
