/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.pmmlrecommendation.utils;

import org.drools.core.command.runtime.pmml.ApplyPmmlModelCommand;
import org.kie.api.KieServices;
import org.kie.api.command.KieCommands;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.runtime.ExecutionResults;
import org.kie.pmml.evaluator.core.utils.PMMLRequestDataBuilder;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class PMMLUtils {

    private static final Logger logger = LoggerFactory.getLogger(PMMLUtils.class);
    private static final String CONTAINER_ID = "Recommender-1.0";
    private static final long EXTENDED_TIMEOUT = 300000L;
    private static final String MODEL_NAME = "KMeans";
    private static final String OUTPUT_FIELD = "cluster";

    private PMMLUtils() {
    }

    public static int getClusterId(int[] buyedItems) {
        logger.info("getClusterId {}", buyedItems);
        Map<String, Object> inputData = new HashMap<>();
        for (int i = 0; i < buyedItems.length ; i ++) {
            inputData.put(String.valueOf(i), (double) buyedItems[i]);
        }
        PMML4Result pmml4Result = evaluate(inputData, MODEL_NAME);
        logger.info("pmml4Result {}", pmml4Result);
        String clusterIdName = (String) pmml4Result.getResultVariables().get(OUTPUT_FIELD);
        return Integer.parseInt(clusterIdName);
    }

    private static PMML4Result evaluate(final Map<String, Object> inputData, final String modelName) {
        logger.info("evaluate with remote invocation {}", modelName);
        final PMMLRequestData pmmlRequestData = getPMMLRequestData(modelName, inputData);
        final ServiceResponse<ExecutionResults> results = getResults(pmmlRequestData);
        return (PMML4Result) results.getResult().getValue("results");
    }

    private static PMMLRequestData getPMMLRequestData(String modelName, Map<String, Object> parameters) {
        String correlationId = "CORRELATION_ID";
        PMMLRequestDataBuilder pmmlRequestDataBuilder = new PMMLRequestDataBuilder(correlationId, modelName);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object pValue = entry.getValue();
            Class class1 = pValue.getClass();
            pmmlRequestDataBuilder.addParameter(entry.getKey(), pValue, class1);
        }
        return pmmlRequestDataBuilder.build();
    }

    private static ServiceResponse<ExecutionResults> getResults(PMMLRequestData pmmlRequestData) {
        logger.info("invoke remote KieServer with {}", pmmlRequestData);
        KieServicesClient kieServicesClient = getKieServicesClient();
        RuleServicesClient ruleClient = kieServicesClient.getServicesClient(RuleServicesClient.class);
        KieCommands commandsFactory = KieServices.Factory.get().getCommands();
        ApplyPmmlModelCommand command = (ApplyPmmlModelCommand) commandsFactory.newApplyPmmlModel(pmmlRequestData);
        return ruleClient.executeCommandsWithResults(CONTAINER_ID, command);
    }

    private static KieServicesClient getKieServicesClient() {
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration("http://localhost:8080/kie" +
                        "-server/services" +
                        "/rest/server"
                , "kieserver", "kieserver1!", EXTENDED_TIMEOUT);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);
        return KieServicesFactory.newKieServicesClient(configuration);
    }
}
