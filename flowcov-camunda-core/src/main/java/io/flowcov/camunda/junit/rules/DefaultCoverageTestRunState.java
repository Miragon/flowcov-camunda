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

import io.flowcov.camunda.model.*;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * State tracking the current class and method coverage run.
 */
public class DefaultCoverageTestRunState implements CoverageTestRunState {

    private Logger log = Logger.getLogger(DefaultCoverageTestRunState.class.getCanonicalName());

    /**
     * The actual class coverage object.
     */
    private ClassCoverage classCoverage = new ClassCoverage();

    /**
     * The test class name.
     */
    private String testClassName;

    /**
     * The name of the currently executing test method.
     */
    private String currentTestMethodName;

    /**
     * A list of process definition keys excluded from the test run.
     */
    private List<String> excludedProcessDefinitionKeys;

    /**
     * Adds the covered element to the current test run coverage.
     *
     * @param coveredElement
     */
    @Override
    public void addCoveredElement(/* @NotNull */ final CoveredElement coveredElement) {

        if (!this.isExcluded(coveredElement)) {
            if (log.isLoggable(Level.FINE)) {
                log.info("addCoveredElement(" + coveredElement + ")");
            }

            classCoverage.addCoveredElement(currentTestMethodName, coveredElement);
        }

    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param coveredElement
     */
    @Override
    public void endCoveredElement(final CoveredElement coveredElement) {

        if (!this.isExcluded(coveredElement)) {
            if (log.isLoggable(Level.FINE)) {
                log.info("endCoveredElement(" + coveredElement + ")");
            }

            classCoverage.endCoveredElement(currentTestMethodName, coveredElement);
        }

    }

    @Override
    public void addCoveredRules(final List<CoveredDmnRule> coveredDmnRule) {
        classCoverage.addCoveredDmnRules(currentTestMethodName, coveredDmnRule);
    }

    /**
     * Adds a test method to the class coverage.
     *
     * @param processEngine
     * @param deploymentId       The deployment ID of the test method run. (Hint: Every test
     *                           method run has its own deployment.)
     * @param processDefinitions The process definitions of the test method deployment.
     * @param testName           The name of the test method.
     */
    @Override
    public void initializeTestMethodCoverage(final ProcessEngine processEngine, final String deploymentId,
                                             final List<ProcessDefinition> processDefinitions,
                                             final List<DecisionDefinition> decisionDefinitions, final String testName) {

        final MethodCoverage testCoverage = new MethodCoverage(deploymentId, testName);

        processDefinitions.stream()
                .map(obj -> new ProcessCoverage(processEngine, obj))
                .forEach(testCoverage::addProcessCoverage);

        decisionDefinitions.stream()
                .map(obj -> new DecisionCoverage(processEngine, obj))
                .forEach(testCoverage::addDecisionCoverage);


        classCoverage.addTestMethodCoverage(testName, testCoverage);
    }

    /**
     * Retrieves the coverage for a test method.
     *
     * @param testName
     * @return
     */
    @Override
    public MethodCoverage getTestMethodCoverage(final String testName) {
        return classCoverage.getTestMethodCoverage(testName);
    }

    /**
     * Retrieves the currently executing test method coverage.
     *
     * @return
     */
    @Override
    public MethodCoverage getCurrentTestMethodCoverage() {
        return classCoverage.getTestMethodCoverage(currentTestMethodName);
    }

    /**
     * Retrieves the class coverage.
     *
     * @return
     */
    @Override
    public ClassCoverage getClassCoverage() {
        return classCoverage;
    }

    /**
     * Retrieves the name of the currently executing test method.
     *
     * @return
     */
    @Override
    public String getCurrentTestMethodName() {
        return currentTestMethodName;
    }

    /**
     * Sets the name of the currently executing test mehod.
     *
     * @param currentTestName
     */
    @Override
    public void setCurrentTestMethodName(final String currentTestName) {
        this.currentTestMethodName = currentTestName;
    }

    @Override
    public String getTestClassName() {
        return testClassName;
    }

    @Override
    public void setTestClassName(final String className) {
        this.testClassName = className;
    }

    @Override
    public void setExcludedProcessDefinitionKeys(final List<String> excludedProcessDefinitionKeys) {
        this.excludedProcessDefinitionKeys = excludedProcessDefinitionKeys;
    }

    private boolean isExcluded(final CoveredElement coveredElement) {
        if (excludedProcessDefinitionKeys != null) {
            return excludedProcessDefinitionKeys.contains(coveredElement.getProcessDefinitionKey());
        }
        return false;
    }

}
