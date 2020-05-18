package io.smallrye.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.jdeparser.FormatPreferences;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JDocComment;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JExprs;
import org.jboss.jdeparser.JFiler;
import org.jboss.jdeparser.JMethodDef;
import org.jboss.jdeparser.JMod;
import org.jboss.jdeparser.JSourceFile;
import org.jboss.jdeparser.JSources;

/**
 * Generator of the common {@code SmallRyeInfo} class.
 */
@Mojo(name = "generate-info", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SmallRyeInfoMojo extends AbstractMojo {
    /**
     * The version of the specification being implemented.
     */
    @Parameter(required = true)
    private String specVersion;

    /**
     * The version of the implementation of the specification.
     */
    @Parameter(required = true, defaultValue = "${project.version}")
    private String implementationVersion;

    /**
     * The package name of the info class, e.g. {@code io.smallrye.foo}.
     */
    @Parameter(required = true)
    private String packageName;

    /**
     * The name of the information class. Normally should be left at the default of {@code SmallRyeInfo}.
     */
    @Parameter(required = true, defaultValue = "SmallRyeInfo")
    private String className;

    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/smallrye-info")
    private String sourceOutput;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private static final Pattern versionPattern = Pattern.compile("([0-9]+)(?:\\.([0-9]+)(?:\\.([0-9]+))?)?(-SNAPSHOT)?");

    /**
     * Execute the operation.
     *
     * @throws MojoExecutionException if the sources could not be generated
     */
    @Override
    public void execute() throws MojoExecutionException {
        // just do a simple parse of the versions
        final Matcher specMatcher = versionPattern.matcher(specVersion);
        if (!specMatcher.matches()) {
            throw new MojoExecutionException(
                    "The specification version \"" + specVersion + "\" does not match the pattern: " + versionPattern);
        }
        final int specMajor = Integer.parseInt(specMatcher.group(1));
        final int specMinor = Integer.parseInt(Optional.ofNullable(specMatcher.group(2)).orElse("0"));
        final int specMicro = Integer.parseInt(Optional.ofNullable(specMatcher.group(3)).orElse("0"));
        final boolean specSnapshot = specMatcher.group(4) != null;
        final Matcher implMatcher = versionPattern.matcher(implementationVersion);
        if (!implMatcher.matches()) {
            throw new MojoExecutionException("The implementation version \"" + implementationVersion
                    + "\" does not match the pattern: " + versionPattern);
        }
        final int implMajor = Integer.parseInt(implMatcher.group(1));
        final int implMinor = Integer.parseInt(Optional.ofNullable(implMatcher.group(2)).orElse("0"));
        final int implMicro = Integer.parseInt(Optional.ofNullable(implMatcher.group(3)).orElse("0"));
        final boolean implSnapshot = implMatcher.group(4) != null;

        final File sourceOutputFile = new File(sourceOutput);
        sourceOutputFile.mkdirs();
        project.addCompileSourceRoot(sourceOutput);
        final JSources sources = JDeparser.createSources(JFiler.newInstance(sourceOutputFile), new FormatPreferences());
        final JSourceFile sourceFile = sources.createSourceFile(packageName, className);
        final JClassDef classDef = sourceFile._class(JMod.PUBLIC | JMod.FINAL, className);
        final JDocComment classDoc = classDef.docComment();
        classDoc.text("Information about the version of this module.");
        classDef.constructor(JMod.PRIVATE);

        JMethodDef method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getSpecMajorVersion");
        method.docComment().text("Get the specification major version.")._return().text("the specification major version");
        method.body()._return(JExprs.decimal(specMajor));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getSpecMinorVersion");
        method.docComment().text("Get the specification minor version.")._return().text("the specification minor version");
        method.body()._return(JExprs.decimal(specMinor));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getSpecMicroVersion");
        method.docComment().text("Get the specification micro version.")._return().text("the specification micro version");
        method.body()._return(JExprs.decimal(specMicro));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, boolean.class, "isSpecSnapshot");
        method.docComment().text("Determine whether the specification is a snapshot.")
                ._return()
                .inlineDocTag("code", "true").text(" if the specification is a snapshot, or ")
                .inlineDocTag("code", "false").text(" otherwise");
        method.body()._return(specSnapshot ? JExpr.TRUE : JExpr.FALSE);

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getImplMajorVersion");
        method.docComment().text("Get the implementation major version.")._return().text("the implementation major version");
        method.body()._return(JExprs.decimal(implMajor));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getImplMinorVersion");
        method.docComment().text("Get the implementation minor version.")._return().text("the implementation minor version");
        method.body()._return(JExprs.decimal(implMinor));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getImplMicroVersion");
        method.docComment().text("Get the implementation micro version.")._return().text("the implementation micro version");
        method.body()._return(JExprs.decimal(implMicro));

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, boolean.class, "isImplSnapshot");
        method.docComment().text("Determine whether the implementation is a snapshot.")
                ._return()
                .inlineDocTag("code", "true").text(" if the implementation is a snapshot, or ")
                .inlineDocTag("code", "false").text(" otherwise");
        method.body()._return(implSnapshot ? JExpr.TRUE : JExpr.FALSE);

        // version of the info tool itself

        method = classDef.method(JMod.PUBLIC | JMod.STATIC, int.class, "getInfoVersion");
        method.docComment().text("Get the SmallRye information class API version.  Use this property to"
                + " determine what methods are available on this class.")._return().text("the version");
        method.body()._return(JExprs.decimal(1));

        try {
            sources.writeSources();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write generated sources", e);
        }
    }
}
