object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
    "-language:implicitConversions",
    "-language:reflectiveCalls",

    "-Wconf:msg=While parsing annotations in:silent",
    // required in place of silencer plugin
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    "-Wconf:cat=unused-imports&src=html/.*:s",
    "-Wconf:src=routes/.*:s"
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Xlint:-missing-interpolator,_",
    "-Xlint:adapted-args",
    "-Xlint:-byname-implicit",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-value-discard",
    "-Ywarn-dead-code",
    "-deprecation",
    "-feature",
    "-unchecked"
  )
}
