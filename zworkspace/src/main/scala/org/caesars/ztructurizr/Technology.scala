package org.caesars.ztructurizr

import scala.language.implicitConversions

class Technology(val technology: String) extends AnyVal

object Technology {
  def apply(technology: String): Technology = new Technology(technology)

  implicit def technologyToString(technology: Technology): String =
    technology.technology
}
