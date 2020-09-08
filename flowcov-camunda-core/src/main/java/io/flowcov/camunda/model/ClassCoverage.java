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
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test class coverage model. The class coverage is an aggregation of all test method coverages.
 */
public class ClassCoverage implements AggregatedCoverage {

    /**
     * Map connecting the test method to the test method run coverage.
     */
    private Map<String, MethodCoverage> testNameToMethodCoverage = new HashMap<String, MethodCoverage>();

    /**
     * Adds a covered element to the test method coverage.
     *
     * @param testName
     * @param coveredElement
     */
    public void addCoveredElement(final String testName, final CoveredElement coveredElement) {
        testNameToMethodCoverage.get(testName).addCoveredElement(coveredElement);
    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param currentTestMethodName
     * @param coveredElement
     */
    public void endCoveredElement(final String currentTestMethodName, final CoveredElement coveredElement) {
        testNameToMethodCoverage.get(currentTestMethodName).endCoveredElement(coveredElement);
    }

    public void addCoveredDmnRules(final String currentTestMethodName, final List<CoveredDmnRule> coveredDmnRules) {
        testNameToMethodCoverage.get(currentTestMethodName).addCoveredDmnRules(coveredDmnRules);
    }

    /**
     * Retrieves a test methods coverage.
     *
     * @param testName The name of the test method.
     * @return
     */
    public MethodCoverage getTestMethodCoverage(final String testName) {
        return testNameToMethodCoverage.get(testName);
    }


    /**
     * Retrieves all method coverage.
     *
     * @return
     */
    public Map<String, MethodCoverage> getTestMethodCoverage() {
        return testNameToMethodCoverage;
    }

    /**
     * Add a test method coverage to the class coverage.
     *
     * @param testName
     * @param testCoverage
     */
    public void addTestMethodCoverage(final String testName, final MethodCoverage testCoverage) {
        testNameToMethodCoverage.put(testName, testCoverage);
    }

    /**
     * Retrieves the class coverage percentage.
     * All covered test methods' elements are aggregated and checked against the
     * process definition elements.
     *
     * @return The coverage percentage.
     */
    @Override
    public double getCoveragePercentage() {

        // All deployments must be the same, so we take the first one
        final MethodCoverage anyDeployment = this.getAnyMethodCoverage();

        final Set<FlowNode> definitionsFlowNodes = anyDeployment.getProcessDefinitionsFlowNodes();
        final Set<SequenceFlow> definitionsSeqenceFlows = anyDeployment.getProcessDefinitionsSequenceFlows();

        final Set<CoveredFlowNode> coveredFlowNodes = this.getCoveredFlowNodes();
        final Set<CoveredSequenceFlow> coveredSequenceFlows = this.getCoveredSequenceFlows();

        final double bpmnElementsCount = definitionsFlowNodes.size() + definitionsSeqenceFlows.size();
        final double coveredElementsCount = coveredFlowNodes.size() + coveredSequenceFlows.size();

        return coveredElementsCount / bpmnElementsCount;
    }

    /**
     * Retrieves the class coverage percentage for the given process definition key.
     * All covered test methods' elements are aggregated and checked against the
     * process definition elements.
     *
     * @param processDefinitionKey
     * @return The coverage percentage.
     */
    @Override
    public double getCoveragePercentage(final String processDefinitionKey) {
        // All deployments must be the same, so we take the first one
        final MethodCoverage anyDeployment = this.getAnyMethodCoverage();

        final Set<FlowNode> definitionsFlowNodes = anyDeployment.getProcessDefinitionsFlowNodes(processDefinitionKey);
        final Set<SequenceFlow> definitionsSeqenceFlows = anyDeployment.getProcessDefinitionsSequenceFlows(processDefinitionKey);

        final Set<CoveredFlowNode> coveredFlowNodes = this.getCoveredFlowNodes(processDefinitionKey);
        final Set<CoveredSequenceFlow> coveredSequenceFlows = this.getCoveredSequenceFlows(processDefinitionKey);

        final double bpmnElementsCount = definitionsFlowNodes.size() + definitionsSeqenceFlows.size();
        final double coveredElementsCount = coveredFlowNodes.size() + coveredSequenceFlows.size();

        return coveredElementsCount / bpmnElementsCount;
    }

    /**
     * Retrieves the covered flow nodes.
     * Flow nodes with the same element ID but different process definition keys are retained.
     *
     * @return A set of covered flow nodes.
     */
    public Set<CoveredFlowNode> getCoveredFlowNodes() {

        final Set<CoveredFlowNode> coveredFlowNodes = new TreeSet<CoveredFlowNode>(CoveredElementComparator.instance());

        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            coveredFlowNodes.addAll(methodCoverage.getCoveredFlowNodes());
        }

        return coveredFlowNodes;
    }

