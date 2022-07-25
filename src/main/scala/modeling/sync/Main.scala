package modeling.sync

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.scalalogging.Logger
import common.CosmosConfig
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.ConsistencyLevel
import collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Main extends App with LazyLogging with CosmosConfig {

  override lazy val logger = Logger("Main Service")
  lazy val client: CosmosClient = new CosmosClientBuilder()
    .endpoint(conf.accountHost)
    .key(conf.accountKey)
    .preferredRegions(List("West US").asJava)
    .consistencyLevel(ConsistencyLevel.EVENTUAL)
    .contentResponseOnWriteEnabled(true)
    .buildClient()
  val deployment = new Deployment
  deployment.createDatabase(client, 1)
  implicit val ec = ExecutionContext.Implicits.global
  deployment.loadDatabase()
  logger.info("""
  '''
    Load finished
  '''""")
  // deployment.deleteDatabases(client, 1)
  /*val demo = new ModelingDemos
  logger.info("Start the query")
  demo.queryCustomer()

  logger.info("Start the direct get customer")
  demo.getCustomer()
   */
}
