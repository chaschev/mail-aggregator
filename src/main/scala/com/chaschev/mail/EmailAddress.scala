package com.chaschev.mail

import org.apache.commons.lang3.StringUtils

/**
  * Created by andrey on 2/3/16.
  */
case class EmailAddress (name: String) {
  lazy val server = StringUtils.substringAfterLast(name, "@")
  lazy val login = StringUtils.substringBeforeLast(name, "@")
}
