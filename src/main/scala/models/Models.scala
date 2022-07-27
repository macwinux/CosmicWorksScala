package models
import com.fasterxml.jackson.annotation.JsonProperty
object Models {

  case class Product(
      @JsonProperty("id") id: String,
      @JsonProperty("categoryId") categoryId: String,
      @JsonProperty("categoryName") categoryName: String,
      @JsonProperty("sku") sku: String,
      @JsonProperty("name") name: String,
      @JsonProperty("description") description: String,
      @JsonProperty("price") price: Double,
      @JsonProperty("tags") tags: Array[Tag]
  )

  case class Tag(
      @JsonProperty("id") id: String,
      @JsonProperty("name") name: String
  )
  case class ProductCategory(
      @JsonProperty("id") id: String,
      @JsonProperty("name") name: String,
      @JsonProperty("type") `type`: String
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

  case class ProductCount(
      @JsonProperty("productCount") productCount: Int,
      @JsonProperty("categoryName") categoryName: String
  )

}
