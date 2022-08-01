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
import com.fasterxml.jackson.annotation.JsonProperty
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.fasterxml.jackson.databind.DeserializationFeature
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
    val customerPagedIterable: CosmosPagedIterable[CustomerV2] =
      container
        .queryItems(
          s"SELECT * FROM c WHERE c.id =\"${customerId}\"",
          queryOptions,
          classOf[CustomerV2]
        )
    import scala.jdk.StreamConverters._
    customerPagedIterable
      .streamByPage(preferredPageSize)
      .toScala(LazyList) foreach { productRes =>
      val result = productRes.getResults()
      logger.info(s"""
          ''''
          Got a page of query result with ${result.size} items and request charge of ${productRes.getRequestCharge}
          ''''
        """)
      logger.info(s"""
        '''''
        Customer id: ${result.asScala.map(_.id).mkString(",")}
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
    productTypesIterable.streamByPage(pageSize).toScala(LazyList) foreach {
      cosmosRes =>
        val result = cosmosRes.getResults()
        logger.info(s"""
          ''''
          Got a page of query result with ${result.size} items and request charge of ${cosmosRes.getRequestCharge}
          ''''
        """)
        logger.info(s"""
      '''''
      Product types ${result.asScala.map(_.name).mkString(",")}
      '''''
      """)
    }
  }

  def queryProductByCategoryId() = {
    val database = client.getDatabase("database-v3")
    val container = database.getContainer("product")
    val size = 100
    val queryOptions =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true)
    val categoryId = "AB952F9F-5ABA-4251-BC2D-AFF8DF412A4A"
    val queryProductByCategoryIterable = container.queryItems(
      s"SELECT * FROM c WHERE c.categoryId = '$categoryId'",
      queryOptions,
      classOf[Product]
    )
    import scala.jdk.StreamConverters._
    queryProductByCategoryIterable
      .streamByPage(size)
      .toScala(LazyList) foreach { cosmosRes =>
      val result = cosmosRes.getResults()
      logger.info(s"""
          ''''
          Got a page of query result with ${result.size} items and request charge of ${cosmosRes.getRequestCharge}
          ''''
        """)
      logger.info(s"""
      '''''
      Products by category $categoryId: ${result.asScala
          .map(_.name)
          .mkString(",")}
      '''''
      """)
    }
  }

  def queryProductForCategory() = {
    val database = client.getDatabase("database-v3")
    val container = database.getContainer("product")
    val size = 100
    val queryOptions =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true)
    val sql = "SELECT COUNT(1) AS productCount, c.categoryName " +
      "FROM c WHERE c.categoryId = '86F3CBAB-97A7-4D01-BABB-ADEFFFAED6B4' " +
      "GROUP BY c.categoryName"

    val queryProductByCategoryIterable =
      container.queryItems(sql, queryOptions, classOf[ProductCount])
    import scala.jdk.StreamConverters._
    queryProductByCategoryIterable
      .streamByPage(size)
      .toScala(LazyList) foreach { cosmosRes =>
      val result = cosmosRes.getResults()
      logger.info(s"""
          ''''
          Got a page of query result with ${result.size} items and request charge of ${cosmosRes.getRequestCharge}
          ''''
        """)
      result.forEach(product => logger.info(s"""
        ''''
          Product count: ${product.productCount}
          Category Name: ${product.categoryName}
        ''''"""))
    }
  }

  def updateProductCategory() = {
    val database = client.getDatabase("database-v3")
    val container = database.getContainer("productCategory")
    val categoryId = "006A1D51-28DA-4956-A7FB-C0B2BF6360CA"
    logger.info("Update the name and replace 'and' with '&'")
    val updateProductCategory: ProductCategory =
      ProductCategory(categoryId, "Accessories, Bottles & Cages", "category")
    val mapper = new ObjectMapper()
    logger.info(
      "Object in Json " + mapper.writeValueAsString(updateProductCategory)
    )
    val productCategoryResponse =
      container.replaceItem(
        updateProductCategory,
        updateProductCategory.id,
        new PartitionKey(updateProductCategory.`type`),
        new CosmosItemRequestOptions()
      )
    logger.info(
      s"Request charge of replace operation: ${productCategoryResponse.getRequestCharge} RU"
    );

    logger.info("Done.");
  }
}
