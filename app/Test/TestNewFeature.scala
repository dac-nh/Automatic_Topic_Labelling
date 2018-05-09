package Test

import scala.util.control.Breaks._

/**
  * Created by Dark Son on 6/11/2017.
  */
object TestNewFeature extends App() {

  def hammingDistance(x: Int, y: Int): Int = {
    var result: Int = 0
    if (x != y) {
      val binaryX = String.format("%" + 32 + "s", x.toBinaryString).replace(' ', '0')
      val binaryY = String.format("%" + 32 + "s", y.toBinaryString).replace(' ', '0')
      val length = binaryX.length

      for (i <- 0 until length) {
        if (!binaryX(i).equals(binaryY(i))) {
          result += 1
        }
      }
    }
    result
  }

  def hammingWeight(n: Int) = {
    var bits = 0
    var mask = 1
    var i = 0
    while ( {i < 32
    }) {
      if ((n & mask) != 0) bits += 1
      mask <<= 1

      {
        i += 1
        i - 1
      }
    }
    bits
  }

  def twoSum(nums: Array[Int], target: Int): Array[Int] = {
    var numMap: Map[Int, Int] = Map()
    for (i <- nums.indices){
      numMap += (nums(i) -> i)
    }
    for (i <- nums.indices){
      if ( numMap.contains(target - nums(i)) && numMap(target - nums(i)) != i ){
        return Array(i, numMap(target - nums(i)))
      }
    }
    throw new Exception("No result")
  }

  longestPalindrome("babad")
  def longestPalindrome(s: String): String = {
    var result: String = ""

    // move center to left
    for (i <- 1 until s.length - 1) {
      var sameWithCenter = 0
      longestPalindromeRecursive (result, i - 1, i + 1, i)
    }
    def longestPalindromeRecursive(currString: String, left: Int, right: Int, current: Int): Unit = {
      // Case character in left position and right position are the same
      if (s(left) == s(right)){
        // in the middle of string
        if (left != 0 && right != (s.length - 1)) {
          longestPalindromeRecursive(s(left) + currString + s(right), left - 1, right + 1, current)
        } else {
          return
        }
      } else {
        if (currString.length > result.length) {
          result = currString
        }
      }
    }
    // Final result
    result
  }
  //  val factory: OrientGraphFactory = new OrientGraphFactory(GeneralConstant.ORIENTDB_ACM_TREE_PATH)
  //  val graphNoTx: OrientGraphNoTx = factory.getNoTx


