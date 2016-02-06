package com.chaschev.mail

import java.util

import org.joda.time.{Duration, DateTime}

import scala.collection.mutable
import java.util.ArrayList
import scala.collection.convert.decorateAsScala._



/**
  * Created by andrey on 2/3/16.
  */
trait MailboxTrait[T <: MailFolderTrait] {
  def lastUpdate: Option[DateTime]

  val email: EmailAddress
  val folders: ArrayList[T]

  def foldersAsScala: List[T] = folders.asScala.toList



  def updatedInLast(duration: Duration): Boolean = {
    lastUpdate match {
      case Some(d) => duration.isLongerThan(new Duration(d, DateTime.now()))
      case None => false
    }
  }

  //to update loaded configuration from server
  def mergeFolders(otherFolders: List[MailFolderTrait]): Unit = {
    val map1 = this.foldersAsScala.groupBy(_.name).mapValues(_.head)
    val map2 = otherFolders.groupBy(_.name).mapValues(_.head)

    val result = new ArrayList[MailFolderTrait]()

    for(f <- this.foldersAsScala){
      if (map2.contains(f.name)) {

        result.add(f) // todo update f with map2(f.name)
       }
    }

    for(f <- otherFolders){
      if(!map1.contains(f.name)){
        result.add(f)
      }
    }

    this.folders.clear()
    this.folders.addAll(result.asInstanceOf[ArrayList[T]])
  }

}
