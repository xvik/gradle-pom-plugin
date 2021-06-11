# Gradle POM plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.com/xvik/gradle-pom-plugin.svg?branch=master)](https://travis-ci.com/xvik/gradle-pom-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-pom-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-pom-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-pom-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-pom-plugin)

### About

Plugin fixes pom generation by [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin
and brings back configuration simplicity.

It will be especially useful for those who come from maven, because plugin tries to 
bring maven's dependencies declaration simplicity. I love gradle, really do, but it seems they want to 
do *everything not like maven* and, as a result, dependencies management and its projection
into maven model (pom generation) is overcomplicated (see details below). 

I admit that gradle dependencies model is more powerful, but it makes even simple use-cases much more complex 
then they should be. Plugin tries to bring back (maven) simplicity.  

Features:

* Fix [dependencies scopes](#dependencies) in the generated pom
    - Fix [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin provided dependencies
* Add `provided` and `optional` configuration (in maven meaning).    
* Add global pom configuration shortcuts (applied to all publications):
    - [pom](#pom-configuration) configuration closure to avoid maven-publish's withXml.
    - [withPomXml](#manual-pom-modification) configuration closure to be able to modify xml manually (shortcut for maven-publish's withXml)
* Compatible with:
    - Gradle [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin
    - Spring [dependency-management](#usage-with-spring-dependency-management-plugin) plugin

If you develop `java` or `groovy` library you may look to [java-lib plugin](https://github.com/xvik/gradle-java-lib-plugin)
which already includes `pom` plugin and configures maven publication for you 
(don't confuse with gradle's `java-library` plugin which only declares `api` and `implementation` configurations).

If your project is hosted on github you may look to [github-info plugin](https://github.com/xvik/gradle-github-info-plugin) 
which fills some pom sections for you automatically. 

Also, you can use [java-library generator](https://github.com/xvik/generator-lib-java) to setup new project with
all plugins configured.

##### Summary

* Configuration closures: `pom`, `withPomXml`  
* Configurations: `provided`, `optional` 
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html)

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-pom-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/pom/ru.vyarus.pom.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.pom)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-pom-plugin:2.1.0'
    }
}
apply plugin: 'ru.vyarus.pom'
```

OR

```groovy
plugins {
    id 'ru.vyarus.pom' version '2.1.0'
}
```

#### Compatibility

Plugin compiled for java 8, compatible with java 11

Gradle | Version
--------|-------
5.x     | 2.1.0
4.6     | [1.3.0](https://github.com/xvik/gradle-pom-plugin/tree/1.3.0)
older   | [1.2.0](https://github.com/xvik/gradle-pom-plugin/tree/1.2.0)

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-pom-plugin)
* Select `Commits` section and click `Get it` on commit you want to use (you may need to wait while version builds if no one requested it before)
    or use `master-SNAPSHOT` to use the most recent snapshot

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

(see [java-lib](https://github.com/xvik/gradle-java-lib-plugin) plugin for automatic publication registration)

Maven scopes reference:

Maven scope | Gradle configuration
------------| ----------------
compile     | implementation, api
runtime     | runtimeOnly
provided    | provided  (**not** compileOnly!)
optional    | optional, [feature variants](#feature-variants)

Also, see [good article](https://reflectoring.io/maven-scopes-gradle-configurations/) describing maven/gradle scope analogies.

`compileOnly` should be used only for really compile-time additions like nullability annotations.

`api` appear only when `java-library` plugin is enabled. Read [this article](https://reflectoring.io/gradle-pollution-free-dependencies/) 
to better understand api-implementation difference for gradle (or simply use `implementaion` by default). 

#### Dependencies

Plugin fixes dependencies scopes in the generated pom:

 Configuration | Fixed scope | Native behaviour | Note    
---------------|-----------|----|---
 api       | compile   | compile |  ony with [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation) plugin 
 implementation | **compile** |  runtime |   
 compileOnly       |  -   | dependencies not added |   
 runtimeOnly        | runtime | runtime |    
 *compile*   | compile | compile |  *deprecated!* avoid using
 *runtime*   | **runtime**  | compile | *deprecated!* avoid using
 providedCompile | **provided** | compile | only with [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin
 providedRuntime | **provided** | compile | only with [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin

Note: in context of gradle `java-library` plugin, both `api` and `implementation` dependencies stored in pom as `compile` dependencies
because there is only build time difference (it's a gradle optimization) between configurations and module still need `implementation` dependencies.
For the end user they both are usual transitive dependencies and must have compile scope. 

For example:

```groovy
plugins {
    id: 'java'
}
dependencies {    
    implementation 'com.foo:dep-compile:1.0'
    runtimeOnly 'com.foo:dep-runtime:1.0'
    provided 'com.foo:dep-provided:1.0'
    optional 'com.foo:dep-optional:1.0' 
}
```

Will result in:

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
        <optinoal>true</optinoal>
    </dependency>
</dependencies>
```

And

```groovy
plugins {
    id: 'java-library'
}
dependencies {    
    implementation 'com.foo:dep-compile:1.0'
    api 'com.foo:dep-api-compile:1.0'        
}
```

Will produce:

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
        <artifactId>dep-api-compile</artifactId>
        <version>1.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

##### Provided dependencies

Provided dependencies assumed to be present in target environment or already exists in 
projects consuming library. Provided dependencies will not be loaded as transitive dependencies,
they will exist in pom just for consultation.

Additional `provided` configuration created for provided dependencies. Gradle `implementation` extends from it
so for gradle `provided` dependencies would be the same as `implementation` and the difference 
will only appear in the resulted pom.

###### compileOnly

Gradle states that  `compileOnly` is the [official gradle analog](https://blog.gradle.org/introducing-compile-only-dependencies)
for maven `provided` scope. But it's not: first of all, gradle does not add such dependencies to pom at all.
More importantly, `compileOnly` dependencies are *not visible* in other scopes. So, for example,
to use these dependencies in test, you'll have to add them *again* in test scope.

The only application for `compileOnly` scope is compile-time libraries, like nullability annotations.     
This *is not* provided scope.

Plugin does not do anything with `compileOnly`: these dependencies will not be present in the resulted pom.

###### Make other scopes as provided

If you already have some other scope and want to identify this dependencies in pom as provided then:

```groovy
configurations.provided.extendsFrom configurations.apt

dependencies {
    implementation 'com.google.code.gson:gson:2.6.2'

    apt 'org.immutables:gson:2.1.19'
}
```      

Suppose `apt` configuration was added by some plugin. Now, apt dependencies will be marked provided in the resulted pom:

```xml
 <dependencies> 
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.6.2</version>
        <scope>compile</scope>
    </dependency>
    <dependency>
        <groupId>org.immutables</groupId>
        <artifactId>gson</artifactId>
        <version>2.1.19</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

###### War plugin

Gradle [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin declares it's own
provided configurations `providedCompile` and `providedRuntime` and these dependencies are not going into 
resulted war, but contained in the generated pom as `compile`. Plugin fixes such dependencies scope to `provided`
(how it could be declared in pure maven).  

##### Optional dependencies

Optional dependencies supposed to be used for not required dependencies, for exampel, activating
additional features.

Additional `optional` configuration created for optinoal dependencies. Gradle `implementation` extends from it
so for gradle `optional` dependencies would be the same as `implementation` and the difference 
will only appear in the resulted pom.

##### Feature variants

Gradle states that native replacement for optional dependencies is 
[gradle feature variants](https://docs.gradle.org/5.6.4/userguide/feature_variants.html#header). For example:

```groovy
java {
    registerFeature('someFeature') {
        usingSourceSet(sourceSets.main)
    }     
}

dependencies {                                                       
    someFeatureImplementation 'com.google.code.findbugs:annotations:3.0.0'                
}
```

```xml
<dependencies>
    <dependency>
      <groupId>com.google.code.findbugs</groupId>
      <artifactId>annotations</artifactId>
      <version>3.0.0</version>
      <scope>compile</scope>
      <optional>true</optional>
    </dependency>
</dependencies>
```

Looks good, but these dependencies *will not* work as you expected from optionals:
these dependencies will not be visible in other scopes. So if you need to test this behavior
then you'll have to add another dependency in test scope. 

Probably, there is a way to workaround this, but, still, simple things must be done simple, so
`optional` configuration would just do what the majority of library developers need.

#### Usage with spring dependency-management plugin

Gradle provides native support for [importing BOMs](https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub::terminology_platform),
but I'm still recommend to use spring's [dependency-management](https://github.com/spring-gradle-plugins/dependency-management-plugin) plugin 
instead of it because of more correct behaviour 
(it uses maven-resolver inside and so resolve dependencies [exactly](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/211#issuecomment-387362326) 
the same as maven).

Do not disable [plugin's pom modifications](https://github.com/spring-gradle-plugins/dependency-management-plugin#pom-generation),
because without it dependencies in pom file will be without version. Plugin will generate dependencyManagement pom section, which will make
pom dependencies without version valid.

##### Why not gradle BOM support

[This article](https://www.nexocode.com/blog/posts/spring-dependencies-in-gradle/) describes replacing spring 
plugin with the core gradle features. Even if I'm not agree with this, examples are very good and may be helpful.

Read [here](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/211#issuecomment-387362326)
why spring plugin is preferable.

And, again, gradle tries to reinvent the wheel by introducing "platforms" (kind of BOM) which, I agree,
is more powerful then simple BOM (together with gradle meta-model), but the majority of developers
*don't need this* (don't need to know about [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html)
plugin, dont need to keep in mind maven model vs gradle metamodel).

Simple things must be simple. Spring plugin is just a "maven working inside gradle", 
which means both gradle and maven will behave *the same*.

#### Pom configuration

By default, `maven-publish` plugin fills pom only with dependencies and artifact id, group and version. 
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
and use [gradle native dsl](#gradle-pom-dsl) for details.

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

If, for any reason, you need to modify pom manually (like in [withXml closure](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:modifying_the_generated_pom))
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

### Gradle pom dsl

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

If pom plugin used together with gradle dsl then pom plugin will merge its configuration into dsl:

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

(and, of course, fixed dependencies)

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - project documentation generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
