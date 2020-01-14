* (breaking) Require gradle 5 or above (stable publishing automatic enabling removed as 5.0 enables it by default)

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