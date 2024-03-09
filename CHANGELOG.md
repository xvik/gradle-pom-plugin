### 3.0.0 (2024-03-09)
* (BREAKING) Drop gradle 5 and 6 support
    - remove compile and runtime configurations support (were deprecated, now removed in gradle) 
* (BREAKING) Conventions merged with extension (due to conventions deprecation):
    - pomGeneration extension renamed to maven
    - old pom convention moved to maven.withPom (exactly the same as before)
    - old withPomXml convention moved to maven.withPomXml, but it's now an Action<XmlProvider>
      (same as publication.pom.withXml) and so asNode() must now be called manually (instead of it)
* Add type-safe pom configuration: maven.pom. This is exactly the same as publication.pom (compatible with kotlin)
* Add debug option (maven.debug()) to show performed xml modifications
    - Pom model diff (for maven.pom configurations) shown only for gralde 8.4 and above
* Configuration cache compatibility
    
Migration: 
- Rename pomGeneration{} into maven{}
- Rename pom{} into maven.withPom{} (exactly the same configuration) 
    - Consider moving to type safe maven.pom - in this case all properties MUST contain '='
      (this would be EXACTLY the same as pom configuration in publication)
- Rename withPomXml{} into maven.withPomXml{} and replace 'it' with 'asNode()'

([full migration doc](https://github.com/xvik/gradle-pom-plugin?tab=readme-ov-file#migration-from-2x-to-3))

### 2.2.2 (2022-11-09)
* Fix fail when repositories declared in settings file only (#35)
  Now, when forceVersions() enabled, plugin will check versions in the generated pom 
  and fail only if any versions would be missed (with a list of artefacts)

### 2.2.1 (2021-10-28)
* Incorrect usage detection: fail if pom or withPomXml closure applied to upper project convention
  This could happen in allprojects section when java plugin applied lately (e.g. in subprojects section)
  and pom closure applied to root project's convention (that's how conventions work) 

### 2.2.0 (2021-06-21)
* Java plugin not required for activation anymore: main features activated with 
  maven-publish plugin (as before, maven-publish registered automatically for java plugin)
* Support usage with java-platform plugin (used for BOM declaration):
    - maven-publish plugin would be activated automatically
    - no additional configurations created (and no scopes auto-fixing applied)
* Put all imported BOMs at the top of dependencyManagement section
  (by default java-platform plugin puts all BOMs last; unifies behaviour with spring plugin)
* New configuration closure: `pomGeneration`
    - disableScopesCorrection() - disables scope corrections applied by the plugin (to see native behaviour)
    - disableBomsReorder() - disables putting all boms at the top (to see native behaviour)  
    - forceVersions() - always put dependency version even when boms used (gradle platforms)
    - removeDependencyManagement() - removes dependencyManagement section from pom (generated for used platforms)
        automatically forces versions (otherwise pom would become invalid).
        Option assumed to be used with gradle platforms to not "leak" platform into generated poms.
        Compatible with spring dependency management plugin
* Allow using multiple pom and withPomXml configurations. Useful in multi-module projects
  (for example, to declare common configuration in allprojects section and specific configurations in subproject itself)

### 2.1.0 (2020-01-19)
* Partially reverting 1.3 behavior: add `optional` and `provided` configurations because it appears that it's not possible in gradle to completely replace them
    - Now `optinoal` and `provided` would be included into `implementation` configuration (and not `compile` as before),
        but this will not change anything from usage perspective 
    - `compileOnly` dependencies no more added as provided (they are removed as before)!
    - These configurations would be available even with `java-library` plugin (in 1.3 they were not)          

Changes comparing to 1.3.0:
- Drop java 7 support
- Require gradle 5 or above (remove stable publishing activation, assume its enabled)
- `implementation` extends `optional` and `provided`, not `compile`
- Support war plugin: `providedCompile` and `providedRuntime` dependencies scope changed to provided (from compile) 

Versions 2.0.0 and 2.0.1 are considered now as failed experiment of relying only on gradle features

### 2.0.1 (2020-01-19) DON'T USE
* Fix compileOnly dependencies managed by spring BOM plugin (without version) generation in pom

### 2.0.0 (2020-01-17) DON'T USE
* (breaking) Drop java 7 support
* (breaking) Require gradle 5 or above (stable publishing automatic enabling removed as 5.0 enables it by default)
* (breaking) Removed provided configuration: compileOnly must be used instead
    - (breaking) Plugin will now add compileOnly dependencies to the resulted pom (in provided scope) 
        because in some cases it may be important to know exact versions by looking on pom
* (breaking) Removed optional configuration: [gradle feature variants](https://docs.gradle.org/5.6.4/userguide/feature_variants.html#header) must be used instead
* Support war plugin: providedCompile and providedRuntime dependencies scope changed to provided (from compile) 

### 1.3.0 (2018-07-09)
* Support new (gradle 4.8) [publishing behaviour](https://docs.gradle.org/4.8/userguide/publishing_maven.html#publishing_maven:deferred_configuration):
    - Plugin requires gradle 4.6 or above (will fail on earlier gradle).
    - Gradle 4.6, 4.7 - legacy mode (as before)    
    - (breaking) Gradle 4.8, <5.0 - automatically enables STABLE_PUBLISHING preview flag 
        in order to avoid "Cannot configure the 'publishing' extension" errors 
        (this was done to imporve stability as such errors are extremely hard to debug). 
        Warning message shown in console about enabled flag. 
        Message is not shown when flag enabled manually in settings.gradle
    - Gradle 5.0 and above - assume stable publishing enabled by default (no flag enabling, no warning message)        
              
Important: when STABLE_PUBLISHING is enabled (gradle 4.8 and above) publishing configurations will NOT work 
in a lazy way as before. Use `afterEvaluate {}` INSIDE publication configuration in order to configure lazy properties               

### 1.2.0 (2017-08-15)
* Fix gradle 4 compatibility: correct runtime dependencies scope 
* Support java-library plugin: 
    - do not register extra configurations (provided, optional) if java-library registered
    - change scope of implementation dependencies to compile in the generated pom

### 1.1.0 (2016-09-04)
* Fix properties tag in pom closure
* Fix non string values support in pom closure
* Add workaround for tag name clashing with project method names: prefix '_' (prefix removed on xml merging)
* Add manual xml modification closure: withPomXml {} (the same as maven-publish withXml {}, but with Node passed as parameter)

### 1.0.3 (2016-07-29)
* Fix repeated tags render like developers (#1)

### 1.0.2 (2016-05-20)
* Pom dependencies scopes update fixed:
    - use both group and artifact for matching
    - avoid marking as provided or optional transitive dependencies which also directly declared in compile

### 1.0.1 (2015-12-04)
* Pom closure value now properly merged into existing pom (avoid duplicate tags)

### 1.0.0 (2015-11-20)
* Initial release