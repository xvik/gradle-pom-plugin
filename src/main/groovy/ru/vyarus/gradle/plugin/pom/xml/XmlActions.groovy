package ru.vyarus.gradle.plugin.pom.xml

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.XmlProvider
import ru.vyarus.gradle.plugin.pom.PomExtension

import static ru.vyarus.gradle.plugin.pom.PomPlugin.OPTIONAL
import static ru.vyarus.gradle.plugin.pom.PomPlugin.PROVIDED

/**
 * All these methods are called within `withXml` closure and all such methods must be static for proper
 * configuration cache support.
 *
 * @author Vyacheslav Rusakov
 * @since 01.03.2024
 */
@CompileStatic(TypeCheckingMode.SKIP)
@SuppressWarnings(['Println', 'ParameterCount'])
class XmlActions {

    private static final String COMPILE = 'compile'

    /**
     * Gradle sets runtime scope for all dependencies and this has to be fixed.
     *
     * @param project project instance
     * @param pomXml pom xml
     */
    @SuppressWarnings('MethodSize')
    static void fixPomDependenciesScopes(String projectName, PomExtension extension, String pubName,
                                         Node pomXml, ConfigurationsModel configurations) {
        Node dependencies = pomXml.dependencies[0]
        Closure correctDependencies = { List<ConfigurationsModel.Dep> deps, Closure action ->
            if (!dependencies || deps.empty) {
                // avoid redundant searches (if no deps in xml or no artifacts in configuration)
                // for example, this may arise if gradleApi() used as dependency
                return
            }
            dependencies.dependency.findAll {
                deps.find { ConfigurationsModel.Dep dep ->
                    boolean res = dep.group == it.groupId.text() && dep.name == it.artifactId.text()
                    if (res && extension.debug) {
                        println("\t - $dep.group:$dep.name:$dep.version (original scope: ${it.scope.text()})")
                    }
                    res
                }
            }.each(action)
            // separate debug logs for configurations
            if (extension.debug) {
                println()
            }
        }
        Closure correctScope = { List<ConfigurationsModel.Dep> deps, String requiredScope ->
            if (!deps.empty) {
                debug(projectName, extension.debug, pubName, "Correct $requiredScope dependencies")
            }
            correctDependencies(deps) {
                it.scope*.value = requiredScope
            }
        }

        // corrections may be disabled but optional and provided configurations have to be always "corrected"
        boolean fixScopes = !extension.disabledScopesCorrection

        if (fixScopes) {
            correctScope(configurations.implementation, COMPILE)
        }

        // OPTIONAL
        if (!configurations.optional.empty) {
            debug(projectName, extension.debug, pubName, 'Correct optional dependencies')
        }
        correctDependencies(configurations.optional) {
            it.scope*.value = COMPILE
            it.appendNode(OPTIONAL, true.toString())
        }

        // PROVIDED
        correctScope(configurations.provided, PROVIDED)

        if (fixScopes && configurations.providedCompile) {
            // war plugin configurations by default added as compile, which is wrong
            correctScope(configurations.providedCompile, PROVIDED)
            correctScope(configurations.providedRuntime, PROVIDED)
        }
    }

    static void fixDependencyManagement(String projectName, PomExtension extension, String pubName, Node pomXml) {
        Node dependencyManagement = pomXml.dependencyManagement[0]
        if (!dependencyManagement) {
            return
        }
        if (extension.removedDependencyManagement) {
            debug(projectName, extension.debug, pubName, 'Removing dependency management section')
            pomXml.remove(dependencyManagement)
        } else if (!extension.disabledBomsReorder) {
            // reorder BOMs (bubble BOMs to top)
            Node dependencies = dependencyManagement.dependencies[0]
            if (dependencies.children().size() > 1) {
                dependencies.dependency.findAll { it.scope.text() != 'import' }.each {
                    debug(projectName, extension.debug, pubName,
                            "Move ${it.groupId.text()}:${it.artifactId.text()} import at the top")
                    dependencies.remove(it)
                    dependencies.append(it)
                }
            }
        }
    }

    static void applyPomXmlModifiers(String projectName, String projectDesc, String projectId, PomExtension extension,
                                     String pubName, XmlProvider pomProvider) {
        Node pom = pomProvider.asNode()
        if (!extension.rawConfigs.empty) {
            debug(projectId, extension.debug, pubName, "Apply ${extension.rawConfigs.size()} withXml closures")
        }
        extension.rawConfigs.each { XmlMerger.mergePom(pom, it) }

        if (!extension.xmlModifiers.empty) {
            debug(projectId, extension.debug, pubName,
                    "Apply ${extension.xmlModifiers.size()} withPomXml customizations")
        }
        extension.xmlModifiers.each { it.execute(pomProvider) }
        // apply defaults if required
        if (!pom.name) {
            debug(projectId, extension.debug, pubName, 'Apply default name')
            pom.appendNode('name', projectName)
        }
        if (projectDesc && !pom.description) {
            debug(projectId, extension.debug, pubName, 'Apply default description')
            pom.appendNode('description', projectDesc)
        }
    }

    static void validateVersions(Node pomXml) {
        Node deps = pomXml.dependencies[0]
        if (!deps) {
            return
        }

        List<String> errors = []
        deps.dependency.findAll {
            if (!it.version.text()) {
                errors.add("\t${it.groupId.text()}:${it.artifactId.text()}\n")
            }
        }
        if (errors) {
            throw new GradleException('No versions resolved for the following dependencies. Most likely, there are ' +
                    'no required repositories declared. Declare missed repositories with, for example: ' +
                    'repositories { mavenCentral() }\n' + errors.join(''))
        }
    }

    static void debug(String projectName, boolean isDebug, String publicationName, String message) {
        if (isDebug) {
            String prj = projectName ? " (in ${XmlUtils.CYAN}$projectName${XmlUtils.RESET})" : ''
            println "POM> $message for ${XmlUtils.PURPLE}$publicationName${XmlUtils.RESET} publication$prj"
        }
    }

    static void printDiff(String projectName, boolean isDebug, String publicationName,
                          String message, String before, String after) {
        String diff = XmlUtils.diffShifted(before, after, '\t')
        if (diff.empty) {
            debug(projectName, isDebug, publicationName, 'No xml modifications detected')
        } else {
            println()
            debug(projectName, isDebug, publicationName, "--------------------------------- $message")
            println diff
            println '     --------------------------------'
        }
    }
}
