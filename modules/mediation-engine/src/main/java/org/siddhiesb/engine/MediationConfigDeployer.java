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
       /* System.out.println("============ Mediation config deployed ======");
        String executionPlan = "define stream inFlow ( ptcontext string, receivingFlow string, nextFlow string);\n" +
                "from inFlow select ptcontext, 'http://localhost:9000/services/SimpleStockQuoteService' as endpoint, receivingFlow insert into sender;\n";
        siddhiManager.addExecutionPlan(executionPlan);*/
        deploy(new File("/home/kasun/development/wso2/wso2src/git/siddhi-esb/repository/mediation-config/siddhi_esb_config.siddhiql"));
        return true;
    }

    public boolean deploy(File mediationConfig) {
        try {
            Scanner fileScanner = new Scanner(mediationConfig);
            StringBuffer executionPlan = new StringBuffer();
            while (fileScanner.hasNextLine()) {
                executionPlan.append(fileScanner.nextLine());
            }
            System.out.println("==== Mediation config hot deployed ====");
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
