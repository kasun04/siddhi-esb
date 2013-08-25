/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.siddhiesb.transport.passthru.workerpool;

import org.siddhiesb.transport.passthru.workerpool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A container which will hold {@link org.siddhiesb.transport.passthru.workerpool.ThreadCleanup} objects
 */
public final class ThreadCleanupContainer {
    private static List<org.siddhiesb.transport.passthru.workerpool.ThreadCleanup> cleanupList = new ArrayList<org.siddhiesb.transport.passthru.workerpool.ThreadCleanup>();

    /**
     * Add a new ThreadCleanup
     *
     * @param cleanup
     */
    public static void addThreadCleanup(org.siddhiesb.transport.passthru.workerpool.ThreadCleanup cleanup) {
        cleanupList.add(cleanup);
    }

    /**
     * Call cleanup ThreadCleanup.cleanup on all registered ThreadCleanups
     */
    public static void cleanupAll() {
        for (org.siddhiesb.transport.passthru.workerpool.ThreadCleanup threadCleanup : cleanupList) {
            threadCleanup.cleanup();
        }
    }
}
