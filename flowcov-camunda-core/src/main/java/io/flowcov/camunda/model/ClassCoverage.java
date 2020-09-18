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

import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
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


    @Override
    public List<CoveredFlowNode> getCoveredFlowNodes(final String processDefinitionKey) {
        return testNameToMethodCoverage.values().stream()
                .map(obj -> obj.getCoveredFlowNodes(processDefinitionKey))
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<CoveredSequenceFlow> getCoveredSequenceFlows(final String processDefinitionKey) {

        return testNameToMethodCoverage.values().stream()
                .map(obj -> obj.getCoveredSequenceFlows(processDefinitionKey))
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Set<CoveredDmnRule> getCoveredDecisionRules(final String decisionKey) {
        return testNameToMethodCoverage.values().stream()
                .map(obj -> obj.getCoveredDecisionRules(decisionKey))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
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
