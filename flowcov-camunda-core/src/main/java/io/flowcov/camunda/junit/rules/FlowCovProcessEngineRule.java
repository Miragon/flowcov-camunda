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

import io.flowcov.camunda.listeners.CompensationEventCoverageHandler;
import io.flowcov.camunda.listeners.FlowNodeHistoryEventHandler;
import io.flowcov.camunda.listeners.PathCoverageParseListener;
import io.flowcov.camunda.model.AggregatedCoverage;
import io.flowcov.camunda.model.ClassCoverage;
import io.flowcov.camunda.util.CoverageReportUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.event.EventHandler;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.Description;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Rule handling the flowcov test coverage
 */
public class FlowCovProcessEngineRule extends ProcessEngineRule {

    private static Logger logger = Logger.getLogger(FlowCovProcessEngineRule.class.getCanonicalName());

    /**
     * The state of the current run (class and current method).
     */
    private CoverageTestRunState coverageTestRunState;

    /**
     * Controls run state initialization.
     * {@see #initializeRunState(Description)}
     */
    private boolean firstRun = true;

    /**
     * Log class and test method coverages?
     */
    private boolean detailedCoverageLogging = false;

    /**
     * Is class coverage handling needed?
     */
    private boolean handleClassCoverage = true;

    /**
     * coverageTestRunStateFactory. Can be changed for aggregated/suite coverage check
     */
    private CoverageTestRunStateFactory coverageTestRunStateFactory = new DefaultCoverageTestRunStateFactory();

    /**
     * Matchers to be asserted on the class coverage percentage.
     */
    private Collection<Matcher<Double>> classCoverageAssertionMatchers = new LinkedList<Matcher<Double>>();

    /**
     * Matchers to be asserted on the individual test method coverages.
     */
    private Map<String, Collection<Matcher<Double>>> testMethodNameToCoverageMatchers = new HashMap<String, Collection<Matcher<Double>>>();

    /**
     * A list of process definition keys excluded from the test run.
     */
    private List<String> excludedProcessDefinitionKeys;

    FlowCovProcessEngineRule() {
        super();
    }

    FlowCovProcessEngineRule(final ProcessEngine processEngine) {
        super(processEngine);
    }

    /**
     * Adds an assertion for a test method's coverage percentage.
     *
     * @param testMethodName
     * @param matcher
     */
    public void addTestMethodCoverageAssertionMatcher(final String testMethodName, final Matcher<Double> matcher) {

        // JDK7 ifAbsent
        Collection<Matcher<Double>> matchers = testMethodNameToCoverageMatchers.get(testMethodName);
        if (matchers == null) {
            matchers = new LinkedList<Matcher<Double>>();
            testMethodNameToCoverageMatchers.put(testMethodName, matchers);
        }

        matchers.add(matcher);

    }

    /**
     * Adds an assertion for the class coverage percentage.
     *
     * @param matcher
     */
    public void addClassCoverageAssertionMatcher(final MinimalCoverageMatcher matcher) {
        classCoverageAssertionMatchers.add(matcher);
    }

    public void setExcludedProcessDefinitionKeys(final List<String> excludedProcessDefinitionKeys) {
        this.excludedProcessDefinitionKeys = excludedProcessDefinitionKeys;
    }

    @Override
    public void starting(final Description description) {

        this.validateRuleAnnotations(description);

        if (processEngine == null) {
            super.initializeProcessEngine();
        }

        this.initializeRunState(description);

        super.starting(description);

        this.initializeMethodCoverage(description);
    }

    @Override
    public void finished(final Description description) {

        if (handleClassCoverage) {
            this.handleClassCoverage(description);
        }

        // run derived finalization only of not used as a class rule
        if (identityService != null) {
            super.finished(description);
        }

    }


    /**
     * Validates the annotation of the rule field in the test class.
     *
     * @param description
     */
    private void validateRuleAnnotations(final Description description) {

        // If the first run is a @ClassRule run, check if @Rule is annotated
        if (firstRun && !description.isTest()) {

            /*
             * Get the fields of the test class and check if there is only one
             * coverage rule and if the coverage rule field is annotation with
             * both @ClassRule and @Rule.
             */

            int numberOfCoverageRules = 0;
            for (final Field field : description.getTestClass().getFields()) {

                final Class<?> fieldType = field.getType();
                if (this.getClass().isAssignableFrom(fieldType)) {

                    ++numberOfCoverageRules;

                    final boolean isClassRule = field.isAnnotationPresent(ClassRule.class);
                    final boolean isRule = field.isAnnotationPresent(Rule.class);
                    if (isClassRule && !isRule) {

                        throw new RuntimeException(this.getClass().getCanonicalName()
                                + " can only be used as a @ClassRule if it is also a @Rule!");
                    }
                }
            }

            // TODO if they really want to have multiple runs, let them?
            if (numberOfCoverageRules > 1) {
                throw new RuntimeException("Only one coverage rule can be used per test class!");
            }
        }
    }

