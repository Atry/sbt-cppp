package com.dongxiguo.viny.sbtProtobufPlugin

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File

final object VinySbtProtobufPlugin extends Plugin {

  // TODO: clean

  final val protocCommand = SettingKey[String]("protoc-command", "protoc executable")

  final val protoc = TaskKey[Seq[File]]("protoc", "Convert proto to java.")

  final val unmanagedInclude = SettingKey[File]("unmanaged-include", "The default directory for manually managed included protos.")

  final val Protobuf = config("protobuf")

  final val TestProtobuf = config("test-protobuf")

  override final def globalSettings =
    super.globalSettings :+ (protocCommand := "protoc")

  final def protocSetting(
    protobufConfiguration: Configuration,
    injectConfiguration: Configuration) =
    protoc in injectConfiguration <<= (
      crossTarget in protobufConfiguration,
      dependencyClasspath in protobufConfiguration, // FIXME: 此处dependencyClasspath不能递归找到依赖项目的依赖项目
      cacheDirectory in protobufConfiguration,
      sourceManaged in injectConfiguration,
      protocCommand in injectConfiguration,
      sources in protobufConfiguration,
      sourceDirectories in protobufConfiguration,
      streams in protobufConfiguration,
      thisProjectRef in protobufConfiguration,
      buildDependencies in protobufConfiguration,
      settingsData in protobufConfiguration) map { (target, includes, cache, sourceManaged, protocCommand, protoSources, protoSourceDirectories, streams, projectRef, deps, data) =>
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

            val dependencyIncludes = for {
              ResolvedClasspathDependency(dep, _) <- deps.classpath(projectRef)
              ui <- (unmanagedInclude in (dep, protobufConfiguration)).get(data)
              if ui.exists
            } yield "--proto_path=" + ui.getPath

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
                dependencyIncludes ++
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
      }

  final val baseProtobufSettings =
    Defaults.configTasks ++
      Defaults.configPaths ++
      Classpaths.configSettings ++
      Defaults.packageTaskSettings(
        packageBin,
        Defaults.concatMappings(Defaults.sourceMappings,
          unmanagedClasspath map { cp =>
            for {
              attributedPath <- cp
              path = attributedPath.data
              f <- path.***.get
              r <- f.relativeTo(path)
            } yield f -> r.getPath
          })) ++
        Seq(
          exportedProducts <<=
            (products.task, packageBin.task, exportJars, compile) flatMap { (psTask, pkgTask, useJars, analysis) =>
              (if (useJars) Seq(pkgTask).join else psTask) map { _ map { f => Classpaths.analyzed(f, analysis) } }
            },
          unmanagedInclude <<= baseDirectory { _ / "include" / "protobuf" },
          managedClasspath <<= (configuration, classpathTypes, update) map {
            (config: Configuration, jarTypes: Set[String], up: UpdateReport) =>
              up.filter(configurationFilter(config.name) && artifactFilter(classifier = config.name)).toSeq.map {
                case (conf, module, art, file) => {
                  Attributed(file)(AttributeMap.empty.put(artifact.key, art).put(moduleID.key, module).put(configuration.key, config))
                }
              }.distinct
          },
          unmanagedClasspath <<= unmanagedInclude map { i => Seq(i).classpath },
          internalDependencyClasspath <<=
            (thisProjectRef, configuration, settingsData, buildDependencies) map { (projectRef: ProjectRef, conf: Configuration, data: Settings[Scope], deps: BuildDependencies) =>
              (for {
                ResolvedClasspathDependency(dep, _) <- deps.classpath(projectRef)
                sourceDirectoriesOption = (sourceDirectories in (dep, conf)).get(data)
                if sourceDirectoriesOption.isDefined
                directory <- sourceDirectoriesOption.get
              } yield directory).classpath
            },
          unmanagedSourceDirectories <<= sourceDirectory { Seq(_) },
          includeFilter in unmanagedSources := "*.proto")

  final val protobufSettings =
    sbt.addArtifact(artifact in packageBin in Protobuf, packageBin in Protobuf) ++
      inConfig(Protobuf)(baseProtobufSettings) ++
      inConfig(TestProtobuf)(baseProtobufSettings) ++
      Seq(
        ivyConfigurations += Protobuf,
        protocSetting(Protobuf, Compile),
        sourceGenerators in Compile <+= protoc in Compile,
        ivyConfigurations += TestProtobuf,
        protocSetting(TestProtobuf, Test),
        sourceGenerators in Test <+= protoc in Test)
}

// vim: set ts=2 sw=2 et:
