package org.caesars.ztructurizr

import com.structurizr.export.plantuml.StructurizrPlantUMLExporter
import com.structurizr.export.AbstractDiagramExporter

sealed trait Exporter {
  def exporter: AbstractDiagramExporter
  def exporterType(wrappedExporter: Exporter): ExporterType = exporter match {
    case _: StructurizrPlantUMLExporter => PlantUML
  }
}

final case class PlantUMLExporter(exporter: StructurizrPlantUMLExporter)
    extends Exporter
