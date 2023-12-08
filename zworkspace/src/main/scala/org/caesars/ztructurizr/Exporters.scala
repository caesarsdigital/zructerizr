package org.caesars.ztructurizr

import com.structurizr.export.AbstractDiagramExporter
import com.structurizr.export.plantuml.StructurizrPlantUMLExporter
final case class Exporters(
    plantUmlExporter: Option[StructurizrPlantUMLExporter]
) {

  def getExporter(exporterType: ExporterType): Option[AbstractDiagramExporter] =
    exporterType match {
      case PlantUML => plantUmlExporter
    }

}

object Exporters {
  val empty: Exporters = Exporters(None)

}
