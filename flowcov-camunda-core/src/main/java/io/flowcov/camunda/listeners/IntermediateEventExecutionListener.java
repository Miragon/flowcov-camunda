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

package io.flowcov.camunda.listeners;

import io.flowcov.camunda.junit.rules.CoverageTestRunState;
import io.flowcov.camunda.model.CoveredFlowNode;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.repository.ProcessDefinition;

/**
 * Execution listener registering intermediate events.
 */
public class IntermediateEventExecutionListener implements ExecutionListener {

    private CoverageTestRunState coverageTestRunState;

    public IntermediateEventExecutionListener(CoverageTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        final CoveredFlowNode coveredFlowNode = createCoveredFlowNode(execution);

        final String eventName = execution.getEventName();
        if (eventName.equals(ExecutionListener.EVENTNAME_START)) {

            coverageTestRunState.addCoveredElement(coveredFlowNode);

        } else if (eventName.equals(ExecutionListener.EVENTNAME_END)) {

            coverageTestRunState.endCoveredElement(coveredFlowNode);
        }

    }

    private CoveredFlowNode createCoveredFlowNode(DelegateExecution execution) {

        // Get the process definition in order to obtain the key
        final RepositoryService repositoryService = execution.getProcessEngineServices().getRepositoryService();
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(
                execution.getProcessDefinitionId()).singleResult();

        final String currentActivityId = execution.getCurrentActivityId();

        final CoveredFlowNode coveredFlowNode = new CoveredFlowNode(processDefinition.getKey(), currentActivityId);

        return coveredFlowNode;
    }

}
