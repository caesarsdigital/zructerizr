package org.caesars.ztructurizr

import org.caesars.ztructurizr.ZWorkspace._

import com.structurizr.model.InteractionStyle
import com.structurizr.model.Tags
import com.structurizr.view.Styles
import com.structurizr.view.ViewSet
import com.structurizr.view.ContainerView
import com.structurizr.view.DynamicView

import zio._
import zio.test._
import zio.test.Assertion._

/*

        Styles styles = views.getConfiguration().getStyles();
        styles.addElementStyle(Tags.ELEMENT).color("#000000");
        styles.addElementStyle(Tags.PERSON).background("#ffbf00").shape(Shape.Person);
        styles.addElementStyle(Tags.CONTAINER).background("#facc2E");
        styles.addElementStyle(MESSAGE_BUS_TAG).width(1600).shape(Shape.Pipe);
        styles.addElementStyle(MICROSERVICE_TAG).shape(Shape.Hexagon);
        styles.addElementStyle(DATASTORE_TAG).background("#f5da81").shape(Shape.Cylinder);
        styles.addRelationshipStyle(Tags.RELATIONSHIP).routing(Routing.Orthogonal);

        styles.addRelationshipStyle(Tags.ASYNCHRONOUS).dashed(true);
        styles.addRelationshipStyle(Tags.SYNCHRONOUS).dashed(false);

        StructurizrClient client = new StructurizrClient("key", "secret");
        client.putWorkspace(4241, workspace);
    }

}
 */

/** Adapated from
  * https://github.com/structurizr/examples/blob/cde555599f86a6fa8ac99934402cbf431f16d3c8/java/src/main/java/com/structurizr/example/MicroservicesExample.java
  */
object MicroservicesExampleSpec extends ZIOSpecDefault {

  // below follows a port of the above Java Code to use ZWorkspace

  val MICROSERVICE_TAG = "Microservice"
  val MESSAGE_BUS_TAG = "Message Bus"
  val DATASTORE_TAG = "Database"

  val workspaceLayer = ZWorkspace.makeLayer(
    "Microservices example",
    "An example of a microservices architecture, which includes asynchronous and parallel behaviour."
  )

  val microservicesExample: RIO[ZWorkspace, Unit] = for {
    workspace <- ZIO.service[ZWorkspace]
    mySoftwareSystem <- workspace.addSoftwareSystem(
      "Customer Information System",
      "Stores information"
    )
    customer <- workspace.addPerson("Customer", "A customer")
    customerApplication <- mySoftwareSystem.addContainerZ(
      "Customer Application",
      "Allows customers to manage their profile.",
      "PureScript Concur"
    )
    customerService <- mySoftwareSystem.addContainerZ(
      "Customer Service",
      "The point of access for customer information.",
      "Scala and ZIO"
    )
    _ <- customerService.addTagz(MICROSERVICE_TAG)
    customerDatabase <- mySoftwareSystem.addContainerZ(
      "Customer Database",
      "Stores customer information.",
      "PostgreSQL"
    )
    _ <- customerDatabase.addTagz(DATASTORE_TAG)
    reportingService <- mySoftwareSystem.addContainerZ(
      "Reporting Service",
      "Creates normalised data for reporting purposes.",
      "Scala and ZIO"
    )
    _ <- reportingService.addTagz(MICROSERVICE_TAG)
    reportingDatabase <- mySoftwareSystem.addContainerZ(
      "Reporting Database",
      "Stores a normalised version of all business data for ad hoc reporting purposes.",
      "PostgreSQL"
    )
    _ <- reportingDatabase.addTagz(DATASTORE_TAG)
    auditService <- mySoftwareSystem.addContainerZ(
      "Audit Service",
      "Provides organisation-wide auditing facilities.",
      "Scala and ZIO"
    )
    _ <- auditService.addTagz(MICROSERVICE_TAG)
    auditStore <- mySoftwareSystem.addContainerZ(
      "Audit Store",
      "Stores information about events that have happened.",
      "PostgreSQL"
    )
    _ <- auditStore.addTagz(DATASTORE_TAG)
    messageBus <- mySoftwareSystem.addContainerZ(
      "Message Bus",
      "Transport for business events.",
      "Kafka"
    )
    _ <- messageBus.addTagz(MESSAGE_BUS_TAG)
    _ <- customer.uzez(customerApplication, "Uses")
    _ <- customerApplication.uzez(
      customerService,
      "Updates customer information using",
      "JSON/HTTPS",
      Some(InteractionStyle.Synchronous)
    )
    _ <- customerService.uzez(
      messageBus,
      "Sends customer update events to",
      "",
      Some(InteractionStyle.Asynchronous)
    )
    _ <- customerService.uzez(
      customerDatabase,
      "Stores data in",
      "JDBC",
      Some(InteractionStyle.Synchronous)
    )
    _ <- customerService.uzez(
      customerApplication,
      "Sends events to",
      "WebSocket",
      Some(InteractionStyle.Asynchronous)
    )
    _ <- messageBus.uzez(
      reportingService,
      "Sends customer update events to",
      "",
      Some(InteractionStyle.Asynchronous)
    )
    _ <- messageBus.uzez(
      auditService,
      "Sends customer update events to",
      "",
      Some(InteractionStyle.Asynchronous)
    )
    _ <- reportingService.uzez(
      reportingDatabase,
      "Stores data in",
      "",
      Some(InteractionStyle.Synchronous)
    )
    _ <- auditService.uzez(
      auditStore,
      "Stores events in",
      "",
      Some(InteractionStyle.Synchronous)
    )
    containerView <- workspace.createContainerView(
      mySoftwareSystem,
      "Containers",
      ""
    )
    _ <- containerView.viewFunction(_.addAllElements)
    dynamicView <- workspace.createDynamicViewOfSoftwareSystem(
      mySoftwareSystem,
      "CustomerUpdateEvent",
      "This diagram shows what happens when a customer updates their details."
    )
    _ <- dynamicView.addRelationshipView(customer, "", "", customerApplication)
    _ <- dynamicView.addRelationshipView(
      customerApplication,
      "",
      "",
      customerService
    )
    _ <- dynamicView.addRelationshipView(
      customerService,
      "",
      "",
      customerDatabase
    )
    _ <- dynamicView.addRelationshipView(customerService, "", "", messageBus)
    _ <- dynamicView.addParallelSequence(
      Seq(
        (messageBus, reportingService, "", ""),
        (reportingService, reportingDatabase, "", "")
      )
    )
    _ <- dynamicView.addParallelSequence(
      Seq(
        (messageBus, auditService, "", ""),
        (auditService, auditStore, "", "")
      )
    )
    _ <- dynamicView.addParallelSequence(
      Seq(
        (customerService, customerApplication, "Confirms update to", "")
      )
    )
    diagrams <- ZWorkspace.exportWorkspace(PlantUML)
    _ <- ZIO.foreach(diagrams)(ZWorkspace.serialize)

  } yield ()

  def spec = suite("MySpec")(test("my test") {
    for {
      _ <- microservicesExample
      _ <- ZIO.succeed(println("DEBUG: done"))
    } yield assert(42)(equalTo(42))

  }).provideLayer(workspaceLayer)

}
