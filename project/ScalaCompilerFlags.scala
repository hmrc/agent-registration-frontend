object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
//    "-explain",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-Wconf:msg=While parsing annotations in:silent",
    "-Yno-flexible-types",
    "-Wconf:msg=Unreachable case:silent", // Suppress unreachable case warnings
    //    "-rewrite",             // Enable rewriting
//    "-new-syntax",          // Enable significant indentation syntax
//    "-indent",              // Enable significant indentation syntax
//    "-source:3.6-migration" // Use Scala 3 migration mode
    "-Wconf:src=target/.*:s" // silence warnings from compiled files (this includes both compiled routes and twirl templates)
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-feature"
  )

}
