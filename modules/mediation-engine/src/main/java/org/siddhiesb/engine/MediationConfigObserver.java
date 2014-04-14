package org.siddhiesb.engine;


import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class MediationConfigObserver {
    public static final String configLocation = "./repository/mediation-config";
    public static final long pollingInterval = 15000;

    MediationConfigDeployer configDeployer;

    public void init(MediationConfigDeployer mediationConfigDeployer) {
        configDeployer = mediationConfigDeployer;
        FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(configLocation);
        FileAlterationMonitor monitor =
                new FileAlterationMonitor(pollingInterval);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                System.out.println("FileCreated");
                configDeployer.deploy(file);
            }

            @Override
            public void onFileChange(File file) {
                System.out.println("FileModified");
                configDeployer.deploy(file);
            }

            @Override
            public void onFileDelete(File file) {
                System.out.println("FileDeleated");
                configDeployer.deploy(file);
            }
        };


        try {
            fileAlterationObserver.addListener(listener);
            monitor.addObserver(fileAlterationObserver);
            monitor.start();
        } catch (Exception e) {
        }
    }

}
