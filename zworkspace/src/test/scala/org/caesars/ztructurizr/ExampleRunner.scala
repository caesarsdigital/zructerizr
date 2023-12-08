package org.caesars.ztructurizr

import zio._

object ExampleRunner extends ZIOAppDefault {
  def run: ZIO[Any, Throwable, Unit] = (for {
    _ <- MicroservicesExampleSpec.microservicesExample
  } yield ()).provideLayer(MicroservicesExampleSpec.workspaceLayer)
}
