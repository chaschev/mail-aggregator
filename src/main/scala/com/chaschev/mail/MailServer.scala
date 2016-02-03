package com.chaschev.mail

import java.io.File
import java.nio.file.{Files, Path, Paths}

import com.chaschev.mail.MailStatus.MailStatus
import org.joda.time.DateTime

import scala.collection.mutable
import scala.collection.mutable.MutableList

sealed abstract class ProgressStatus

case class notStarted() extends ProgressStatus
case class inProgress() extends ProgressStatus
case class timeouts() extends ProgressStatus
case class errors() extends ProgressStatus
case class finished() extends ProgressStatus



case class MailFolder(
   name: String,
   status: MailStatus
)


case class MailFolderEntryCached(
    name: String,
    status: MailStatus,
    messages: List[MailMessage]
)

case object CacheManager {
  val CACHE_PATH = Paths.get(".cache")

  def makeFsSafe(s: String): String = s.replaceAll("[\\W^.]+", "")

  def getPath(server: String, mailbox: String, folder: String): Path = {
    import CacheManager.{makeFsSafe => safe}

    CacheManager.CACHE_PATH.resolve(Paths.get(safe(server), safe(mailbox), safe(folder)))
  }

  // initializes with an empty cache if doesn't exist
  def getFile(server: String, mailbox: String, folder: String): MailFolderCache = {
    val file = getPath(server, mailbox, folder).toFile

    if(file.exists()) return MailFolderCache.fromFile(file)

    val parent = file.getParentFile

    if(!parent.exists()){
      if(!parent.mkdirs()) throw new RuntimeException("unable to make a parent folder: " + parent.getAbsolutePath)
    }

    return MailFolderCache(file, new mutable.MutableList[MailFolderEntryCached])
  }
}

object MailFolderCache {
  def fromFile(file: File): MailFolderCache ={
    ???
  }
}

case class MailFolderCache(
   file: File,
   entries: MutableList[MailFolderEntryCached]
) {

}

case class CacheManager(){
  def initFolders = {
    Files.createDirectory(CacheManager.CACHE_PATH)
  }

  def initMailFolders(folders: List[MailFolder]): Unit = {
      ???
  }
}

case class MailMessage(
  date: DateTime,
  status: MailStatus
)

case class MailServer (
  name: String,
  address: String,
  port: Int = 993,
  folders: List[MailFolder] = Nil
  ) {

  //to update loaded configuration from server
  def mergeFolders(folders: List[MailFolder]): MailServer = {
    val map1 = this.folders.groupBy(_.name).mapValues(_.head)
    val map2 = folders.groupBy(_.name).mapValues(_.head)

    val result: MutableList[MailFolder] = MutableList()

    for(f <- this.folders){
        result += (if (map2.contains(f.name)) {
          map2(f.name)
        } else {
          f
        })
    }

    for(f <- folders){
      if(!map1.contains(f.name)){
        result += f
      }
    }

    this.copy(folders = result.toList)
  }

}
