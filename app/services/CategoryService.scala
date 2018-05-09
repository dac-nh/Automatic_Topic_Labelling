package services

import javax.inject._

import Library.GeneralConstant
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.metadata.schema.clusterselection.ODefaultClusterSelectionStrategy
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient._
import com.tinkerpop.blueprints.{Direction, Vertex}
import play.Logger
import play.api.libs.json.{JsArray, JsValue, Json}
import services.tools.FileDirectionHandler
import services.tools.algorithm.{Euclidean, KullbackLeiblerDivergence, LDAProcessing}

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by Dark Son on 6/25/2017.
  */
trait CategoryService {
  def saveTopicToOrientDb: Boolean

  def getAllTopicLabel: JsArray

  def getListTopicsAsJsonArray: JsArray

  def getListKeywordOfTopic(id: String): JsArray

  def getListDocumentOfTopic(id: String): JsArray

  def getPaperAbstract(paperId: String): JsValue

  def runForAccurateTesing(): JsArray

  def topicLabeling(path: String): JsArray // Topic labelling

  def estimateTopicsForVector(qKeyword: List[String]): JsValue

  def getTopicKeywordDocumentByTopic(label: String): JsValue // Searching

  def getTopicAndSubTopicByLabel(label: String): JsArray
}

@Singleton
class Category extends CategoryService {
  val factory: OrientGraphFactory = new OrientGraphFactory(GeneralConstant.ORIENTDB_ACM_TREE_PATH)
  val graphNoTx: OrientGraphNoTx = factory.getNoTx
  graphNoTx.begin()

