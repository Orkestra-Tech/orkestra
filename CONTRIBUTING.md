# Contributing

Contributions to Orkestra are more than welcomed!


## Getting help

The most important when you contribute is to know that you are not alone!  
Please come to ask anything to the maintainers, contributors and other users on GitHub issues or on the Gitter channel.


## Modules

- `orkestra-core` Main module.
- `orkestra-github` GitHub integration plugin.
- `orkestra-cron` Cron plugin using Kubernetes Cron.
- `orkestra-lock` Lock plugin using Elasticsearch.
- `orkestra-sbt` SBT plugin for easy project setup.
- `orkestra-integration-tests` Integration tests.
- `docs` Documentation with sbt-microsite.


## Compiling

```
compile
```


## Testing

```
test
```


## Documentation

The documentation website uses [sbt-microsite](https://47deg.github.io/sbt-microsites/).  
You'll need Jekyll to build the website. You can follow the installation instructions
[here](https://jekyllrb.com/docs/installation/).


## Publishing documentation

Publishing the documentation to GitHub Pages:
```
docs/publishMicrosite
```
See [sbt-microsite](https://47deg.github.io/sbt-microsites/) for more info.


## Publishing artifacts locally

```
publishLocal
```


## Releasing artifacts

First of all you need to setup the credentials to the Sonatype Nexus Repository:  
*~/.sbt/1.0/sonatype.sbt*:
```
credentials += Credentials("Sonatype Nexus Repository Manager",
                           "oss.sonatype.org",
                           "<sonatype user name>",
                           "<sonatype password>")
```
The sonatype credentials can be found on https://oss.sonatype.org/ > `Profile` > `User Token`.  
See [sbt-sonatype](https://github.com/xerial/sbt-sonatype) for more info.

Then you can run:
```
releaseEarly
```
