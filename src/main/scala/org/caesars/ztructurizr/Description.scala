package org.caesars.ztructurizr

class Description(val description: String) extends AnyVal

object Description {
  def apply(description: String): Description = new Description(description)

  implicit def descriptionToString(description: Description): String =
    description.description

}
