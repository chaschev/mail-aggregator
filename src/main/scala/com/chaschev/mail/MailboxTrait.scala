package com.chaschev.mail

import org.joda.time.{Duration, DateTime}

import scala.collection.mutable
import scala.collection.mutable.MutableList

/**
  * Created by andrey on 2/3/16.
  */
trait MailboxTrait[T <: MailFolderTrait] {
  def lastUpdate: Option[DateTime]

  val email: EmailAddress
  val folders: mutable.MutableList[T]

  def updatedInLast(duration: Duration): Boolean = {
    lastUpdate match {
      case Some(d) => duration.isLongerThan(new Duration(DateTime.now(), d))
      case None => false
    }
  }

  //to update loaded configuration from server
  def mergeFolders(otherFolders: List[MailFolderTrait]): mutable.MutableList[T] = {
    val map1 = this.folders.groupBy(_.name).mapValues(_.head)
    val map2 = otherFolders.groupBy(_.name).mapValues(_.head)

    val result: mutable.MutableList[MailFolderTrait] = mutable.MutableList[MailFolderTrait]()

    for(f <- this.folders){
      result += (if (map2.contains(f.name)) {
        map2(f.name)
      } else {
        f
      })
    }

    for(f <- otherFolders){
      if(!map1.contains(f.name)){
        result += f
      }
    }

    this.folders.clear()
    this.folders ++= result.asInstanceOf[mutable.MutableList[T]]
  }

}
