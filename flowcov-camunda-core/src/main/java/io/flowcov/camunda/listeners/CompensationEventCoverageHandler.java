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
import io.flowcov.camunda.model.CoveredFlowNode;
import io.flowcov.camunda.util.Api;
import org.camunda.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.camunda.bpm.engine.impl.event.CompensationEventHandler;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import java.lang.invoke.MethodHandle;

/**
 * Handler for Compensation Boundary Events
 */
public class CompensationEventCoverageHandler extends CompensationEventHandler {

    private FlowCovTestRunState coverageTestRunState;
    private MethodHandle handleEvent;

    @Override
    public void handleEvent(final EventSubscriptionEntity eventSubscription, final Object payload, final Object localPayload,
                            final String businessKey, final CommandContext commandContext) {
        this.addCompensationEventCoverage(eventSubscription);
        super.handleEvent(eventSubscription, payload, localPayload, businessKey, commandContext);
    }

    private void addCompensationEventCoverage(final EventSubscriptionEntity eventSubscription) {
        if (Api.Camunda.supportsCompensationEventCoverage()) {

            final ActivityImpl activity = eventSubscription.getActivity();

            // Get process definition key
            final ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) activity.getProcessDefinition();
            final String processDefinitionKey = processDefinition.getKey();

            // Get compensation boundary event ID
            final ActivityImpl sourceEvent = (ActivityImpl) activity.getProperty(
                    BpmnProperties.COMPENSATION_BOUNDARY_EVENT.getName());

            if (sourceEvent != null) {

                final String sourceEventId = sourceEvent.getActivityId();

                // Register covered element
                final CoveredFlowNode compensationBoundaryEvent = new CoveredFlowNode(processDefinitionKey, sourceEventId, sourceEventId + ":" + eventSubscription.getId(), "boundaryEvent");
                coverageTestRunState.addCoveredElement(compensationBoundaryEvent);
                coverageTestRunState.endCoveredElement(compensationBoundaryEvent);

            }
        }
    }

    public void setCoverageTestRunState(final FlowCovTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }
}