package services.tools

import Library.GeneralConstant
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}
import org.neo4j.driver.v1.Values.parameters
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase}

import scala.io.Source

object PreparationNeo4j extends App() {
  // Spark Initiation
  System.setProperty("hadoop.home.dir", "C:\\hadoop") // Set Hadoop path
  val conf = new SparkConf().setAppName(s"TopicModel").setMaster("local[*]").set("spark.executor.memory", "2g")
  val spark = SparkSession.builder().config(conf).getOrCreate()
  val sc = spark.sparkContext
  val sqlContext = new SQLContext(sc)

  // Neo4j Initiation
  val driver = GraphDatabase.driver(GeneralConstant.NEO4J_URL,
    AuthTokens.basic(GeneralConstant.NEO4J_USERNAME, GeneralConstant.NEO4J_PASSWORD))
  val session = driver.session

  run() // Execute

  // Cast from Excel to Neo4j
  def run(): Unit = {
    if (topicProcessing()) {
      println("Saving topics to Neo4j successfully")
      saveTopicAndKeywordToNeo4j()
      println("Preparation is successful")
    } else {
      println("Saving topics to Neo4j failed")
    }
    session.run("MATCH (rootTopic:A) DELETE rootTopic")
    session.close()
    driver.close()
  }

  // Read topic from excel
  def topicProcessing(): Boolean = {

    try {
      // Read excel file
      val data = readExcel(GeneralConstant.TOPIC_EXCEL_FILE)
      val dataArray = data.collect() // Get data from excel
      // Topic
      println("Have " + dataArray.length + " topics")
      for (temp <- dataArray) {
        session.run("CREATE (topic:A {label: {label}, id: {id}})",
          parameters("label", temp.getValuesMap(Seq("Name"))("Name"), "id", temp.getValuesMap(Seq("ID CAT"))("ID CAT").asInstanceOf[Double].toInt.toString))
      }
      true
    }
    catch {
      case ex: Exception => {
        println("[topicProcessing] Meet error: " + ex.getMessage)
        false
      }
    }

  }

  // Read Keyword from excel
  def keywordProcessing(): Unit = {
    // Read excel file
    val data = readExcel(GeneralConstant.KEYWORD_EXCEL_FILE)
    val dataArray = data.collect() // Get data from excel

    // Keyword
    println("Have " + dataArray.length + " keywords")
    for (temp <- dataArray) {
      val keyword = temp.getValuesMap(Seq("STT", "IDCAT", "NAME"))
      val keywordId = keyword("STT").asInstanceOf[Double].toInt.toString
      val topicId = keyword("IDCAT").asInstanceOf[Double].toInt.toString
      val keywordName = keyword("NAME").asInstanceOf[String].toString

      // Check that if keyword is existed
      val keywordInNeo4j = session.run("MATCH (keyword: Keyword) WHERE keyword.label = {keyword} RETURN keyword.id as id",
        parameters("keyword", keywordName)
      )
      if (keywordInNeo4j.hasNext) {
        session.run("MATCH (topic:Topic), (keyword:Keyword) WHERE topic.id = {topicId} AND keyword.id = {keywordId}" +
          "CREATE (topic)-[:Has {weight: \"0.333\"}]->(keyword)",
          parameters("topicId", topicId, "keywordId", keywordInNeo4j.next().get("id").asString())
        )
      } else {
        session.run("MATCH (topic:Topic) WHERE topic.id = {topicId}" +
          "CREATE (keyword:Keyword {label: {label}, id: {id}})," +
          "(topic)-[:Has {weight: \"0.333\"}]->(keyword)",
          parameters("topicId", topicId, "label", keywordName, "id", keywordId)
        )
      }
    }
  }

  // Save topic to Neo4j
  def saveTopicAndKeywordToNeo4j(): Unit = {
    val stringList = Source.fromFile(GeneralConstant.ACM_1998_PATH).getLines.toList
    val rootArrayId: Array[Int] = new Array[Int](5)
    rootArrayId(0) = 0
    session.run("CREATE (currentTopic:Topic {label: {label}, id: {id}})",
      parameters("label", GeneralConstant.ACM_1998_ROOT_NAME, "id", GeneralConstant.ACM_1998_ROOT_ID))
    val listNode = session.run("MATCH (topic:A) RETURN topic.label as label, topic.id as id")
    var listNodeMap: Map[String, Int] = Map()
    // Cast ListNode to ListNodeMap
    while (listNode.hasNext) {
      val node = listNode.next()
      listNodeMap += (node.get("label").asString() -> node.get("id").asString().toInt)
    }
    if (saveTopicToNeo4jRecursive(stringList, rootArrayId, listNodeMap)) {
      keywordProcessing()
      println("Saving keywords to Neo4j successfully")
    }

    def saveTopicToNeo4jRecursive(stringList: List[String],
                                  rootArrayId: Array[Int],
                                  listNodeMap: Map[String, Int]): Boolean = {
      try {
        if (stringList.nonEmpty) {
          val currentTopic = stringList.head
          val currentLevel = currentTopic.count(_ == '\t') + 1
          val currentNode = currentTopic.replace("\t", "")

          // Get node Id
          rootArrayId(currentLevel) = listNodeMap(currentNode)
          // Add edge
          session.run("MATCH (rootTopic:Topic) WHERE rootTopic.id = {rootTopicId}" +
            "CREATE (currentTopic:Topic {label: {label}, id: {id}})," +
            "(rootTopic)-[:Branch]->(currentTopic)",
            parameters("rootTopicId", rootArrayId(currentLevel - 1).toString,
              "label", currentNode,
              "id", rootArrayId(currentLevel).toString))
          saveTopicToNeo4jRecursive(stringList.tail, rootArrayId, listNodeMap)
        }
        true
      } catch {
        case ex: Exception => {
          println("[saveTopicToNeo4jRecursive] Meet problem: " + ex)
          false
        }
      }
    }
  }

  def readExcel(file: String): DataFrame = sqlContext.read
    .format("com.crealytics.spark.excel")
    .option("location", file)
    .option("useHeader", "true")
    .option("treatEmptyValuesAsNulls", "true")
    .option("inferSchema", "true")
    .option("addColorColumns", "False")
    .load()
}
