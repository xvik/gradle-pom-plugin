# Gradle POM plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-pom-plugin.svg)](https://travis-ci.org/xvik/gradle-pom-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-pom-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-pom-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-pom-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-pom-plugin)

### About

Plugin enhance [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin behaviour.

Features:
* Support gradle [`java-library` plugin](#java-library-plugin)
* Adds `optional` and `provided` configurations when used with `java` or `groovy` plugins (affect only resulted pom)
* Fix dependencies scopes in generated pom
* Add `pom` configuration closure to avoid maven-publish's withXml.
* Add `withPomXml` configuration closure to be able to modify xml manually (shortcut for maven-publish's withXml)
* Compatible with [spring's dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin)

Note: Gradle 4.8 provides similar pom dsl: [why pom plugin still matter](#pom-plugin-vs-gradle-pom-dsl)

If you develop java or groovy library you may look to [java-lib plugin](https://github.com/xvik/gradle-java-lib-plugin)
which already includes `pom` plugin and configures maven publication for you (don't confuse with gradle's java-library plugin which only declares api and implementation configurations).

If your project is hosted on github you may look to [github-info plugin](https://github.com/xvik/gradle-github-info-plugin) 
which fills some pom sections for you automatically. 

Also, you can use [java-library generator](https://github.com/xvik/generator-lib-java) to setup new project with
all plugins configured.

##### Summary

* Configuration closures: `pom`, `withPomXml`
* Configurations: `optional`, `provided` (if `java-library` not enabled)       
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html)

### Setup

**IMPORTANT**: version 1.3.0 and above 

* Requires gradle 4.6 or above. For lower gradle use version [1.2.0](https://github.com/xvik/gradle-pom-plugin/tree/1.2.0).
* For gradle 4.8 and above plugin will enable [STABLE_PUBLISHING preview feature](https://docs.gradle.org/4.8/userguide/publishing_maven.html#publishing_maven:deferred_configuration) -
disable lazy evaluation of publishing configuration (unification).
This is required to overcome hard to track `Cannot configure the 'publishing' extension` errors.
If you need some properties to evaluate lazily wrap them in `afterEvaluate` *inside* publishing configuration.
* In gradle 5 this preview option will be enabled by default. 


Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-pom-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.pom).

[![JCenter](https://api.bintray.com/packages/vyarus/xvik/gradle-pom-plugin/images/download.svg)](https://bintray.com/vyarus/xvik/gradle-pom-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-pom-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-pom-plugin:1.3.0'
    }
}
apply plugin: 'ru.vyarus.pom'
```

OR

```groovy
plugins {
    id 'ru.vyarus.pom' version '1.3.0'
}
```

Plugin must be applied after java or groovy plugins. Otherwise it will do nothing.

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-pom-plugin)
* Select `Commits` section and click `Get it` on commit you want to use (you may need to wait while version builds if no one requested it before)

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:gradle-pom-plugin:b5a8aee24f'
    }
}
apply plugin: 'ru.vyarus.pom'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.pom') {
                  useModule('ru.vyarus:gradle-pom-plugin:b5a8aee24f')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.pom'
  }
  ```  

</details>  

### Usage

Plugin requires [java](https://docs.gradle.org/current/userguide/java_plugin.html) or 
[groovy](https://docs.gradle.org/current/userguide/groovy_plugin.html) or 
[java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugins to be enabled. 

Plugin implicitly applies [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin. 
Publication must be [configured manually](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:publishing_component_to_maven), for example:

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
}
```

#### Dependencies

```groovy
dependencies {    
    compile 'com.foo:dep-compile:1.0'
    runtime 'com.foo:dep-runtime:1.0'
    provided 'com.foo:dep-provided:1.0'
    optional 'com.foo:dep-optional:1.0'        
}
```

Plugin correct dependencies scopes according to configuration, so the resulted pom will contain:

```xml
<dependencies>
     <dependency>
        <groupId>com.foo</groupId>
        <artifactId>dep-compile</artifactId>
        <version>1.0</version>
        <scope>compile</scope>
    </dependency>
    <dependency>
        <groupId>com.foo</groupId>
        <artifactId>dep-runtime</artifactId>
        <version>1.0</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.foo</groupId>
        <artifactId>dep-provided</artifactId>
        <version>1.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.foo</groupId>
        <artifactId>dep-optional</artifactId>
        <version>1.0</version>
        <scope>compile</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### Java and Groovy plugins

Currently (gradle 4), `compile` and `runtime` configurations are deprecated in favour of new `java-library` plugin's 
`api` and `implementation`. Old configurations could still be used (due to legacy reasons, 
for [groovy projects](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat)
or due to [increased memory consumption](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_memory)). 

Extra `optional` and `provided` configurations registered only if `java-library` plugin not applied
(because otherwise obviously you want to use new configurations and will not use deprecated compile).

Available [configurations](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management) 
and the resulted scope in pom:

| Configuration | Pom scope |  
|---------------|-----------|
| compile       | compile   |
| runtime       | runtime   |
| optional      | compile (with optional marker) |
| provided      | provided  |
| compileOnly   | Artifacts not present in pom |
| runtimeOnly   | runtime  |

##### Provided and optional configurations

Plugin registers `provided` and `optional` configurations and makes `compile` configuration extend them.
So, for gradle, `provided` and `optional` dependencies will work the same as `compile`.

Only during pom generation plugin will detect dependencies from `provided` or `optional` and mark them accordingly 
in generated pom.

If you want to include some other configuration dependencies as provided in pom, you can do:

```groovy
configurations.provided.extendsFrom configurations.myConf
```

But, don't forget that `compile` already extends `provided`, so your configuration will be transitively included 
 into compile. Anyway, in resulted pom all dependencies from `myConf` will have provided scope.

#### Java-library plugin

When [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html#)
plugin used, `optional` and `provided` configurations are **not registered**. 
Use `compileOnly` instead of provided. 

Available [configurations](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph) 
and the resulted scope in pom:

| Configuration | Migrate from (legacy conf. name) | Pom scope |
|---------------|----------------------------------|-----------|
| api            | compile   |  compile   |
| implementation | compile   |  compile   |
| compileOnly   |  provided  |  Artifacts not present in pom |
| runtimeOnly   | runtime    |   runtime  |
| apiElements    |  --       |  compile   |
| runtimeElements | --       |  runtime  |

#### Usage with [dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin)

Do not disable [plugin's pom modifications](https://github.com/spring-gradle-plugins/dependency-management-plugin#pom-generation),
because without it dependencies in pom file will be without version. Plugin will generate dependencyManagement pom section, which will make
pom dependencies without version valid.

#### Pom configuration

By default, maven-publish plugin fills pom only with dependencies and artifact id, group and version. 
Other information could be configured through `pom` closure:

```groovy
pom {
    name 'Project Name'
    description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }
    developers {
        developer {
            id "dev1"
            name "Dev1 Name"
            email "dev1@email.com"
        }
    }
}
```

Closure doesn't restrict structure: any tags may be used. 
If `name` and `description` not specified, they will be applied implicitly from `project.name` and `project.description`.

Here is [complete example](https://github.com/xvik/gradle-pom-plugin/blob/master/src/test/groovy/ru/vyarus/gradle/plugin/pom/PomSectionsTest.groovy#L28)
of all possible maven pom sections definition (you can use any tags if needed, not just these).

If pom already have some tags (e.g. set manually with withXml or by some plugin), plugin will override values and properly merge pom.
No duplicate tags will be created.

Only one `pom` closure may be defined: next pom closure completely override previous one.
If [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) declared, 
then pom closure will affect all of them. In this case, use it for general info 
and use [gradle native dsl](https://docs.gradle.org/4.8/release-notes.html#customizing-the-generated-pom) for details.

##### Clashed tag names

As `pom` closure is normal groovy closure, you may face situations when tag name clash with some method in your gradle project.

By default there is only one such case:

```groovy
pom {
    parent {
        name 'name'
        relativePath 'path'
    }
}
```

`relativePath` tag will not be present in resulted pom, because it clashes with gradle [Project.relativePath](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:relativePath(java.lang.Object)) method
and it will be called instead of "just holding" tag name.
 
Special prefix '_' may be used in such cases: 

```groovy
pom {
    parent {
        name 'name'
        _relativePath 'path'
    }
}
```

This prefix will solve clash with real method and will be dropped during xml generation. You can use this prefix with any tag. 

Another (standard) solution for this problem is using [delegate](http://groovy-lang.org/closures.html#closure-owner) reference: `delegate.relativePath`. 
But, for me, solution with prefix is simpler and easier to read.

##### Testing

To test resulted pom you can use [pom generation task](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:generate-pom):

```bash
$ gradlew generatePomFileForMavenJavaPublication
```

Note that 'MavenJava' in task name is publication name and in your case task name could be different.

Pom will be generated by default in `build/publications/mavenJava/pom-default.xml`

#### Manual pom modification

If for, some any reason, you need to modify pom manually (like in [withXml closure](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:modifying_the_generated_pom))
you can use define `withPomXml` configuration closure:

```groovy
pom {
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }    
}

withPomXml {
    it.appendNode('description', 'A demonstration of maven POM customization')
}
```

Generated pom xml passed to closure as parameter (no need to call asNode() as in gradle withXml block), so block above could declare parameter explicitly

```groovy
withPomXml { Node node ->
    node.appendNode('description', 'A demonstration of maven POM customization')
}
```

See [Node api](http://docs.groovy-lang.org/latest/html/api/groovy/util/Node.html) and [groovy xml guide](http://groovy-lang.org/processing-xml.html#_manipulating_xml).

`withPomXml` called just after `pom` closure merging into main pom, but before applying default name and description (because you may define them manually).
So xml Node passed into closure contains all modification applied by plugin (except default name and description).

**NOTE** pom plugin uses [withXml](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:modifying_the_generated_pom) to apply pom modifications. 
If other plugins use withXml too and these plugins registered after pom plugin, then their xml modification will be executed after pom plugin and after `withPomXml` block.
Most likely, this will not be an issue, but just keep it in mind when using manual xml modifications.

### Pom plugin vs gradle pom dsl

Since gradle 4.8 you can use [dsl like in pom plugin](https://docs.gradle.org/4.8/release-notes.html#customizing-the-generated-pom) in raw gradle:

```groovy
publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            // native gradle syntax!
            pom {
                name = 'first'
                scm {
                  url = "http://subversion.example.com/svn/project/trunk/"
                }
            }
        }
    }
}
```

So why use pom plugin now?

Because maven publication configuration could be moved to external plugin
(like [ru.vyarus.java-lib](https://github.com/xvik/gradle-java-lib-plugin) which
 configures maven-compatible publication artifacts) and, in this case, only pom should
 be customized:
 
```groovy
plugins {
    id 'ru.vyarus.java-lib'
}

pom {
    name 'first'
    scm {
      url "http://subversion.example.com/svn/project/trunk/"
    }
}
```  

Also, pom plugin applies pom declaration to all registered maven publications which
is useful, for example, for gradle plugin where publication is prepared by closed source plugin.

If pom plugin used together with gradle dsl then pom plugin will merge it's configuration into dsl:

```groovy
plugins {
    id 'ru.vyarus.pom'
}

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            // native gradle syntax!
            pom {
                name = 'first'
                description = 'desc'
                scm {
                  connection = "scm:svn:http://subversion.example.com/svn/project/trunk/"
                  url = "http://subversion.example.com/svn/project/trunk/"
                }
            }
        }
    }
}

pom {
    name 'custom name'
    scm {
      url "http://custom.url/"
    }
}
```

And the resulted pom will contain:

```xml
<pom>
    <name>custom name</name>
    <description>desc</description>
    <scm>
        <connection>scm:svn:http://subversion.example.com/svn/project/trunk/</connection>
        <url>http://custom.url/</url>            
    </scm>
</pom>
``` 

Plus, pom plugin automatically fixes dependencies scopes, which is also important. 

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - project documentation generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
