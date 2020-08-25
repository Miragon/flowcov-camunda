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
import io.flowcov.camunda.model.CoveredFlowNode;
import io.flowcov.camunda.model.CoveredSequenceFlow;
import lombok.val;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.logging.Logger;

/**
 * Listener taking note of covered sequence flows.
 */
public class PathCoverageExecutionListener implements ExecutionListener {

    private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    /**
     * The state of the currently running coverage test.
     */
    private CoverageTestRunState coverageTestRunState;

    public PathCoverageExecutionListener(CoverageTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        if (coverageTestRunState == null) {
            logger.warning("Coverage execution listener in use but no coverage run state assigned!");
            return;
        }

        val repositoryService = execution.getProcessEngineServices().getRepositoryService();

        // Get the process definition in order to obtain the key
        val processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(
                execution.getProcessDefinitionId()).singleResult();

        val transitionId = execution.getCurrentTransitionId();

        // Record sequence flow coverage
        val coveredSequenceFlow = new CoveredSequenceFlow(processDefinition.getKey(),
                transitionId);
        coverageTestRunState.addCoveredElement(coveredSequenceFlow);

        // Record possible event coverage
        handleEvent(transitionId, processDefinition, repositoryService);
    }

    /**
     * Events aren't reported like SequenceFlows and Activities, so we need
     * special handling. If a sequence flow has an event as the source or the
     * target, we add it to the coverage. It's pretty straight forward if a
     * sequence flow is active, then it's source has been covered anyway and it
     * will most definitely arrive at its target.
     *
     * @param transitionId
     * @param processDefinition
     * @param repositoryService
     */
    private void handleEvent(String transitionId, ProcessDefinition processDefinition,
                             RepositoryService repositoryService) {

        val modelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        val modelElement = modelInstance.getModelElementById(transitionId);
        if (modelElement.getElementType().getInstanceType() == SequenceFlow.class) {

            val sequenceFlow = (SequenceFlow) modelElement;

            // If there is an event at the sequence flow source add it to the
            // coverage
            val source = sequenceFlow.getSource();
            addEventToCoverage(processDefinition, source);

            // If there is an event at the sequence flow target add it to the
            // coverage
            val target = sequenceFlow.getTarget();
            addEventToCoverage(processDefinition, target);
        }
    }

    private void addEventToCoverage(ProcessDefinition processDefinition, FlowNode node) {

        if (node instanceof IntermediateThrowEvent) {

            val coveredElement = new CoveredFlowNode(processDefinition.getKey(), node.getId());
            // We consider entered throw elements as also ended
            coveredElement.setEnded(true);

            coverageTestRunState.addCoveredElement(coveredElement);
        }
    }

}