    @Override
    public Set<CoveredFlowNode> getCoveredFlowNodes(final String processDefinitionKey) {

        final Set<CoveredFlowNode> coveredFlowNodes = new TreeSet<CoveredFlowNode>(CoveredElementComparator.instance());

        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            coveredFlowNodes.addAll(methodCoverage.getCoveredFlowNodes(processDefinitionKey));
        }

        return coveredFlowNodes;
    }

    /**
     * Retrieves a set of covered flow node IDs for the given process definition key.
     */
    @Override
    public Set<String> getCoveredFlowNodeIds(final String processDefinitionKey) {

        final Set<String> coveredFlowNodeIds = new HashSet<String>();
        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            coveredFlowNodeIds.addAll(methodCoverage.getCoveredFlowNodeIds(processDefinitionKey));
        }

        return coveredFlowNodeIds;
    }

    /**
     * Retrieves the covered sequence flows.
     * Sequence flows with the same element ID but different process definition keys are retained.
     *
     * @return A set of covered flow nodes.
     */
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows() {

        final Set<CoveredSequenceFlow> coveredSequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());

        for (final MethodCoverage deploymentCoverage : testNameToMethodCoverage.values()) {

            coveredSequenceFlows.addAll(deploymentCoverage.getCoveredSequenceFlows());

        }

        return coveredSequenceFlows;
    }

    /**
     * Retrieves a set of covered sequence flow IDs for the given process
     * definition key.
     */
    @Override
    public Set<String> getCoveredSequenceFlowIds(final String processDefinitionKey) {

        final Set<String> coveredSequenceFlowIds = new HashSet<String>();
        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            coveredSequenceFlowIds.addAll(methodCoverage.getCoveredSequenceFlowIds(processDefinitionKey));
        }

        return coveredSequenceFlowIds;
    }

    @Override
    public Set<CoveredDmnRule> getCoveredDecisionRules(final String decisionKey) {
        return testNameToMethodCoverage.values().stream()
                .map(obj -> obj.getCoveredDecisionRules(decisionKey))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of covered sequence flows for the given process
     * definition key.
     *
     * @param processDefinitionKey
     */
    @Override
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows(final String processDefinitionKey) {

        final Set<CoveredSequenceFlow> coveredSequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());
        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            coveredSequenceFlows.addAll(methodCoverage.getCoveredSequenceFlows(processDefinitionKey));
        }

        return coveredSequenceFlows;
    }

    /**
     * Retrieves the process definitions of the coverage test.
     * Since there are multiple deployments (one for each test method) the first
     * set of process definitions found is return.
     */
    @Override
    public Set<ProcessDefinition> getProcessDefinitions() {
        return this.getAnyMethodCoverage().getProcessDefinitions();
    }

    /**
     * Retrieves the process definitions of the coverage test.
     * Since there are multiple deployments (one for each test method) the first
     * set of process definitions found is return.
     */
    @Override
    public Set<DecisionDefinition> getDecisionDefinitions() {
        return this.getAnyMethodCoverage().getDecisionDefinitions();
    }

    /**
     * Retrieves the first method coverage found.
     *
     * @return
     */
    protected MethodCoverage getAnyMethodCoverage() {

        // All deployments must be the same, so we take the first one
        final MethodCoverage anyDeployment = testNameToMethodCoverage.values().iterator().next();
        return anyDeployment;
    }

    /**
     * Asserts if all method deployments are equal. (BPMNs with the same business keys)
     */
    public void assertAllDeploymentsEqual() {

        Set<ProcessDefinition> processDefinitions = null;
        for (final MethodCoverage methodCoverage : testNameToMethodCoverage.values()) {

            final Set<ProcessDefinition> deploymentProcessDefinitions = methodCoverage.getProcessDefinitions();

            if (processDefinitions == null) {
                processDefinitions = deploymentProcessDefinitions;
            }

            Assert.assertEquals("Class coverage can only be calculated if all tests deploy the same BPMN resources.",
                    processDefinitions, deploymentProcessDefinitions);

        }

    }

}
