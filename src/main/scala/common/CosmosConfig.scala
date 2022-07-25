package common

import pureconfig._
import pureconfig.generic.auto._
trait CosmosConfig {
  val conf: ServiceConf = ConfigSource
    .resources("application.conf")
    .loadOrThrow[ServiceConf]
}
