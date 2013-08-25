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

import java.net.InetAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Utility methods used by the transport.
 */

public class PassThroughTransportUtils {
    private static Log log = LogFactory.getLog(PassThroughTransportUtils.class);

    /**
     * This method tries to determine the hostname of the given InetAddress without
     * triggering a reverse DNS lookup.  {@link java.net.InetAddress#getHostName()}
     * triggers a reverse DNS lookup which can be very costly in cases where reverse
     * DNS fails. Tries to parse a symbolic hostname from {@link java.net.InetAddress#toString()},
     * which is documented to return a String of the form "hostname / literal IP address"
     * with 'hostname' blank if not already computed & stored in <code>address</code>.
     * <p/>
     * If the hostname cannot be determined from InetAddress.toString(),
     * the value of {@link java.net.InetAddress#getHostAddress()} is returned.
     *
     * @param address The InetAddress whose hostname has to be determined
     * @return hostsname, if it can be determined. hostaddress, if not.          
     */
    public static String getHostName(InetAddress address) {
        String result;
        String hostAddress = address.getHostAddress();
        String inetAddr = address.toString();
        int index1 = inetAddr.lastIndexOf('/');
        int index2 = inetAddr.indexOf(hostAddress);
        if (index2 == index1 + 1) {
            if (index1 == 0) {
                result = hostAddress;
            } else {
                result = inetAddr.substring(0, index1);
            }
        } else {
            result = hostAddress;
        }
        return result;
    }

    /**
     * Whatever this method returns as the IP is ignored by the actual http/s listener when
     * its getServiceEPR is invoked. This was originally copied from axis2
     *
     * @return Returns String.
     * @throws java.net.SocketException if the socket can not be accessed
     */
    public static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";
        while (e.hasMoreElements()) {
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration addresses = netface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }


    private static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

}
