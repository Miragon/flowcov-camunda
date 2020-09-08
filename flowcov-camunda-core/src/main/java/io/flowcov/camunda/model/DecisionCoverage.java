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

import lombok.Getter;
import lombok.ToString;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionRule;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Coverage of a process definition.
 */
@ToString
public class DecisionCoverage {


    private static final Logger logger = Logger.getLogger(DecisionCoverage.class.getCanonicalName());

    /**
     * The decision definition being covered.
     */
    private DecisionDefinition decisionDefinition;

    /**
     * Covered Dmn Rules
     */
    @Getter
    private Set<CoveredDmnRule> coveredDmnRules = new HashSet<>();


    /**
     * Decision Rules of the definition
     */
    @Getter
    private Set<DecisionRule> definitionDecisionRules;

    /**
     * Constructor assembling a pristine decision coverage object from the
     * decision definition and DMN model information retrieved from the process
     * engine.
     *
     * @param processEngine
     * @param decisionDefinition
     */
    public DecisionCoverage(final ProcessEngine processEngine, final DecisionDefinition decisionDefinition) {

        this.decisionDefinition = decisionDefinition;

        final DmnModelInstance modelInstance = processEngine.getRepositoryService().getDmnModelInstance(
                this.getDecisionDefinitionId());
        definitionDecisionRules = this.getAssignedRules(modelInstance.getModelElementsByType(DecisionRule.class));
    }

    private Set<DecisionRule> getAssignedRules(final Collection<DecisionRule> rules) {

        return rules.stream()
                .filter(this::isAssigned)
                .collect(Collectors.toSet());
    }

    private boolean isAssigned(final ModelElementInstance node) {

        if (node == null) {
            return false;
        }

        if (node instanceof Decision) {
            return ((Decision) node).getId().equals(decisionDefinition.getKey());
        }

        return this.isAssigned(node.getParentElement());
    }

    /**
     * Adds a covered DMN Rule to the coverage.
     *
     * @param rules
     */
    public void addCoveredDmnRule(final List<CoveredDmnRule> rules) {
        this.coveredDmnRules.addAll(rules);
    }

    /**
     * Retrieves the coverage percentage for all elements.
     *
     * @return
     */
    public double getCoveragePercentage() {
        return ((double) this.getNumberOfAllCovered()) / ((double) this.getNumberOfAllDefined());
    }

    public Set<String> getCoveredDmnRuleIds() {

        return this.coveredDmnRules.stream()
                .map(CoveredDmnRule::getRuleId)
                .collect(Collectors.toSet());
    }

    public DecisionDefinition getDecisionDefintion() {
        return decisionDefinition;
    }

    public String getDecisionDefinitionId() {
        return decisionDefinition.getId();
    }

    public String getDecisionDefinitionKey() {
        return decisionDefinition.getKey();
    }


    /**
     * Retrieves the number of covered flow node and sequence flow elements.
     *
     * @return
     */
    private int getNumberOfAllCovered() {
        return coveredDmnRules.size();
    }

    /**
     * Retrieves the number of flow node and sequence flow elements for the
     * process definition.
     *
     * @return
     */
    private int getNumberOfAllDefined() {
        return definitionDecisionRules.size();
    }

}
