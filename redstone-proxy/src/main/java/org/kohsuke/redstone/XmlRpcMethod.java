package org.kohsuke.redstone;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the XML-RPC method name to be sent on the wire.
 *
 * <p>
 * If specified on the type and the method doesn't have this annotation,
 * the method name is inferred as "SPECIFIEDNAME.methodName". This convention
 * allows the object name portion to be specified on the type.
 *
 * @author Kohsuke Kawaguchi
 */
@Documented
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface XmlRpcMethod {
    String value();
}
