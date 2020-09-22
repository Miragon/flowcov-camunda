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

package io.flowcov.camunda.junit;

import io.flowcov.camunda.listeners.CompensationEventCoverageHandler;
import io.flowcov.camunda.listeners.CoverageHistoryEventHandler;
import io.flowcov.camunda.listeners.ElementCoverageParseListener;
import io.flowcov.camunda.model.ClassCoverage;
import io.flowcov.camunda.util.CoverageReportUtil;
import lombok.val;
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
import java.util.Collection;
import java.util.List;
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
    private FlowCovTestRunState coverageTestRunState;

    /**
     * Controls run state initialization.
     * {@see #initializeRunState(Description)}
     */
    private boolean firstRun = true;

    /**
     * coverageTestRunStateFactory. Can be changed for aggregated/suite coverage check
     */
    private FlowCovTestRunStateFactory coverageTestRunStateFactory = new FlowCovTestRunStateFactory();

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

        this.handleClassCoverage(description);

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
     * Sets the test run state for the coverage listeners.
     */
    private void initializeListenerRunState() {

        val processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

        // Configure activities listener

        val historyEventHandler = (CoverageHistoryEventHandler) processEngineConfiguration.getHistoryEventHandler();
        historyEventHandler.setCoverageTestRunState(coverageTestRunState);

        // Configure sequence flow listener

        final List<BpmnParseListener> bpmnParseListeners = processEngineConfiguration.getCustomPostBPMNParseListeners();

        for (final BpmnParseListener parseListener : bpmnParseListeners) {
            if (parseListener instanceof ElementCoverageParseListener) {
                val listener = (ElementCoverageParseListener) parseListener;
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


    private void handleClassCoverage(final Description description) {

        // If the rule is a class rule get the class coverage
        //if (!description.isTest()) {

        final ClassCoverage classCoverage = coverageTestRunState.getClassCoverage();

        // Make sure the class coverage deals with the same deployments for
        // every test method
        classCoverage.assertAllDeploymentsEqual();

        // Create graphical report
        CoverageReportUtil.createClassReport(processEngine, coverageTestRunState);

    }

    private void assertCoverage(final double coverage, final Collection<Matcher<Double>> matchers) {

        for (final Matcher<Double> matcher : matchers) {

            Assert.assertThat(coverage, matcher);
        }

    }

    private boolean isExcluded(final ProcessDefinition processDefinition) {
        if (excludedProcessDefinitionKeys != null) {
            return excludedProcessDefinitionKeys.contains(processDefinition.getKey());
        }
        return false;
    }

    public void setCoverageTestRunStateFactory(final FlowCovTestRunStateFactory coverageTestRunStateFactory) {
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
