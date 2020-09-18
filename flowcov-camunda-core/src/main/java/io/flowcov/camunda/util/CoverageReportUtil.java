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

package io.flowcov.camunda.util;

import io.flowcov.camunda.api.Build;
import io.flowcov.camunda.api.bpmn.*;
import io.flowcov.camunda.api.dmn.DmnModel;
import io.flowcov.camunda.api.dmn.DmnTestClass;
import io.flowcov.camunda.api.dmn.DmnTestMethod;
import io.flowcov.camunda.api.dmn.Rule;
import io.flowcov.camunda.junit.FlowCovTestRunState;
import io.flowcov.camunda.model.ClassCoverage;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility for generating graphical class and method coverage reports.
 */
public class CoverageReportUtil {

    private static final Logger logger = Logger.getLogger(CoverageReportUtil.class.getCanonicalName());

    /**
     * Root directory for all coverage reports.
     */
    public static final String TARGET_DIR_ROOT = "target/flowcov/";

    /**
     * Generates a coverage report for the whole test class. This method
     * requires that all tests have been executed with the same resources
     * deployed.
     *
     * @param processEngine
     * @param coverageTestRunState
     */
    public static void createClassReport(final ProcessEngine processEngine, final FlowCovTestRunState coverageTestRunState) {

        final ClassCoverage coverage = coverageTestRunState.getClassCoverage();
        final String reportDirectory = getReportDirectoryPath();

        createReport(coverage, reportDirectory, coverageTestRunState.getTestClassName());

    }

    /**
     * Generates a coverage report.
     *
     * @param coverage
     * @param reportDirectory The directory where the report will be stored.
     * @param testClazz       Optional test class name for info box
     */
    private static void createReport(final ClassCoverage coverage, final String reportDirectory, final String testClazz) {

        try {
            val build = new Build();

            for (val definition : coverage.getProcessDefinitions()) {
                build.getBpmnModels().add(parseProcessDefinition(coverage, testClazz, definition));
            }

            for (val definition : coverage.getDecisionDefinitions()) {
                build.getDmnModels().add(parseDecisionDefinition(coverage, testClazz, definition));
            }

            FlowCovReporter.generateReport(
                    reportDirectory + '/' + testClazz + "/flowCovReport.json",
                    build);
        } catch (final IOException ex) {

            logger.log(Level.SEVERE, "Unable to load process definition!", ex);
            throw new RuntimeException();
        }

    }

    private static BpmnModel parseProcessDefinition(final ClassCoverage coverage, final String testClazz, final ProcessDefinition
            processDefinition) throws IOException {

        val bpmnXml = getBpmnXml(processDefinition);

        val model = BpmnModel.builder()
                .bpmnXml(bpmnXml)
                .processDefinitionKey(processDefinition.getKey())
                .name(processDefinition.getName())
                .version(processDefinition.getVersionTag())
                .hash(bpmnXml.hashCode())
                .build();

        final List<BpmnTestMethod> testMethods = coverage.getTestMethodCoverage()
                .values()
                .stream()
                .filter(m -> m.getName() != null).map(value -> {
                    model.setTotalNodeCount(value.getProcessElementCount(processDefinition.getKey()));
                    val coveredFlowNodes = value.getCoveredFlowNodes(processDefinition.getKey())
                            .stream()
                            .map(node -> FlowNode.builder()
                                    .executionStartCounter(node.getExecutionStartCounter())
                                    .executionEndCounter(node.getExecutionEndCoutner())
                                    .key(node.getElementId())
                                    .type(node.getType())
                                    .build()
                            ).collect(Collectors.toList());

                    val coveredSequenceFlowIds = value.getCoveredSequenceFlows(
                            processDefinition.getKey()).stream()
                            .map(obj -> SequenceFlow.builder()
                                    .key(obj.getTransitionId())
                                    .executionStartCounter(obj.getExecutionStartCounter())
                                    .build())
                            .collect(Collectors.toList());

                    return BpmnTestMethod.builder()
                            .flowNodes(coveredFlowNodes)
                            .sequenceFlows(coveredSequenceFlowIds)
                            .name(value.getName())
                            .build();

                }).collect(Collectors.toList());

        final var testClass = BpmnTestClass.builder()
                .name(testClazz)
                .testMethods(testMethods)
                .build();

        model.getTestClasses().add(testClass);

        return model;
    }


    private static DmnModel parseDecisionDefinition(final ClassCoverage coverage, final String testClazz, final DecisionDefinition
            decisionDefinition) throws IOException {

        val bpmnXml = getDmnXml(decisionDefinition);

        val model = DmnModel.builder()
                .dmnXml(bpmnXml)
                .decisionKey(decisionDefinition.getKey())
                .name(decisionDefinition.getName())
                .version(decisionDefinition.getVersionTag())
                .hash(bpmnXml.hashCode())
                .build();

        final List<DmnTestMethod> testMethods = coverage.getTestMethodCoverage()
                .values()
                .stream()
                .filter(m -> m.getName() != null).map(value -> {

                    model.setRuleCount(value.getDecisionRuleCount(decisionDefinition.getKey()));
                    val coveredFlowNodes = value.getCoveredDecisionRules(decisionDefinition.getKey())
                            .stream()
                            .map(rule -> Rule.builder()
                                    .key(rule.getRuleId())
                                    .build()
                            ).collect(Collectors.toList());

                    return DmnTestMethod.builder()
                            .rules(coveredFlowNodes)
                            .name(value.getName())
                            .build();

                }).collect(Collectors.toList());

        final var testClass = DmnTestClass.builder()
                .name(testClazz)
                .executionEndTime(LocalDateTime.now())
                .testMethods(testMethods)
                .build();

        model.getTestClasses().add(testClass);

        return model;
    }


    /**
     * Retrieves directory path for all coverage reports of a test class.
     *
     * @return
     */
    private static String getReportDirectoryPath() {
        return TARGET_DIR_ROOT;
    }

    /**
     * Retrieves a process definitions BPMN XML.
     *
     * @param processDefinition
     * @return
     * @throws IOException Thrown if the BPMN resource is not found.
     */
    protected static String getBpmnXml(final ProcessDefinition processDefinition) throws IOException {

        InputStream inputStream = CoverageReportUtil.class.getClassLoader().getResourceAsStream(
                processDefinition.getResourceName());
        if (inputStream == null) {
            inputStream = new FileInputStream(processDefinition.getResourceName());
        }

        return IOUtils.toString(inputStream);
    }

    /**
     * Retrieves a decision definitions BPMN XML.
     *
     * @param decisionDefinition
     * @return
     * @throws IOException Thrown if the BPMN resource is not found.
     */
    protected static String getDmnXml(final DecisionDefinition decisionDefinition) throws IOException {

        InputStream inputStream = CoverageReportUtil.class.getClassLoader().getResourceAsStream(
                decisionDefinition.getResourceName());
        if (inputStream == null) {
            inputStream = new FileInputStream(decisionDefinition.getResourceName());
        }

        return IOUtils.toString(inputStream);
    }

}
