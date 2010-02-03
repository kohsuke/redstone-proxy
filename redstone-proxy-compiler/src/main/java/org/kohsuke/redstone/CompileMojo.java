package org.kohsuke.redstone;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.writer.FileCodeWriter;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates type-safe XML-RPC proxy interfaces by using introspection.
 *
 * @goal compile
 * @phase generate-sources
 * @author Kohsuke Kawaguchi
 */
public class CompileMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    public MavenProject project;

    /**
     * URL of the XML-RPC endpoint.
     *
     * @parameter
     * @required
     */
    public String endpoint;

    /**
     * Fully qualified name of the generated class.
     *
     * @parameter
     * @required
     */
    public String className;

    /**
     * Type name aliases.
     *
     * @parameter
     */
    public Map<String,String> typeAliases = new HashMap<String, String>();

    /**
     * POM can use inline Groovy script to define a custom {@link Generator} subtype.
     *
     * @parameter
     */
    public String groovy;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File dir = new File(project.getBuild().getDirectory(),"generated-sources/xml-rpc");
            dir.mkdirs();

            Generator gen = createGenerator();
            gen.getAliases().putAll(typeAliases);

            getLog().info("Generating proxy from "+endpoint);
            gen.generate(endpoint, className);
            gen.getCodeModel().build(new FileCodeWriter(dir));

            project.getCompileSourceRoots().add(dir.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate proxy",e);
        } catch (JClassAlreadyExistsException e) {
            throw new MojoExecutionException("Failed to generate proxy",e);
        }
    }

    protected Generator createGenerator() throws IOException, MojoFailureException {
        if (groovy==null)
            return new Generator();

        Binding binding = new Binding();
        binding.setVariable("project",project);
        binding.setVariable("mojo",this);
        GroovyShell shell = new GroovyShell(binding);

        File asFile = new File(project.getBasedir(),groovy);
        if (asFile.exists())
            return safeCast(shell.evaluate(asFile));
        else
            return safeCast(shell.evaluate(groovy));
    }

    private Generator safeCast(Object o) throws MojoFailureException {
        if (o instanceof Generator) {
            return (Generator) o;
        } else {
            throw new MojoFailureException("Expected a Generator instance but got "+o+" instead");
        }
    }
}
