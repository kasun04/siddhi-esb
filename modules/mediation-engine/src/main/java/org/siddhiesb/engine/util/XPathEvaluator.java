package org.siddhiesb.engine.util;

import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.transport.passthru.PassThroughConstants;
import org.siddhiesb.transport.passthru.Pipe;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.executor.expression.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.expression.ExpressionExecutor;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.query.api.definition.Attribute;

import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;


import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;


@SiddhiExtension(namespace = "siddhiESB", function = "evalXPath")
    public class XPathEvaluator extends FunctionExecutor {

    public static final String XPATH_CTX = "$ctx:";
    public static final String XPATH_HTTP_CTX = "$http:";

    private ExpressionExecutor expressionExecutor;

    /*Saxon*/
    private XPathSelector selector;
    private DocumentBuilder documentBuilder;

    private String httpCtxName = "";
    private String ctxName = "";

    @Override
    public void init(Attribute.Type[] types, SiddhiContext siddhiContext) {
        /* ToDo :NPEs FIx this */
        String xpathExpr = ((ConstantExpressionExecutor) attributeExpressionExecutors.get(0)).execute(null).toString();

        if (xpathExpr.startsWith(XPATH_HTTP_CTX)) {
            httpCtxName = xpathExpr.split(":")[1];
        } else if (xpathExpr.startsWith(XPATH_CTX)) {
            ctxName = xpathExpr.split(":")[1];

        } else {
            /*Saxon*/
            Processor proc = new Processor(false);
            XPathCompiler xpath = proc.newXPathCompiler();

            documentBuilder = proc.newDocumentBuilder();
            documentBuilder.setDTDValidation(false);

            try {
                selector = xpath.compile(xpathExpr).load();
            } catch (SaxonApiException e) {
                e.printStackTrace();
            }

        }
    }
     
    public void destroy(){

    }
    
    @Override
    protected Object process(Object eventObj) {
        String resultVal = "";
        if (eventObj instanceof InEvent) {
            if (((InEvent) eventObj).getData0() instanceof CommonContext) {
                CommonContext commonContext = (CommonContext) ((InEvent) eventObj).getData0();
                if (httpCtxName != null && !"".equals(httpCtxName)) {
                    Map headerMap = (Map) commonContext.getProperty(PassThroughConstants.HTTP_HEADERS);
                    resultVal = (String)headerMap.get(httpCtxName);
                }  else if (ctxName != null && !"".equals(ctxName)) {
                    resultVal = (String) commonContext.getProperty(ctxName);
                } else {
                    /*Calling Saxon XPath*/
                    resultVal = evaluateSaxonXpath(commonContext).toString();
                }
            }
        }
        return resultVal;
    }

    private Object evaluateSaxonXpath(CommonContext commonContext) {

        String resultStr = "";
        Pipe pipe = (Pipe) commonContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(pipe.getInputStream());
            bufferedInputStream.mark(128 * 1024);
            OutputStream resetOutStream = pipe.resetOutputStream();

            ReadableByteChannel inputChannel = Channels.newChannel(bufferedInputStream);
            WritableByteChannel outputChannel = Channels.newChannel(resetOutStream);
            if (!fastChannelCopy(inputChannel, outputChannel)) {
                try {
                    bufferedInputStream.reset();
                    bufferedInputStream.mark(0);
                    commonContext.setProperty(PassThroughConstants.BUFFERED_INPUT_STREAM, bufferedInputStream);
                } catch (Exception e) {
                }
                return null;
            }
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (Exception e) {
            }
            pipe.setRawSerializationComplete(true);

            XdmValue xdmValue = null;
            try {
                XdmNode doc = documentBuilder.build(new StreamSource(bufferedInputStream));
                selector.setContextItem(doc);

                // Evaluate the expression.
                xdmValue = selector.evaluate();
            } catch (SaxonApiException e) {
                e.printStackTrace();
            }
            if (xdmValue.size() > 0) {
                resultStr = xdmValue.itemAt(0).getStringValue();
            }
        }
        return resultStr;
    }


    public boolean fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        int i = 1;
        int size = 1024 * 8;
        try {
            while (src.read(buffer) != -1) {
                int remains = size - (8 * 1024 * i);
                if (remains < 0) {//remains zero..
                    return false;
                }

                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                dest.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
                i++;

            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Attribute.Type getReturnType() {
        return Attribute.Type.STRING;
    }


    @Override
    public Object execute(AtomicEvent event) {

        return process(event);
    }


    public static void main(String[] args) {

        String ctxFoo = "$ctx:Foo";
        System.out.println("Val : " + ctxFoo.startsWith(XPATH_CTX));


    }
}
