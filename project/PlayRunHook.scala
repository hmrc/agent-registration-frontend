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
    println(s"http://localhost:$httpPort/agent-registration")
    println(s"http://localhost:$httpPort/agent-registration/test-only/show-agent-application")
    println(s"http://localhost:$httpPort/agent-registration/register")
  }
}