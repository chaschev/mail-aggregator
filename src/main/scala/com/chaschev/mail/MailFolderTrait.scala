package com.chaschev.mail

import org.joda.time.DateTime

/**
  * Created by andrey on 2/3/16.
  */
trait MailFolderTrait {
  val name: String

  def lastUpdate: DateTime

  def castTo[T <: MailFolderTrait](aClass: Class[T]) : T
}
