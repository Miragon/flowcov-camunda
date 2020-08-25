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

import io.flowcov.camunda.util.CoveredElementComparator;
import lombok.Getter;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.*;

/**
 * Coverage of an individual test method.
 * <p>
 * A test method annotated with @Deployment does an independent deployment of the listed
 * resources, hence this coverage is equivalent to a deployment coverage.
 */
public class MethodCoverage implements AggregatedCoverage {

    /**
     * The ID of the deployment done for the method.
     */
    private final String deploymentId;


    /**
     * The name of the test method.
     */
    @Getter
    private final String name;

    /**
     * Map holding the coverages for each process definition (accessed by the process definition key).
     */
    private Map<String, ProcessCoverage> processDefinitionKeyToProcessCoverage = new HashMap<>();

    public MethodCoverage(String deploymentId, String name) {
        this.deploymentId = deploymentId;
        this.name = name;
    }

    /**
     * Add a process coverage to the method coverage.
     *
     * @param processCoverage
     */
    public void addProcessCoverage(ProcessCoverage processCoverage) {

        final String processDefinitionId = processCoverage.getProcessDefinitionKey();
        processDefinitionKeyToProcessCoverage.put(processDefinitionId, processCoverage);
    }

    /**
     * Add a covered element to the method coverage.
     * The element is added according to the object fields.
     *
     * @param element
     */
    public void addCoveredElement(CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.addCoveredElement(element);
    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param element
     */
    public void endCoveredElement(CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.endCoveredElement(element);
    }

    /**
     * Retrieves the coverage percentage for all process definitions deployed
     * with the method.
     */
    public double getCoveragePercentage() {

        // Aggregate element collections

        final Set<CoveredFlowNode> deploymentCoveredFlowNodes = new HashSet<>();
        final Set<FlowNode> deploymentDefinitionsFlowNodes = new HashSet<FlowNode>();

        final Set<CoveredSequenceFlow> deploymentCoveredSequenceFlows = new HashSet<>();
        final Set<SequenceFlow> deploymentDefinitionsSequenceFlows = new HashSet<SequenceFlow>();

        // Collect defined and covered elements for all definitions in the method deployment
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            // Flow nodes

            final Set<CoveredFlowNode> coveredFlowNodes = processCoverage.getCoveredFlowNodes();
            deploymentCoveredFlowNodes.addAll(coveredFlowNodes);

            final Collection<FlowNode> definitionFlowNodes = processCoverage.getDefinitionFlowNodes();
            deploymentDefinitionsFlowNodes.addAll(definitionFlowNodes);

            // Sequence flows

            final Set<CoveredSequenceFlow> coveredSequenceFlows = processCoverage.getCoveredSequenceFlows();
            deploymentCoveredSequenceFlows.addAll(coveredSequenceFlows);

            final Collection<SequenceFlow> definitionSequenceFlows = processCoverage.getDefinitionSequenceFlows();
            deploymentDefinitionsSequenceFlows.addAll(definitionSequenceFlows);

        }

        // Calculate coverage
        final double coveragePercentage = getCoveragePercentage(
                deploymentCoveredFlowNodes, deploymentDefinitionsFlowNodes,
                deploymentCoveredSequenceFlows, deploymentDefinitionsSequenceFlows);

        return coveragePercentage;
    }

    /**
     * Retrieves the coverage percentage for the given process definition key
     * with the method.
     *
     * @param processDefinitionKey
     */
    public double getCoveragePercentage(String processDefinitionKey) {

        ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        final Set<CoveredFlowNode> coveredFlowNodes = processCoverage.getCoveredFlowNodes();
        final Set<FlowNode> definitionFlowNodes = processCoverage.getDefinitionFlowNodes();

        final Set<CoveredSequenceFlow> coveredSequenceFlows = processCoverage.getCoveredSequenceFlows();
        final Set<SequenceFlow> definitionSequenceFlows = processCoverage.getDefinitionSequenceFlows();

        // Calculate coverage
        final double coveragePercentage = getCoveragePercentage(
                coveredFlowNodes, definitionFlowNodes,
                coveredSequenceFlows, definitionSequenceFlows);

        return coveragePercentage;
    }

    /**
     * Calculates the process coverage percentage according to the passed defined and covered elements.
     *
     * @param coveredFlowNodes         Covered flow nodes possibly from multiple process definitions.
     * @param definitionsFlowNodes     Flow nodes of this test methods deployed process definitions.
     * @param coveredSequenceFlows     Covered sequence flows possibly from multiple process definitions.
     * @param definitionsSequenceFlows Flow nodes of this test methods deployed process definitions,
     * @return Coverage percentage of all process definitions combined.
     */
    private double getCoveragePercentage(Set<CoveredFlowNode> coveredFlowNodes, Set<FlowNode> definitionsFlowNodes,
                                         Set<CoveredSequenceFlow> coveredSequenceFlows, Set<SequenceFlow> definitionsSequenceFlows) {

        final int numberOfDefinedElements = definitionsFlowNodes.size() + definitionsSequenceFlows.size();
        final int numberOfCoveredElemenets = coveredFlowNodes.size() + coveredSequenceFlows.size();

        return (double) numberOfCoveredElemenets / (double) numberOfDefinedElements;

    }