  /**
    * save Topic to OrientDB
    *
    * @return
    */
  override def saveTopicToOrientDb: Boolean = {
    val stringList = Source.fromFile(GeneralConstant.ACM_URL).getLines.toList

    if (stringList.nonEmpty) {
      // create topic vertex class
      val acmTree: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.TOPIC_VERTEX)
      acmTree.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      acmTree.createProperty(GeneralConstant.TOPIC_LABEL, OType.STRING)
      // create topic-topic edge class
      graphNoTx.createEdgeType(GeneralConstant.TOPIC_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      // create keyword vertex class
      val keywordTree: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.KEYWORD_VERTEX)
      keywordTree.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      keywordTree.createProperty(GeneralConstant.KEYWORD_LABEL, OType.STRING)
      // Create edge keyword-topic edge class
      val keywordEdge: OrientEdgeType = graphNoTx.createEdgeType(GeneralConstant.KEYWORD_RELATIONSHIP)
      keywordEdge.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      keywordEdge.createProperty(GeneralConstant.KEYWORD_WEIGHT, OType.DOUBLE)
      // Create document vertex class
      val documentVertex: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.DOCUMENT_VERTEX)
      documentVertex.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      documentVertex.createProperty(GeneralConstant.DOCUMENT_LABEL, OType.STRING)
      // Create edge keyword-topic edge class
      graphNoTx.createEdgeType(GeneralConstant.DOCUMENT_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
      graphNoTx.commit()

      try {
        graphNoTx.begin()
        // create root class and add to rootArray
        val rootArray: Array[Vertex] = new Array[Vertex](10)
        val root: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.TOPIC_VERTEX, Nil: _*)
        root.setProperty(GeneralConstant.TOPIC_LABEL, GeneralConstant.TOPIC_ROOT_LABEL)
        graphNoTx.commit()
        rootArray(0) = root
        // call function to write to orient Db
        saveTopicToOrientDbRecursive(graphNoTx, stringList, rootArray)
        graphNoTx.commit()
      } catch {
        case ex: Exception =>
          Logger.info("[saveTopicToOrientDb]Catch an Exception: " + ex)
          println("[saveTopicToOrientDb]Catch an Exception: " + ex)
          graphNoTx.rollback()
      }
    }
    true
  }

  def saveTopicToOrientDbRecursive(graphNoTx: OrientGraphNoTx, stringList: List[String], rootArray: Array[Vertex]): Boolean = {
    if (stringList.nonEmpty) {
      val currentTopic = stringList.head
      val currentTabNum = currentTopic.count(_ == '\t')
      var currentNode = ""

      if (currentTopic.charAt(0) == ' ') {
        currentNode = currentTopic.replace("\t", "").replaceFirst(" ", "")
      } else {
        currentNode = currentTopic.replace("\t", "")
      }
      val newNode: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.TOPIC_VERTEX, Nil: _*)
      newNode.setProperty(GeneralConstant.TOPIC_LABEL, currentNode)
      // Add new node to root Array
      rootArray(currentTabNum) = newNode
      // Add edge
      graphNoTx.addEdge("class:" + GeneralConstant.TOPIC_RELATIONSHIP, rootArray(currentTabNum), rootArray(currentTabNum - 1), null)
      saveTopicToOrientDbRecursive(graphNoTx, stringList.tail, rootArray)
    }
    true
  }

  /** DONE writeACMCToOrientDb **/

  /** EVENT **/
  // Get List Topic as Json
  override def getListTopicsAsJsonArray: JsArray = {
    var result: JsArray = Json.arr()
    println("[getListTopicsAsJsonArray] Processing")
    val listTopic = graphNoTx.getVerticesOfClass(GeneralConstant.TOPIC_VERTEX, false)
    val topic = listTopic.head
    // get List child
    val listChildVertex = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
    val topicAsJson: JsValue = Json.obj("id" -> topic.getId.asInstanceOf[ORecordId].getIdentity.toString,
      "label" -> topic.getProperty(GeneralConstant.TOPIC_LABEL).toString,
      "group" -> GeneralConstant.TOPIC_ROOT_LABEL,
      "child" -> getListChildTopicsAsJsonArrayRecursive(listChildVertex, topic)
    )
    result = result :+ topicAsJson
    println("[getListTopicsAsJsonArray] Finished")
    result
  }

  // Function recursive of getListTopic
  def getListChildTopicsAsJsonArrayRecursive(listTopic: Iterable[Vertex], parentTopic: Vertex): JsArray = {
    var result: JsArray = Json.arr()
    var nodeAsJsonArray = Json.arr()
    var edgeAsJsonArray = Json.arr()
    // Prepare data for list child vertices in Json Array
    for (topic <- listTopic) {
      val listChildVertices = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
      // Node Json
      val topicAsJson: JsValue = Json.obj("id" -> topic.getId.asInstanceOf[ORecordId].getIdentity.toString,
        "label" -> topic.getProperty(GeneralConstant.TOPIC_LABEL).toString,
        "group" -> parentTopic.getId.asInstanceOf[ORecordId].getClusterPosition.toString,
        "child" -> getListChildTopicsAsJsonArrayRecursive(listChildVertices, topic)
      )
      nodeAsJsonArray = nodeAsJsonArray :+ topicAsJson
      // Edge Json
      val edgeAsJson: JsValue = Json.obj("from" -> topic.getId.asInstanceOf[ORecordId].getIdentity.toString,
        "to" -> parentTopic.getId.asInstanceOf[ORecordId].getIdentity.toString,
        "arrows" -> "'to, middle'")
      edgeAsJsonArray = edgeAsJsonArray :+ edgeAsJson
    }
    result = result :+ nodeAsJsonArray
    result = result :+ edgeAsJsonArray
    result
  }


  // Get all list topic label for search
  override def getAllTopicLabel(): JsArray = {
    var result: JsArray = Json.arr()
    println("[getAllTopicLabel] Processing")
    val topics = graphNoTx.getVerticesOfClass(GeneralConstant.TOPIC_VERTEX, false)
    topics.foreach(topic => {
      result = result :+ Json.toJson(topic.getProperty(GeneralConstant.TOPIC_LABEL).toString)
    })
    println("[getAllTopicLabel] Finished")
    result
  }

  // Get List Keyword as Json
  override def getListKeywordOfTopic(id: String): JsArray = {
    var result: JsArray = Json.arr()
    val topic = graphNoTx.getVertex(id)
    val listKeywordEdge = topic.getEdges(Direction.IN, GeneralConstant.KEYWORD_RELATIONSHIP)
    var max = 0.0
    if (listKeywordEdge.nonEmpty) {
      for (keywordEdge <- listKeywordEdge) {
        val weight = keywordEdge.getProperty(GeneralConstant.KEYWORD_WEIGHT).asInstanceOf[Double]
        // Get Max value
        if (max < weight) {
          max = weight
        }
        val keywordAsJson: JsValue = Json.obj(GeneralConstant.KEYWORD_LABEL -> keywordEdge.getVertex(Direction.OUT).getProperty(GeneralConstant.KEYWORD_LABEL).toString,
          GeneralConstant.KEYWORD_WEIGHT -> weight.toString
        )
        result = result :+ keywordAsJson
      }
    }
    result = result :+ Json.obj("max" -> max)
    result
  }

  // Get List Paper as Json
  override def getListDocumentOfTopic(id: String): JsArray = {
    var result: JsArray = Json.arr()
    val topic = graphNoTx.getVertex(id)
    val listDocument = getListDocumentOfTopicRecursive(topic)
    var count = 0
    for (document <- listDocument) {
      if (count >= 100) {
        return result
      }
      val id = document.split('\t')(0) // keyword
      val content = document.split('\t')(1).toString // weight
      val documentAsJson: JsValue = Json.obj("id" -> id, GeneralConstant.DOCUMENT_LABEL -> content)
      result = result :+ documentAsJson
      count += 1
    }
    result
  }

  def getListDocumentOfTopicRecursive(topic: Vertex): ListBuffer[String] = {
    var result: ListBuffer[String] = ListBuffer()
    val listTopic = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
    if (listTopic.nonEmpty) {
      for (child <- listTopic) {
        result = result ++ getListDocumentOfTopicRecursive(child)
      }
    } else {
      val listDocument = topic.getVertices(Direction.OUT, GeneralConstant.DOCUMENT_RELATIONSHIP)
      for (document <- listDocument) {
        result += document.getId + "\t" + document.getProperty(GeneralConstant.DOCUMENT_LABEL).toString
      }
    }
    result
  }

  /** DONE **/

  override def runForAccurateTesing(): JsArray = {
    val path = "C:\\Projects\\13520173_NguyenHuuDac_FinalThesis\\data\\keyword_from_phupham_testcase\\machine_learning" // Path of folder to run data testing
    val listFile = FileDirectionHandler.getListFile(path) // Get list file automatically
    listFile.foreach(file => {

      // Write document name to a file
      //      val fileStream = new FileWriter(GeneralConstant.TESTING_DATA_PATH, true) // write to file
      //      fileStream.append("\n" + file.getName + "\n")
      //      fileStream.close()

      var mainTopic = topicLabeling(file.getPath)
    })
    topicLabeling("")
  }

  /**
    * Topic Labeling
    */
  override def topicLabeling(path: String): JsArray = {
    var result: JsArray = Json.arr()
    var documentKeywordAsJson: JsArray = Json.arr()
    // Get list keyword of Q
    val qKeyword = LDAProcessing.useLDAForTopicLabeling(path) // LDA processing
    var max = 0.0 // Highest percentage of keyword
    //    val fileStream = new FileWriter(GeneralConstant.TESTING_DATA_PATH, true) // write to file
    for (keyword <- qKeyword) {
      val keywordName = keyword.split('\t')(0)
      val keywordWeight = keyword.split('\t')(1).toDouble
      // Convert to Json
      if (max < keywordWeight) {
        max = keywordWeight
      }
      //      fileStream.append(keywordName + ":" + keywordWeight + "\n")
      val documentAsJson: JsValue = Json.obj(GeneralConstant.KEYWORD_LABEL -> keywordName, GeneralConstant.KEYWORD_WEIGHT -> keywordWeight)
      documentKeywordAsJson = documentKeywordAsJson :+ documentAsJson
    }
    documentKeywordAsJson = documentKeywordAsJson :+ Json.obj("max" -> max)
    //    fileStream.close()
    val topicsAsJson = estimateTopicsForVector(qKeyword) // Estimate topics of vector

    result = result :+ topicsAsJson // Topic name
    result = result :+ documentKeywordAsJson // List Keywords
    result
  }

  /**
    * estimateTopicsForVector
    *
    * @param qKeyword
    * @return
    */
  override def estimateTopicsForVector(qKeyword: List[String]): JsValue = {
    var result: JsValue = Json.arr()
    var topicMapEuclidean: Map[String, Double] = Map() // For Euclidean algorithm
    var topicMapKL: Map[String, Double] = Map() // For KL Divergence algorithm
    var keywordsMap: Map[String, Double] = Map()
    var qMapStandard = keywordsMap // Document's keyword map

    // Calculate qMapStandard and KeywordsMap
    for (keyword <- qKeyword) {
      val keywordName = keyword.split('\t')(0)
      val keywordWeight = keyword.split('\t')(1).toDouble

      keywordsMap += (keywordName -> 0.0)
      qMapStandard += (keywordName -> keywordWeight)
    }
    // Get list of P and apply KL Divergence
    val root = graphNoTx.getVerticesOfClass(GeneralConstant.TOPIC_VERTEX, false).head
    getLeafKeyword(root)

    def getLeafKeyword(topic: Vertex): Unit = {
      val topics = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
      if (topics.isEmpty) {
        // qMap and pMap for KL Divergence
        var pMap = keywordsMap
        var qMap = qMapStandard

        val pKeyword = topic.getEdges(Direction.IN, GeneralConstant.KEYWORD_RELATIONSHIP)
        for (keyword <- pKeyword) {
          pMap += (keyword.getVertex(Direction.OUT).getProperty(GeneralConstant.KEYWORD_LABEL).toString -> keyword.getProperty(GeneralConstant.KEYWORD_WEIGHT))

          // Update qMap
          if (!qMap.contains(keyword.getVertex(Direction.OUT).getProperty(GeneralConstant.KEYWORD_LABEL).toString)) {
            qMap += (keyword.getVertex(Direction.OUT).getProperty(GeneralConstant.KEYWORD_LABEL).toString -> 0)
          }
        }
        if (qMap.size != (keywordsMap.size + pKeyword.size)) {
          val resultDistanceEuclidean = Euclidean.run(pMap, qMap) // calculate the distance between paper and topic model
          val resultDistanceKL = KullbackLeiblerDivergence.run(pMap, qMap) // calculate the distance between paper and topic model

          // TopicMap
          topicMapEuclidean += (topic.getProperty(GeneralConstant.TOPIC_LABEL).toString -> resultDistanceEuclidean)
          topicMapKL += (topic.getProperty(GeneralConstant.TOPIC_LABEL).toString -> resultDistanceKL)
        }
      } else {
        for (child <- topics) {
          getLeafKeyword(child)
        }
      }
    }

    //    var fileStream = new FileWriter(GeneralConstant.TESTING_DATA_PATH, true)

    //    println("\nEuclidean distance:")
    // Topic List Euclidean
    //    fileStream.write("\nEuclidean")
    val topicListEuclidean = ListMap(topicMapEuclidean.toSeq.sortWith(_._2 < _._2): _*).take(11)
    val highestDistanceTopicEuclidean: Double = topicListEuclidean.takeRight(1).head._2 // Sum all probabilities of topics
    var topicAsJsonEuclidean: JsArray = Json.arr()
    topicListEuclidean.dropRight(1).foreach(x => {

      //      fileStream.write("\n" + x._1 + ":" + x._2) // Write to file

      topicAsJsonEuclidean = topicAsJsonEuclidean :+
        Json.obj(GeneralConstant.KEYWORD_LABEL -> x._1,
          GeneralConstant.KEYWORD_WEIGHT -> x._2)
    })
    //    fileStream.close()
    //    fileStream = new FileWriter(GeneralConstant.TESTING_DATA_PATH, true)

    //    println("\nK-L Divergence:")
    // Topic list K-L Divergence
    //    fileStream.write("\nKL")
    val topicListKL = ListMap(topicMapKL.toSeq.sortWith(_._2 < _._2): _*).take(11)
    val highestDistanceTopicKL: Double = topicListKL.takeRight(1).head._2 // Sum all probabilities of topics
    var topicAsJsonKL: JsArray = Json.arr()
    topicListKL.dropRight(1).foreach(x => {

      //      fileStream.write("\n" + x._1 + ":" + x._2) // Write to file

      topicAsJsonKL = topicAsJsonKL :+
        Json.obj(GeneralConstant.KEYWORD_LABEL -> x._1,
          GeneralConstant.KEYWORD_WEIGHT -> x._2)
    })
    //    fileStream.close()
    result = Json.obj("euclidean" -> Json.obj("highest" -> highestDistanceTopicEuclidean, "listTopic" -> topicAsJsonEuclidean),
      "kl" -> Json.obj("highest" -> highestDistanceTopicKL, "listTopic" -> topicAsJsonKL))
    result
  }

  /** DONE **/

  /** Get Paper's Abstract **/
  override def getPaperAbstract(paperId: String): JsValue = {
    val paper = graphNoTx.getVertex(paperId)

    val contents = Source.fromFile(paper.getProperty(GeneralConstant.DOCUMENT_PATH).toString).getLines.toList
    var content: String = ""
    contents.tail.foreach(line => content += line + "\n")

    val result: JsValue = Json.obj("content" -> content)
    result
  }

  /** DONE **/

  /** Searching **/
  override def getTopicAndSubTopicByLabel(label: String): JsArray = {
    var result: JsArray = Json.arr()
    val topic = graphNoTx.getVertices(GeneralConstant.TOPIC_LABEL, label).head
    // get List child
    val listChildVertex = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
    val topicAsJson: JsValue = Json.obj("id" -> topic.getId.asInstanceOf[ORecordId].getIdentity.toString,
      "label" -> topic.getProperty(GeneralConstant.TOPIC_LABEL).toString,
      "group" -> GeneralConstant.TOPIC_ROOT_LABEL,
      "child" -> getListChildTopicsAsJsonArrayRecursive(listChildVertex, topic)
    )
    result = result :+ topicAsJson
    result
  }

  override def getTopicKeywordDocumentByTopic(label: String): JsValue = {
    var result: JsArray = Json.arr()
    var nodeAsJsonArray = Json.arr()
    var edgeAsJsonArray = Json.arr()

    val results: OrientDynaElementIterable = graphNoTx
      .command(new OCommandSQL("SELECT * FROM " + GeneralConstant.TOPIC_VERTEX + " WHERE " + GeneralConstant.TOPIC_LABEL + " LIKE '%" + label + "%'"))
      .execute()
    results.foreach(v => {

      val topic = v.asInstanceOf[OrientVertex]
      val topicId = topic.getId.toString
      val topicLabel = topic.getProperty(GeneralConstant.TOPIC_LABEL)
      // add topic to Json
      val topicAsJson: JsValue = Json.obj("id" -> topicId,
        "label" -> topicLabel,
        "group" -> GeneralConstant.TOPIC_VERTEX
      )
      nodeAsJsonArray = nodeAsJsonArray :+ topicAsJson // Add to node

      // Get list upper level of topic
      val listUpperTopic = topic.getVertices(Direction.OUT, GeneralConstant.TOPIC_RELATIONSHIP)
      listUpperTopic.foreach(parent => {
        val edgeAsJson: JsValue = Json.obj("from" -> topicId,
          "to" -> parent.getId.toString,
          "arrows" -> "'to, middle'")
        edgeAsJsonArray = edgeAsJsonArray :+ edgeAsJson // add to edge
      })

      // Get list documents of topic
      val listDocuments = getListDocumentOfTopicRecursive(topic)
      listDocuments.foreach(document => {
        val documentId = document.split('\t')(0) // keyword
        val content = document.split('\t')(1).toString // weight
        val documentAsJson: JsValue = Json.obj("id" -> documentId, "label" -> content, "group" -> GeneralConstant.DOCUMENT_VERTEX)
        nodeAsJsonArray = nodeAsJsonArray :+ documentAsJson

        val edgeAsJson: JsValue = Json.obj("from" -> topicId,
          "to" -> documentId,
          "arrows" -> "'to, middle'")
        edgeAsJsonArray = edgeAsJsonArray :+ edgeAsJson
      })

      // Get list keywords of topic
      val topicKeywords = topic.getEdges(Direction.IN, GeneralConstant.KEYWORD_RELATIONSHIP)
      //      var keywordsMap: Map[String, String] = Map()
      for (topicKeyword <- topicKeywords) {
        val keyword = topicKeyword.getVertex(Direction.OUT)
        val keywordAsJson: JsValue = Json.obj("id" -> keyword.getId.toString,
          "label" -> keyword.getProperty(GeneralConstant.KEYWORD_LABEL).toString,
          "group" -> GeneralConstant.KEYWORD_VERTEX)
        nodeAsJsonArray = nodeAsJsonArray :+ keywordAsJson
        //      keywordsMap += (keyword.getId.toString + '\t' + keyword.getProperty(GeneralConstant.KEYWORD_LABEL).toString -> topicKeyword.getProperty(GeneralConstant.KEYWORD_WEIGHT))
        val edgeAsJson: JsValue = Json.obj("from" -> keyword.getId.toString,
          "to" -> topicId,
          "arrows" -> "'to, middle'",
          "label" -> topicKeyword.getProperty(GeneralConstant.KEYWORD_WEIGHT).toString)
        edgeAsJsonArray = edgeAsJsonArray :+ edgeAsJson
      }
    })
    result = result :+ nodeAsJsonArray
    result = result :+ edgeAsJsonArray
    result
  }
}