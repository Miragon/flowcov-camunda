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

import io.flowcov.camunda.junit.rules.CoverageTestRunState;
import io.flowcov.camunda.model.CoveredDmnRule;
import io.flowcov.camunda.model.CoveredFlowNode;
import io.flowcov.camunda.util.Api;
import lombok.val;
import org.camunda.bpm.engine.history.HistoricDecisionOutputInstance;
import org.camunda.bpm.engine.impl.history.event.*;
import org.camunda.bpm.engine.impl.history.handler.DbHistoryEventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extends the {@link DbHistoryEventHandler} in order to notify the process test
 * coverage of a covered activity.
 */
public class FlowNodeHistoryEventHandler extends DbHistoryEventHandler {

    private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    /**
     * The state of the currently running coverage test.
     */
    private CoverageTestRunState coverageTestRunState;

    public FlowNodeHistoryEventHandler() {
    }

    @Override
    public void handleEvent(final HistoryEvent historyEvent) {
        super.handleEvent(historyEvent);

        if (coverageTestRunState == null) {
            logger.warning("Coverage history event listener in use but no coverage run state assigned!");
            return;
        }

        if (historyEvent instanceof HistoricActivityInstanceEventEntity) {

            final HistoricActivityInstanceEventEntity activityEvent = (HistoricActivityInstanceEventEntity) historyEvent;

            if (activityEvent.getActivityType().equals("multiInstanceBody"))
                return;

            final CoveredFlowNode coveredActivity =
                    new CoveredFlowNode(historyEvent.getProcessDefinitionKey(), activityEvent.getActivityId());

            // Cover event start
            if (this.isInitialEvent(historyEvent)) {
                coverageTestRunState.addCoveredElement(coveredActivity);
            }
            // Cover event end
            else if (this.isEndEvent(historyEvent)) {
                coverageTestRunState.endCoveredElement(coveredActivity);
            }

        }

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


    @Override
    protected boolean isInitialEvent(final HistoryEvent historyEvent) {
        final String isInitialEvent = "isInitialEvent";
        return Api.feature(DbHistoryEventHandler.class, isInitialEvent, HistoryEvent.class).isSupported() ? super.isInitialEvent(historyEvent) :
                (Boolean) Api.feature(DbHistoryEventHandler.class, isInitialEvent, String.class).invoke(this, historyEvent.getEventType());
    }

    /**
     * Aimed to be the opposite of
     * {@link DbHistoryEventHandler#isInitialEvent(HistoryEvent event)}
     * for the purpose of the process test coverage - which just deals with
     * history events of type HistoricActivityInstanceEventEntity.
     * <p>
     * Future versions of Camunda will eventually introduce additional events
     * requiring this method to be updated. Be careful to deal with backwards
     * compatibility issues when doing that.
     *
     * @param historyEvent
     * @return
     */
    private boolean isEndEvent(final HistoryEvent historyEvent) {

        final EnumSet<HistoryEventTypes> endEventTypes = EnumSet.of(
                HistoryEventTypes.ACTIVITY_INSTANCE_END,
                HistoryEventTypes.PROCESS_INSTANCE_END,
                HistoryEventTypes.TASK_INSTANCE_COMPLETE
        );

        // They should have handled compare/equals in the enum itself
        for (final HistoryEventTypes endEventType : endEventTypes) {

            if (historyEvent.getEventType().equals(endEventType.getEventName())) {

                return true;
            }
        }

        return false;
    }

    public void setCoverageTestRunState(final CoverageTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }

}
