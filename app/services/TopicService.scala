package services

import javax.inject._

import Library.GeneralConstant
import org.neo4j.driver.v1.Values.parameters
import org.neo4j.driver.v1.{AuthTokens, Driver, GraphDatabase, Session}
import play.api.libs.json.{JsArray, JsValue, Json}
import services.tools.algorithm.{KullbackLeiblerDivergence, LDAProcessing}

import scala.collection.immutable.ListMap

trait TopicService {
  def getTopicListNeo4j: JsArray

  def getListKeyword(label: String): JsArray

  def topicLabelingForEachDocument(path: String, maxLevel: Int): JsArray
}

@Singleton
class Topic extends TopicService {
  // Neo4j Initiation
  val driver: Driver = GraphDatabase.driver(GeneralConstant.NEO4J_URL,
    AuthTokens.basic(GeneralConstant.NEO4J_USERNAME, GeneralConstant.NEO4J_PASSWORD))
  val session: Session = driver.session

  // Get Topic list from neo4j
  override def getTopicListNeo4j: JsArray = {
    var result: JsArray = Json.arr()
    var nodeAsJsonArray = Json.arr()
    var edgeAsJsonArray = Json.arr()

    // Edge & Node
    // Root Node
    val listRelationshipTopic = session.run("MATCH (topicOut)-[:Branch]->(topicIn) RETURN topicOut, topicIn")
    //    val topicAsJson: JsValue = Json.obj("id" -> GeneralConstant.ACM_1998_ROOT_ID,
    //      "label" -> GeneralConstant.ACM_1998_ROOT_NAME,
    //      "group" -> GeneralConstant.ACM_1998_ROOT_NAME
    //    )
    //    nodeAsJsonArray = nodeAsJsonArray :+ topicAsJson

    while (listRelationshipTopic.hasNext) {
      val edge = listRelationshipTopic.next()
      val parentTopicId = edge.get("topicOut").get("id").asString()
      val currentTopicId = edge.get("topicIn").get("id").asString()
      val currentTopicLabel = edge.get("topicIn").get("label").asString()

      // Node
      val topicAsJson: JsValue = Json.obj("id" -> currentTopicId,
        "label" -> currentTopicLabel,
        "group" -> parentTopicId
      )
      nodeAsJsonArray = nodeAsJsonArray :+ topicAsJson

      // Edge
      val edgeAsJson: JsValue = Json.obj("from" -> parentTopicId,
        "to" -> currentTopicId,
        "label" -> "Branch",
        "arrows" -> "'to, middle'")
      edgeAsJsonArray = edgeAsJsonArray :+ edgeAsJson
    }
    result = result :+ nodeAsJsonArray
    result = result :+ edgeAsJsonArray
    result
  }

  // Get List Keyword of Topic as Json for web
  override def getListKeyword(label: String): JsArray = {
    var result: JsArray = Json.arr()
    val keywordMap = ListMap(getListKeywordOfTopic(label).toSeq.sortWith(_._2 > _._2): _*)
    var count: Int = 0
    val max = keywordMap.head._2
    for (key <- keywordMap) {
      val keywordAsJson: JsValue = Json.obj("label" -> key._1,
        "weight" -> key._2.toString)
      result = result :+ keywordAsJson
      count += 1 // Count Keyword Number
      if (count == 20) {
        result = result :+ Json.obj("max" -> max)
        return result
      }
    }
    result = result :+ Json.obj("max" -> max)
    result
  }

