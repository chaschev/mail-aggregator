package com.chaschev.mail.graph

import scala.collection.mutable

case class GraphNode(
  num: Int,
  name: String
)

case class Graph(
  stringToNode:mutable.Map[String, GraphNode] = mutable.Map[String, GraphNode](),
  list: mutable.Buffer[GraphNode] = mutable.Buffer(),
  graph: mutable.Map[String, mutable.Set[GraphNode]] = mutable.Map[String, mutable.Set[GraphNode]]()
) {
  def addAll(from: Iterable[String], to: Iterable[String]): Unit ={
    for(s1 <- from) {
      for(s2 <- to) {
        add(s1, s2)
      }
    }

  }

  def addAllAll(from: Iterable[String], to: Iterable[String]): Unit ={
    for(s1 <- from) {
      for(s2 <- to) {
        add(s1, s2)
      }
    }

    for(s1 <- to) {
      for(s2 <- to) {
        if(!s1.equals(s1)) {
          add(s1, s2)
        }
      }
    }

  }

  // for( i < j ) get siblings for indirect traversion
  def getNondirectSiblings(n1: GraphNode): Iterable[GraphNode] = {
    graph.get(n1.name).map {_.filter(_.num < n1.num)}.getOrElse(List.empty)
  }

  def add(s1: String, s2: String): Unit = {
    val n1 = getOrCreateNode(s1)
    val n2 = getOrCreateNode(s2)

    addEdgeWhenNodeExists(n1, n2)
    addEdgeWhenNodeExists(n2, n1)
  }

  private def addEdgeWhenNodeExists(n1: GraphNode, n2: GraphNode): Unit ={
    val set = graph.get(n1.name) match {
      case Some(s) => s
      case None =>
        val r = mutable.Set[GraphNode]()
        graph.put(n1.name, r)
        r
    }

    set.add(n2)
  }

  def getOrCreateNode(s: String): GraphNode ={
    stringToNode.get(s) match {
      case Some(n) => n
      case None =>
        addNewNode(s)
    }
  }

  private def addNewNode(s: String): GraphNode = {
    val node = GraphNode(list.size +1, s)
    list += node
    stringToNode.put(s, node)
    node
  }
}