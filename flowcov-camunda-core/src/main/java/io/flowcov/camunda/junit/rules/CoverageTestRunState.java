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

import io.flowcov.camunda.model.ClassCoverage;
import io.flowcov.camunda.model.CoveredDmnRule;
import io.flowcov.camunda.model.CoveredElement;
import io.flowcov.camunda.model.MethodCoverage;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.List;

/**
 * State tracking the current class and method coverage run.
 */
public interface CoverageTestRunState {

    /**
     * Adds the covered element to the current test run coverage.
     *
     * @param coveredElement
     */
    void addCoveredElement(/* @NotNull */ CoveredElement coveredElement);

    /**
     * Mark a covered element execution as ended.
     *
     * @param coveredElement
     */
    void endCoveredElement(CoveredElement coveredElement);


    /**
     * Adds the covered DMN Rule
     *
     * @param coveredDmnRule
     */
    void addCoveredRules(List<CoveredDmnRule> coveredDmnRule);

    /**
     * Adds a test method to the class coverage.
     *
     * @param processEngine
     * @param deploymentId       The deployment ID of the test method run. (Hint: Every test
     *                           method run has its own deployment.)
     * @param processDefinitions The process definitions of the test method deployment.
     * @param testName           The name of the test method.
     */
    void initializeTestMethodCoverage(ProcessEngine processEngine, String deploymentId,
                                      List<ProcessDefinition> processDefinitions,
                                      List<DecisionDefinition> decisionDefinitions, String testName);

    /**
     * Retrieves the coverage for a test method.
     *
     * @param testName
     * @return
     */
    MethodCoverage getTestMethodCoverage(String testName);

    /**
     * Retrieves the currently executing test method coverage.
     *
     * @return
     */
    MethodCoverage getCurrentTestMethodCoverage();

    /**
     * Retrieves the class coverage.
     *
     * @return
     */
    ClassCoverage getClassCoverage();

    /**
     * Retrieves the name of the currently executing test method.
     *
     * @return
     */
    String getCurrentTestMethodName();

    /**
     * Sets the name of the currently executing test mehod.
     *
     * @param currentTestName
     */
    void setCurrentTestMethodName(String currentTestName);

    String getTestClassName();

    void setTestClassName(String className);

    void setExcludedProcessDefinitionKeys(List<String> excludedProcessDefinitionKeys);

}
