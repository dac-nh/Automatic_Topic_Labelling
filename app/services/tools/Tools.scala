package services.tools


import Library.GeneralConstant
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.impls.orient.{OrientGraphFactory, OrientGraphNoTx}
import org.neo4j.driver.v1.Values.parameters
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase}

import scala.collection.JavaConversions._

object Tools extends App() {
  // OrientDb
  val factory: OrientGraphFactory = new OrientGraphFactory(GeneralConstant.ORIENTDB_ACM_TREE_PATH)
  val graphNoTx: OrientGraphNoTx = factory.getNoTx
  // Neo4j Initiation
  val driver = GraphDatabase.driver(GeneralConstant.NEO4J_URL,
    AuthTokens.basic(GeneralConstant.NEO4J_USERNAME, GeneralConstant.NEO4J_PASSWORD))
  val session = driver.session
  importFromOrientDbToNeo4j()

  def importFromOrientDbToNeo4j(): Unit = {
    // Get all keyword vertices
    val keywords = graphNoTx.getVerticesOfClass(GeneralConstant.KEYWORD_VERTEX, false)
    var id: Int = 0
    keywords.foreach(keyword => {
      val keywordLabel = keyword.getProperty(GeneralConstant.KEYWORD_LABEL).toString
      // Save keyword to Neo4j
      session.run("CREATE (keyword:Keyword {label: {label}, id: {id}})",
        parameters("label", keywordLabel, "id", id.toString))

      val edges = keyword.getEdges(Direction.OUT) // get all edge of keyword
      // Loop all edges of keyword
      println(keywordLabel )
      edges.foreach(edge => {
        val keywordWeight = edge.getProperty(GeneralConstant.KEYWORD_WEIGHT).toString
        val topicId = edge.getVertex(Direction.IN).getId.toString.replaceAll("#17:", "")
        session.run("MATCH (topic:Topic), (keyword:Keyword) WHERE topic.id = {topicId} AND keyword.id = {keywordId}" +
          "CREATE (topic)-[:Has {weight: {keywordWeight}}]->(keyword)",
          parameters("topicId", topicId, "keywordId", id.toString, "keywordWeight", keywordWeight)
        )
      })
      id += 1 // update keyword id
    })
    // Get all edge and other side node of edge
    // save to neo4j
  }
}
