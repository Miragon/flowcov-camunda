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
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.ArrayList;
import java.util.List;

public class AggregatedCoverageTestRunState implements CoverageTestRunState {

    private CoverageTestRunState currentCoverageTestRunState = null;

    private List<ClassCoverage> classCoverages = new ArrayList<ClassCoverage>();

    public void switchToNewState(CoverageTestRunState newState) {
        finishState();
        currentCoverageTestRunState = newState;
    }

    private void finishState() {
        if (currentCoverageTestRunState != null) {
            classCoverages.add(currentCoverageTestRunState.getClassCoverage());
            currentCoverageTestRunState = null;
        }
    }

    public AggregatedCoverage getAggregatedCoverage() {
        finishState();
        return new AggregatedClassCoverage(classCoverages);
    }

    @Override
    public void addCoveredElement(CoveredElement coveredElement) {
        currentCoverageTestRunState.addCoveredElement(coveredElement);
    }

    @Override
    public void endCoveredElement(CoveredElement coveredElement) {
        currentCoverageTestRunState.endCoveredElement(coveredElement);
    }

    @Override
    public void initializeTestMethodCoverage(ProcessEngine processEngine, String deploymentId, List<ProcessDefinition> processDefinitions, String testName) {
        currentCoverageTestRunState.initializeTestMethodCoverage(processEngine, deploymentId, processDefinitions, testName);
    }

    @Override
    public MethodCoverage getTestMethodCoverage(String testName) {
        return currentCoverageTestRunState.getTestMethodCoverage(testName);
    }

    @Override
    public MethodCoverage getCurrentTestMethodCoverage() {
        return currentCoverageTestRunState.getCurrentTestMethodCoverage();
    }

    @Override
    public ClassCoverage getClassCoverage() {
        return currentCoverageTestRunState.getClassCoverage();
    }

    @Override
    public String getCurrentTestMethodName() {
        return currentCoverageTestRunState.getCurrentTestMethodName();
    }

    @Override
    public void setCurrentTestMethodName(String currentTestName) {
        currentCoverageTestRunState.setCurrentTestMethodName(currentTestName);
    }

    @Override
    public String getTestClassName() {
        return currentCoverageTestRunState.getTestClassName();
    }

    @Override
    public void setTestClassName(String className) {
        currentCoverageTestRunState.setTestClassName(className);
    }

    @Override
    public void setExcludedProcessDefinitionKeys(List<String> excludedProcessDefinitionKeys) {
        currentCoverageTestRunState.setExcludedProcessDefinitionKeys(excludedProcessDefinitionKeys);
    }
}
