package services.tools

import java.io._

import Library.GeneralConstant
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.metadata.schema.clusterselection.ODefaultClusterSelectionStrategy
import com.tinkerpop.blueprints.impls.orient.{OrientEdgeType, OrientGraphFactory, OrientGraphNoTx, OrientVertexType}
import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import play.Logger
import services.tools.algorithm.LDAProcessing

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by Dark Son on 7/11/2017.
  */
object Preparation extends App() {
  val factory: OrientGraphFactory = new OrientGraphFactory(GeneralConstant.ORIENTDB_ACM_TREE_PATH_CITESEERX)
  val graphNoTx: OrientGraphNoTx = factory.getNoTx
  try {
    println("Creating ACM tree in Orient DB")
    saveTopicToOrientDb
    println("Generating ACM folder based on ACM tree")
    createTopicFolder // Create folder based on ACM tree
    println("Casting paper from Crawled Data folder to ACM topic model folder") // Casting crawled data into folder path
    saveCrawledDataToFolder(GeneralConstant.ORIENTDB_TRAINING_DATA_PATH_CITESEERX) // Use for CiteseerX
//    saveCrawledDataToFolderOtherSite(GeneralConstant.ORIENTDB_TRAINING_DATA_PATH) // New method
    println("Training topic")
    topicTraining
    println("DONE!")
  } catch {
    case ex: Exception => Logger.info(s"[PROBLEM][PREPARATION] $ex.")
  }

  /**
    * initiationOrientDb
    *
    * @return
    */
  def initiationOrientDb: Boolean = {
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
    documentVertex.createProperty(GeneralConstant.DOCUMENT_PATH, OType.STRING)
    // Create edge keyword-topic edge class
    graphNoTx.createEdgeType(GeneralConstant.DOCUMENT_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    graphNoTx.commit()
    true
  }

  /**
    * save Topic to OrientDB
    *
    * @return
    */
  def saveTopicToOrientDb: Boolean = {
    val stringList = Source.fromFile(GeneralConstant.ACM_URL).getLines.toList

    if (stringList.nonEmpty) {
      initiationOrientDb
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
          Logger.error("[saveTopicToOrientDb]Catch an Exception: " + ex)
          println("[saveTopicToOrientDb]Catch an Exception: " + ex)
          graphNoTx.rollback()
      }
    }
    true
  }

