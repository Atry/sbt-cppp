// Copyright 2014 杨博 (Yang Bo)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance at the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

crossBuildingSettings

sbtPlugin := true

name := "sbt-cppp"

organization := "com.dongxiguo"

scalacOptions += "-deprecation"

version := "0.1.4"

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

publishTo <<= (isSnapshot) { isSnapshot: Boolean =>
  if (isSnapshot)
    Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots") 
  else
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

licenses := Seq(
  "Apache License, Version 2.0" ->
  url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/Atry/sbt-cppp"))

scmInfo := Some(ScmInfo(
  url("https://github.com/Atry/sbt-cppp"),
  "scm:git:git://github.com/Atry/sbt-cppp.git",
  Some("scm:git:git@github.com:Atry/sbt-cppp.git")))

pomExtra :=
  <developers>
    <developer>
      <id>Atry</id>
      <name>杨博</name>
      <timezone>+8</timezone>
      <email>pop.atry@gmail.com</email>
    </developer>
  </developers>

// vim: et sts=2 sw=2
