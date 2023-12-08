package org.caesars.ztructurizr

import com.structurizr.export.plantuml.StructurizrPlantUMLExporter
import com.structurizr.export.AbstractDiagramExporter

final case class Exporters(
    plantUmlExporter: Option[StructurizrPlantUMLExporter]
) {

  def getExporter(exporterType: ExporterType): Option[AbstractDiagramExporter] =
    plantUmlExporter

}

object Exporters {
  val empty: Exporters = Exporters(None)

}
