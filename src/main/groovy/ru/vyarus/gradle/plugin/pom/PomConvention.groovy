package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * {@link PomPlugin} extension container. Not used as extension (project.extensions), but as convention
 * (project.conventions) because actual closure is structure free may define any pom sections (free xml).
 * <p>
 * Example usage:
 * <pre>
 * <code>
 *     pom {
 *         licenses {
 *              license {
 *                  name "The MIT License"
 *              }
 *         }
 *         developers {
 *             developer {
 *                 id "dev1"
 *                 name "Dev1 Name"
 *                 email "dev1@email.com"
 *             }
 *         }
 *     }
 * </code>
 * <pre>
 * Multiple declarations could be used.
 * <p>
 * If manual pom modification is required use:
 * <pre><code>
 *     withPomXml {
 *         it.appendNode('description', 'A demonstration of maven POM customization')
 *     }
 * </code></pre>
 * withPomXml convention usage is equivalent to maven-publish plugin withXml closure, but without need to call
 * asNode() because node is already provided as parameter.
 *
 * @author Vyacheslav Rusakov
 * @since 04.11.2015
 */
@CompileStatic
class PomConvention {
    List<Closure> configs = []
    List<Closure> xmlModifiers = []

    // in multi-module project used for incorrect initialization detection when sub module's convention
    // wasn't initialized and root module's convention used (hard to track problems)
    private String projectPath

    PomConvention(Project project) {
        // project name where convention was registered
        projectPath = project.path
    }

    /**
     * @param config user pom
     */
    void pom(Closure config) {
        validateTargetProjectCorrectness('pom', config)
        this.configs.add(config)
    }

    /**
     * Modification closure is called just after user pom merge. Pom xml passed to closure as {@link Node} parameter.
     * @param modifier manual pom modification closure
     */
    void withPomXml(Closure modifier) {
        validateTargetProjectCorrectness('withPomXml', modifier)
        this.xmlModifiers.add(modifier)
    }

    private void validateTargetProjectCorrectness(String type, Closure config) {
        String srcProject = findDeclarationProjectPath(config)
        // catching mistakes of root project configuration from module, but allowing subproject configuration
        // from upper project (last condition): project(':mod').pom { ... }
        if (srcProject && projectPath != srcProject && !projectPath.startsWith(srcProject)) {
            throw new GradleException("${type.capitalize()} closure declared in project '$srcProject'" +
                    " actually applied in '$projectPath' project (conventions are searched through project " +
                    "hierarchy). This means that $type closure was applied too early - before pom plugin created " +
                    'convention in sub module. ' +
                    "Most likely, this is because java plugin applied only in submodules section and this $type " +
                    "closure is in allprojects section. To fix this, wrap $type section with afterEvaluate " +
                    'to delay configuration until subproject convention would be created: \n' +
                    '\tafterEvaluate { \n' +
                    "\t\t$type { ... }\n" +
                    '\t}')
        }
    }

    /**
     * Closures are hierarchical. Need to only find reference to the top closure, representing project.
     *
     * @param config configuration closure (from build)
     * @return project path
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('Instanceof')
    private String findDeclarationProjectPath(Closure config) {
        Object own = config.owner
        while (own instanceof Closure && !own.hasProperty('path')) {
            own = ((Closure) own).owner
        }
        // assuming this to be a project name
        return own?.path
    }
}
