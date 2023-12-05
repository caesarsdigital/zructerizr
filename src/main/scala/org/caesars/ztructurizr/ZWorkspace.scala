package org.caesars.ztructurizr

import com.structurizr.Workspace;
// import com.structurizr.api.StructurizrClient;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.model._;
import com.structurizr.view._;

import java.io.File

import zio._

// TODO: https://github.com/structurizr/examples/blob/main/java/src/main/java/com/structurizr/example/MicroservicesExample.java
// TODO: https://youtu.be/4HEd1EEQLR0?feature=shared&t=2988

final class ZWorkspace private (
    workspace: Workspace,
    val parallelSequenceSem: Ref[Semaphore]
) {

  def addPerson(
      name: String,
      description: String
  ): Task[Person] = ZIO.attempt(workspace.getModel.addPerson(name, description))

  def addSoftwareSystem(
      name: String,
      description: String
  ): Task[SoftwareSystem] =
    ZIO.attempt(workspace.getModel.addSoftwareSystem(name, description))

  def views: UIO[ViewSet] = ZIO.succeed(workspace.getViews)

  /** Adds a unidirectional "uses" style relationship between this element and
    * the specified element.
    *
    * @param source
    *   the source of the relationship
    * @param destination
    *   the target of the relationship
    * @param description
    *   a description of the relationship (e.g. "uses", "gets data from", "sends
    *   data to")
    * @param technology
    *   the technology details (e.g. JSON/HTTPS)
    * @param interactionStyle
    *   the interaction style (sync vs async)
    * @param tags
    *   a sequence of tags
    * @return
    *   the relationship that has just been created and added to the model
    */
  def uses[S <: StaticStructureElement](
      source: S,
      destination: StaticStructureElement,
      description: String = "",
      technology: String = "",
      interactionStyle: Option[InteractionStyle] = None,
      tags: Seq[String] = Seq.empty[String]
  ): Task[Option[Relationship]] = ZIO.attempt(
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

  /** Creates a container view, where the scope of the view is the specified
    * software system.
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
      description: String
  ): Task[ContainerView] = ZIO.attempt(
    workspace.getViews.createContainerView(
      softwareSystem,
      key,
      description
    )
  )

  /** Creates a component view, where the scope of the view is the specified
    * container.
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
      description: String = ""
  ): Task[ComponentView] = ZIO.attempt(
    workspace.getViews.createComponentView(container, key, description)
  )

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
      description: String = ""
  ): Task[DynamicView] =
    ZIO.attempt(workspace.getViews.createDynamicView(key, description))

  /** Creates a dynamic view, where the scope is the specified software system.
    * The following elements can be added to the resulting view:
    *
    * <ul> <li>People</li> <li>Software systems</li> <li>Containers that reside
    * inside the specified software system</li> </ul>
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
      description: String = ""
  ): Task[DynamicView] = ZIO.attempt(
    workspace.getViews.createDynamicView(softwareSystem, key, description)
  )

  /** Creates a dynamic view, where the scope is the specified container. The
    * following elements can be added to the resulting view:
    *
    * <ul> <li>People</li> <li>Software systems</li> <li>Containers with the
    * same parent software system as the specified container</li> <li>Components
    * within the specified container</li> </ul>
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
      description: String = ""
  ): Task[DynamicView] = ZIO.attempt(
    workspace.getViews.createDynamicView(container, key, description)
  )

}

object ZWorkspace {

  private def apply(workspace: Workspace): Task[ZWorkspace] = for {
    sem <- Semaphore.make(1)
    semRef <- Ref.make(sem)
  } yield new ZWorkspace(workspace, semRef)

  def apply(name: String, description: String): Task[ZWorkspace] = for {
    workspace <- ZIO.attempt(new Workspace(name, description))
    zworkspace <- ZWorkspace(workspace)
  } yield zworkspace

  def apply(workspaceFile: File): Task[ZWorkspace] = for {
    dslParser <- ZIO.succeed(new StructurizrDslParser)
    _ <- ZIO.attempt(dslParser.parse(workspaceFile))
    workspace = dslParser.getWorkspace()
    zworkspace <- ZWorkspace(workspace)
  } yield zworkspace

  private def makeLayer(workspace: Workspace): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace(workspace))

  def makeLayer(name: String, description: String): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace(name, description))

  def makeLayer(workspaceFile: File): TaskLayer[ZWorkspace] =
    ZLayer.fromZIO(ZWorkspace(workspaceFile))

  def addContainer(
      softwareSystem: SoftwareSystem,
      name: String,
      description: String,
      technology: String
  ): Task[Container] = ZIO.attempt(
    softwareSystem.addContainer(name, description, technology)
  )

  implicit class ZSoftwareSystem(val softwareSystem: SoftwareSystem)
      extends AnyVal {
    def addContainer(
        name: String,
        description: String,
        technology: String
    ): Task[Container] = ZWorkspace.addContainer(
      softwareSystem,
      name,
      description,
      technology
    )

  }

  def addComponent(
      container: Container,
      name: String,
      description: String,
      technology: String
  ): Task[Component] = ZIO.attempt(
    container.addComponent(name, description, technology)
  )

  implicit class ZContainer(val container: Container) extends AnyVal {
    def addComponent(
        name: String,
        description: String,
        technology: String
    ): Task[Component] = ZWorkspace.addComponent(
      container,
      name,
      description,
      technology
    )
  }

  sealed trait RelationshipViewable
  final case class StaticViewable(element: StaticStructureElement)
      extends RelationshipViewable
  final case class CustomViewable(element: CustomElement)
      extends RelationshipViewable

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
      description: String,
      technology: String,
      destination: RelationshipViewable
  ): Task[RelationshipView] =
    (source, destination) match {
      case (StaticViewable(source), StaticViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (CustomViewable(source), CustomViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (StaticViewable(source), CustomViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
      case (CustomViewable(source), StaticViewable(destination)) =>
        ZIO.attempt(view.add(source, description, technology, destination))
    }

  def addParallelSequence(
      view: DynamicView,
      relationships: Seq[
        (RelationshipViewable, RelationshipViewable, String, String)
      ]
  ): RIO[ZWorkspace, Unit] = {
    val semTask = (for {
      _ <- ZIO.attempt(view.startParallelSequence())
      _ <- ZIO.foreach(relationships) {
        case (source, destination, description, technology) =>
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
      sem <- zworkspace.parallelSequenceSem.get
      _ <- sem.withPermit(semTask)
    } yield ()
  }

  implicit class ZDynamicView(val view: DynamicView) extends AnyVal {
    def addRelationshipView(
        source: RelationshipViewable,
        description: String,
        technology: String,
        destination: RelationshipViewable
    ): Task[RelationshipView] = ZWorkspace.addRelationshipView(
      view,
      source,
      description,
      technology,
      destination
    )

    def addParallelSequence(
        relationships: Seq[
          (RelationshipViewable, RelationshipViewable, String, String)
        ]
    ): RIO[ZWorkspace, Unit] =
      ZWorkspace.addParallelSequence(view, relationships)
  }

}
