package org.caesars.ztructurizr

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.implicitConversions

import com.structurizr.Workspace
import com.structurizr.dsl.StructurizrDslParser
import com.structurizr.`export`.Diagram
import com.structurizr.`export`.plantuml.{PlantUMLDiagram, StructurizrPlantUMLExporter}
import com.structurizr.model._
import com.structurizr.view._
import zio._

/** Some useful but Java-oriented references:
  * https://github.com/structurizr/examples/blob/main/java/src/main/java/com/structurizr/example/MicroservicesExample.java
  * https://youtu.be/4HEd1EEQLR0?feature=shared&t=2988
  */

final case class ZtructurizrException(message: String) extends Exception(message)
final class ZWorkspace private (
    private val workspace: Workspace,
    val parallelSequenceSem: Ref[Semaphore],
    val modelSem: Ref[Semaphore],
    val exporters: Ref[Exporters],
    val systemLandscapeView: Ref[Option[SystemLandscapeView]],
) {

  def getExporter(
      exporterType: ExporterType
  ): Task[StructurizrPlantUMLExporter] = for {
    exps <- exporters.get
    exporter <- exporterType match {
      case PlantUML =>
        exps.plantUmlExporter match {
          case Some(plantUmlExporter) =>
            ZIO.succeed(plantUmlExporter)
          case None =>
            for {
              umlExp <- ZIO.attempt(new StructurizrPlantUMLExporter)
              _      <- exporters.update(_.copy(plantUmlExporter = Some(umlExp)))
            } yield umlExp
        }
    }
  } yield exporter

  def addPerson(
      name: String,
      description: Description
  ): Task[Person] = for {
    sem    <- modelSem.get
    person <- sem.withPermit(ZIO.attempt(workspace.getModel.addPerson(name, description)))
  } yield person

  def addSoftwareSystem(
      name: String,
      description: Description
  ): Task[SoftwareSystem] = for {
    sem            <- modelSem.get
    softwareSystem <- sem.withPermit(ZIO.attempt(workspace.getModel.addSoftwareSystem(name, description)))
  } yield softwareSystem

  def views: UIO[ViewSet] = ZIO.succeed(workspace.getViews)

  /** Creates a container view, where the scope of the view is the specified software system.
    *
    * @param softwareSystem
    *   the SoftwareSystem object representing the scope of the view
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    *   a ContainerView object
    * @throws IllegalArgumentException
    *   if the software system is null or the key is not unique
    */
  def createContainerView(
      softwareSystem: SoftwareSystem,
      key: String,
      description: Description
  ): Task[ContainerView] = for {
    sem <- modelSem.get
    containerView <- sem.withPermit(
      ZIO.attempt(
        workspace.getViews.createContainerView(
          softwareSystem,
          key,
          description
        )
      )
    )
  } yield containerView

  /** Creates a component view, where the scope of the view is the specified container.
    *
    * @param container
    *   the Container object representing the scope of the view
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    *   a ContainerView object
    * @throws IllegalArgumentException
    *   if the container is null or the key is not unique
    */
  def createComponentView(
      container: Container,
      key: String,
      description: Description = Description("")
  ): Task[ComponentView] = for {
    sem <- modelSem.get
    componentView <- sem.withPermit(
      ZIO.attempt(
        workspace.getViews.createComponentView(container, key, description)
      )
    )
  } yield componentView

  /** Creates a dynamic view.
    *
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    *   a DynamicView object
    * @throws IllegalArgumentException
    *   if the key is not unique
    */
  def createDynamicView(
      key: String,
      description: Description = Description("")
  ): Task[DynamicView] = for {
    sem <- modelSem.get
    dynamicView <- sem.withPermit(
      ZIO.attempt(workspace.getViews.createDynamicView(key, description))
    )
  } yield dynamicView

  /** Creates a dynamic view, where the scope is the specified software system. The following elements can be added to the resulting view:
    *
    * <ul> <li>People</li> <li>Software systems</li> <li>Containers that reside inside the specified software system</li> </ul>
    *
    * @param softwareSystem
    *   the SoftwareSystem object representing the scope of the view
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    *   a DynamicView object
    * @throws IllegalArgumentException
    *   if the software system is null or the key is not unique
    */
  def createDynamicViewOfSoftwareSystem(
      softwareSystem: SoftwareSystem,
      key: String,
      description: Description = Description("")
  ): Task[DynamicView] = for {
    sem <- modelSem.get
    dynamicView <- sem.withPermit(
      ZIO.attempt(
        workspace.getViews.createDynamicView(softwareSystem, key, description)
      )
    )
  } yield dynamicView

  /** Creates a dynamic view, where the scope is the specified container. The following elements can be added to the resulting view:
    *
    * <ul> <li>People</li> <li>Software systems</li> <li>Containers with the same parent software system as the specified container</li>
    * <li>Components within the specified container</li> </ul>
    *
    * @param container
    *   the Container object representing the scope of the view
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    *   a DynamicView object
    * @throws IllegalArgumentException
    *   if the container is null or the key is not unique
    */
  def createDynamicViewOfContainer(
      container: Container,
      key: String,
      description: Description = Description("")
  ): Task[DynamicView] = for {
    sem <- modelSem.get
    dynamicView <- sem.withPermit(
      ZIO.attempt(
        workspace.getViews.createDynamicView(container, key, description)
      )
    )
  } yield dynamicView

  /** Represents a System Landscape view that sits "above" the C4 model, showing the software systems and people in a given environment. In
    * most cases, likely only one of these views would be needed, and you should <b>prefer to use createSystemLandscapeViewUniquex</b>.
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    */
  def createSystemLandscapeView(
      key: String,
      description: Description = Description("")
  ): Task[SystemLandscapeView] = for {
    sem <- modelSem.get
    systemLandscapeView <- sem.withPermit(
      ZIO.attempt(workspace.getViews.createSystemLandscapeView(key, description))
    )
  } yield systemLandscapeView

  /** Represents a System Landscape view that sits "above" the C4 model, showing the software systems and people in a given environment.
    * Returns a unique view, or fails with a ZtructurizrException if the unique view already exists. Adds all software systems and all
    * people to this view. In the event you want mulitple System Landscape views, use createSystemLandscapeView instead.
    * @param key
    *   the key for the view (must be unique)
    * @param description
    *   a description of the view
    * @return
    */
  def createSystemLandscapeViewUnique(
      key: String,
      description: Description = Description("")
  ): Task[SystemLandscapeView] =
    for {
      existingSystemLandscapeViewOpt <- systemLandscapeView.get
      sysLandscapeView <- existingSystemLandscapeViewOpt match {
        case Some(existingSystemLandscapeView) =>
          ZIO.fail(
            ZtructurizrException(
              s"Unique System Landscape View already exists with key: ${existingSystemLandscapeView.getKey}"
                + s" can't create another with key: $key."
            )
          )
        case None =>
          for {
            sysLView <- createSystemLandscapeView(key, description)
            _        <- ZIO.attempt(sysLView.addAllElements())
            _        <- systemLandscapeView.set(Some(sysLView))
          } yield sysLView
      }
    } yield sysLandscapeView
}

