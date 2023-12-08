package org.caesars.ztructurizr

import scala.language.implicitConversions

class Description(val description: String) extends AnyVal

object Description {
  def apply(description: String): Description = new Description(description)

  implicit def descriptionToString(description: Description): String =
    description.description

}
