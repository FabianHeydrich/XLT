/*
 * Copyright (c) 2002-2022 Gargoyle Software Inc.
 * Copyright (c) 2005-2022 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.html.xpath;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.xceptance.xlt.engine.util.TimerUtils;

import net.sourceforge.htmlunit.xpath.XPathContext;
import net.sourceforge.htmlunit.xpath.objects.XBoolean;
import net.sourceforge.htmlunit.xpath.objects.XNodeSet;
import net.sourceforge.htmlunit.xpath.objects.XNumber;
import net.sourceforge.htmlunit.xpath.objects.XObject;
import net.sourceforge.htmlunit.xpath.objects.XString;
import net.sourceforge.htmlunit.xpath.xml.utils.PrefixResolver;

/**
 * Collection of XPath utility methods.
 *
 * @author Ahmed Ashour
 * @author Chuck Dumont
 * @author Ronald Brill
 */
public final class XPathHelper {

    private static final ThreadLocal<Boolean> PROCESS_XPATH_ = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    /**
     * Private to avoid instantiation.
     */
    private XPathHelper() {
        // Empty.
    }

    /**
     * Evaluates an XPath expression from the specified node, returning the resultant nodes.
     *
     * @param <T> the type class
     * @param node the node to start searching from
     * @param xpathExpr the XPath expression
     * @param resolver the prefix resolver to use for resolving namespace prefixes, or null
     * @return the list of objects found
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> getByXPath2(final DomNode node, final String xpathExpr,
            final PrefixResolver resolver) {
        if (xpathExpr == null) {
            throw new IllegalArgumentException("Null is not a valid XPath expression");
        }

        PROCESS_XPATH_.set(Boolean.TRUE);
        final List<T> list = new ArrayList<>();
        try {
            final XObject result = evaluateXPath(node, xpathExpr, resolver);

            if (result instanceof XNodeSet) {
                final NodeList nodelist = result.nodelist();
                for (int i = 0; i < nodelist.getLength(); i++) {
                    list.add((T) nodelist.item(i));
                }
            }
            else if (result instanceof XNumber) {
                list.add((T) Double.valueOf(result.num()));
            }
            else if (result instanceof XBoolean) {
                list.add((T) Boolean.valueOf(result.bool()));
            }
            else if (result instanceof XString) {
                list.add((T) result.str());
            }
            else {
                throw new RuntimeException("Unproccessed " + result.getClass().getName());
            }
        }
        catch (final Exception e) {
            throw new RuntimeException("Could not retrieve XPath >" + xpathExpr + "< on " + node, e);
        }
        finally {
            PROCESS_XPATH_.set(Boolean.FALSE);
        }
        return list;
    }

    /**
     * Returns whether the thread is currently evaluating XPath expression or no.
     * @return whether the thread is currently evaluating XPath expression or no
     */
    public static boolean isProcessingXPath() {
        return PROCESS_XPATH_.get().booleanValue();
    }

    /**
     * Evaluates an XPath expression to an XObject.
     * @param contextNode the node to start searching from
     * @param str a valid XPath string
     * @param prefixResolver prefix resolver to use for resolving namespace prefixes, or null
     * @return an XObject, which can be used to obtain a string, number, nodelist, etc (should never be {@code null})
     * @throws TransformerException if a syntax or other error occurs
     */
    private static XObject evaluateXPath(final DomNode contextNode,
            final String str, final PrefixResolver prefixResolver) throws TransformerException {
        final XPathContext xpathSupport = new XPathContext();
        final Node xpathExpressionContext;
        if (contextNode.getNodeType() == Node.DOCUMENT_NODE) {
            xpathExpressionContext = ((Document) contextNode).getDocumentElement();
        }
        else {
            xpathExpressionContext = contextNode;
        }

        PrefixResolver resolver = prefixResolver;
        if (resolver == null) {
            resolver = new HtmlUnitPrefixResolver(xpathExpressionContext);
        }

        final boolean caseSensitive = contextNode.getPage().hasCaseSensitiveTagNames();

        final XPathAdapter xpath = new XPathAdapter(str, resolver, null, caseSensitive);
        final int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);
        return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
    }

    // XC start
    private static final Log log = LogFactory.getLog(XPathHelper.class);

    public static boolean useJaxen = true;

    private static final Pattern pattern = Pattern.compile("(@[a-zA-Z]+)");

    private static String preProcessXPath(String string, Node node)
    {
        // Not a very clean way
        final Matcher matcher = pattern.matcher(string);
        while (matcher.find())
        {
            final String attribute = matcher.group(1);
            string = string.replace(attribute, attribute.toLowerCase());
        }

        return string;
    }

    /**
     * Evaluates an XPath expression from the specified node, returning the resultant nodes.
     *
     * @param <T> the type class
     * @param node the node to start searching from
     * @param xpathExpr the XPath expression
     * @param resolver the prefix resolver to use for resolving namespace prefixes, or null
     * @return the list of objects found
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getByXPath(final DomNode node, final String xpathExpr, 
                                         final PrefixResolver resolver)
    {
        final long start = TimerUtils.getTime();
        
        try
        {
            if (useJaxen)
            {
                if (xpathExpr == null)
                {
                    throw new NullPointerException("Null is not a valid XPath expression");
                }

                try
                {
                    PROCESS_XPATH_.set(Boolean.TRUE);

                    String expr = preProcessXPath(xpathExpr, node);

                    DOMXPath path = new DOMXPath(expr);
                    List<Object> result = path.selectNodes(node);

                    return (List<T>) result;
                }
                catch (JaxenException ex)
                {
                    throw new RuntimeException("Failed to evaluate xpath expression: " + xpathExpr, ex);
                }
                finally
                {
                    PROCESS_XPATH_.set(false);
                }
            }
            else
            {
                return getByXPath2(node, xpathExpr, resolver);
            }
        }
        finally
        {
            if (log.isDebugEnabled())
            {
                final long end = TimerUtils.getTime();

                log.debug(String.format("XPATH Perf %d ms - %s", (end - start), xpathExpr));
            }
        }
    }
    // XC end
}
