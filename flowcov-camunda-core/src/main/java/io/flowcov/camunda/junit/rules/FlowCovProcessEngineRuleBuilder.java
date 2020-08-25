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

import org.camunda.bpm.engine.ProcessEngine;

import java.util.Arrays;

/**
 * Fluent Builder for TestCoverageProcessEngineRule.
 */
public class FlowCovProcessEngineRuleBuilder {

    /**
     * If you set this property to a ratio (e.g. "1.0" for full coverage),
     * the @ClassRule will fail the test run if the coverage is less.<br>
     * Example parameter for running java:<br>
     * <code>-Dorg.camunda.bpm.extension.process_test_coverage.ASSERT_AT_LEAST=1.0</code>
     */
    public static final String DEFAULT_ASSERT_AT_LEAST_PROPERTY = "org.camunda.bpm.extension.process_test_coverage.ASSERT_AT_LEAST";

    private final FlowCovProcessEngineRule rule;

    private FlowCovProcessEngineRuleBuilder() {
        this.rule = new FlowCovProcessEngineRule();
    }

    private FlowCovProcessEngineRuleBuilder(ProcessEngine processEngine) {
        this.rule = new FlowCovProcessEngineRule(processEngine);
    }

    /**
     * Creates a TestCoverageProcessEngineRuleBuilder with the default class
     * coverage assertion property activated.
     *
     * @return
     */
    public static FlowCovProcessEngineRuleBuilder create() {
        return createBase().optionalAssertCoverageAtLeastProperty(DEFAULT_ASSERT_AT_LEAST_PROPERTY);
    }

    /**
     * Creates a TestCoverageProcessEngineRuleBuilder with the default class
     * coverage assertion property activated.
     *
     * @return
     */
    public static FlowCovProcessEngineRuleBuilder create(ProcessEngine processEngine) {
        return createBase(processEngine).optionalAssertCoverageAtLeastProperty(DEFAULT_ASSERT_AT_LEAST_PROPERTY);
    }

    /**
     * Set the system property name for minimal class coverage assertion.
     *
     * @param key System property name.
     * @return
     */
    public FlowCovProcessEngineRuleBuilder optionalAssertCoverageAtLeastProperty(String key) {

        String assertAtLeast = System.getProperty(key);
        if (assertAtLeast != null) {
            try {

                final MinimalCoverageMatcher minimalCoverageMatcher = new MinimalCoverageMatcher(
                        Double.parseDouble(assertAtLeast));
                rule.addClassCoverageAssertionMatcher(minimalCoverageMatcher);

            } catch (NumberFormatException e) {
                throw new RuntimeException("BAD TEST CONFIGURATION: optionalAssertCoverageAtLeastProperty( \"" + key
                        + "\" ) must be double");
            }
        }
        return this;
    }

    /**
     * @return a basic builder with nothing preconfigured
     */
    public static FlowCovProcessEngineRuleBuilder createBase() {
        return new FlowCovProcessEngineRuleBuilder();
    }

    /**
     * @return a basic builder with nothing preconfigured
     */
    public static FlowCovProcessEngineRuleBuilder createBase(ProcessEngine processEngine) {
        return new FlowCovProcessEngineRuleBuilder(processEngine);
    }

    /**
     * Enables detailed logging of individual class and method coverage objects.
     *
     * @return
     */
    public FlowCovProcessEngineRuleBuilder withDetailedCoverageLogging() {
        rule.setDetailedCoverageLogging(true);
        return this;
    }

    /**
     * Configures whenever class coverage handling is needed.
     *
     * @param needHandleClassCoverage boolean
     * @return
     */
    public FlowCovProcessEngineRuleBuilder handleClassCoverage(boolean needHandleClassCoverage) {
        rule.setHandleClassCoverage(needHandleClassCoverage);
        return this;
    }

    /**
     * Configures CoverageTestRunStateFactory used to create CoverageTestRunState. Useful for sharing state between several test-classes
     *
     * @param coverageTestRunStateFactory
     * @return
     */
    public FlowCovProcessEngineRuleBuilder setCoverageTestRunStateFactory(CoverageTestRunStateFactory coverageTestRunStateFactory) {
        rule.setCoverageTestRunStateFactory(coverageTestRunStateFactory);
        return this;
    }

    /**
     * Asserts if the class coverage is greater than the passed percentage.
     *
     * @param percentage
     * @return
     */
    public FlowCovProcessEngineRuleBuilder assertClassCoverageAtLeast(double percentage) {

        if (0 > percentage || percentage > 1) {
            throw new RuntimeException(
                    "BAD TEST CONFIGURATION: coverageAtLeast " + percentage + " (" + 100 * percentage + "%) ");
        }

        rule.addClassCoverageAssertionMatcher(new MinimalCoverageMatcher(percentage));
        return this;

    }

    public FlowCovProcessEngineRuleBuilder excludeProcessDefinitionKeys(String... processDefinitionKeys) {
        rule.setExcludedProcessDefinitionKeys(Arrays.asList(processDefinitionKeys));
        return this;
    }

    /**
     * Builds the coverage rule.
     *
     * @return
     */
    public FlowCovProcessEngineRule build() {
        return rule;
    }
}
