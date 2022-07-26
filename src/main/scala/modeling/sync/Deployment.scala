package modeling.sync

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.models.ThroughputProperties
import common.CosmosConfig
import com.azure.cosmos.models.CosmosDatabaseResponse
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.models.CosmosContainerProperties
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosContainerResponse
import com.azure.cosmos.models.CosmosContainerRequestOptions
import scala.util.Try
import com.azure.cosmos.models.CosmosDatabaseRequestOptions
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import com.azure.cosmos.CosmosAsyncClient
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.ConsistencyLevel
import java.nio.file.Path
import java.nio.file.Paths
import reactor.core.scala.publisher.SFlux
import com.azure.cosmos.models.CosmosBulkOperations
import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.PartitionKey
import scala.util.Failure
import scala.util.Success
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.scalalogging.Logger
import org.json.JSONArray
import reactor.core.publisher.Flux
import java.util.ArrayList
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.azure.cosmos.implementation.Index
import org.json.JSONObject

class Deployment extends CosmosConfig with LazyLogging {
  override lazy val logger = Logger[Deployment]
  def createDatabase(cosmosDBClient: CosmosClient, schemaVersion: Int) = {
    val (schemaVersionStart, schemaVersionEnd) = schemaVersion match {
      case 0 => (schemaVersion, schemaVersion)
      case _ => (1, 4)
    }
    (schemaVersionStart to schemaVersionEnd) foreach { schemaVersionCounter =>
      logger.info(s"""
      '''
      create started for schema $schemaVersionCounter
      '''
      """)
      createDatabaseAndContainers(
        cosmosDBClient,
        "database-v" + schemaVersionCounter,
        schemaVersionCounter
      )
    }
  }
  def createDatabaseAndContainers(
      cosmosDBClient: CosmosClient,
      database: String,
      schema: Int
  ) = {
    logger.info(s"""
      '''
      Creating database and containers for schema v$schema
      DatabaseName: $database key:provided
      '''
      """)

    val databaseSchema: List[List[SchemaDetails]] = getSchemaDetails()

    if (schema >= 1 & schema <= 4) {
      val throughputProps: ThroughputProperties =
        ThroughputProperties.createAutoscaledThroughput(conf.throughput)

      val cosmosDatabaseResponse: CosmosDatabaseResponse =
        cosmosDBClient.createDatabaseIfNotExists(database, throughputProps)

      val cosmosDatabase: CosmosDatabase =
        cosmosDBClient.getDatabase(cosmosDatabaseResponse.getProperties.getId)

      databaseSchema(schema - 1).foreach { schemaDetails: SchemaDetails =>
        val autoScaleContainerProperties: CosmosContainerProperties =
          new CosmosContainerProperties(
            schemaDetails.ContainerName,
            schemaDetails.Pk
          )
        val databaseResponse: CosmosContainerResponse =
          cosmosDatabase.createContainer(
            autoScaleContainerProperties,
            new CosmosContainerRequestOptions
          )
        val container: CosmosContainer =
          cosmosDatabase.getContainer(databaseResponse.getProperties.getId)
        logger.info(s"""
          '''
          container ${cosmosDatabase.getId}.${container.getId} created!
          '''
          """)
      }
    }
  }

  def deleteDatabases(cosmosDBClient: CosmosClient, schemaVersion: Int) = {
    val (schemaVersionStart, schemaVersionEnd) = schemaVersion match {
      case 0 => (schemaVersion, schemaVersion)
      case _ => (1, 4)
    }

    logger.info(s"""
          '''
          Are you sure you want to detele all the databases? y/n"" +
          '''  
          """)
    val input = scala.io.StdIn.readLine()
    logger.info(s"input: $input")
    if (input.equals("y")) {
      (schemaVersionStart to schemaVersionEnd) foreach { schemaVersionCounter =>
        logger.info(s"""
          '''
          Delete started for schema $schemaVersionCounter
          '''
          """)
        Try(
          deleteDatabasesAndContainers(
            cosmosDBClient,
            "database-v" + schemaVersionCounter,
            schemaVersionCounter
          )
        ) match {
          case Success(_) =>
            logger.info(s"""
              '''
              Delete deleted, exiting program.
              '''
              """)
          case Failure(ex) => logger.warn(ex.getMessage())
        }
      }

      System.exit(0)
    }
  }

  def deleteDatabasesAndContainers(
      cosmosDBClient: CosmosClient,
      database: String,
      schema: Int
  ): Unit = {
    logger.info(s"""
          '''
          Deleting database and cointainers for schema v$schema
          DatabaseName:$database key:provided
          '''
          """)
    cosmosDBClient.getDatabase(database).delete()
  }

  def loadDatabase() = {
    implicit val ec = ExecutionContext.Implicits.global
    val tasks = (1 to 4).map { v =>
      Future(
        loadContainersFromFolder(v, "cosmic-works-v" + v, "database-v" + v)
      )
    }
    val aggr = Future.sequence(tasks)
    import scala.concurrent.duration._
    Await.result(aggr, Duration.Inf)

  }

