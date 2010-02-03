package org.kohsuke.redstone;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import org.kohsuke.redstone.XmlRpcProxy;
import redstone.xmlrpc.XmlRpcClient;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates a type-safe proxy that uses {@link XmlRpcProxy}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Generator {
    private final JCodeModel cm;

    private final Map<String,String> aliases = new HashMap<String, String>();

    public static void main(String[] args) throws IOException, JClassAlreadyExistsException {
        new Generator().generate("http://api.browsershots.org/xmlrpc/", "test.Foo");
    }

    public Generator() {
        this(new JCodeModel());
    }

    public Generator(JCodeModel cm) {
        this.cm = cm;
    }

    /**
     * Returns a live map that specifies type name aliases.
     *
     * <p>
     * According to the XML-RPC spec, there's well-defined finite set of type names that are valid,
     * but in practice many servers exist that return all kinds of random names.
     *
     * <p>
     * The caller of this class can define type name aliases by updating the live map returned from this method,
     * to accomodate such servers.
     */
    public Map<String,String> getAliases() {
        return aliases;
    }

    public JCodeModel getCodeModel() {
        return cm;
    }

    public void generate(String endpoint, String fqcn) throws IOException, JClassAlreadyExistsException {
        JDefinedClass $foo = cm._class(fqcn, ClassType.INTERFACE);

        XmlRpcObject p = (XmlRpcObject)XmlRpcProxy.createProxy(
                new XmlRpcClient(endpoint,false), XmlRpcObject.class);
        for (String name : p.listMethods()) {
            String help = p.methodHelp(name);
            for (List<String> sig : p.methodSignature(name)) {
                JMethod m = $foo.method(0, toType(sig.get(0)), toMethodName(name));
                m.annotate(XmlRpcMethod.class).param("value",name);
                m.javadoc().add(help);

                List<String> params = sig.subList(0, sig.size());
                List<String> names = toParameterNames(name,help,params);
                int i=0;
                for (String param : params)
                    m.param(toType(param),names.get(i++));
            }
        }
    }

    /**
     * Computes the parameter names of the method.
     *
     * Subtypes can override this method to parse parameter names from the help text.
     *
     *
     * @param methodName
     *      The XML-RPC method name.
     * @param help
     *      The method help.
     * @param parameters
     *      Types and numbers of arguments.
     */
    protected List<String> toParameterNames(String methodName, String help, final List<String> parameters) {
        return new AbstractList<String>() {
            @Override
            public String get(int index) {
                return "p"+index;
            }

            @Override
            public int size() {
                return parameters.size();
            }
        };
    }

    /**
     * Massage a XML-RPC method name into a Java method name.
     */
    protected String toMethodName(String name) {
        // ignore everything before '.' if present
        return name.substring(name.lastIndexOf('.')+1);
    }

    /**
     * Converts XML-RPC type into {@link JType}.
     *
     * According to http://xmlrpc-c.sourceforge.net/introspection.html ,  I'm only supposed to support
     * XML element names and its upper case variants, but in practice server seems to return all kinds of values,
     * like "binary", "list", etc.
     */
    public JType toType(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        if (aliases.containsKey(name))
            name = aliases.get(name);

        if (name.equals("i4") || name.equals("int"))
            return cm.INT;
        if (name.equals("boolean") || name.equals("bool"))
            return cm.BOOLEAN;
        if (name.equals("string"))
            return cm.ref(String.class);
        if (name.equals("double"))
            return cm.DOUBLE;
        if (name.equals("dateTime.iso8601"))
            return cm.ref(Calendar.class);
        if (name.equals("struct") || name.equals("dict"))
            return cm.ref(Map.class);
        if (name.equals("array") || name.equals("list"))
            return cm.ref(List.class);
        if (name.equals("base64") || name.equals("binary"))
            return cm.ref(byte[].class);

        throw new IllegalArgumentException("Unknown type: "+name);
    }
}