  def saveTopicToOrientDbRecursive(graphNoTx: OrientGraphNoTx, stringList: List[String], rootArray: Array[Vertex]): Unit = {
    if (stringList.nonEmpty) {
      var currentTopic = stringList.head
      val currentTabNum = currentTopic.count(_ == '\t')
      currentTopic = currentTopic.replace("\t", "")
      val newNode: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.TOPIC_VERTEX, Nil: _*)
      if (currentTopic.charAt(0) == ' ') {
        newNode.setProperty(GeneralConstant.TOPIC_LABEL, currentTopic.replaceFirst(" ", ""))
      } else {
        newNode.setProperty(GeneralConstant.TOPIC_LABEL, currentTopic)
      }
      // Add new node to root Array
      rootArray(currentTabNum) = newNode
      // Add edge
      graphNoTx.addEdge("class:" + GeneralConstant.TOPIC_RELATIONSHIP, rootArray(currentTabNum), rootArray(currentTabNum - 1), null)
      saveTopicToOrientDbRecursive(graphNoTx, stringList.tail, rootArray)
    }
  }

  /** DONE **/

  /**
    * Create folder corresponding to ACM topic model
    */
  def createTopicFolder: Unit = {
    graphNoTx.begin()
    val vertices = graphNoTx.getVerticesOfClass(GeneralConstant.TOPIC_VERTEX, false)
    // Get the root node
    val vertex = vertices.head
    // Path to store data in ACM folder tree
    val path = GeneralConstant.ORIENTDB_DATA_PATH_CITESEERX + vertex.getProperty(GeneralConstant.TOPIC_LABEL).toString.replaceAll(",", "_")
    val childNode = vertex.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP) // child node
    // get child list
    createTopicFolderRecursive(childNode, path)
  }

  def createTopicFolderRecursive(vertices: Iterable[Vertex], path: String): Unit = {
    if (vertices.nonEmpty) {
      val currentVertex = vertices.head
      val currentChildNode = currentVertex.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
      // New path
      val newPath = path + '\\' + currentVertex.getProperty(GeneralConstant.TOPIC_LABEL).toString
        .replace(' ', '_')
        .replace(',', '_')
        .replace("/", "-")

      FileDirectionHandler.createPath(newPath) // create folder
      currentVertex.setProperty(GeneralConstant.TOPIC_PATH, newPath) // save path to orient Db
      graphNoTx.commit()

      createTopicFolderRecursive(currentChildNode, newPath) // Recursive child node
      createTopicFolderRecursive(vertices.tail, path) // Read in same level
    }
  }

  /** Done **/

  /**
    * Cast data from Crawled folder in to ACM topic model folder
    *
    * @param path
    * @return
    */
  // With citeseerx
  def saveCrawledDataToFolder(path: String): Boolean = {
    val listFile = FileDirectionHandler.getListFile(path) // Get list file automatically
    listFile.foreach(file => {
      val fileName = file.getName.replaceAll(".txt", "")
      println("\n" + "Processing topic: " + fileName)

      val topic = graphNoTx.getVertices(GeneralConstant.TOPIC_VERTEX + '.' + GeneralConstant.TOPIC_LABEL, fileName).head

      val stringLine = Source.fromFile(file, "ISO-8859-1").getLines.toList // Get contents in file
      saveDataToFolder(stringLine.tail)

      def saveDataToFolder(stringLine: List[String]): Unit = {
        if (stringLine.nonEmpty) {
          val line = stringLine.head
          if (line.split("\\|").length == 3) {
            // id|name|content
            try {
              val documentId = line.split("\\|")(0)
              val documentName = line.split("\\|")(1)
              val content = line.split("\\|")(2)

              val fileStream = new PrintStream(new File(topic.getProperty(GeneralConstant.TOPIC_PATH).toString + "\\"
                + documentId + ".txt"))
              fileStream.println(documentName)
              fileStream.println(content)
              fileStream.close()
            } catch {
              case ex: Exception =>
                Logger.error("[saveDataToFolder]Catch an Exception: " + ex)
                println("[saveDataToFolder]Catch an Exception: " + ex)
            }
          }
          saveDataToFolder(stringLine.tail)
        }
      }
    }
    )
    true
  }

  // Other site
  def saveCrawledDataToFolderOtherSite(path: String): Boolean = {
    val listFile = FileDirectionHandler.getListFile(path) // Get list file automatically
    listFile.foreach(file => {
      val fileName = file.getName.replaceAll(".txt", "")
      println("\n" + "Processing topic: " + fileName)

      val topic = graphNoTx.getVertices(GeneralConstant.TOPIC_VERTEX + '.' + GeneralConstant.TOPIC_LABEL, fileName).head

      val stringLine = Source.fromFile(file, "ISO-8859-1").getLines.toList // Get contents in file
      val fileStream = new PrintStream(new File(topic.getProperty(GeneralConstant.TOPIC_PATH).toString + "\\"
        + fileName + ".txt"))
      saveDataToFolder(stringLine)

      def saveDataToFolder(stringLine: List[String]): Unit = {
        if (stringLine.nonEmpty) {
          val lines = stringLine
          try {
            var content: String = ""
            lines.foreach(line => content += line + "\n")
            println("File: " + fileName)
            fileStream.println(fileName)
            fileStream.println(content)
            fileStream.close()
          } catch {
            case ex: Exception =>
              Logger.error("[saveDataToFolder]Catch an Exception: " + ex)
              println("[saveDataToFolder]Catch an Exception: " + ex)
          }
          saveDataToFolder(stringLine.tail)
        }
      }
    }
    )
    true
  }

  /** DONE **/

  /** Training Topic **/
  def topicTraining: Boolean = {
    val topics = graphNoTx.getVerticesOfClass(GeneralConstant.TOPIC_VERTEX, false)
    val topic = topics.head
    graphNoTx.begin()
    val result = topicTrainingRecursive(topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP))
    for (keywordMap <- result) {
      val key = keywordMap._1
      val value = keywordMap._2
      val currentKeyword: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.KEYWORD_VERTEX, Nil: _*)
      currentKeyword.setProperty(GeneralConstant.KEYWORD_LABEL, key)
      val currentEdge: Edge = graphNoTx.addEdge("class:" + GeneralConstant.KEYWORD_RELATIONSHIP,
        currentKeyword,
        topic,
        null)
      currentEdge.setProperty(GeneralConstant.KEYWORD_WEIGHT, value)
    }
    graphNoTx.commit()
    true
  }

  def topicTrainingRecursive(listTopic: Iterable[Vertex]): Map[String, Double] = {
    var result: Map[String, Double] = Map()
    try {
      for (topic <- listTopic) {
        var listTopicKeyword: Map[String, Double] = Map()
        val newPath = topic.getProperty(GeneralConstant.TOPIC_PATH).toString
        val newListTopic = topic.getVertices(Direction.IN, GeneralConstant.TOPIC_RELATIONSHIP)
        // This topic is the final leaf of this branch
        if (newListTopic.isEmpty) {
          // Check if text file is existed
          if (FileDirectionHandler.checkTextFile(newPath)) {
            // Save list document to orientDb
            val listFile = FileDirectionHandler.getListFile(newPath)
            for (file <- listFile) {
              val document: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.DOCUMENT_VERTEX, Nil: _*) // add document to orietnDb
              val fileName = Source.fromFile(file, "ISO-8859-1").getLines.toList.head // Get file name
              document.setProperty(GeneralConstant.DOCUMENT_LABEL, fileName)
              document.setProperty(GeneralConstant.DOCUMENT_PATH, file.getAbsolutePath)
              // edge from keyword to node
              graphNoTx.addEdge("class:" + GeneralConstant.DOCUMENT_RELATIONSHIP, topic, document, null) // create edge from topic to document
            }
            // LDA Processing
            val listKeyword = LDAProcessing.useLDA(newPath)

            // Get list keywords and weight
            for (keyword <- listKeyword) {
              val key = keyword.split('\t')(0) // keyword
              val value = keyword.split('\t')(1).toDouble // weight
              listTopicKeyword += (key -> value)
              result += (key -> value) // update result
            }
          }
        } else {
          val keywordMap = topicTrainingRecursive(newListTopic)
          // Loop all keyword in keyword map
          for (key <- keywordMap.keys) {
            // Check if the keyword have been existed in the final result
            listTopicKeyword += (key -> keywordMap(key)) // List keyword each topic
            if (result.contains(key)) {
              result += (key -> (result(key) + keywordMap(key))) // List keyword at final
            } else {
              result += (key -> keywordMap(key))
            }
          }
        }
        // Save to orient Db
        for (keywordMap <- listTopicKeyword) {
          val key = keywordMap._1
          val value = keywordMap._2
          val listKeywordByKey = graphNoTx.getVertices(GeneralConstant.KEYWORD_VERTEX + '.' + GeneralConstant.KEYWORD_LABEL, key)
          if (listKeywordByKey.isEmpty) {
            // add new keyword to orietnDb
            val currentKeyword: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.KEYWORD_VERTEX, Nil: _*)
            currentKeyword.setProperty(GeneralConstant.KEYWORD_LABEL, key)
            val currentEdge: Edge = graphNoTx.addEdge("class:" + GeneralConstant.KEYWORD_RELATIONSHIP,
              currentKeyword,
              topic,
              null)
            currentEdge.setProperty(GeneralConstant.KEYWORD_WEIGHT, value)
          } else {
            // edge from keyword to node
            val currentKeyword = listKeywordByKey.head
            val currentEdge: Edge = graphNoTx.addEdge("class:" + GeneralConstant.KEYWORD_RELATIONSHIP,
              currentKeyword,
              topic,
              null)
            currentEdge.setProperty(GeneralConstant.KEYWORD_WEIGHT, value)
          }
        }
      }
    } catch {
      case ex: Exception => Logger.error("[topicTrainingRecursive] Catch an Exception: " + ex)
    }
    result
  }

  /** DONE **/
}
