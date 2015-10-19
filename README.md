# DANE SMIMEA Toolset

Project to showcase the DANE SMIMEA standard for email signing and encryption.

The project is devloped with Scala, sbt, Jersey, and AngularJS

The project components are:
- dst-core - common core code for DaneSmimeaService, DST workflow, Email sending, and Email fetching
- dst-web - Jersey/AngularJS based web application that pulls it all together.

## Building

The build is done with Simple Build Tool (SBT) which is a Scala standard.

This is a multi project build, so some sbt commands will be to sub projects and will be prefixed with the
module name: `web\SOMETASK`.

The web portion of the project is based on the main Webapp plugin.  This gives a handfull of webapp related build
tasks such as preparing a webapp directory without creating a war `web\webapp:prepare`, running a web container
locally, or deploying to a cloud webapp container.  Documentation is here: https://github.com/earldouglas/xsbt-web-plugin/blob/master/docs/1.1.1.md
(I know it's creepy to use a library that is hosted on a personal github account, however, this is the state of Scala and
this is the webapp plugin referenced by the main Scala plugins page.)

#### Building entire project:
`$ sbt clean package`

War will be found at `./dst-web/target/scala-2.11/dst-web-x.x.war`.
The war file may be deployed to any servlet container.
Additional config is needed; see below.

Core jar will be found at `./dst/dst-core/target/scala-2.11/dst-core_2.11-x.x.jar`.

#### Running locally:
`$ sbt`
`> web/container:start`

This will build the

#### Building a webapp directory without war packaging:
`$ web/webapp:prepare`

## Configuration

Configuration is supported by TypeSafe's config classes, with a custom config loader that gives a lot of flexibility.

To get started locally, you must provide the passwords for the default config.  Do this by creating a file called `dst.conf`
in the app directory or a parent directory.  It should have the following content:
```
EmailSender.password = "PASSWORD_1"
EmailFetcher.password = "PASSWORD_2"
```

If you are deploying to ElasticBeanstalk see the config instructions below

### Config Loading

Configuration is loaded from the following locations in order of precedence from highest to lowest:

- System properties - System properties may be specified on the Java command line or in ElasticBeanstalk configuration properties.
- dst.conf files - The application will search for dst.conf files in the current application directory and all of it's parents.
- reference.conf profile.xxxxx (where xxxxx is specified by property profile.current=prod or profile.current=dev) -
The specified profile may have properties that override default properties.
- reference.conf - A file found in the source resources directory.  It specifies default properties and profile properties.

## Deploying to AWS Bitnami Tomcat instance
*We have a bug that has appeared when deploying to the latest Eleastic Beanstalk Tomcat instances.*

For the short term, we are deploying to a basic deploy of a Tomcat server on AWS

#### From the dst source root directory
```
$ sbt clean package
$ scp -i ~/keys/grier-aws.pem dst-web/target/scala-2.11/dst-web-0.8.war bitnami@52.2.156.120:
```

#### ssh to the AWS Bitnami server
```
$ ssh -i ~/keys/grier-aws.pem bitnami@52.2.156.120
```

#### From the AWS Bitnami server
```
$ sudo mv dst-web-0.8.war stack/apache-tomcat/webapps/ROOT.war
$ sudo rm -rf stack/apache-tomcat/webapps/ROOT
$ sudo stack/ctlscript.sh restart tomcat
```

## Deploying to Elastic Beanstalk
*We have a bug that has appeared when deploying to the latest Eleastic Beanstalk Tomcat instances.*

These are rough instructions for deploying to ElasticBeanstalk. (EB is pretty self explanatory)

The production url for DST, http://dst.grierforensics.com/ , is set up as a cname pointing at EB url
http://dst-prod.elasticbeanstalk.com/ .  Therefore, updating the EB webapp running at dst-prod.elasticbeanstalk.com will
update the main public url.

First, to nnavigate to the ElasticBeanstalk DST Application on AWS

- Log onto AWS here: https://grierforensics.signin.aws.amazon.com/console
- Click the Elastic Beanstalk link.

### Easy Deploy (involves quick downtime)

- Click the dst-prod environment (There will probably be only one environment.
The one you want will show a link at the top for "dst-prod.elasticbeanstalk.com" )
- Click "Upload and Deploy"
- Choose File and pick the war file you build using the above instructions.
- The Version label will default to the war name.  This value may not be reused, so if you will have to add a suffix if
 you are trying to up load a war with exactly the same name.
- Click Deploy and wait for EB to upload the war and restart Tomcat

#### ElasticBeanstalk Configuration

For each EB Application Environment there is configuration.  The following should be set.

- Click Configuration
- Click the Gear Icon on the Software Configuration panel.
- The following Environment Properties should be set:
    - profile.current=prod
    - EmailSender.password=GET_FROM_ADMIN
    - EmailFetcher.password=GET_FROM_ADMIN
    - (any of the other config may be overridden here)

When you save the configuration, ignore warnings about IAM profile, and Tomcat will restart.

### No Downtime Deploy

Roughly,

- Clone prod environment using a new environment url. (the config will be cloned as well, which is good)
- Upload new war to then new environment.
- Verify the new environment.
- Swap URLs on the new environment and the existing prod environment.
- Delete the pre-existing prod environment.



