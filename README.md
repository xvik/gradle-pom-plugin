# Gradle POM plugin
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/gradle-pom-plugin.svg)](https://travis-ci.org/xvik/gradle-pom-plugin)

### About

Plugin enhance [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin behaviour and
adds `optional` and `provided` dependencies support.

Features:
* Adds `optional` and `provided` configurations (affect only resulted pom)
* Fix dependencies scopes in generated pom (from default runtime)
* Add `pom` configuration closure to avoid maven-publish's withXml.
* Add `withPomXml` configuration closure to be able to modify xml manually (shortcut for maven-publish's withXml)
* Compatible with [spring's dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin)

If you develop java or groovy library you may look to [java-lib plugin](https://github.com/xvik/gradle-java-lib-plugin)
which already includes `pom` plugin and configures maven publication for you.

If your project is hosted on github you may look to [github-info plugin](https://github.com/xvik/gradle-github-info-plugin) 
which fills some pom sections for you automatically. 

Also, you can use [java-library generator](https://github.com/xvik/generator-lib-java) to setup new project with
all plugins configured.

### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-pom-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.pom).

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-pom-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-pom-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-pom-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-pom-plugin:1.1.0'
    }
}
apply plugin: 'ru.vyarus.pom'
```

OR

```groovy
plugins {
    id 'ru.vyarus.pom' version '1.1.0'
}
```

Plugin must be applied after java or groovy plugins. Otherwise it will do nothing.

### Usage

Plugin implicitly applies `maven-publish` plugin. 
Publication must be [configured manually](https://docs.gradle.org/current/userguide/publishing_maven.html), for example:

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

Compile configuration extends provided and optional configurations, so for gradle these dependencies will be the same
as compile (the difference is only important for resulted pom).

```groovy
dependencies {    
    compile 'com.foo:dep-compile:1.0'
    runtime 'com.foo:dep-runtime:1.0'
    provided 'com.foo:dep-provided:1.0'
    optional 'com.foo:dep-optional:1.0'        
}
```

In resulted pom will be:

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

Note: by default, maven-publish sets runtime scope for all dependencies, but plugin fixes that.

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

##### Usage with [dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin)

Do not disable [plugin's pom modifications](https://github.com/spring-gradle-plugins/dependency-management-plugin#pom-generation),
because without it dependencies in pom file will be without version. Plugin will generate dependencyManagement pom section, which will make
pom dependencies without version valid.

#### Pom configuration

By default, maven-publish plugin fills pom only with dependencies and artifact id, group and version. 
Other information could be configured through `pom` closure:

```groovy
pom {
    name 'Project Name'
    description 'My awesome project
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
and use [withXml](https://docs.gradle.org/current/userguide/publishing_maven.html#N17E99) for details.

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

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks

-
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
