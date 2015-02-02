package com.grierforensics.danesmimeatoolset.util

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer

object ConfigLoader extends LazyLogging {

  def loadConfig(overrideFileName: String, globalPaths: File*): Config = {
    val defaultConfigs: Config = ConfigFactory.load

    val propertyConfigs: Config = ConfigFactory.defaultOverrides
    logger.debug("Config System.property overrides:\n" + propertyConfigs.root.render)

    val fileConfigs: Config = accumulateFileConfigs(overrideFileName, globalPaths: _*)
    logger.debug("Config " + overrideFileName + " overrides:\n" + fileConfigs.root.render)

    val profileContext: Config = fileConfigs.withFallback(defaultConfigs)
    val profileConfig: Config = getProfileOverrides(profileContext)
    logger.debug("Config profile." + profileContext.getString("profile.current") + " overrides:\n" + profileConfig.root.render)

    propertyConfigs.withFallback(fileConfigs.withFallback(profileConfig.withFallback(defaultConfigs)))
  }

  private def getProfileOverrides(config: Config): Config = {
    try {
      config.getString("profile.current") match {
        case profile: String => config.getConfig("profile." + profile)
        case null => ConfigFactory.empty
      }
    }
    catch {
      case e: Exception => {
        logger.warn("why an exception?????  Moving on....", e)
        ConfigFactory.empty
      }
    }
  }

  private def accumulateFileConfigs(overrideFileName: String, globalPaths: File*): Config = {
    val pathsToTry = ListBuffer[File]()
    var currentDir = new File(".").getCanonicalFile
    while (currentDir != null) {
      pathsToTry += currentDir
      currentDir = currentDir.getParentFile
    }
    pathsToTry ++= globalPaths

    pathsToTry.foldLeft(ConfigFactory.empty)((config, dir) => tryDir(config, dir, overrideFileName))
  }

  private def tryDir(config: Config, dir: File, fileName: String): Config = {
    val file: File = new File(dir, fileName)
    if (file.exists) {
      val c: Config = ConfigFactory.parseFile(file)
      logger.info("Config - merged file: " + file)
      c.withFallback(config)
    } else {
      config
    }
  }
}

object ConfigHolder {
  private var _config = ConfigLoader.loadConfig("conf.conf")

  def config = _config
}

trait Configed {
  protected def config: Config = ConfigHolder.config
}


