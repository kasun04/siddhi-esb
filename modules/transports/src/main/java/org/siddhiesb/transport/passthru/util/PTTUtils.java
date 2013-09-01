package org.siddhiesb.transport.passthru.util;


import java.util.UUID;

public class PTTUtils {

    public static String generateUUID() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString();
    }
}
