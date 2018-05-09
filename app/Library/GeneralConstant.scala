package Library

/**
  * Created by Dark Son on 6/15/2017.
  */
object GeneralConstant {
  val TESTING_DATA_PATH: String = "target\\data\\test_result.txt" // Default store to target
  /** * OrientDB ***/
  // new version
  val ORIENTDB_ACM_TREE_PATH: String = "plocal:target\\database\\orientdb_acmtree" // Default store to main orient db database
  val ORIENTDB_DATA_PATH: String = "target\\data-orientdb-2012\\" // save content of document in target
  val ORIENTDB_TRAINING_DATA_PATH: String = getClass.getResource("../data/training_data_2012_new").getPath // Default storage of training data
  // CiteSeerX
  val ORIENTDB_ACM_TREE_PATH_CITESEERX: String = "plocal:target\\database\\orientdb_acmtree_citeseerx"
  val ORIENTDB_DATA_PATH_CITESEERX: String = "target\\data-orientdb-2012-citeseerx\\"
  val ORIENTDB_TRAINING_DATA_PATH_CITESEERX: String = getClass.getResource("../data/training_data_2012_old_citeseerx").getPath

  val ORIENTDB_TESTING_PATH: String = "plocal:target\\database\\orientdb_testing" // Default store to test with phupham project

  val ACM_URL: String = getClass.getResource("../AcmTree.txt").getFile

  val TOPIC_ROOT_LABEL: String = "ACM Tree"
  val TOPIC_VERTEX: String = "topic"
  val TOPIC_LABEL: String = "label"
  val TOPIC_PATH: String = "path"
  val TOPIC_RELATIONSHIP: String = "branch"

  val KEYWORD_VERTEX: String = "keyword"
  val KEYWORD_LABEL: String = "label"
  val KEYWORD_WEIGHT: String = "weight"
  val KEYWORD_RELATIONSHIP: String = "belong"

  val DOCUMENT_VERTEX: String = "document"
  val DOCUMENT_LABEL: String = "label"
  val DOCUMENT_PATH: String = "path"
  val DOCUMENT_RELATIONSHIP: String = "contain"
  /** * END OrientDB ***/
  /** Neo4j **/
  val NEO4J_URL: String = "bolt://localhost:7687"
  val NEO4J_USERNAME: String = "neo4j"
  val NEO4J_PASSWORD: String = "123456"
  val TOPIC_NODE: String = "Topic"
  val TOPIC_EDGE: String = "Branch"
  val TOPIC_KEYWORD_EDGE: String = "Keyword"
  val KEYWORD_NODE: String = "Has"
  val TOPIC_EXCEL_FILE: String = getClass.getResource("../data/data_acm_1998/Categories.xlsx").getPath
  val KEYWORD_EXCEL_FILE: String = getClass.getResource("../data/data_acm_1998/tonghop.xlsx").getPath
  val ACM_1998_PATH: String = getClass.getResource("../data/data_acm_1998/ACM1998.txt").getPath
  val ACM_1998_ROOT_NAME: String = "ACM Tree 1998"
  val ACM_1998_ROOT_ID: String = "0"

  /** END Neo4j **/

  val STOPWORD_PATH: String = getClass.getResource("../stopWord.txt").getFile
}
