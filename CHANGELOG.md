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