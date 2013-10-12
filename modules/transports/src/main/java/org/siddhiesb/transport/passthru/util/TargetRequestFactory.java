/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.siddhiesb.transport.passthru.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.siddhiesb.common.api.CommonAPIConstants;
import org.siddhiesb.common.api.PassThruContext;
import org.siddhiesb.transport.passthru.PassThroughConstants;
import org.siddhiesb.transport.passthru.TargetRequest;
import org.siddhiesb.transport.passthru.config.TargetConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public class TargetRequestFactory {
    
	private static Log log = LogFactory.getLog(TargetRequestFactory.class);

    public static TargetRequest create(PassThruContext passThruContext,
                                       HttpRoute route, 
                                       TargetConfiguration configuration) {
        try {
            String epAddress = (String)passThruContext.getProperty(CommonAPIConstants.ENDPOINT);
            String httpMethod = (String)passThruContext.getProperty("HTTP_METHOD");
            boolean hasEntityBody = true;
            URL url = new URL(epAddress);
            TargetRequest request = new TargetRequest(configuration, route, url, httpMethod, hasEntityBody);

            Map headersMap = (Map) passThruContext.getProperty(PassThroughConstants.HTTP_HEADERS);

            /*Add all incoming headers to the request*/
            Set<Map.Entry<String, String>> headerEntries = headersMap.entrySet();
            for (Map.Entry<String, String> headerEntry : headerEntries) {
                request.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }

            return request;
        } catch (MalformedURLException e) {
            handleException("Invalid to address : ", e);
        }
        return null;
    }


    private static void handleException(String s, Exception e)  {
        log.error(s, e);
    }
}
