package com.chaschev.mail.graph

import java.io.{FileOutputStream, PrintStream}

import com.chaschev.mail.MailApp.GlobalContext
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

/**
  * Created by andrey on 2/23/16.
  */
trait GraphWriter {
  def write(graph: Graph, aliasTable: AliasTable, out1: PrintStream, out2: Option[PrintStream] = None)
}

object GephiWriter {
  val logger: Logger = LogManager.getLogger(getClass)
}

class GephiWriter extends GraphWriter {
  import GephiWriter.logger

  override def write(graph: Graph, aliasTable: AliasTable, out1: PrintStream, out2: Option[PrintStream] = None): Unit = {
    out1.println("Id,Label,Interval")

    for(node <- graph.list) {
      out1.println(s"${node.num},${node.num} ${node.name},")
    }

    out1.close()

    out2.get.println("Source,Target,Type")

    val nodesCount = graph.list.length
    var edgesCount = 0

    for(node1 <- graph.list) {
      val nodes2  = graph.getNondirectSiblings(node1)

      if(graph.isIsolated(node1)) {
        logger.warn(s"$node1 is isolated")
      }

      for(node2 <- nodes2) {
        out2.get.println(s"${node1.num},${node2.num},Undirected")
        edgesCount += 1
      }
    }

    out2.get.close()

    println(s"done. nodes: $nodesCount, edges: $edgesCount")
    println("top writers:")

    val toSet: Set[(String, mutable.Set[GraphNode])] = graph.graph.toSet
    val sortedList = toSet.toList.sortBy(x => -x._2.size)
    val top10 = sortedList.take(10)
    for((name, set) <- top10) {
      println(s"$name ${set.size}")
    }
  }
}


