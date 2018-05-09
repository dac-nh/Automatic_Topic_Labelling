package services.tools

import Library.GeneralConstant
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.metadata.schema.clusterselection.ODefaultClusterSelectionStrategy
import com.tinkerpop.blueprints.impls.orient.{OrientEdgeType, OrientGraphFactory, OrientGraphNoTx, OrientVertexType}
import com.tinkerpop.blueprints.{Edge, Vertex}

import scala.io.Source

/**
  * USE FOR MEASURING ACCURACY ONLY
  */
object PreparationFoAccurateTesting extends App() {
  val factory: OrientGraphFactory = new OrientGraphFactory(GeneralConstant.ORIENTDB_TESTING_PATH)
  val graphNoTx: OrientGraphNoTx = factory.getNoTx
  initiationOrientDb
  /**
    * initiationOrientDb
    *
    * @return
    */
  def initiationOrientDb: Boolean = {
    // Get all list file name
    val path = "E:\\Developing\\Thesis\\lda_keyword_extraction_phupham\\lda"
    val listFile = FileDirectionHandler.getListFile(path) // Get list file automatically

    // Topic
    val testingData: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.TOPIC_VERTEX)
    testingData.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    testingData.createProperty(GeneralConstant.TOPIC_LABEL, OType.STRING)
    // create topic-topic edge class
    graphNoTx.createEdgeType(GeneralConstant.TOPIC_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    // Keyword
    val keywordTree: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.KEYWORD_VERTEX)
    keywordTree.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    keywordTree.createProperty(GeneralConstant.KEYWORD_LABEL, OType.STRING)

    // Create edge keyword-topic edge class
    val keywordEdge: OrientEdgeType = graphNoTx.createEdgeType(GeneralConstant.KEYWORD_RELATIONSHIP)
    keywordEdge.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    keywordEdge.createProperty(GeneralConstant.KEYWORD_WEIGHT, OType.DOUBLE)

    graphNoTx.commit()

    // Crate root
    val root: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.TOPIC_VERTEX, Nil: _*)
    root.setProperty(GeneralConstant.TOPIC_LABEL, GeneralConstant.TOPIC_ROOT_LABEL)
    graphNoTx.commit()

    // Read file
    listFile.foreach(file => {
      val fileName = file.getName.replaceAll(".txt", "")
      println("\n" + "Processing topic: " + fileName)

      // Create node topic
      val topic: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.TOPIC_VERTEX, Nil: _*)
      topic.setProperty(GeneralConstant.TOPIC_LABEL, fileName)
      graphNoTx.addEdge("class:" + GeneralConstant.TOPIC_RELATIONSHIP, topic, root, null) // edge from child topic to parent topic
      graphNoTx.commit()

      /// Each line in a topic file
      val stringLine = Source.fromFile(file, "ISO-8859-1").getLines.toList // Get contents in file
      stringLine.foreach(line => {
        val key = line.split('=')(0) // keyword
        val value = line.split('=')(1).toDouble // weight

        val currentKeyword: Vertex = graphNoTx.addVertex("class:" + GeneralConstant.KEYWORD_VERTEX, Nil: _*)
        currentKeyword.setProperty(GeneralConstant.KEYWORD_LABEL, key) // label
        val currentEdge: Edge = graphNoTx.addEdge("class:" + GeneralConstant.KEYWORD_RELATIONSHIP,
          currentKeyword,
          topic,
          null)
        currentEdge.setProperty(GeneralConstant.KEYWORD_WEIGHT, value) // value of keyword
        graphNoTx.commit()
      })
    })

    //    // create topic vertex class
    //    val testingData: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.TOPIC_VERTEX)
    //    testingData.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    //    testingData.createProperty(GeneralConstant.TOPIC_LABEL, OType.STRING)
    //    // create topic-topic edge class
    //    graphNoTx.createEdgeType(GeneralConstant.TOPIC_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    //    // create keyword vertex class
    //    val keywordTree: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.KEYWORD_VERTEX)
    //    keywordTree.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    //    keywordTree.createProperty(GeneralConstant.KEYWORD_LABEL, OType.STRING)

    //    // Create document vertex class
    //    val documentVertex: OrientVertexType = graphNoTx.createVertexType(GeneralConstant.DOCUMENT_VERTEX)
    //    documentVertex.setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    //    documentVertex.createProperty(GeneralConstant.DOCUMENT_LABEL, OType.STRING)
    //    documentVertex.createProperty(GeneralConstant.DOCUMENT_PATH, OType.STRING)
    //    // Create edge keyword-topic edge class
    //    graphNoTx.createEdgeType(GeneralConstant.DOCUMENT_RELATIONSHIP).setClusterSelection(ODefaultClusterSelectionStrategy.NAME)
    //    graphNoTx.commit()
    true
  }
}
