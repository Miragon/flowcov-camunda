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

import org.camunda.bpm.engine.ProcessEngine;

import java.util.Arrays;

/**
 * Fluent Builder for FlowCovProcessEngineRule.
 */
public class FlowCovProcessEngineRuleBuilder {

    private final FlowCovProcessEngineRule rule;

    private FlowCovProcessEngineRuleBuilder() {
        this.rule = new FlowCovProcessEngineRule();
    }

    private FlowCovProcessEngineRuleBuilder(final ProcessEngine processEngine) {
        this.rule = new FlowCovProcessEngineRule(processEngine);
    }

    /**
     * Creates a TestCoverageProcessEngineRuleBuilder with the default class
     * coverage assertion property activated.
     *
     * @return
     */
    public static FlowCovProcessEngineRuleBuilder create() {
        return createBase();
    }

    /**
     * Creates a TestCoverageProcessEngineRuleBuilder with the default class
     * coverage assertion property activated.
     *
     * @return
     */
    public static FlowCovProcessEngineRuleBuilder create(final ProcessEngine processEngine) {
        return createBase(processEngine);
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
    public static FlowCovProcessEngineRuleBuilder createBase(final ProcessEngine processEngine) {
        return new FlowCovProcessEngineRuleBuilder(processEngine);
    }

    /**
     * Configures CoverageTestRunStateFactory used to create CoverageTestRunState. Useful for sharing state between several test-classes
     *
     * @param coverageTestRunStateFactory
     * @return
     */
    public FlowCovProcessEngineRuleBuilder setCoverageTestRunStateFactory(final FlowCovTestRunStateFactory coverageTestRunStateFactory) {
        rule.setCoverageTestRunStateFactory(coverageTestRunStateFactory);
        return this;
    }

    public FlowCovProcessEngineRuleBuilder excludeProcessDefinitionKeys(final String... processDefinitionKeys) {
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
