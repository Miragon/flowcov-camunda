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
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Coverage of a process definition.
 */
@ToString
@Getter
public class ProcessCoverage {

    private static final Logger logger = Logger.getLogger(ProcessCoverage.class.getCanonicalName());

    /**
     * The process definition being covered.
     */
    private ProcessDefinition processDefinition;

    /**
     * Covered flow nodes.
     */
    private List<CoveredFlowNode> coveredFlowNodes = new ArrayList<>();

    /**
     * Flow nodes of the process definition.
     */
    private Set<FlowNode> definitionFlowNodes;

    /**
     * Covered sequence flows.
     */
    private List<CoveredSequenceFlow> coveredSequenceFlows = new ArrayList<>();

    /**
     * Sequence flows of the process definition.
     */
    private Set<SequenceFlow> definitionSequenceFlows;

    /**
     * Constructor assembling a pristine process coverage object from the
     * process definition and BPMN model information retrieved from the process
     * engine.
     *
     * @param processEngine
     * @param processDefinition
     */
    public ProcessCoverage(final ProcessEngine processEngine, final ProcessDefinition processDefinition) {

        this.processDefinition = processDefinition;

        final BpmnModelInstance modelInstance = processEngine.getRepositoryService().getBpmnModelInstance(
                this.getProcessDefinitionId());

        definitionFlowNodes = this.getExecutableFlowNodes(modelInstance.getModelElementsByType(FlowNode.class));
        definitionSequenceFlows = this.getExecutableSequenceNodes(modelInstance.getModelElementsByType(SequenceFlow.class));

    }

    public String getProcessDefinitionId() {
        return processDefinition.getId();
    }

    public String getProcessDefinitionKey() {
        return processDefinition.getKey();
    }

    private Set<FlowNode> getExecutableFlowNodes(final Collection<FlowNode> flowNodes) {
        return flowNodes.stream()
                .filter(this::isExecutable)
                .collect(Collectors.toSet());
    }

    private boolean isExecutable(final ModelElementInstance node) {

        if (node == null) {
            return false;
        }

        if (node instanceof org.camunda.bpm.model.bpmn.instance.Process) {
            return ((org.camunda.bpm.model.bpmn.instance.Process) node).isExecutable();
        }

        return this.isExecutable(node.getParentElement());
    }

    private Set<SequenceFlow> getExecutableSequenceNodes(final Collection<SequenceFlow> sequenceFlows) {
        return sequenceFlows.stream()
                .filter(s -> definitionFlowNodes.contains(s.getSource()))
                .collect(Collectors.toSet());
    }

    /**
     * Adds a covered element to the coverage.
     *
     * @param element
     */
    public void addCoveredElement(final CoveredElement element) {

        if (element instanceof CoveredFlowNode) {

            coveredFlowNodes.add((CoveredFlowNode) element);

        } else if (element instanceof CoveredSequenceFlow) {

            coveredSequenceFlows.add((CoveredSequenceFlow) element);

        } else {
            logger.log(Level.SEVERE,
                    "Attempted adding unsupported element to process coverage. Process definition ID: {0} Element ID: {1}",
                    new Object[]{element.getProcessDefinitionKey(), element.getElementId()});
        }

    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param element A search object. Only the original object in the
     *                coveredFlowNodes Set will be modified.
     */
    public void endCoveredElement(final CoveredElement element) {

        // Only flow nodes can be ended
        if (element instanceof CoveredFlowNode) {
            final CoveredFlowNode endedFlowNode = (CoveredFlowNode) element;

            coveredFlowNodes.stream()
                    .filter(obj -> obj.getFlowNodeInstanceId().equals(endedFlowNode.getFlowNodeInstanceId()))
                    .findFirst().get().setExecutionEndCoutner(endedFlowNode.getExecutionEndCoutner());
        } else {
            logger.log(Level.SEVERE,
                    "Attempted ending unsupported element to process coverage. Process definition ID: {0} Element ID: {1}",
                    new Object[]{element.getProcessDefinitionKey(), element.getElementId()});
        }
    }

    public List<String> getCoveredFlowNodeIds() {
        return coveredFlowNodes.stream()
                .map(CoveredFlowNode::getElementId)
                .collect(Collectors.toList());
    }

    public List<String> getCoveredSequenceFlowIds() {
        return coveredSequenceFlows.stream()
                .map(CoveredSequenceFlow::getElementId)
                .collect(Collectors.toList());
    }
}