  //  // Spark Initiation
  //  System.setProperty("hadoop.home.dir", "C:\\hadoop") // Set Hadoop path
  //  val conf = new SparkConf().setAppName(s"TopicModel").setMaster("local[*]").set("spark.executor.memory", "2g")
  //  val spark = SparkSession.builder().config(conf).getOrCreate()
  //  val sc = spark.sparkContext
  //  val sqlContext = new SQLContext(sc)
  //
  //  // Neo4j Initiation
  //  val driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123456"))
  //  val session = driver.session
  //
  //  preparationFromExcel()
  //
  //  // Cast from Excel to Neo4j
  //  def preparationFromExcel(): Unit = {
  //    if (topicProcessing()) {
  //      println("Saving topics to Neo4j successfully")
  //      saveTopicAndKeywordToNeo4j()
  //      println("Preparation is successful")
  //    }
  //  }
  //
  //  // Read topic from excel
  //  def topicProcessing(): Boolean = {
  //    // Read excel file
  //    val data = readExcel("E:\\Developing\\Thesis\\topics_keywords\\Categories.xlsx")
  //    val dataArray = data.collect() // Get data from excel
  //    // Topic
  //    println("Have " + dataArray.length + " topics")
  //    for (temp <- dataArray) {
  //      session.run("CREATE (topic:A {label: {label}, id: {id}})",
  //        parameters("label", temp.getValuesMap(Seq("Name"))("Name"), "id", temp.getValuesMap(Seq("ID CAT"))("ID CAT").asInstanceOf[Double].toInt.toString))
  //    }
  //    true
  //  }
  //
  //  // Read Keyword from excel
  //  def keywordProcessing(): Unit = {
  //    // Read excel file
  //    val data = readExcel("E:\\Developing\\Thesis\\topics_keywords\\tonghop.xlsx")
  //    val dataArray = data.collect() // Get data from excel
  //
  //    // Keyword
  //    println("Have " + dataArray.length + " keywords")
  //    for (temp <- dataArray) {
  //      val keyword = temp.getValuesMap(Seq("IDCAT", "NAME"))
  //      val topicId = keyword("IDCAT").asInstanceOf[Double].toInt.toString
  //      val keywordName = keyword("NAME").asInstanceOf[String].toString
  //
  //      session.run("MATCH (topic:Topic) WHERE topic.id = {id}" +
  //        "CREATE (keyword:Keyword {label: {label}})," +
  //        "(topic)-[:Has]->(keyword)",
  //        parameters("id", topicId, "label", keywordName))
  //    }
  //  }
  //
  //
  //  //  readXLSXFile()
  //  //  @throws[IOException]
  //  //  def readXLSXFile(): Unit = {
  //  //    val ExcelFileToRead = new FileInputStream("E:\\\\Developing\\\\Thesis\\\\topics_keywords\\\\tonghop.xlsx")
  //  //    val wb = new XSSFWorkbook(ExcelFileToRead)
  //  //    val test = new XSSFWorkbook
  //  //    val sheet = wb.getSheetAt(0)
  //  //    val rows = sheet.rowIterator
  //  //    while (rows.hasNext) {
  //  //      val row = rows.next.asInstanceOf[XSSFRow]
  //  //      val cells = row.cellIterator
  //  //      while (cells.hasNext) {
  //  //        val cell = cells.next.asInstanceOf[XSSFCell]
  //  //        if (cell.getCellType == Cell.CELL_TYPE_STRING) System.out.print(cell.getStringCellValue + " ")
  //  //        else if (cell.getCellType == Cell.CELL_TYPE_NUMERIC) System.out.print(cell.getNumericCellValue + " ")
  //  //        else {
  //  //        }
  //  //      }
  //  //      System.out.println()
  //  //    }
  //  //  }
  //  //
  //  //  @throws[IOException]
  //  //  def writeXLSXFile(): Unit = {
  //  //    val excelFileName = "C:/Test.xlsx"
  //  //    val sheetName = "Sheet1"
  //  //    val wb = new XSSFWorkbook
  //  //    val sheet = wb.createSheet(sheetName)
  //  //    var r = 0
  //  //    while (r < 5) {
  //  //      val row = sheet.createRow(r)
  //  //      var c = 0
  //  //      while (c < 5) {
  //  //        val cell = row.createCell(c)
  //  //        cell.setCellValue("Cell " + r + " " + c)
  //  //
  //  //        {
  //  //          c += 1
  //  //          c - 1
  //  //        }
  //  //      }
  //  //
  //  //      {
  //  //        r += 1
  //  //        r - 1
  //  //      }
  //  //    }
  //  //    val fileOut = new FileOutputStream(excelFileName)
  //  //    wb.write(fileOut)
  //  //    fileOut.flush()
  //  //    fileOut.close()
  //  //  }
  //
  //  def readExcel(file: String): DataFrame = sqlContext.read
  //    .format("com.crealytics.spark.excel")
  //    .option("location", file)
  //    .option("useHeader", "true")
  //    .option("treatEmptyValuesAsNulls", "true")
  //    .option("inferSchema", "true")
  //    .option("addColorColumns", "False")
  //    .load()
  //
  //  // Save topic to Neo4j
  //  def saveTopicAndKeywordToNeo4j(): Unit = {
  //    val stringList = Source.fromFile("E:\\Developing\\Thesis\\ACM1998.txt").getLines.toList
  //    val rootArrayId: Array[Int] = new Array[Int](5)
  //    rootArrayId(0) = 0
  //    session.run("CREATE (currentTopic:Topic {label: {label}, id: {id}})",
  //      parameters("label", "ACM Tree", "id", "0"))
  //    val listNode = session.run("MATCH (topic:A) RETURN topic.label as label, topic.id as id")
  //    var listNodeMap: Map[String, Int] = Map()
  //    // Cast ListNode to ListNodeMap
  //    while (listNode.hasNext) {
  //      val node = listNode.next()
  //      listNodeMap += (node.get("label").asString() -> node.get("id").asString().toInt)
  //    }
  //    saveTopicToNeo4jRecursive(stringList, rootArrayId, listNodeMap)
  //    keywordProcessing()
  //    println("Saving keywords to Neo4j successfully")
  //  }
  //
  //  def saveTopicToNeo4jRecursive(stringList: List[String],
  //                                rootArrayId: Array[Int],
  //                                listNodeMap: Map[String, Int]): Boolean = {
  //    if (stringList.nonEmpty) {
  //      val currentTopic = stringList.head
  //      val currentTabNum = currentTopic.count(_ == '\t') + 1
  //      val currentNode = currentTopic.replace("\t", "")
  //      // Get node Id
  //      rootArrayId(currentTabNum) = listNodeMap(currentNode)
  //      // Add edge
  //      session.run("MATCH (rootTopic:Topic) WHERE rootTopic.id = {rootTopicId}" +
  //        "CREATE (currentTopic:Topic {label: {label}, id: {id}})," +
  //        "(rootTopic)-[:branch]->(currentTopic)",
  //        parameters("rootTopicId", rootArrayId(currentTabNum - 1).toString,
  //          "label", currentNode,
  //          "id", rootArrayId(currentTabNum).toString))
  //      saveTopicToNeo4jRecursive(stringList.tail, rootArrayId, listNodeMap)
  //    }
  //    true
  //  }
  //
  //  driver.close()
}
