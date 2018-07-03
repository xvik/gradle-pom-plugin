* Gradle 4.8 compatibility (support new [publishing behaviour](https://docs.gradle.org/4.8/userguide/publishing_maven.html#publishing_maven:deferred_configuration)):
    - Requires gradle 4.8 or above (fail with usage error on earlier gradle) 
    - (breaking) Automatically enables STABLE_PUBLISHING preview flag 
        (in order to avoid "Cannot configure the 'publishing' extension" errors) 
        with warning message in console. Message is not shown when flag enabled manually in settings.gradle
        
Note that STABLE_PUBLISHING is not required by plugin itself (it may work without it),
but there is a high chance of problems (caused by other gradle plugins (like behaviour change in JavaGradlePlugin)).
So option is set to avoid as much problems as possible with publications. 
You may need to rewrite publication configurations with afterConfiguration block applied inside (to configure some properties lazily)
In gradle 5.0 option will be enabled by default (and warning will not be shown anymore).                  

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