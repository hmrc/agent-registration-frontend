import play.sbt.PlayRunHook

object PlayRunHook {
  def apply(httpPort: Int): PlayRunHook = {
    new PlayRunHook {

      override def afterStarted(): Unit = {
        printLinksForConvenience(httpPort)
      }
    }
  }

  private def printLinksForConvenience(httpPort: Int): Unit = {
    println(s"http://localhost:$httpPort/")
    println(s"http://localhost:$httpPort/agent-registration-frontend")
    println(s"http://localhost:$httpPort/agent-registration-frontend/test-only")
  }
}