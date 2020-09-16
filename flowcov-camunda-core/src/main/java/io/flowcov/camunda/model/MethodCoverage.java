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
import lombok.val;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Coverage of an individual test method.
 * <p>
 * A test method annotated with @Deployment does an independent deployment of the listed
 * resources, hence this coverage is equivalent to a deployment coverage.
 */
@ToString
public class MethodCoverage implements AggregatedCoverage {

    /**
     * The ID of the deployment done for the method.
     */
    private final String deploymentId;


    /**
     * The name of the test method.
     */
    @Getter
    private final String name;

    /**
     * Map holding the coverages for each process definition (accessed by the process definition key).
     */
    private Map<String, ProcessCoverage> processDefinitionKeyToProcessCoverage = new HashMap<>();

    private Map<String, DecisionCoverage> decisionKeyToDecisionCoverage = new HashMap<>();


    public MethodCoverage(final String deploymentId, final String name) {
        this.deploymentId = deploymentId;
        this.name = name;
    }

    /**
     * Add a process coverage to the method coverage.
     *
     * @param processCoverage
     */
    public void addProcessCoverage(final ProcessCoverage processCoverage) {

        final String processDefinitionId = processCoverage.getProcessDefinitionKey();
        processDefinitionKeyToProcessCoverage.put(processDefinitionId, processCoverage);
    }

    public void addDecisionCoverage(final DecisionCoverage decisionCoverage) {

        final String decisionDefinitionId = decisionCoverage.getDecisionDefinitionKey();
        decisionKeyToDecisionCoverage.put(decisionDefinitionId, decisionCoverage);
    }

    /**
     * Add a covered element to the method coverage.
     * The element is added according to the object fields.
     *
     * @param element
     */
    public void addCoveredElement(final CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.addCoveredElement(element);
    }

    /**
     * Mark a covered element execution as ended.
     *
     * @param element
     */
    public void endCoveredElement(final CoveredElement element) {

        final String processDefinitionKey = element.getProcessDefinitionKey();
        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);

        processCoverage.endCoveredElement(element);
    }


    public void addCoveredDmnRules(final List<CoveredDmnRule> coveredDmnRules) {

        val decisionKey2coveredRuleMap = coveredDmnRules.stream()
                .collect(Collectors.groupingBy(CoveredDmnRule::getDecisionKey));
        decisionKey2coveredRuleMap.forEach(this::addCoveredDmnRules);
    }

    private void addCoveredDmnRules(final String decisionKey, final List<CoveredDmnRule> coveredDmnRules) {
        val decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        decisionCoverage.addCoveredDmnRule(coveredDmnRules);
    }

    /**
     * Retrieves a set of covered flow nodes of the process definitions deployed by this test method.
     *
     * @return
     */
    public List<CoveredFlowNode> getCoveredFlowNodes() {
        return processDefinitionKeyToProcessCoverage.values().stream()
                .map(ProcessCoverage::getCoveredFlowNodes)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<CoveredSequenceFlow> getCoveredSequenceFlows() {
        return processDefinitionKeyToProcessCoverage.values().stream()
                .map(ProcessCoverage::getCoveredSequenceFlows)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Integer getDecisionRuleCount(final String decisionKey) {

        final DecisionCoverage decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        return decisionCoverage.getDefinitionDecisionRules().size();
    }

    public Integer getProcessElementCount(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getDefinitionFlowNodes().size() + processCoverage.getDefinitionSequenceFlows().size();
    }

    @Override
    public List<CoveredFlowNode> getCoveredFlowNodes(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredFlowNodes();
    }

    @Override
    public Set<CoveredDmnRule> getCoveredDecisionRules(final String decisionKey) {

        final DecisionCoverage decisionCoverage = decisionKeyToDecisionCoverage.get(decisionKey);
        return decisionCoverage.getCoveredDmnRules();
    }


    @Override
    public List<CoveredSequenceFlow> getCoveredSequenceFlows(final String processDefinitionKey) {

        final ProcessCoverage processCoverage = processDefinitionKeyToProcessCoverage.get(processDefinitionKey);
        return processCoverage.getCoveredSequenceFlows();
    }

    @Override
    public Set<ProcessDefinition> getProcessDefinitions() {

        final Set<ProcessDefinition> processDefinitions = new TreeSet<ProcessDefinition>(
                new Comparator<ProcessDefinition>() {

                    // Avoid removing process definitions with the same key, but coming from different BPMNs.
                    @Override
                    public int compare(final ProcessDefinition o1, final ProcessDefinition o2) {
                        return o1.getResourceName().compareTo(o2.getResourceName());
                    }
                });

        for (final ProcessCoverage processCoverage : processDefinitionKeyToProcessCoverage.values()) {
            processDefinitions.add(processCoverage.getProcessDefinition());
        }

        return processDefinitions;
    }


    @Override
    public Set<DecisionDefinition> getDecisionDefinitions() {

        final Set<DecisionDefinition> decisionDefinitions = new TreeSet<DecisionDefinition>(
                new Comparator<DecisionDefinition>() {

                    // Avoid removing process definitions with the same key, but coming from different BPMNs.
                    @Override
                    public int compare(final DecisionDefinition o1, final DecisionDefinition o2) {
                        return (o1.getKey() + o1.getResourceName()).compareTo(o2.getKey() + o2.getResourceName());
                    }
                });

        for (final DecisionCoverage decisionCoverage : decisionKeyToDecisionCoverage.values()) {
            decisionDefinitions.add(decisionCoverage.getDecisionDefintion());
        }

        return decisionDefinitions;
    }


}
