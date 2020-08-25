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
import io.flowcov.camunda.api.bpmn.BpmnModel;
import io.flowcov.camunda.api.bpmn.BpmnTestClass;
import io.flowcov.camunda.api.bpmn.BpmnTestMethod;
import io.flowcov.camunda.api.bpmn.FlowNode;
import io.flowcov.camunda.junit.rules.CoverageTestRunState;
import io.flowcov.camunda.model.ClassCoverage;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.ProcessEngine;
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
    public static void createClassReport(ProcessEngine processEngine, CoverageTestRunState coverageTestRunState) {

        ClassCoverage coverage = coverageTestRunState.getClassCoverage();
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
    private static void createReport(ClassCoverage coverage, String reportDirectory, String testClazz) {

        try {
            val run = Build.builder()
                    .build();

            for (val definition : coverage.getProcessDefinitions()) {
                run.getBpmnModels().add(parseProcessDefinition(coverage, testClazz, definition));
            }

            FlowCovReporter.generateReport(
                    reportDirectory + '/' + testClazz + "/flowCovReport.json",
                    run);
        } catch (IOException ex) {

            logger.log(Level.SEVERE, "Unable to load process definition!", ex);
            throw new RuntimeException();
        }

    }

    private static BpmnModel parseProcessDefinition(ClassCoverage coverage, String testClazz, ProcessDefinition
            processDefinition) throws IOException {

        val bpmnXml = getBpmnXml(processDefinition);

        val model = BpmnModel.builder()
                .bpmnXml(bpmnXml)
                .processDefinitionKey(processDefinition.getKey())
                .name(processDefinition.getName())
                .version(processDefinition.getVersionTag())
                .hash(bpmnXml.hashCode())
                .build();

        List<BpmnTestMethod> testMethods = coverage.getTestMethodCoverage()
                .values()
                .stream()
                .filter(m -> m.getName() != null).map(value -> {

                    val coveredFlowNodes = value.getCoveredFlowNodes(processDefinition.getKey())
                            .stream()
                            .map(node -> FlowNode.builder()
                                    .ended(node.hasEnded())
                                    .key(node.getElementId())
                                    .build()
                            ).collect(Collectors.toList());

                    val coveredSequenceFlowIds = value.getCoveredSequenceFlowIds(
                            processDefinition.getKey());

                    return BpmnTestMethod.builder()
                            .flowNodes(coveredFlowNodes)
                            .sequenceFlowIds(coveredSequenceFlowIds)
                            .name(value.getName())
                            .coverage(value.getCoveragePercentage(processDefinition.getKey()))
                            .build();

                }).collect(Collectors.toList());

        var testClass = BpmnTestClass.builder()
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
    protected static String getBpmnXml(ProcessDefinition processDefinition) throws IOException {

        InputStream inputStream = CoverageReportUtil.class.getClassLoader().getResourceAsStream(
                processDefinition.getResourceName());
        if (inputStream == null) {
            inputStream = new FileInputStream(processDefinition.getResourceName());
        }

        return IOUtils.toString(inputStream);
    }

}
