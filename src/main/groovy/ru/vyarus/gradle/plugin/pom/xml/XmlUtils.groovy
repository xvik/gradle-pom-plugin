package ru.vyarus.gradle.plugin.pom.xml

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import groovy.transform.CompileStatic
import groovy.xml.XmlUtil
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.publish.maven.internal.tasks.MavenPomFileGenerator

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Function

/**
 * Xml utilities (for debug).
 *
 * @author Vyacheslav Rusakov
 * @since 13.02.2024
 */
@CompileStatic
class XmlUtils {

    public static final String RESET = '\u001B[0m'
    public static final String RED = '\u001B[31m'
    public static final String GREEN = '\u001B[32m'
    public static final String YELLOW = '\u001B[33m'
    public static final String BLUE = '\u001B[34m'
    public static final String PURPLE = '\u001B[35m'
    public static final String CYAN = '\u001B[36m'

    private static final String NL = '\n'

    /**
     * @param node groovy xml node
     * @return xml as string
     */
    static String toString(Node node) {
        return NL + XmlUtil.serialize(node)
                .replaceAll('\r', '')
                // on java 11 groovy inserts blank lines between tags
                .replaceAll('\n {1,}\n', NL)
                .replaceAll(' +\n', NL)
    }

    /**
     * NOTE: works from gradle 8.4 (before MavenPomFileGenerator does not contain static method)!
     *
     * @param node pom model (type-safe)
     * @return xml as string
     */
    static String toString(MavenPom node) {
        MavenPomFileGenerator.MavenPomSpec spec = MavenPomFileGenerator.generateSpec(node as MavenPomInternal)
        Path temp = Files.createTempFile('xml', 'tmp')
        try {
            spec.writeTo(temp.toFile())
            return temp.toFile().text
        } finally {
            Files.delete(temp)
        }
    }

    /**
     * Build strings diff and shift all lines with prefix.
     *
     * @param from base string
     * @param to modified string
     * @param shift prefix to apply for all lines
     * @return shirted diff
     */
    static String diffShifted(String from, String to, String shift) {
        String res = diff(from, to)
        return res.empty ? '' : res.split(NL).collect { shift + it }.join(NL)
    }

    /**
     * Build diff between two strings. Shows only modified lines with 2 more lines above (for context),
     * Also, shows line numbers.
     *
     * @param from base string
     * @param to modified string
     * @return diff string
     */
    @SuppressWarnings(['NestedForLoop', 'DuplicateNumberLiteral'])
    static String diff(String from, String to) {
        List<String> original = Arrays.asList(from.split(NL))
        List<String> revised = Arrays.asList(to.split(NL))

        final String plus = '+'
        final String minus = '-'

        DiffRowGenerator generator = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .mergeOriginalRevised(true)
                .ignoreWhiteSpaces(true)
                .inlineDiffByWord(true)
                .lineNormalizer { it }  // to disable html escapes
                .equalizer(DiffRowGenerator.IGNORE_WHITESPACE_EQUALIZER)
                .oldTag({ it ? minus + RED : RESET + minus } as Function<Boolean, String>)
                .newTag({ it ? plus + GREEN : RESET + plus } as Function<Boolean, String>)
                .build()
        List<DiffRow> rows = generator.generateDiffRows(original, revised)
        List<Integer> showRows = []
        int last = -1
        for (int i = 0; i < rows.size(); i++) {
            DiffRow row = rows.get(i)
            if (DiffRow.Tag.EQUAL != row.tag) {
                // show 2 rows before
                int prev = Math.max(last + 1, i - 2)
                for (int j = prev; j <= i; j++) {
                    showRows.add(j)
                }
                last = i
            }
        }

        StringBuilder res = new StringBuilder()
        int prev = -1
        for (int i : showRows) {
            DiffRow row = rows.get(i)
            if (prev > 0 && prev != i - 1) {
                // between blocks
                res.append(NL)
            }
            res.append(String.format('%4s | ', i)).append(row.oldLine).append(NL)
            prev = i
        }

        return res.toString()
    }
}