    /**
     * Initialize the current test method coverage.
     *
     * @param description
     */
    private void initializeMethodCoverage(final Description description) {

        // Not a @ClassRule run and deployments present
        if (deploymentId != null) {

            final List<ProcessDefinition> deployedProcessDefinitions = processEngine.getRepositoryService()
                    .createProcessDefinitionQuery()
                    .deploymentId(deploymentId)
                    .list();

            final List<ProcessDefinition> relevantProcessDefinitions = deployedProcessDefinitions.stream()
                    .filter(obj -> !this.isExcluded(obj))
                    .collect(Collectors.toList());

            final List<DecisionDefinition> decisionDefinitions = processEngine.getRepositoryService()
                    .createDecisionDefinitionQuery()
                    .deploymentId(deploymentId)
                    .list();


            //Hier auch nach DMN suchen

            coverageTestRunState.initializeTestMethodCoverage(
                    processEngine,
                    deploymentId,
                    relevantProcessDefinitions,
                    decisionDefinitions,
                    description.getMethodName());

        }
    }

    /**
     * Initialize the coverage run state depending on the rule annotations and
     * notify the state of the current test name.
     *
     * @param description
     */
    private void initializeRunState(final Description description) {

        // Initialize new state once on @ClassRule run or on every individual
        // @Rule run
        if (firstRun) {
            coverageTestRunState = coverageTestRunStateFactory.create(description.getClassName(), excludedProcessDefinitionKeys);
            this.initializeListenerRunState();
            firstRun = false;
        }

        coverageTestRunState.setCurrentTestMethodName(description.getMethodName());
    }

    /**
     * Sets the test run state for the coverage listeners. logging.
     * {@see ProcessCoverageInMemProcessEngineConfiguration}
     */
    private void initializeListenerRunState() {

        final ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

        // Configure activities listener

        final FlowNodeHistoryEventHandler historyEventHandler = (FlowNodeHistoryEventHandler) processEngineConfiguration.getHistoryEventHandler();
        historyEventHandler.setCoverageTestRunState(coverageTestRunState);

        // Configure sequence flow listener

        final List<BpmnParseListener> bpmnParseListeners = processEngineConfiguration.getCustomPostBPMNParseListeners();

        for (final BpmnParseListener parseListener : bpmnParseListeners) {

            if (parseListener instanceof PathCoverageParseListener) {

                final PathCoverageParseListener listener = (PathCoverageParseListener) parseListener;
                listener.setCoverageTestRunState(coverageTestRunState);
            }
        }

        // Compensation event handler
        final EventHandler compensationEventHandler = processEngineConfiguration.getEventHandler("compensate");
        if (compensationEventHandler instanceof CompensationEventCoverageHandler) {

            final CompensationEventCoverageHandler compensationEventCoverageHandler = (CompensationEventCoverageHandler) compensationEventHandler;
            compensationEventCoverageHandler.setCoverageTestRunState(coverageTestRunState);

        } else {
            logger.warning("CompensationEventCoverageHandler not registered with process engine configuration!"
                    + " Compensation boundary events coverage will not be registered.");
        }

    }

    /**
     * If the rule is a @ClassRule log and assert the coverage and create a
     * graphical report. For the class coverage to work all the test method
     * deployments have to be equal.
     *
     * @param description
     */
    private void handleClassCoverage(final Description description) {

        // If the rule is a class rule get the class coverage
        //if (!description.isTest()) {

        final ClassCoverage classCoverage = coverageTestRunState.getClassCoverage();

        // Make sure the class coverage deals with the same deployments for
        // every test method
        classCoverage.assertAllDeploymentsEqual();

        final double classCoveragePercentage = classCoverage.getCoveragePercentage();

        // Log coverage percentage
        logger.info(
                coverageTestRunState.getTestClassName() + " test class coverage is: " + classCoveragePercentage);

        this.logCoverageDetail(classCoverage);

        // Create graphical report
        CoverageReportUtil.createClassReport(processEngine, coverageTestRunState);

        this.assertCoverage(classCoveragePercentage, classCoverageAssertionMatchers);

        // }
    }

    private void assertCoverage(final double coverage, final Collection<Matcher<Double>> matchers) {

        for (final Matcher<Double> matcher : matchers) {

            Assert.assertThat(coverage, matcher);
        }

    }

    /**
     * Logs the string representation of the passed coverage object.
     *
     * @param coverage
     */
    private void logCoverageDetail(final AggregatedCoverage coverage) {

        if (logger.isLoggable(Level.FINE) || this.isDetailedCoverageLogging()) {
            logger.log(Level.INFO, coverage.toString());
        }

    }

    private boolean isExcluded(final ProcessDefinition processDefinition) {
        if (excludedProcessDefinitionKeys != null) {
            return excludedProcessDefinitionKeys.contains(processDefinition.getKey());
        }
        return false;
    }

    public boolean isDetailedCoverageLogging() {
        return detailedCoverageLogging;
    }

    public void setDetailedCoverageLogging(final boolean detailedCoverageLogging) {
        this.detailedCoverageLogging = detailedCoverageLogging;
    }

    public void setHandleClassCoverage(final boolean handleClassCoverage) {
        this.handleClassCoverage = handleClassCoverage;
    }

    public void setCoverageTestRunStateFactory(final CoverageTestRunStateFactory coverageTestRunStateFactory) {
        this.coverageTestRunStateFactory = coverageTestRunStateFactory;
    }

    @Override
    public org.junit.runners.model.Statement apply(final org.junit.runners.model.Statement base, final Description description) {
        return super.apply(base, description);

    }

    ;

    @Override
    protected void succeeded(final Description description) {
        super.succeeded(description);
        logger.info(description.getDisplayName() + " succeeded.");
    }

    @Override
    protected void failed(final Throwable e, final Description description) {
        super.failed(e, description);
        logger.info(description.getDisplayName() + " failed.");
    }

}
