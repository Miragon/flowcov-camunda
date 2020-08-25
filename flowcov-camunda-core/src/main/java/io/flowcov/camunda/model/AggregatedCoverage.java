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

package io.flowcov.camunda.model;

import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.Set;

/**
 * A coverage that may have multiple deployed process definitions.
 */
public interface AggregatedCoverage {

    /**
     * Retrieves covered flow node IDs for the given process definition key.
     *
     * @param processDefinitionKey
     * @return
     */
    Set<String> getCoveredFlowNodeIds(String processDefinitionKey);

    /**
     * Retrieves covered flow nodes for the given process definition key.
     *
     * @param processDefinitionKey
     * @return
     */
    Set<CoveredFlowNode> getCoveredFlowNodes(String processDefinitionKey);

    /**
     * Retrieves covered sequence flow IDs for the given process definition key.
     *
     * @param processDefinitionKey
     * @return
     */
    Set<String> getCoveredSequenceFlowIds(String processDefinitionKey);

    /**
     * Retrieves covered sequence flow IDs for the given process definition key.
     *
     * @param processDefinitionKey
     * @return
     */
    Set<CoveredSequenceFlow> getCoveredSequenceFlows(String processDefinitionKey);

    /**
     * Retrieves the process definitions of the coverage.
     *
     * @return
     */
    Set<ProcessDefinition> getProcessDefinitions();

    /**
     * Retrives the coverage percentage for all process definitions.
     *
     * @return
     */
    double getCoveragePercentage();

    /**
     * Retrieves the coverage percentage for the given process definition key.
     *
     * @param processDefinitionKey
     * @return
     */
    double getCoveragePercentage(String processDefinitionKey);
}