  def loadContainersFromFolder(
      schemaVersion: Int,
      sourceDatabaseName: String,
      targetDatabaseName: String
  )(implicit ec: ExecutionContext): Unit = {
    val databaseSchema = getSchemaDetails
    val clientAsync = getCosmosClient
    val database = clientAsync.getDatabase(targetDatabaseName)
    val currentRelativePath: Path = Paths.get("")
    val folder =
      s"${currentRelativePath.toAbsolutePath.toString}/src/main/scala/data/$sourceDatabaseName/"
        .replace("\\", "/")
    logger.info(s"""
          ''''
          folder: $folder
          Preparing to load containers and data for $targetDatabaseName.....
          ''''
      """)
    val path = new java.io.File(folder)
    val listOfFiles = path.listFiles()
    listOfFiles.filter(_.isFile).foreach { file =>
      Try {
        logger.info(s"""
          ''''
          New container thread...
          Loading data for container: ${file.getName} in database $targetDatabaseName
          schemaVersion: $schemaVersion
          ''''
          """)
        val schemaDetails: List[SchemaDetails] =
          databaseSchema(schemaVersion - 1)
        val pk: Option[String] = schemaDetails find (schema =>
          file.getName().equals(schema.ContainerName)
        ) map { schema =>
          schema.Pk.substring(1)
        }
        val jsonArrayString = scala.io.Source.fromFile(file).mkString
        val jsonArray = new JSONArray(jsonArrayString)
        val jsonObjects: IndexedSeq[JSONObject] =
          (0 until jsonArray.length).map(jsonArray.getJSONObject)
        val docList = new ArrayList[JsonNode]()
        jsonObjects.foreach { obj =>
          val mapper = new ObjectMapper()
          docList.add(mapper.readTree(obj.toString()))
        }
        val docsToInsert = Flux.fromIterable(docList)
        val productCategoryContainer = database.getContainer(file.getName)
        bulkCreateGeneric(docsToInsert, productCategoryContainer, pk.get)
        file
      } match {
        case Success(value) =>
          logger.info(s"""
          ''''
          Finished loading data for container: ${value.getName}
          ''''
          """)
        case Failure(ex) => logger.error("Exception: " + ex)
      }
    }
  }

  def bulkCreateGeneric(
      items: Flux[JsonNode],
      cosmosDBClient: CosmosAsyncContainer,
      pk: String
  ) {
    val cosmosItemOperations = items.map { item: JsonNode =>
      val pkField = item.get(pk).asText()
      println("pkField: " + pkField)
      println(item)
      CosmosBulkOperations.getCreateItemOperation(
        item,
        new PartitionKey(pkField)
      )
    }
    cosmosDBClient
      .executeBulkOperations(cosmosItemOperations)
      .blockLast()
  }

  def getCosmosClient: CosmosAsyncClient =
    new CosmosClientBuilder()
      .endpoint(conf.accountHost)
      .key(conf.accountKey)
      .contentResponseOnWriteEnabled(true)
      .consistencyLevel(ConsistencyLevel.SESSION)
      .buildAsyncClient()
  def getSchemaDetails() = {

    val databaseSchema_1: List[SchemaDetails] = List(
      SchemaDetails("customer"),
      SchemaDetails("customerAddress"),
      SchemaDetails("customerPassword"),
      SchemaDetails("product"),
      SchemaDetails("productCategory"),
      SchemaDetails("productTag"),
      SchemaDetails("productTags"),
      SchemaDetails("salesOrder"),
      SchemaDetails("salesOrderDetail")
    )

    val databaseSchema_2: List[SchemaDetails] = List(
      SchemaDetails("customer"),
      SchemaDetails("product", "/categoryId"),
      SchemaDetails("productCategory", "/type"),
      SchemaDetails("productTag", "/type"),
      SchemaDetails("salesOrder", "/customerId")
    )

    val databaseSchema_3: List[SchemaDetails] = List(
      SchemaDetails("leases"),
      SchemaDetails("customer"),
      SchemaDetails("product", "/categoryId"),
      SchemaDetails("productCategory", "/type"),
      SchemaDetails("productTag", "/type"),
      SchemaDetails("salesOrder", "/customerId")
    )

    val databaseSchema_4: List[SchemaDetails] = List(
      SchemaDetails("customer", "/customerId"),
      SchemaDetails("product", "/categoryId"),
      SchemaDetails("productMeta", "/type"),
      SchemaDetails("salesByCategory", "/categoryId")
    )

    List(
      databaseSchema_1,
      databaseSchema_2,
      databaseSchema_3,
      databaseSchema_4
    )
  }
  case class SchemaDetails(
      ContainerName: String,
      Pk: String = "/id"
  )
}
