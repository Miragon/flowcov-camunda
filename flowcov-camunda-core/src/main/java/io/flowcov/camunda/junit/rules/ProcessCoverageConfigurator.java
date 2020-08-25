/*
 * Copyright 2020 FlowSquad GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.flowcov.camunda.junit.rules;

import io.flowcov.camunda.listeners.CompensationEventCoverageHandler;
import io.flowcov.camunda.listeners.FlowNodeHistoryEventHandler;
import io.flowcov.camunda.listeners.PathCoverageParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.event.EventHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods to configure the process coverage extensions on a given ProcessEngineConfigurationImpl
 */
public class ProcessCoverageConfigurator {

    public static void initializeProcessCoverageExtensions(ProcessEngineConfigurationImpl configuration) {
        initializeFlowNodeHandler(configuration);
        initializePathCoverageParseListener(configuration);
        initializeCompensationEventHandler(configuration);
    }

    private static void initializePathCoverageParseListener(ProcessEngineConfigurationImpl configuration) {
        List<BpmnParseListener> bpmnParseListeners = configuration.getCustomPostBPMNParseListeners();
        if (bpmnParseListeners == null) {
            bpmnParseListeners = new LinkedList<BpmnParseListener>();
            configuration.setCustomPostBPMNParseListeners(bpmnParseListeners);
        }

        bpmnParseListeners.add(new PathCoverageParseListener());
    }

    private static void initializeFlowNodeHandler(ProcessEngineConfigurationImpl configuration) {
        final FlowNodeHistoryEventHandler historyEventHandler = new FlowNodeHistoryEventHandler();
        configuration.setHistoryEventHandler(historyEventHandler);

    }

    private static void initializeCompensationEventHandler(ProcessEngineConfigurationImpl configuration) {
        if (configuration.getCustomEventHandlers() == null) {
            configuration.setCustomEventHandlers(new LinkedList<EventHandler>());
        }

        configuration.getCustomEventHandlers().add(new CompensationEventCoverageHandler());
    }

}
