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
import lombok.val;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.*;
import java.util.stream.Collectors;

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

    private Map<String, DecisionCoverage> decisionKeyToDecisionCoverage = new HashMap<>();


    public MethodCoverage(final String deploymentId, final String name) {
        this.deploymentId = deploymentId;
        this.name = name;
    }

    /**
     * Add a process coverage to the method coverage.
     *
     * @param processCoverage
     */
    public void addProcessCoverage(final ProcessCoverage processCoverage) {

        final String processDefinitionId = processCoverage.getProcessDefinitionKey();
        processDefinitionKeyToProcessCoverage.put(processDefinitionId, processCoverage);
    }

    public void addDecisionCoverage(final DecisionCoverage decisionCoverage) {

        final String decisionDefinitionId = decisionCoverage.getDecisionDefinitionKey();
        decisionKeyToDecisionCoverage.put(decisionDefinitionId, decisionCoverage);
    }

    /**
     * Add a covered element to the method coverage.
     * The element is added according to the object fields.
     *
     * @param element
     */
    public void addCoveredElement(final CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.addCoveredElement(element);
    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param element
     */
    public void endCoveredElement(final CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.endCoveredElement(element);
    }


    public void addCoveredDmnRules(final List<CoveredDmnRule> coveredDmnRules) {

        val decisionKey2coveredRuleMap = coveredDmnRules.stream()
                .collect(Collectors.groupingBy(CoveredDmnRule::getDecisionKey));
        decisionKey2coveredRuleMap.forEach(this::addCoveredDmnRules);
    }

    private void addCoveredDmnRules(final String decisionKey, final List<CoveredDmnRule> coveredDmnRules) {
        val decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        decisionCoverage.addCoveredDmnRule(coveredDmnRules);
    }


    /**
     * Retrieves the coverage percentage for all process definitions deployed
     * with the method.
     */
    @Override
    public double getCoveragePercentage() {

        // Aggregate element collections

        final Set<CoveredFlowNode> deploymentCoveredFlowNodes = new HashSet<>();
        final Set<FlowNode> deploymentDefinitionsFlowNodes = new HashSet<FlowNode>();

        final Set<CoveredSequenceFlow> deploymentCoveredSequenceFlows = new HashSet<>();
        final Set<SequenceFlow> deploymentDefinitionsSequenceFlows = new HashSet<SequenceFlow>();

        // Collect defined and covered elements for all definitions in the method deployment
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

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
        final double coveragePercentage = this.getCoveragePercentage(
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
    @Override
    public double getCoveragePercentage(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        final Set<CoveredFlowNode> coveredFlowNodes = processCoverage.getCoveredFlowNodes();
        final Set<FlowNode> definitionFlowNodes = processCoverage.getDefinitionFlowNodes();

        final Set<CoveredSequenceFlow> coveredSequenceFlows = processCoverage.getCoveredSequenceFlows();
        final Set<SequenceFlow> definitionSequenceFlows = processCoverage.getDefinitionSequenceFlows();

        // Calculate coverage
        final double coveragePercentage = this.getCoveragePercentage(
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
    private double getCoveragePercentage(final Set<CoveredFlowNode> coveredFlowNodes, final Set<FlowNode> definitionsFlowNodes,
                                         final Set<CoveredSequenceFlow> coveredSequenceFlows, final Set<SequenceFlow> definitionsSequenceFlows) {

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
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

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
    public Set<FlowNode> getProcessDefinitionsFlowNodes(final String processDefinitionKey) {

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
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

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
    public Set<SequenceFlow> getProcessDefinitionsSequenceFlows(final String processDefinitionKey) {

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
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<CoveredFlowNode> definitionFlowNodes = processCoverage.getCoveredFlowNodes();
            flowNodes.addAll(definitionFlowNodes);

        }

        return flowNodes;
    }

    public Set<CoveredSequenceFlow> getCoveredSequenceFlows() {

        final Set<CoveredSequenceFlow> sequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {

            final Set<CoveredSequenceFlow> definitionSequenceFlows = processCoverage.getCoveredSequenceFlows();
            sequenceFlows.addAll(definitionSequenceFlows);

        }

        return sequenceFlows;
    }


    @Override
    public Set<String> getCoveredFlowNodeIds(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredFlowNodeIds();
    }

    @Override
    public Set<CoveredFlowNode> getCoveredFlowNodes(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredFlowNodes();
    }

    @Override
    public Set<String> getCoveredSequenceFlowIds(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredSequenceFlowIds();
    }

    @Override
    public Set<CoveredDmnRule> getCoveredDecisionRules(final String decisionKey) {

        final DecisionCoverage decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        return decisionCoverage.getCoveredDmnRules();
    }

    public Integer getDecisionRuleCount(final String decisionKey) {

        final DecisionCoverage decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        return decisionCoverage.getDefinitionDecisionRules().size();
    }

    @Override
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredSequenceFlows();
    }


    @Override
    public Set<ProcessDefinition> getProcessDefinitions() {

        final Set<ProcessDefinition> processDefinitions = new TreeSet<ProcessDefinition>(
                new Comparator<ProcessDefinition>() {

                    // Avoid removing process definitions with the same key, but coming from different BPMNs.
                    @Override
                    public int compare(final ProcessDefinition o1, final ProcessDefinition o2) {
                        return o1.getResourceName().compareTo(o2.getResourceName());
                    }
                });

        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {
            processDefinitions.add(processCoverage.getProcessDefinition());
        }

        return processDefinitions;
    }


    @Override
    public Set<DecisionDefinition> getDecisionDefinitions() {

        final Set<DecisionDefinition> decisionDefinitions = new TreeSet<DecisionDefinition>(
                new Comparator<DecisionDefinition>() {

                    // Avoid removing process definitions with the same key, but coming from different BPMNs.
                    @Override
                    public int compare(final DecisionDefinition o1, final DecisionDefinition o2) {
                        return (o1.getKey() + o1.getResourceName()).compareTo(o2.getKey() + o2.getResourceName());
                    }
                });

        for (final DecisionCoverage decisionCoverage : decisionKeyToDecisionCoverage.values()) {
            decisionDefinitions.add(decisionCoverage.getDecisionDefintion());
        }

        return decisionDefinitions;
    }

    @Override
    public String toString() {

        /*
         * String representation mainly used for junit output and debug.
         */

        final StringBuilder builder = new StringBuilder();
        builder.append("Deployment ID: ");
        builder.append(deploymentId);
        builder.append("\nDeployment process definitions:\n");

        // List of process coverage string representations
        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {
            builder.append(processCoverage);
            builder.append('\n');
        }

        return builder.toString();
    }

}
