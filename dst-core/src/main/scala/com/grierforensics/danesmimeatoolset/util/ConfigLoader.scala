// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.util

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer

/** Singleton holding a loaded config */
object ConfigHolder {
  val config = ConfigLoader.loadConfig()
}


/** Exposes ConfigHolder's config to the extending class. */
trait Configged {
  protected def config: Config = ConfigHolder.config
}


/** Utility object for loading config from various locations with varying precedence and supporting config profiles.
  *
  * Basic config can be defined in code modules under files named reference.conf at the root of the classpath.
  * This is the standard for com.typesafe.config
  *
  *
  * Profile config will override basic config.
  * Profiles are defined in reference.conf files and the current
  * profile can be indicated in any of the config precedence levels.  Set the current profile with the "profile.current"
  * property.
  *
  * Here is an example of defining a prod profile:
  * {{{
  * profile.prod {
  *   someproperty = "somevalue"
  * }
  * }}}
  *
  * Here is an example of setting the prod profile as current:
  * {{{
  * profile.current = prod
  * }}}
  *
  *
  * Next in precedence, override file config will override basic and profile config properties.
  * Config loading will start from each given path and continue checking parent directories for config files with the
  * given name.  Files with deeper directory paths will take precedence of shallower directory paths.
  *
  *
  * System Properties will take highest precedence.
  *
  * */
object ConfigLoader extends LazyLogging {

  /** Loads all levels of config precedence. */
  def loadConfig(globalPaths: File*): Config = {
    val defaultConfigs: Config = ConfigFactory.load
    val overrideFileName = defaultConfigs.getString("config.filename")

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

