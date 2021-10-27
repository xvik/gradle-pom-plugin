# Gradle POM plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.com/xvik/gradle-pom-plugin.svg?branch=master)](https://travis-ci.com/xvik/gradle-pom-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-pom-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-pom-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-pom-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-pom-plugin)

### About

Plugin simplifies [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin
usage and corrects generated pom.

Plugin should make gradle more familiar for maven users (at least simpler to use).
With it, multi-module projects could be organized [the same way as maven projects](#multi-module-projects):
all dependencies managed in the root project (and, optionally, published as bom). 

Features:

* Fix [dependencies scopes](#dependencies) in the generated pom
    - Fix [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin's provided dependencies
* Add `provided` and `optional` configuration (in maven meaning).    
* Add global pom configuration shortcuts (applied to all publications):
    - [pom](#pom-configuration) configuration closure (applied to all publications).
    - [withPomXml](#manual-pom-modification) configuration closure for manual xml modifications (same as maven-publish's withXml, but applied for all publications)
* Moves declared BOMs on top in the generated dependencyManagement section (fixes java-platform behaviour)
* Automatic pom enhancements [could be disabled](#pom-generation-options) to see the native gradle behaviour)
* Optionally, could remove used BOMs, applying resolved versions instead (useful with gradle platforms to simplify resulted poms)
* Simplifies [multi-module projects configuration](#multi-module-projects)
* Compatible with:
    - Gradle java, groovy and [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin
    - Gradle [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) plugin (BOM case)   
    - Spring [dependency-management](#usage-with-spring-dependency-management-plugin) plugin

If you develop `java` or `groovy` library you may look at [java-lib plugin](https://github.com/xvik/gradle-java-lib-plugin)
which already includes this plugin and configures maven publication for you 
(don't confuse with gradle's `java-library` plugin which only declares `api` and `implementation` configurations).

If your project is hosted on github you may look at [github-info plugin](https://github.com/xvik/gradle-github-info-plugin) 
which fills some pom sections for you automatically. 

Also, you can use [java-library generator](https://github.com/xvik/generator-lib-java) to setup new project with
all plugins configured.

##### Summary

* Configuration closures: `pom`, `pomGeneration`, `withPomXml`  
* Configurations: `provided`, `optional`
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html)

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-pom-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-pom-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/pom/ru.vyarus.pom.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.pom)

```groovy
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-pom-plugin:2.2.1'
    }
}
apply plugin: 'ru.vyarus.pom'
```

OR

```groovy
plugins {
    id 'ru.vyarus.pom' version '2.2.1'
}
```

#### Compatibility

Plugin compiled for java 8, compatible with java 11.

Gradle | Version
--------|-------
5.0     | 2.2.1
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

If [java](https://docs.gradle.org/current/userguide/java_plugin.html),
[groovy](https://docs.gradle.org/current/userguide/groovy_plugin.html), 
[java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) or
[java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) plugins enabled,
`maven-publish` would be registered automatically. For example:

```groovy
plugins {
    id 'java' // or groovy or java-library
    id 'ru.vyarus.pom'
}
```

```groovy
plugins {
    id 'java-platform'
    id 'ru.vyarus.pom'
}
```

in both cases `maven-publish` plugin would be activated implicitly.

Configuration closures added when [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin 
registered (and so will be available even if you don't use java plugins above and only register maven-publish manually).

For example the following is also a valid usage:

```groovy
plugins {
    id 'maven-publish'
    id 'ru.vyarus.pom'
}
```
 
Publication **must be [configured manually](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:publishing_component_to_maven)**, for example:

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

`compileOnly` should be used only for compile-time only dependencies like nullability annotations.

`api` configuration appear only when `java-library` plugin is enabled. Read [this article](https://reflectoring.io/gradle-pollution-free-dependencies/) 
to better understand api-implementation difference for gradle (or simply use `implementaion` by default). 

#### Dependencies

Plugin fixes dependencies scopes in the generated pom:

 Configuration | Fixed scope | Native behaviour | Note    
---------------|-----------|----|---
 api       | compile   | compile |  ony with [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation) plugin 
 implementation | **compile** |  runtime |   
 compileOnly       |  -   | dependencies not added |   
 runtimeOnly        | runtime | runtime |    
 *compile*   | compile | compile |  *removed in gradle 7*
 *runtime*   | **runtime**  | compile | *removed in gradle 7*
 providedCompile | **provided** | compile | only with [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin
 providedRuntime | **provided** | compile | only with [war](https://docs.gradle.org/current/userguide/war_plugin.html#sec:war_dependency_management) plugin

Note: in context of gradle `java-library` plugin, both `api` and `implementation` dependencies stored in pom as `compile` dependencies
because there is only build time difference (it's a gradle optimization) between configurations and module still need `implementation` dependencies.
For the end user they both are usual transitive dependencies and must have compile scope. 

For example:

```groovy
plugins {
    id: 'java'
    id 'ru.vyarus.pom'
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
    id 'ru.vyarus.pom'
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

NOTE: scope modifications could be disabled with `pomGeneration` section (see below)

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
This *is not* a provided scope.

Plugin does not do anything with `compileOnly`: these dependencies will not be present in the resulted pom.

###### Make other scopes as provided

If you already have some other scope and want to identify its dependencies in pom as provided then:

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

Optional dependencies supposed to be used for not required dependencies, for example, activating
additional features.

Additional `optional` configuration created for optional dependencies. Gradle `implementation` extends from it
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

Looks good, but these dependencies *will not* work as you expect from optionals:
these dependencies will not be visible in other scopes. So if you need to test this behavior
then you'll have to add another dependency in test scope. 

Probably, there is a way to workaround this, but, still, simple things must be done simple, so
`optional` configuration would just do what the majority of library developers need.

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

Here is a [complete example](https://github.com/xvik/gradle-pom-plugin/blob/master/src/test/groovy/ru/vyarus/gradle/plugin/pom/PomSectionsTest.groovy#L28)
of all possible maven pom sections definition (you can use any tags if needed, not just these).

If pom already have some tags (e.g. set manually with withXml or by some plugin), plugin will *override values* and *properly merge* pom.
*No duplicate tags will be created.*

*Multiple `pom` closures may be defined* (useful for multi-module projects).

If [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) declared, 
then pom closure will affect *all of them*. If you need different data in poms then use pom closure only
for general info and [gradle native dsl](#gradle-pom-dsl) for different parts.

##### Clashed tag names

As `pom` closure is normal groovy closure, you may face situations when tag name clash with some method in your gradle project.

By default, there is only one such case:

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
you can use `withPomXml` configuration closure:

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

*Multiple `withPomXml` closures could be used* (useful for multi-module projects).

`withPomXml` called just after `pom` closure merging into main pom, but before applying default name and description (because you may define them manually).
So xml Node passed into closure contains all modification applied by the plugin (except default name and description).

**NOTE** pom plugin uses [withXml](https://docs.gradle.org/current/userguide/publishing_maven.html#sec:modifying_the_generated_pom) to apply pom modifications. 
If other plugins use withXml too and these plugins registered after pom plugin, then their xml modification will be executed after pom plugin and after `withPomXml` block.
Most likely, this will not be an issue, but just keep it in mind when using manual xml modifications.

#### Pom generation options

Plugin behaviour could be controlled with `pomGeneration` closure.

For example, to disable scopes correction:

```groovy
pomGeneration {
    disableScopesCorrection()
}
```

(you may use it to see the default gradle behaviour) 

All options:

- `disableScopesCorrection()` - disable dependencies scopes correction (to see native gradle behaviour)
- `disableBomsReorder()` - disable moving declared BOMs on top of dependencyManagement section (to see native gradle behaviour)  
- `forceVersions()` - always put dependency version, even when platforms or BOM with spring plugin used
  (use [recommended gradle way](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies) to force version)
- `removeDependencyManagement()` - removes `dependencyManagement` section from the generated pom
    and implicitly activates `forceVersions()` (otherwise pom could become invalid).
    Useful with gradle platforms (see examples below)

(if you prefer property style declaration then you can use extension fields instead of methods (names differ from methods!):
forcedVersions, removedDependencyManagement, disabledScopesCorrection (e.g. pomGeneration.forcedVersions = true))

##### Improving BOMs usage

If you use BOMs for dependency management:

```groovy
dependencies {
    // declare BOM
    implementation platform('com.group:some-bom:1.0')
    // dependency version managed by BOM
    implementation 'com.other.group:some-dep'
}
```

(could be spring plugin instead of platform, behaviour is the same)

The resulted pom would look like (native behavior):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.group</groupId>
            <artifactId>some-bom</artifactId>
            <version>1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>com.other.group</groupId>
        <artifactId>some-dep</artifactId>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

Note that dependency version is not set (assuming resolution through bom).

In order to force dependency versions use:

```groovy
pomGeneration {
    forceVersions()
}
```

To completely remove dependency management section from generated pom:

```groovy
pomGeneration {
    removeDependencyManagement()
}
```

the resulted pom would become:

```xml
<dependencies>
    <dependency>
        <groupId>com.other.group</groupId>
        <artifactId>some-dep</artifactId>
        <version>1.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

Gradle platforms are very handy for dependency management in the root project (maven style),
but they should not "leak" into resulted poms. See the complete multi-module declaration example below.

### Gradle pom dsl

Plugin was initially created when pom declaration in `maven-publish` plugin was clumsy, but
since gradle 4.8 you can use [dsl like in pom plugin](https://docs.gradle.org/4.8/release-notes.html#customizing-the-generated-pom) in raw gradle:

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

### Multi-module projects

#### BOM options 

In order to unify dependencies management in multi-module project you'll have to use
something like BOM (all versions in one place, modules just declare what they use).

There are two ways to declare BOMs with gradle:

- spring's [dependency-management](https://github.com/spring-gradle-plugins/dependency-management-plugin) plugin
- gradle's [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) plugin

From my experience gradle platforms works much better. There is a major drawback: gradle platform
will not allow you to declare dependency exclusions. It is a surprise for maven users, but you'll have to 
change your mind and apply required exclusions only in target module (where you apply dependency).

You can read more about gradle native BOMs support in [this article](https://www.nexocode.com/blog/posts/spring-dependencies-in-gradle/) (for an overview)

There are also some differences in boms import behaviour: spring plugin will behave exactly 
as maven. You can read more: [why spring plugin is still useful](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/211#issuecomment-387362326).

Major drawback for spring plugin is: it applies bom to all configurations (yes, it's possible to apply
only to some, but also cause other problems). For example, I often see spotbugs plugin affected,
because it uses custom configuration which could be ruined by the bom imported into project 
(and so I have to manually force all required versions there).

For simple cases always prefer gradle native support. For complex cases, try to use platform too and
go to spring plugin only if you can't avoid it. Don't get me wrong, spring plugin is very good, 
but you'll have much fewer problems with platforms.

#### Example BOM publication

Here is an example of multi-module project where:

- All dependencies declared in the root project with java-platform (maven way!)
- Root project published as BOM (not canonical BOM: contains all dependencies and all project modules; 
  this way allows including other boms and optional dependencies - simpler to use)
- All subprojects are java projects. All depend on platform, declared in root, but platform
   is removed from generated pom (to avoid bom delcare module, which again use bom for dependencies resolution)

```groovy
plugins {
    id 'java-platform'
    id 'ru.vyarus.pom'
}

javaPlatform {
    allowDependencies()
}

// root project is a BOM, published as 'module-bom' artifact
// Here must be all dependency versions, used by all modules
dependencies {
    // import other BOMs
    api platform('ru.vyarus.guicey:guicey-bom:5.2.0-1')
    // dependencies
    constraints {
        api 'org.webjars:webjars-locator:0.40'
    }

    // add subprojects to BOM
    project.subprojects.each {
        api it
    }
}

// without it root project's name and description would be used
pom {
    name 'test-bom'
    description 'Test project BOM'
}

publishing {
    publications {
        bom(MavenPublication) {
            // artifact name required for published artifact instead of root project name
            artifactId = 'test-bom'
            from components.javaPlatform
        }
    }
}

// maven pom related configuration applied to all projects (including root)
allprojects {
    
    repositories { mavenCentral(); mavenLocal() }

    group = 'com.sample'
    
    // delay required because otherwise pom closure would be applied before java plugin activation
    afterEvaluate {
        // general pom info, required for all poms (including BOM)
        pom {
            developers {
                developer {
                    id 'johnd'
                    name 'John Doe'
                    email 'johnd@somemail.com'
                }
            }
        }
    }

    // disable gradle metadata publishing (because it confuse and cause problems)
    tasks.withType(GenerateModuleMetadata).configureEach {
        enabled = false
    }
}

// all sub-modules are java normal modules, using root project as bom
subprojects {
    apply plugin: 'java'
    apply plugin: 'ru.vyarus.pom'
    
    sourceCompatibility = 1.8

    // common dependencies for all modules
    dependencies {
        // use versions declared in the root module
        implementation platform(project(':'))

        compileOnly 'com.github.spotbugs:spotbugs-annotations:4.2.3'
        implementation 'org.slf4j:slf4j-api'
    }

    // use only direct dependencies in the generated pom, removing BOM
    pomGeneration {
        removeDependencyManagement()
    }
 
    publishing {
        publications {
            mavenJava(MavenPublication) {
                from components.java
            }
        }
    }
}
```

Suppose there are one module (settings.gradle):

```groovy
include 'test'
```

In that module we ony declare required dependencies:

```groovy
dependencies {
    implementation 'org.webjars:webjars-locator'
}
```

Now if we generate poms (`generatePomFileForBomPublication` and `generatePomFileForMavenPublication`) the root project's bom would be (`com.sample:test-bom` artifact):

```xml
<dependencyManagement>
    <dependencies>
        <!-- constraints -->
        <dependency>
            <groupId>org.webjars</groupId>
            <artifactId>webjars-locator</artifactId>
            <version>0.40</version>
        </dependency>
        <!-- imported BOM -->
        <dependency>
            <groupId>ru.vyarus.guicey</groupId>
            <artifactId>guicey-bom</artifactId>
            <version>5.2.0-1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- module -->
        <dependency>
            <groupId>com.sample</groupId>
            <artifactId>test</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

And pom for modules (`com.sample:test` artifact):

```xml
<dependencies>
    <!-- compileOnly dependency does not appear here -->
    <!-- dependency declared for all modules -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.30</version>
        <scope>compile</scope>
    </dependency>
    <!-- dependency declared in module -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>webjars-locator</artifactId>
        <version>0.40</version>
    </dependency>
</dependencies>
```

(alternatively, `publishToMavenLocal` could be used to look generated artifacts in local maven repo)

The complete multi-module project example could be generated with [java-library generator](https://github.com/xvik/generator-lib-java).

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - project documentation generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
