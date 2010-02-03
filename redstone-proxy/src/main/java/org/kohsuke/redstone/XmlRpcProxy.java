/*
    Copyright (c) 2005 Redstone Handelsbolag

    This library is free software; you can redistribute it and/or modify it under the terms
    of the GNU Lesser General Public License as published by the Free Software Foundation;
    either version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License along with this
    library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
    Boston, MA  02111-1307  USA
*/
package org.kohsuke.redstone;

import redstone.xmlrpc.XmlRpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * Proxy implementation for {@link XmlRpcClient} that uses {@link XmlRpcMethod}
 *
 *  @author Kohsuke Kawaguchi
 */
public class XmlRpcProxy implements InvocationHandler
{
    /** The encapsulated XmlRpcClient receiving the converted dynamic calls */
    protected XmlRpcClient client;

    /**
     *  Creates a new dynamic proxy object that implements all the
     *  supplied interfaces.  This object may be type cast to any of
     *  the interface supplied in the call.  Method calls through the
     *  interfaces will be translated to XML-RPC calls to the server
     *  in the supplied url.
     *
     *  @param url The XML-RPC server that will receive calls through
     *             the interfaces.
     *
     *  @param interfaces The list of interfaces the proxy should
     *                    implement.
     *
     *  @return An object implementing the supplied interfaces with
     *          XML-RPC support.
     */

    public static Object createProxy(XmlRpcClient client, Class... interfaces) {
        return Proxy.newProxyInstance(
            interfaces[ 0 ].getClassLoader(),
            interfaces,
            new XmlRpcProxy(client) );
    }

    public XmlRpcProxy(XmlRpcClient client) {
        this.client = client;
    }

    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        // methods like equals, hashCode
        if (method.getDeclaringClass()==Object.class) {
            try {
                return method.invoke(this,args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        return client.invoke( getMethodName(method), args );
    }

    /**
     * Determines the XML-RPC method name.
     */
    protected String getMethodName(Method method) {
        XmlRpcMethod a = method.getAnnotation(XmlRpcMethod.class);
        if (a!=null)    return a.value();

        Class<?> c = method.getDeclaringClass();
        a = c.getAnnotation(XmlRpcMethod.class);
        if (a!=null)    return a.value()+'.'+method.getName();

        return method.getName();
    }
}