name := "spinalhdl-sphincsplus"
version := "0.1.0"
scalaVersion := "2.11.6"

lazy val externalLibs = project in file("lib")

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "1.3.6",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "1.3.6"
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys ++= Seq[BuildInfoKey](
      BuildInfoKey.map(name) { case (k, v) => "project" + k.capitalize -> v.capitalize },
      "externalLibs" -> s"${baseDirectory.in(externalLibs).value.getAbsolutePath}"
    ),
    buildInfoPackage := "sphincsplus"
  )

fork := true