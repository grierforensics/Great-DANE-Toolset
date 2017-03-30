# Great DANE Toolset

The Great DANE Toolset is a standalone web server demonstrating Great DANE functionality using DANE SMIMEA
for email signing and encryption.

See the [Great DANE Engine](https://github.com/grierforensics/Great-DANE-Engine) for an up-to-date
implementation of the core Great DANE features.

The project is developed with Scala, sbt, Jersey, and AngularJS.

The project components are:
- dst-core - common core code for DaneSmimeaService, DST workflow, Email sending, and Email fetching
- dst-web - Jersey/AngularJS based web application that pulls it all together.

## Build

The build is done with Simple Build Tool (SBT).

The web portion of the project is based on the main Webapp plugin.  This gives a handful of webapp related build
tasks such as preparing a webapp directory without creating a war `web\webapp:prepare`, running a web container
locally, or deploying to a cloud webapp container.  See: https://github.com/earldouglas/xsbt-web-plugin.

### Build entire project

`$ sbt clean package`

War will be found at `./dst-web/target/scala-2.11/dst-web-x.x.war`.
The war file may be deployed to any servlet container.
Additional config is needed; see below.

Core jar will be found at `./dst/dst-core/target/scala-2.11/dst-core_2.11-x.x.jar`.

### Run locally

`$ sbt tomcat:start tomcat:join`

### Build a webapp directory without war packaging

`$ sbt webappPrepare`

## Configuration

Configuration is supported by TypeSafe's config classes, with a custom config loader that gives a lot of flexibility.

To get started locally, you must provide the passwords for the default config.  Do this by creating a file called `dst.conf`
in the app directory or a parent directory.  It should have the following content:
```
EmailSender.password = "PASSWORD_1"
EmailFetcher.password = "PASSWORD_2"
```

### Config Loading

Configuration is loaded from the following locations in order of precedence from highest to lowest:

- System properties - System properties may be specified on the Java command line or in ElasticBeanstalk configuration properties.
- dst.conf files - The application will search for dst.conf files in the current application directory and all of it's parents.
- reference.conf + -D<profile>, where <profile> is `profile.current=prod` or `profile.current=dev`).
  The specified profile may have properties that override default properties.
- reference.conf - A file found in the source resources directory, which specifies both default properties and profile properties.

## License

Dual-licensed under Apache License 2.0 and 3-Clause BSD License. See LICENSE.