    /**
     * Retrieves the flow nodes of all the process definitions in the method deployment.
     *
     * @return
     */
    public Set<FlowNode> getProcessDefinitionsFlowNodes() {

        final Set<FlowNode> flowNodes = new HashSet<FlowNode>();
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<FlowNode> definitionFlowNodes = processCoverage.getDefinitionFlowNodes();
            flowNodes.addAll(definitionFlowNodes);

        }

        return flowNodes;
    }

    /**
     * Retrieves the flow nodes for the process definition identified by the passed key in the method deployment.
     *
     * @param processDefinitionKey
     * @return
     */
    public Set<FlowNode> getProcessDefinitionsFlowNodes(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        final Set<FlowNode> definitionFlowNodes = processCoverage.getDefinitionFlowNodes();

        return definitionFlowNodes;
    }

    /**
     * Retrieves the sequence flows of all the process definitions in the method deployment.
     *
     * @return
     */
    public Set<SequenceFlow> getProcessDefinitionsSequenceFlows() {

        final Set<SequenceFlow> sequenceFlows = new HashSet<SequenceFlow>();
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<SequenceFlow> definitionSequenceFlows = processCoverage.getDefinitionSequenceFlows();
            sequenceFlows.addAll(definitionSequenceFlows);

        }

        return sequenceFlows;
    }

    /**
     * Retrieves the sequence flows for the process definition identified by the passed key in the method deployment.
     *
     * @param processDefinitionKey
     * @return
     */
    public Set<SequenceFlow> getProcessDefinitionsSequenceFlows(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        final Set<SequenceFlow> sequenceFlows = processCoverage.getDefinitionSequenceFlows();

        return sequenceFlows;
    }

    /**
     * Retrieves a set of covered flow nodes of the process definitions deployed by this test method.
     *
     * @return
     */
    public Set<CoveredFlowNode> getCoveredFlowNodes() {

        final Set<CoveredFlowNode> flowNodes = new TreeSet<CoveredFlowNode>(CoveredElementComparator.instance());
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<CoveredFlowNode> definitionFlowNodes = processCoverage.getCoveredFlowNodes();
            flowNodes.addAll(definitionFlowNodes);

        }

        return flowNodes;
    }

    /**
     * Retrieves a set of covered sequence flows of the process definitions deployed by this test method.
     *
     * @return
     */
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows() {

        final Set<CoveredSequenceFlow> sequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<CoveredSequenceFlow> definitionSequenceFlows = processCoverage.getCoveredSequenceFlows();
            sequenceFlows.addAll(definitionSequenceFlows);

        }

        return sequenceFlows;
    }

    /**
     * Retrieves a set of element IDs of covered flow nodes of the process definition identified by the passed key.
     *
     * @return
     */
    public Set<String> getCoveredFlowNodeIds(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredFlowNodeIds();
    }

    @Override
    public Set<CoveredFlowNode> getCoveredFlowNodes(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredFlowNodes();
    }

    /**
     * Retrieves a set of element IDs of sequence flows of the process definition identified by the passed key.
     *
     * @return
     */
    public Set<String> getCoveredSequenceFlowIds(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredSequenceFlowIds();
    }

    /**
     * Retrieves a set of elements of sequence flows of the process definition identified by the passed key.
     *
     * @return
     */
    @Override
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows(String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredSequenceFlows();
    }

    /**
     * Retrieves the process definitions of the test method's deployment. The process definitions
     * are compared by resource name and not the process key. As a result process
     * definitions having the same process definition key but coming from separate BPMNs
     * may be returned.
     */
    public Set<ProcessDefinition> getProcessDefinitions() {

        final Set<ProcessDefinition> processDefinitions = new TreeSet<ProcessDefinition>(
                new Comparator<ProcessDefinition>() {

                    // Avoid removing process definitions with the same key, but coming from different BPMNs.
                    @Override
                    public int compare(ProcessDefinition o1, ProcessDefinition o2) {
                        return o1.getResourceName().compareTo(o2.getResourceName());
                    }
                });

        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {
            processDefinitions.add(processCoverage.getProcessDefinition());
        }

        return processDefinitions;
    }

    @Override
    public String toString() {

        /*
         * String representation mainly used for junit output and debug.
         */

        StringBuilder builder = new StringBuilder();
        builder.append("Deployment ID: ");
        builder.append(deploymentId);
        builder.append("\nDeployment process definitions:\n");

        // List of process coverage string representations
        for (ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {
            builder.append(processCoverage);
            builder.append('\n');
        }

        return builder.toString();
    }

}
