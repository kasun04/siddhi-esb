package org.siddhiesb.engine;

import org.siddhiesb.common.api.ConfigDeployerAPI;
import org.wso2.siddhi.core.SiddhiManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class MediationConfigDeployer implements ConfigDeployerAPI {
    SiddhiManager siddhiManager;

    MediationConfigObserver mediationConfigObserver;
    public void init() {
        mediationConfigObserver = new MediationConfigObserver();
        mediationConfigObserver.init(this);
    }

    public boolean deploy() {
        String workingDir = System.getProperty("user.dir");
        deploy(new File( workingDir + "/repository/mediation-config/siddhi_esb_config.siddhiql"));
        return true;
    }

    public boolean deploy(File mediationConfig) {
        try {
            Scanner fileScanner = new Scanner(mediationConfig);
            StringBuffer executionPlan = new StringBuffer();
            while (fileScanner.hasNextLine()) {
                executionPlan.append(fileScanner.nextLine());
            }
            siddhiManager.addExecutionPlan(executionPlan.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        return true;
    }

    public void setSiddhiManager(SiddhiManager siddhiManager) {
        this.siddhiManager = siddhiManager;
    }
}
