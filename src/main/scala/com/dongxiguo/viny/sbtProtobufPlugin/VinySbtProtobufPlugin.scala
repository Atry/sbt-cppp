package com.dongxiguo.viny.sbtProtobufPlugin

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File

final object VinySbtProtobufPlugin extends Plugin {
  
  // TODO: clean

  final val protocCommand = SettingKey[String]("protoc-command", "protoc executable")

  final val protoc = TaskKey[Seq[File]]("protoc", "Convert proto to java.")

  final val packageProto = TaskKey[File]("package-proto", "Package all proto source files.")

  final val unmanagedInclude = TaskKey[File]("unmanaged-include", "The default directory for manually managed included protos.")

  override final def globalSettings =
    super.globalSettings :+ (protocCommand := "protoc")

  private def protoSettings(task: Scoped) = {
    sbt.inTask(task)(
      Seq(
        unmanagedInclude <<= baseDirectory map { _ / "include" / "protobuf" },
        managedClasspath <<= (classpathConfiguration, classpathTypes, update) map {
          (config: Configuration, jarTypes: Set[String], up: UpdateReport) =>
            up.filter(configurationFilter(config.name) && artifactFilter(classifier = "proto", `type` = "proto")).toSeq.map {
              case (conf, module, art, file) => {
                Attributed(file)(AttributeMap.empty.put(artifact.key, art).put(moduleID.key, module).put(configuration.key, config))
              }
            } distinct
        },
        unmanagedClasspath <<=
          (thisProjectRef, configuration, settingsData, buildDependencies) flatMap { (projectRef: ProjectRef, conf: Configuration, data: Settings[Scope], deps: BuildDependencies) =>
            Classpaths.interDependencies(projectRef, deps, conf, conf, data, true, { (dep: ResolvedReference, conf: String, data: Settings[Scope]) =>
              (unmanagedInclude in (dep, ConfigKey(conf), task)).get(data) match {
                case None =>
                  constant(Nil)
                case Some(include) => {
                  include map { include =>
                    Seq(include).classpath
                  }
                }
              }
            })
          },
        internalDependencyClasspath <<=
          (thisProjectRef, classpathConfiguration, configuration, settingsData, buildDependencies) flatMap { (projectRef: ProjectRef, conf: Configuration, self: Configuration, data: Settings[Scope], deps: BuildDependencies) =>
            Classpaths.interDependencies(projectRef, deps, conf, self, data, false, { (dep: ResolvedReference, conf: String, data: Settings[Scope]) =>
              if ((packageProto in (dep, ConfigKey(conf))).get(data).isDefined) {
                (sourceDirectories in (dep, ConfigKey(conf), packageProto)).get(data) match {
                  case None =>
                    constant(Nil)
                  case Some(sourceDirectories) => {
                    constant(sourceDirectories.classpath)
                  }
                }
              } else {
                constant(Nil)
              }
            })
          },
        sourceManaged <<= (crossTarget, configuration) { (target, config) =>
          target / "protobuf-managed" / Defaults.nameForSrc(config.name)
        },
        includeFilter in unmanagedSources := "*.proto",
        unmanagedSourceDirectories <<= sourceDirectory { sd => Seq(sd / "protobuf") }))
  }

  final def baseZipProtoSettings =
    sbt.inTask(packageProto)(Classpaths.configSettings ++ Defaults.configTasks ++ Defaults.configPaths) ++
      protoSettings(packageProto) ++
      Defaults.packageTaskSettings(packageProto, Defaults.sourceMappings) :+
      (artifact in packageProto ~= {
        _.copy(`type` = "proto", classifier = Some("proto"))
      })

  final def baseProtocSettings =
    sbt.inTask(protoc)(Classpaths.configSettings ++ Defaults.configTasks ++ Defaults.configPaths) ++
      protoSettings(protoc) ++
      Seq(
        protoc <<= (
          crossTarget in protoc,
          dependencyClasspath in protoc,
          cacheDirectory in protoc,
          sourceManaged,
          protocCommand,
          sources in protoc,
          sourceDirectories in protoc,
          streams in protoc) map { (target, includes, cache, sourceManaged, protocCommand, protoSources, protoSourceDirectories, streams) =>
            val cachedTranfer = FileFunction.cached(cache / "protoc", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
              IO.withTemporaryDirectory { temporaryDirectory =>
                val unpack = FileFunction.cached(cache / "unpacked_include", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { protoJars: Set[File] =>
                  for {
                    protoJar <- protoJars
                    // TODO: Filter功能
                    output <- IO.unzip(protoJar, target / "unpacked_include")
                  } yield output
                }
                val (unpacking, rawIncludes) =
                  includes.partition { _.data.getName.endsWith(".jar") }
                val unpacked = unpack(unpacking.map { _.data }(collection.breakOut))
                val unpackedIncludes = if (unpacked.isEmpty) {
                  Nil
                } else {
                  Seq("--proto_path=" + (target / "unpacked_include").getPath)
                }

                val includeSourcePath = for {
                  directory <- protoSourceDirectories
                  if directory.exists
                } yield "--proto_path=" + directory.getPath
                val rawIncludesPath = for {
                  attributedDirectory <- rawIncludes
                  if attributedDirectory.data.exists
                } yield "--proto_path=" + attributedDirectory.data.getPath
                val processBuilder =
                  Seq(
                    protocCommand,
                    "--java_out=" + temporaryDirectory.getPath) ++
                    includeSourcePath ++
                    rawIncludesPath ++
                    unpackedIncludes ++
                    in.map { _.getPath }
                streams.log.info(processBuilder.mkString("\"", "\" \"", "\""))
                processBuilder !< streams.log match {
                  case 0 => {
                    val moveMapping = (temporaryDirectory ** globFilter("*.java")) x {
                      _.relativeTo(temporaryDirectory).map {
                        sourceManaged / _.getPath
                      }
                    }
                    IO.move(moveMapping)
                    moveMapping.map { _._2 }(collection.breakOut)
                  }
                  case result => {
                    throw new MessageOnlyException("protoc returns " + result)
                  }
                }
              }
            }
            cachedTranfer(protoSources.toSet).toSeq
          },
        sourceGenerators <+= protoc)

  val protobufSettings =
    sbt.addArtifact(artifact in packageProto in Compile, packageProto in Compile) ++
      inConfig(Compile)(baseZipProtoSettings ++ baseProtocSettings)

}

// vim: set ts=2 sw=2 et:
