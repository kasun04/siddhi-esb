package org.siddhiesb.engine.util;

import org.apache.synapse.util.xmlstreamingxpath.XMLStreamingXPath;
import org.siddhiesb.common.api.PassThruContext;
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

@SiddhiExtension(namespace = "siddhiESB", function = "evalXPath")
public class XPathEvaluator extends FunctionExecutor {

    private ExpressionExecutor expressionExecutor;
    private XMLStreamingXPath xmlStreamingXPath;

    @Override
    public void init(Attribute.Type[] types, SiddhiContext siddhiContext) {
        expressionExecutor = attributeExpressionExecutors.get(1);

        /* ToDo :NPEs FIx this */
        String xpathExpr = ((ConstantExpressionExecutor) attributeExpressionExecutors.get(0)).execute(null).toString();
        xmlStreamingXPath = new XMLStreamingXPath();

        xmlStreamingXPath.setXpathQuery(xpathExpr);
    }

    @Override
    protected Object process(Object eventObj) {
        String resultVal = "";
        if (eventObj instanceof InEvent) {
            if (((InEvent) eventObj).getData0() instanceof PassThruContext) {
                PassThruContext passThruContext = (PassThruContext) ((InEvent) eventObj).getData0();
                resultVal = evaluateXpath(passThruContext).toString();
            }
        }
        return resultVal;
    }


    private Object evaluateXpath(PassThruContext passThruContext) {
        String resultStr = "";
        Pipe pipe = (Pipe) passThruContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
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
                    passThruContext.setProperty(org.apache.synapse.transport.passthru.PassThroughConstants.BUFFERED_INPUT_STREAM, bufferedInputStream);
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

            resultStr= (String) xmlStreamingXPath.getValueOf(bufferedInputStream);

            /*Call Streaming XPath HERE*/
        }
        return resultStr;
    }

    public boolean fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16*1024);
        int i =1;
        int size = 1024*8;
        try {
            while (src.read(buffer) != -1) {
                int remains =size-(8*1024*i);
                if(remains<0){//remains zero..
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

        String exampleXML =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                        "\n" + "      <getQuote>\n" +
                        "         <request>\n" +
                        "            <symbol>kasun</symbol>\n" +
                        "         </request>\n" +
                        "      </getQuote>";
        String exampleXML2 =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                        "\n" + "      <getQuote>\n" +
                        "         <request>\n" +
                        "            <symbol>kasun</symbol>\n" +
                        "         </request>\n" +
                        "      </getQuote>";

        String exampleXML3 =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                        "\n" + "      <getQuote>\n" +
                        "         <request>\n" +
                        "            <symbol>kasun</symbol>\n" +
                        "         </request>\n" +
                        "      </getQuote>";

        InputStream inputStream = new ByteArrayInputStream(exampleXML.getBytes());
        String xpathString = "/getQuote/request/symbol";
        XMLStreamingXPath xmlStreamingXPath = new XMLStreamingXPath();
        xmlStreamingXPath.setXpathQuery(xpathString);

        String val1 = (String)xmlStreamingXPath.getValueOf(inputStream);
        String val2 = (String) xmlStreamingXPath.getValueOf(new ByteArrayInputStream(exampleXML2.getBytes()));
        String val3 = (String) xmlStreamingXPath.getValueOf(new ByteArrayInputStream(exampleXML3.getBytes()));

        System.out.println("Val " + val1);
        System.out.println("Val " + val2);
        System.out.println("Val " + val3);


    }
}