object ZWorkspace {

  sealed trait Exportable
  final case class DynamicViewExportable(dynamicView: DynamicView)               extends Exportable
  final case class ComponentExportable(componentView: ComponentView)             extends Exportable
  final case class ContainerExportable(containerView: ContainerView)             extends Exportable
  final case class CustomExportable(customView: CustomView)                      extends Exportable
  final case class DeploymentExportable(deploymentView: DeploymentView)          extends Exportable
  final case class SystemContextExportable(systemContextView: SystemContextView) extends Exportable
  final case class SystemLandscapeExportable(
      systemLandscapeView: SystemLandscapeView
  ) extends Exportable

  implicit def containerToExportable(
      containerView: ContainerView
  ): ContainerExportable = ContainerExportable(containerView)
  implicit def exportableToContainer(
      exportable: ContainerExportable
  ): ContainerView = exportable.containerView

  def diagram(
      exporterType: ExporterType,
      exportable: Exportable
  ): RIO[ZWorkspace, Diagram] = {
    val semTask = for {
      zworkspace <- ZIO.service[ZWorkspace]
      exporter   <- zworkspace.getExporter(exporterType)
      diagram <- exportable match {
        case DynamicViewExportable(dynamicView) =>
          ZIO.attempt(exporter.`export`(dynamicView))
        case ComponentExportable(componentView) =>
          ZIO.attempt(exporter.`export`(componentView))
        case ContainerExportable(containerView) =>
          ZIO.attempt(exporter.`export`(containerView))
        case CustomExportable(customView) =>
          ZIO.attempt(exporter.`export`(customView))
        case DeploymentExportable(deploymentView) =>
          ZIO.attempt(exporter.`export`(deploymentView))
        case SystemContextExportable(systemContextView) =>
          ZIO.attempt(exporter.`export`(systemContextView))
        case SystemLandscapeExportable(systemLandscapeView) =>
          ZIO.attempt(exporter.`export`(systemLandscapeView))
      }
    } yield diagram

    for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.modelSem.get
      diagram    <- sem.withPermit(semTask)
    } yield diagram
  }

  /** @param exportable
    * @param exporterType
    * @param animationStep
    *   Note that animationStep is ignored for CustomView, SystemContextView, and SystemLandscapeView.
    * @return
    */
  def diagram(
      exporterType: ExporterType,
      exportable: Exportable,
      animationStep: Int
  ): RIO[ZWorkspace, Diagram] = {
    val semTask = for {
      zworkspace <- ZIO.service[ZWorkspace]
      exporter   <- zworkspace.getExporter(exporterType)
      diagram <- exportable match {
        case DynamicViewExportable(dynamicView) =>
          ZIO.attempt(exporter.`export`(dynamicView, animationStep.toString))
        case ComponentExportable(componentView) =>
          ZIO.attempt(exporter.`export`(componentView, animationStep))
        case ContainerExportable(containerView) =>
          ZIO.attempt(exporter.`export`(containerView, animationStep))
        case CustomExportable(customView) =>
          ZIO.attempt(exporter.`export`(customView /*, animationStep */ ))
        case DeploymentExportable(deploymentView) =>
          ZIO.attempt(exporter.`export`(deploymentView, animationStep))
        case SystemContextExportable(systemContextView) =>
          ZIO.attempt(exporter.`export`(systemContextView /*, animationStep */ ))
        case SystemLandscapeExportable(systemLandscapeView) =>
          ZIO.attempt(exporter.`export`(systemLandscapeView /*, animationStep */ ))
      }
    } yield diagram

    for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.modelSem.get
      diagram    <- sem.withPermit(semTask)
    } yield diagram
  }

  def diagramWorkspace(
      exporterType: ExporterType
  ): RIO[ZWorkspace, List[Diagram]] = {
    val semTask = for {
      zworkspace             <- ZIO.service[ZWorkspace]
      systemLandscapeViewOpt <- zworkspace.systemLandscapeView.get
      _ <- systemLandscapeViewOpt match {
        case Some(sysLandscapeView) => ZIO.attempt(sysLandscapeView.addAllElements())
        case None                   => ZIO.unit
      }
      exporter <- zworkspace.getExporter(exporterType)
      diagrams <- ZIO
        .attempt(exporter.`export`(zworkspace.workspace))
        .map(CollectionHasAsScala(_).asScala.toList)
    } yield diagrams

    for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.modelSem.get
      diagrams   <- sem.withPermit(semTask)
    } yield diagrams

  }

  def diagramDefinition(
      diagram: Diagram
  ): Task[Option[String]] = diagram match {
    case plantUmlDiagram: PlantUMLDiagram =>
      ZIO.attempt(plantUmlDiagram.getDefinition).map(Option(_))
    case _ => ZIO.succeed(None)
  }

  /** Saves a diagram to a file. Fails with a ZtructurizrException if we can't get the definition from the diagram.
    *
    * @param diagram
    * @param baseFileName
    * @return
    */
  def saveToFile(
      diagram: Diagram,
      baseFileName: Path
  ): Task[Unit] = for {
    defn <- diagramDefinition(diagram)
    fileExt = Option(diagram.getFileExtension).getOrElse("txt")
    _ <- defn match {
      case Some(defn) => writeFile(s"$baseFileName.$fileExt", defn)
      case None =>
        ZIO.fail(
          ZtructurizrException(
            s"No definition when writing to $baseFileName.$fileExt"
          )
        )
    }
  } yield ()

  def saveToFile(
      diagram: Diagram,
      baseFileName: String
  ): Task[Unit] = for {
    defn <- diagramDefinition(diagram)
    fileExt = Option(diagram.getFileExtension).getOrElse("txt")
    _ <- defn match {
      case Some(defn) => writeFile(s"$baseFileName.$fileExt", defn)
      case None =>
        ZIO.fail(
          new Exception(
            s"No definition when writing to $baseFileName.$fileExt"
          )
        )
    }
  } yield ()

  implicit class ZDiagram(val diagram: Diagram) extends AnyVal {
    def definition: Task[Option[String]] = ZWorkspace.diagramDefinition(diagram)
    def saveToFile(baseFileName: String): Task[Unit] =
      ZWorkspace.saveToFile(diagram, baseFileName)
  }

  //     ext <- ZIO.attempt(diagram.getFileExtension())

  /** Constructs a ZWorkspace from an unmanaged Structurizr workspace.
    *
    * @param workspace
    * @return
    *   Task[ZWorkspace]
    */
  private def unsafeZWorkspace(workspace: Workspace): Task[ZWorkspace] = for {
    modelSem            <- Semaphore.make(1)
    parallelSequenceSem <- Semaphore.make(1)
    parSeqSemRef        <- Ref.make(modelSem)
    modelSemRef         <- Ref.make(parallelSequenceSem)
    exportersRef        <- Ref.make(Exporters.empty)
    systemLandscapeView <- Ref.make(Option.empty[SystemLandscapeView])
  } yield new ZWorkspace(workspace, parSeqSemRef, modelSemRef, exportersRef, systemLandscapeView)

  def apply(name: String, description: Description): Task[ZWorkspace] = for {
    workspace  <- ZIO.attempt(new Workspace(name, description))
    zworkspace <- ZWorkspace.unsafeZWorkspace(workspace)
  } yield zworkspace

  def apply(workspaceFile: File): Task[ZWorkspace] = for {
    dslParser <- ZIO.succeed(new StructurizrDslParser)
    _         <- ZIO.attempt(dslParser.parse(workspaceFile))
    workspace = dslParser.getWorkspace()
    zworkspace <- ZWorkspace.unsafeZWorkspace(workspace)
  } yield zworkspace

  /** Constructs a ZWorkspace layer from an unmanaged Structurizr workspace.
    *
    * @param workspace
    * @return
    *   TaskLayer[ZWorkspace]
    */
  def makeLayerUnsafe(workspace: Workspace): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace.unsafeZWorkspace(workspace))

  def makeLayer(name: String, description: Description): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace(name, description))

  def makeLayer(workspaceFile: File): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace(workspaceFile))

  def addContainerZ(
      softwareSystem: SoftwareSystem,
      name: String,
      description: Description,
      technology: Technology
  ): RIO[ZWorkspace, Container] = for {
    zworkspace <- ZIO.service[ZWorkspace]
    sem        <- zworkspace.modelSem.get
    container  <- sem.withPermit(ZIO.attempt(softwareSystem.addContainer(name, description, technology)))
  } yield container

  implicit class ZSoftwareSystem(val softwareSystem: SoftwareSystem) extends AnyVal {
    def addContainerZ(
        name: String,
        description: Description,
        technology: Technology
    ): RIO[ZWorkspace, Container] = ZWorkspace.addContainerZ(
      softwareSystem,
      name,
      description,
      technology
    )

  }

  def addComponentZ(
      container: Container,
      name: String,
      description: Description,
      technology: Technology
  ): RIO[ZWorkspace, Component] = for {
    zworkspace <- ZIO.service[ZWorkspace]
    sem        <- zworkspace.modelSem.get
    component  <- sem.withPermit(ZIO.attempt(container.addComponent(name, description, technology)))
  } yield component

  implicit class ZContainer(val container: Container) extends AnyVal {
    def addComponentZ(
        name: String,
        description: Description,
        technology: Technology
    ): RIO[ZWorkspace, Component] = ZWorkspace.addComponentZ(
      container,
      name,
      description,
      technology
    )
  }

  implicit class ZContainerView(val view: ContainerView) extends AnyVal {
    def viewFunction[T](
        fn: ContainerView => T
    ): RIO[ZWorkspace, T] = for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.modelSem.get
      result     <- sem.withPermit(ZIO.attempt(fn(view)))
    } yield result
  }

  def addTagz(
      element: Element,
      tags: String*
  ): RIO[ZWorkspace, Unit] = for {
    zworkspace <- ZIO.service[ZWorkspace]
    sem        <- zworkspace.modelSem.get
    _          <- sem.withPermit(ZIO.attempt(element.addTags(tags: _*)))
  } yield ()

  implicit class ZElement(val element: Element) extends AnyVal {
    def addTagz(
        tags: String*
    ): RIO[ZWorkspace, Unit] = ZWorkspace.addTagz(element, tags: _*)
  }

  sealed trait RelationshipViewable
  final case class StaticViewable(element: StaticStructureElement) extends RelationshipViewable
  final case class CustomViewable(element: CustomElement)          extends RelationshipViewable

  implicit def staticViewableToStaticStructureElement(
      viewable: StaticViewable
  ): StaticStructureElement = viewable.element
  implicit def staticStructureElementToStaticViewable(
      element: StaticStructureElement
  ): StaticViewable = StaticViewable(element)
  implicit def customViewableToCustomElement(
      viewable: CustomViewable
  ): CustomElement = viewable.element
  implicit def customElementToCustomViewable(
      element: CustomElement
  ): CustomViewable = CustomViewable(element)

  def addRelationshipView(
      view: DynamicView,
      source: RelationshipViewable,
      description: Description,
      technology: Technology,
      destination: RelationshipViewable
  ): RIO[ZWorkspace, RelationshipView] = {
    val semTask = (source, destination) match {
      case (StaticViewable(source), StaticViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (CustomViewable(source), CustomViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (StaticViewable(source), CustomViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (CustomViewable(source), StaticViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
    }
    for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.modelSem.get
      relView    <- sem.withPermit(semTask)
    } yield relView

  }

  def addParallelSequence(
      view: DynamicView,
      relationships: Seq[
        (RelationshipViewable, RelationshipViewable, Description, Technology)
      ]
  ): RIO[ZWorkspace, Unit] = {
    val semTask = (for {
      _ <- ZIO.attempt(view.startParallelSequence())
      _ <- ZIO.foreach(relationships) { case (source, destination, description, technology) =>
        addRelationshipView(
          view,
          source,
          description,
          technology,
          destination
        )
      }
      _ <- ZIO.attempt(view.endParallelSequence())
    } yield ()).uninterruptible

    for {
      zworkspace <- ZIO.service[ZWorkspace]
      sem        <- zworkspace.parallelSequenceSem.get
      _          <- sem.withPermit(semTask)
    } yield ()
  }

  implicit class ZDynamicView(val view: DynamicView) extends AnyVal {
    def addRelationshipView(
        source: RelationshipViewable,
        description: Description,
        technology: Technology,
        destination: RelationshipViewable
    ): RIO[ZWorkspace, RelationshipView] = ZWorkspace.addRelationshipView(
      view,
      source,
      description,
      technology,
      destination
    )

    def addParallelSequence(
        relationships: Seq[
          (RelationshipViewable, RelationshipViewable, Description, Technology)
        ]
    ): RIO[ZWorkspace, Unit] =
      ZWorkspace.addParallelSequence(view, relationships)
  }

  /** Adds a unidirectional "uses" style relationship between the source element and the specified destination element.
    *
    * @param source
    *   the source of the relationship
    * @param destination
    *   the target of the relationship
    * @param description
    *   a description of the relationship (e.g. "uses", "gets data from", "sends data to")
    * @param technology
    *   the technology details (e.g. JSON/HTTPS)
    * @param interactionStyle
    *   the interaction style (sync vs async)
    * @param tags
    *   a sequence of tags
    * @return
    *   the relationship that has just been created and added to the model
    */
  def uzez[S <: StaticStructureElement](
      source: S,
      destination: StaticStructureElement,
      description: Description = Description(""),
      technology: Technology = Technology(""),
      interactionStyle: Option[InteractionStyle] = None,
      tags: Seq[String] = Seq.empty[String]
  ): RIO[ZWorkspace, Option[Relationship]] = for {
    zworkspace <- ZIO.service[ZWorkspace]
    sem        <- zworkspace.modelSem.get
    relOpt <- sem.withPermit(
      ZIO.attempt(
        Option(
          source.uses(
            destination,
            description,
            technology,
            interactionStyle.orNull,
            tags.toArray
          )
        )
      )
    )
  } yield relOpt

  /** Adds a unidirectional "uses" style relationship between this element and the specified element.
    *
    * @param destination
    *   the target of the relationship
    * @param description
    *   a description of the relationship (e.g. "uses", "gets data from", "sends data to")
    * @param technology
    *   the technology details (e.g. JSON/HTTPS)
    * @param interactionStyle
    *   the interaction style (sync vs async)
    * @param tags
    *   a sequence of tags
    * @return
    *   the relationship that has just been created and added to the model
    */
  implicit class ZStaticStructureElement(val element: StaticStructureElement) extends AnyVal {
    def uzez(
        destination: StaticStructureElement,
        description: Description = Description(""),
        technology: Technology = Technology(""),
        interactionStyle: Option[InteractionStyle] = None,
        tags: Seq[String] = Seq.empty[String]
    ): RIO[ZWorkspace, Option[Relationship]] = ZWorkspace.uzez(
      element,
      destination,
      description,
      technology,
      interactionStyle,
      tags
    )
  }

  private def writeFile(
      path: Path,
      content: String
  ): IO[IOException, Path] = {
    ZIO
      .attempt(Files.write(path, content.getBytes(StandardCharsets.UTF_8)))
      .refineToOrDie[IOException]
  }

  private def writeFile(
      pathIn: String,
      content: String
  ): IO[IOException, Path] = for {
    path    <- ZIO.attempt(Paths.get(pathIn)).refineToOrDie[IOException]
    pathOut <- writeFile(path, content)
  } yield pathOut

  implicit class DescriptionStringOps(val value: String) extends AnyVal {
    def description: Description = Description(value)
  }

  implicit class TechnologyStringOps(val value: String) extends AnyVal {
    def technology: Technology = Technology(value)
  }

}
