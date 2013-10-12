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

import org.siddhiesb.common.api.PassThruContext;
import org.siddhiesb.transport.passthru.*;
import org.siddhiesb.transport.passthru.config.SourceConfiguration;

import java.util.Map;

public class SourceResponseFactory {

    public static SourceResponse create(PassThruContext passThruContext,
                                        SourceRequest sourceRequest,
                                        SourceConfiguration sourceConfiguration) {
        int statusCode = 200;//get status code
        SourceResponse sourceResponse =
                new SourceResponse(sourceConfiguration, statusCode, sourceRequest);

        Map headersMap = (Map) passThruContext.getProperty(PassThroughConstants.HTTP_HEADERS);
        addResponseHeader(sourceResponse, headersMap);
        return sourceResponse;
    }

	private static void addResponseHeader(SourceResponse sourceResponse, Map transportHeaders) {
	    for (Object entryObj : transportHeaders.entrySet()) {
	        Map.Entry entry = (Map.Entry) entryObj;
	        if (entry.getValue() != null && entry.getKey() instanceof String &&
	                entry.getValue() instanceof String) {
	            sourceResponse.addHeader((String) entry.getKey(), (String) entry.getValue());
	        }
	    }
    }
    
}
