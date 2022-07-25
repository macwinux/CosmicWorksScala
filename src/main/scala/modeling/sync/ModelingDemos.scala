package modeling.sync

import com.azure.cosmos.CosmosClientBuilder
import common.CosmosConfig
import com.azure.cosmos.ConsistencyLevel
import collection.JavaConverters._
import com.azure.cosmos.CosmosAsyncClient
import com.azure.cosmos.models.CosmosQueryRequestOptions
import models.Models._
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.cosmos.util.CosmosPagedFlux
import reactor.core.scala.publisher.SFlux
import com.typesafe.scalalogging.Logger
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.LazyLogging
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.CosmosItemResponse
import com.azure.cosmos.CosmosDatabase
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import cats.instances.lazyList
class ModelingDemos(client: CosmosClient)
    extends AutoCloseable
    with CosmosConfig
    with LazyLogging {

  override lazy val logger = Logger(LoggerFactory.getLogger("ModelingDemos"))
  val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  def close() {
    client.close()
  }

  def queryCustomer() {
    val database = client.getDatabase("database-v2")
    val container: CosmosContainer = database.getContainer("customer")
    val preferredPageSize = 10
    val queryOptions = new CosmosQueryRequestOptions()
    queryOptions.setQueryMetricsEnabled(true)
    val customerId = "FFD0DD37-1F0E-4E2E-8FAC-EAF45B0E9447"
    import scala.jdk.CollectionConverters._
    val customerPagedIterable: CosmosPagedIterable[CustomerV2] =
      container
        .queryItems(
          s"SELECT * FROM c WHERE c.id =\"${customerId}\"",
          queryOptions,
          classOf[CustomerV2]
        )
    import scala.jdk.StreamConverters._
    val list: LazyList[CustomerV2] =
      customerPagedIterable.stream.toScala(LazyList)
    list foreach { productRes =>
      logger.info(s"""
      '''''
      Item Ids ${productRes.id}
      '''''
      """)
    }
  }

  def getCustomer() {
    Try {
      val database: CosmosDatabase = client.getDatabase("database-v2")
      val container: CosmosContainer = database.getContainer("customer")
      val customerId = "FFD0DD37-1F0E-4E2E-8FAC-EAF45B0E9447"
      val item: CosmosItemResponse[CustomerV2] =
        container.readItem(
          customerId,
          new PartitionKey(customerId),
          classOf[CustomerV2]
        )
      item
    } match {
      case Success(item) =>
        logger.info(
          s"""
          ''''
          Point Read for a single customer.\n Item successfully read with id ${item.getItem} with a charge of ${item.getRequestCharge.floor}  and within duration ${item.getRequestCharge}
          ''''
          """
        )
      case Failure(ex) =>
        ex.printStackTrace()
        logger.error(s"Read item failed with ${ex}")
    }
  }

  def listAllProductCategories() = {
    val database = client.getDatabase("database-v2")
    val container = database.getContainer("productCategory")
    val pageSize = 100
    val queryOptions =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true)
    val productTypesIterable = container.queryItems(
      "SELECT * FROM c WHERE c.type = 'category'",
      queryOptions,
      classOf[ProductCategory]
    )
    import scala.jdk.StreamConverters._
    productTypesIterable.stream().toScala(LazyList) foreach { cosmosRes =>
      logger.info(s"""
      '''''
      Product types ${cosmosRes.name}
      '''''
      """)
    }
  }
}
