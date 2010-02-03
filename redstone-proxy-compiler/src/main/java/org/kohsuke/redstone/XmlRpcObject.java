package org.kohsuke.redstone;

import java.util.List;

/**
 * Type-safe proxy for XML-RPC introspection.
 *
 * @author Kohsuke Kawaguchi
 */
@XmlRpcMethod("system")
public interface XmlRpcObject {
    List<String> listMethods();
    String methodHelp(String methodName);
    List<List<String>> methodSignature(String methodName);
}