  // Topic labelling for each document as Json
  override def topicLabelingForEachDocument(path: String, maxLevel: Int): JsArray = {
    var result: JsArray = Json.arr()
    var level: Int = 1 // level of topic
    var documentKeywordAsJson: JsArray = Json.arr() // List keyword of this document
    var globalKeywordsMap: Map[String, Double] = Map() // Global Keyword Map

    // List keyword result of document - qMap
    var standardQMap = globalKeywordsMap // Document's keyword map
    val qKeywordAsList = LDAProcessing.useLDAForTopicLabeling(path)
    var max = 0.0 // Highest percentage of keyword
    for (keyword <- qKeywordAsList) {
      val keywordName = keyword.split('\t')(0)
      val keywordWeight = keyword.split('\t')(1).toDouble

      globalKeywordsMap += (keywordName -> 0.0)
      standardQMap += (keywordName -> keywordWeight)
      // Convert to Json
      if (max < keywordWeight) {
        max = keywordWeight
      }
      val documentAsJson: JsValue = Json.obj(GeneralConstant.KEYWORD_LABEL -> keywordName, GeneralConstant.KEYWORD_WEIGHT -> keywordWeight)
      documentKeywordAsJson = documentKeywordAsJson :+ documentAsJson
    }
    documentKeywordAsJson = documentKeywordAsJson :+ Json.obj("max" -> max)


    // topic keyword list - pMap and execute
    def execute(label: String): String = {
      var result: String = ""

      // List topic
      var topicMap: Map[String, Double] = Map() // Topics of this document
      val topicList = session.run("MATCH (topicOut)-[:Branch]->(topicIn) WHERE topicOut.label = {topicLabel} RETURN topicIn",
        parameters("topicLabel", label))

      while (topicList.hasNext) {
        var keywordsMap = globalKeywordsMap // Each level keywords Map
        val topic = topicList.next()
        val currentTopicLabel = topic.get("topicIn").get("label").asString()

        val pKeyword = getListKeywordOfTopic(currentTopicLabel) // get keyword of topic
        val pMap = pKeyword ++ keywordsMap // synchronize keywordsMap and keywords of topic
        pKeyword.foreach { case (key, _) => keywordsMap += (key -> 0.0) } // Add topic' keyword to keywordMap
        val qMap = keywordsMap ++ standardQMap // update qMap

        val resultKlDivergence = KullbackLeiblerDivergence.run(pMap, qMap)
        // Add to topic map
        if (resultKlDivergence != 0) {
          topicMap += (currentTopicLabel -> resultKlDivergence)
        }
      }

      if (topicMap.nonEmpty) {
        topicMap = ListMap(topicMap.toSeq.sortWith(_._2 < _._2): _*) // Sort topicMap
        //      val minDistance: Double = topicMap.head._2
        //    topicMap.foreach { case (label, distance) => if (distance == minDistance) {
        //    }
        //    }
        result = topicMap.head._1
        if (level != maxLevel) {
          level += 1 // update level
          result += " -> " + execute(topicMap.head._1)
        }
      }

      result
    }

    val topicsOfDocument: String = execute(GeneralConstant.ACM_1998_ROOT_NAME)
    result = result :+ Json.obj("topic" -> topicsOfDocument)
    result = result :+ documentKeywordAsJson
    result
  }

  // Get List Keyword of Topic
  def getListKeywordOfTopic(label: String): Map[String, Double] = {
    var result: Map[String, Double] = Map()
    // Get keyword list and add to map
    val keywordList = session.run("MATCH (topic:Topic)-[relationship:Has]->(keyword) WHERE topic.label = {topic} RETURN keyword.label as label, relationship.weight as weight",
      parameters("topic", label)
    )
    while (keywordList.hasNext) {
      val keyword = keywordList.next()
      val keywordLabel = keyword.get("label").asString()
      val keywordWeight = keyword.get("weight").asString().toDouble
      result += (keywordLabel -> keywordWeight)
    }
    // List keyword of Child topic
    val topicList = session.run("MATCH (root:Topic)-[:Branch]->(topic) WHERE root.label = {rootLabel} RETURN topic.label as label",
      parameters("rootLabel", label))
    // Loop all root
    while (topicList.hasNext) {
      val topic = topicList.next()
      val topicLabel = topic.get("label").asString()

      // Recursive to get list child keyword
      val keywordMap = getListKeywordOfTopic(topicLabel)
      for (key <- keywordMap.keys) {
        // Check if the keyword have been existed in the final result
        result += (key -> keywordMap(key)) // List keyword each topic
        if (result.contains(key)) {
          result += (key -> (result(key) + keywordMap(key))) // List keyword at final
        } else {
          result += (key -> keywordMap(key))
        }
      }
    }
    result
  }
}
