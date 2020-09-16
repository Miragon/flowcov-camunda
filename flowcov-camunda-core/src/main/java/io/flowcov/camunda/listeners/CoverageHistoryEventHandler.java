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

package io.flowcov.camunda.listeners;

import io.flowcov.camunda.junit.FlowCovTestRunState;
import io.flowcov.camunda.model.CoveredDmnRule;
import lombok.val;
import org.camunda.bpm.engine.history.HistoricDecisionOutputInstance;
import org.camunda.bpm.engine.impl.history.event.HistoricDecisionEvaluationEvent;
import org.camunda.bpm.engine.impl.history.event.HistoricDecisionInstanceEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.DbHistoryEventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extends the {@link DbHistoryEventHandler} in order to notify the process test
 * coverage of a covered rule.
 */
public class CoverageHistoryEventHandler extends DbHistoryEventHandler {

    private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    /**
     * The state of the currently running coverage test.
     */
    private FlowCovTestRunState coverageTestRunState;

    public CoverageHistoryEventHandler() {
    }

    @Override
    public void handleEvent(final HistoryEvent historyEvent) {
        super.handleEvent(historyEvent);

        if (historyEvent instanceof HistoricDecisionEvaluationEvent) {

            val decisionEvent = (HistoricDecisionEvaluationEvent) historyEvent;
            val rules = this.parseHistoricDecisionInstanceEntity(decisionEvent.getRootHistoricDecisionInstance());

            if (decisionEvent.getRequiredHistoricDecisionInstances() != null && !decisionEvent.getRequiredHistoricDecisionInstances().isEmpty()) {
                val requiredRules = decisionEvent.getRequiredHistoricDecisionInstances()
                        .stream()
                        .map(this::parseHistoricDecisionInstanceEntity)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                rules.addAll(requiredRules);
            }

            coverageTestRunState.addCoveredRules(rules);
        }

    }

    private List<CoveredDmnRule> parseHistoricDecisionInstanceEntity(final HistoricDecisionInstanceEntity instance) {

        if (instance.getOutputs() == null || instance.getOutputs().isEmpty()) {
            return new ArrayList<>();
        }

        return instance.getOutputs().stream()
                .map(HistoricDecisionOutputInstance::getRuleId)
                .distinct()
                .map(rule -> CoveredDmnRule.builder()
                        .decisionKey(instance.getDecisionDefinitionKey())
                        .ruleId(rule)
                        .drdKey(instance.getDecisionRequirementsDefinitionKey())
                        .build()
                )
                .collect(Collectors.toList());
    }

    public void setCoverageTestRunState(final FlowCovTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }

}
