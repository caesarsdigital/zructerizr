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
        // TODO:

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
    "An example of a microservices architecture, which includes asynchronous and parallel behaviour.".description
  )

  val microservicesExample: RIO[ZWorkspace, Unit] = for {
    workspace <- ZIO.service[ZWorkspace]
    mySoftwareSystem <- workspace.addSoftwareSystem(
      "Customer Information System",
      "Stores information".description
    )
    customer <- workspace.addPerson("Customer", "A customer".description)
    customerApplication <- mySoftwareSystem.addContainerZ(
      "Customer Application",
      "Allows customers to manage their profile.".description,
      "PureScript Concur".technology
    )
    customerService <- mySoftwareSystem.addContainerZ(
      "Customer Service",
      "The point of access for customer information.".description,
      "Scala and ZIO".technology
    )
    _ <- customerService.addTagz(MICROSERVICE_TAG)
    customerDatabase <- mySoftwareSystem.addContainerZ(
      "Customer Database",
      "Stores customer information.".description,
      "PostgreSQL".technology
    )
    _ <- customerDatabase.addTagz(DATASTORE_TAG)
    reportingService <- mySoftwareSystem.addContainerZ(
      "Reporting Service",
      "Creates normalised data for reporting purposes.".description,
      "Scala and ZIO".technology
    )
    _ <- reportingService.addTagz(MICROSERVICE_TAG)
    reportingDatabase <- mySoftwareSystem.addContainerZ(
      "Reporting Database",
      "Stores a normalised version of all business data for ad hoc reporting purposes.".description,
      "PostgreSQL".technology
    )
    _ <- reportingDatabase.addTagz(DATASTORE_TAG)
    auditService <- mySoftwareSystem.addContainerZ(
      "Audit Service",
      "Provides organisation-wide auditing facilities.".description,
      "Scala and ZIO".technology
    )
    _ <- auditService.addTagz(MICROSERVICE_TAG)
    auditStore <- mySoftwareSystem.addContainerZ(
      "Audit Store",
      "Stores information about events that have happened.".description,
      "PostgreSQL".technology
    )
    _ <- auditStore.addTagz(DATASTORE_TAG)
    messageBus <- mySoftwareSystem.addContainerZ(
      "Message Bus",
      "Transport for business events.".description,
      "Kafka".technology
    )
    _ <- messageBus.addTagz(MESSAGE_BUS_TAG)
    _ <- customer.uzez(customerApplication, "Uses".description)
    _ <- customerApplication.uzez(
      customerService,
      "Updates customer information using".description,
      "JSON/HTTPS".technology,
      Some(InteractionStyle.Synchronous)
    )
    _ <- customerService.uzez(
      messageBus,
      "Sends customer update events to".description,
      "".technology,
      Some(InteractionStyle.Asynchronous)
    )
    _ <- customerService.uzez(
      customerDatabase,
      "Stores data in".description,
      "JDBC".technology,
      Some(InteractionStyle.Synchronous)
    )
    _ <- customerService.uzez(
      customerApplication,
      "Sends events to".description,
      "WebSocket".technology,
      Some(InteractionStyle.Asynchronous)
    )
    _ <- messageBus.uzez(
      reportingService,
      "Sends customer update events to".description,
      "".technology,
      Some(InteractionStyle.Asynchronous)
    )
    _ <- messageBus.uzez(
      auditService,
      "Sends customer update events to".description,
      "".technology,
      Some(InteractionStyle.Asynchronous)
    )
    _ <- reportingService.uzez(
      reportingDatabase,
      "Stores data in".description,
      "".technology,
      Some(InteractionStyle.Synchronous)
    )
    _ <- auditService.uzez(
      auditStore,
      "Stores events in".description,
      "".technology,
      Some(InteractionStyle.Synchronous)
    )
    containerView <- workspace.createContainerView(
      mySoftwareSystem,
      "Containers".technology,
      "".description
    )
    _ <- containerView.viewFunction(_.addAllElements)
    dynamicView <- workspace.createDynamicViewOfSoftwareSystem(
      mySoftwareSystem,
      "CustomerUpdateEvent",
      "This diagram shows what happens when a customer updates their details.".description
    )
    _ <- dynamicView.addRelationshipView(
      customer,
      "".description,
      "".technology,
      customerApplication
    )
    _ <- dynamicView.addRelationshipView(
      customerApplication,
      "".description,
      "".technology,
      customerService
    )
    _ <- dynamicView.addRelationshipView(
      customerService,
      "".description,
      "".technology,
      customerDatabase
    )
    _ <- dynamicView.addRelationshipView(
      customerService,
      "".description,
      "".technology,
      messageBus
    )
    _ <- dynamicView.addParallelSequence(
      Seq(
        (messageBus, reportingService, "".description, "".technology),
        (reportingService, reportingDatabase, "".description, "".technology)
      )
    )
    _ <- dynamicView.addParallelSequence(
      Seq(
        (messageBus, auditService, "".description, "".technology),
        (auditService, auditStore, "".description, "".technology)
      )
    )
    _ <- dynamicView.addParallelSequence(
      Seq(
        (
          customerService,
          customerApplication,
          "Confirms update to".description,
          "".technology
        )
      )
    )
    diagrams <- ZWorkspace.exportWorkspace(PlantUML)
    diagramsWithPaths = diagrams.zipWithIndex.map { case (diagram, index) =>
      val path = s"figures/microservices-example-$index"
      (diagram, path)
    }
    _ <- ZIO.foreach(diagramsWithPaths) { case (diagram, path) =>
      diagram.saveToFile(path)
    }

  } yield ()

  def spec = suite("MySpec")(test("my test") {
    for {
      _ <- microservicesExample
      _ <- ZIO.succeed(println("DEBUG: done"))
    } yield assert(42)(equalTo(42))

  }).provideLayer(workspaceLayer)

}
