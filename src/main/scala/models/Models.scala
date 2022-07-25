package models
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import com.fasterxml.jackson.annotation.JsonProperty
object Models {

  case class Product(
      id: String,
      categoryId: String,
      categoryName: String,
      sku: String,
      name: String,
      description: String,
      price: Double,
      tags: Array[Tag]
  )

  case class Tag(
      id: String,
      name: String
  )
  case class ProductCategory(
      id: String,
      name: String,
      `type`: String
  )

  case class SalesOrder(
      id: String,
      `type`: String,
      customerId: String,
      orderDate: String,
      shipDate: String,
      details: Array[SalesOrderDetails]
  )

  case class SalesOrderDetails(
      sku: String,
      name: String,
      price: Double,
      quantity: Int
  )

  case class CustomerV1(
      id: String,
      title: String,
      firstName: String,
      lastName: String,
      emailAddress: String,
      phoneNumber: String,
      creationDate: String
  )

  case class CustomerV2(
      @JsonProperty("id") id: String,
      @JsonProperty("title") title: String,
      @JsonProperty("firstName") firstName: String,
      @JsonProperty("lastName") lastName: String,
      @JsonProperty("emailAddress") emailAddress: String,
      @JsonProperty("phoneNumber") phoneNumber: String,
      @JsonProperty("creationDate") creationDate: String,
      @JsonProperty("addresses") addresses: Array[CustomerAddress],
      @JsonProperty("password") password: Password
  )

  case class CustomerAddress(
      @JsonProperty("addressLine1") addressLine1: String,
      @JsonProperty("addressLine2") addressLine2: String,
      @JsonProperty("city") city: String,
      @JsonProperty("state") state: String,
      @JsonProperty("country") country: String,
      @JsonProperty("zipCode") zipCode: String,
      @JsonProperty("location") location: Location
  )

  case class Location(
      @JsonProperty("type") `type`: String,
      @JsonProperty("coordinates") coordinates: Array[Float]
  )

  case class Password(
      @JsonProperty("hash") hash: String,
      @JsonProperty("salt") salt: String
  )

  case class CustomerV4(
      id: String,
      `type`: String,
      customerId: String,
      title: String,
      firstName: String,
      lastName: String,
      emailAddress: String,
      phoneNumber: String,
      creationDate: String,
      addresses: Array[CustomerAddress],
      password: Password,
      salesOrderCount: Int
  )
}