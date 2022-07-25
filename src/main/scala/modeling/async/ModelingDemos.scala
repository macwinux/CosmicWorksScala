package modeling.async

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
import com.azure.cosmos.CosmosAsyncContainer

class ModelingDemos extends AutoCloseable with CosmosConfig {

  val logger = Logger[ModelingDemos]
  lazy val client: CosmosAsyncClient = new CosmosClientBuilder()
    .endpoint(conf.accountHost)
    .key(conf.accountKey)
    .preferredRegions(List("West US").asJava)
    .consistencyLevel(ConsistencyLevel.EVENTUAL)
    .contentResponseOnWriteEnabled(true)
    .buildAsyncClient()

  val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  def close() {
    client.close()
  }

  def queryCustomer() {
    val database = client.getDatabase("database-v2")
    val container: CosmosAsyncContainer = database.getContainer("customer")
    val preferredPageSize = 10
    val queryOptions = new CosmosQueryRequestOptions()
    queryOptions.setQueryMetricsEnabled(true)
    val customerId = "0012D555-C7DE-4C4B-B4A4-2E8A6B8E1161"
    import scala.jdk.CollectionConverters._
    val customerPagedFlux: CosmosPagedFlux[CustomerV2] =
      container
        .queryItems(
          s"SELECT * FROM c WHERE c.id =\"${customerId}\"",
          queryOptions,
          classOf[CustomerV2]
        )
    import scala.jdk.StreamConverters._
    val list: LazyList[CustomerV2] =
      customerPagedFlux.toStream().toScala(LazyList)
    list foreach { productRes =>
      println(s"Item Ids ${productRes.id}")
      logger.info(s"Item Ids ${productRes.id}")
    }

  }
}
