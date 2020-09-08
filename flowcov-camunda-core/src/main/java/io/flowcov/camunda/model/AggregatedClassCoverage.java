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

import io.flowcov.camunda.util.CoveredElementComparator;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test class coverage model. The class coverage is an aggregation of all test method coverages.
 */
public class AggregatedClassCoverage implements AggregatedCoverage {

    private List<ClassCoverage> classCoverages;
    private Map<String, List<ClassCoverage>> classCoveragesByProcessDefinitionKey = new HashMap<>();
    private Map<String, List<ClassCoverage>> classCoveragesByDecisionKey = new HashMap<>();

    public AggregatedClassCoverage(final List<ClassCoverage> classCoverages) {
        this.classCoverages = classCoverages;
        this.organizeByProcessDefinitionKey();
        this.organizeByDecisionKey();
    }

    private void organizeByProcessDefinitionKey() {
        for (final ClassCoverage classCoverage : classCoverages) {
            for (final ProcessDefinition processDefinition : classCoverage.getProcessDefinitions()) {
                final String key = processDefinition.getKey();
                if (!classCoveragesByProcessDefinitionKey.containsKey(key)) {
                    classCoveragesByProcessDefinitionKey.put(key, new ArrayList<>());
                }
                classCoveragesByProcessDefinitionKey.get(key).add(classCoverage);
            }
        }
    }

    private void organizeByDecisionKey() {
        for (final ClassCoverage classCoverage : classCoverages) {
            for (final DecisionDefinition decisionDefinition : classCoverage.getDecisionDefinitions()) {
                final String key = decisionDefinition.getKey();
                if (!classCoveragesByDecisionKey.containsKey(key)) {
                    classCoveragesByDecisionKey.put(key, new ArrayList<>());
                }
                classCoveragesByDecisionKey.get(key).add(classCoverage);
            }
        }
    }

    @Override
    public Set<String> getCoveredFlowNodeIds(final String processDefinitionKey) {

        final Set<String> coveredFlowNodeIds = new HashSet<String>();
        for (final ClassCoverage classCoverage : classCoveragesByProcessDefinitionKey.get(processDefinitionKey)) {

            coveredFlowNodeIds.addAll(classCoverage.getCoveredFlowNodeIds(processDefinitionKey));
        }

        return coveredFlowNodeIds;
    }

    @Override
    public Set<CoveredFlowNode> getCoveredFlowNodes(final String processDefinitionKey) {

        final Set<CoveredFlowNode> coveredFlowNodes = new TreeSet<CoveredFlowNode>(CoveredElementComparator.instance());

        for (final ClassCoverage classCoverage : classCoveragesByProcessDefinitionKey.get(processDefinitionKey)) {

            coveredFlowNodes.addAll(classCoverage.getCoveredFlowNodes(processDefinitionKey));
        }

        return coveredFlowNodes;
    }

    @Override
    public Set<String> getCoveredSequenceFlowIds(final String processDefinitionKey) {

        final Set<String> coveredSequenceFlowIds = new HashSet<String>();
        for (final ClassCoverage classCoverage : classCoveragesByProcessDefinitionKey.get(processDefinitionKey)) {

            coveredSequenceFlowIds.addAll(classCoverage.getCoveredSequenceFlowIds(processDefinitionKey));
        }

        return coveredSequenceFlowIds;
    }

    @Override
    public Set<CoveredDmnRule> getCoveredDecisionRules(final String decisionKey) {
        return classCoveragesByDecisionKey.get(decisionKey).stream()
                .map(obj -> obj.getCoveredDecisionRules(decisionKey))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<CoveredSequenceFlow> getCoveredSequenceFlows(final String processDefinitionKey) {

        final Set<CoveredSequenceFlow> coveredSequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());
        for (final ClassCoverage classCoverage : classCoveragesByProcessDefinitionKey.get(processDefinitionKey)) {

            coveredSequenceFlows.addAll(classCoverage.getCoveredSequenceFlows(processDefinitionKey));
        }

        return coveredSequenceFlows;
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

        for (final ClassCoverage classCoverage : classCoverages) {
            processDefinitions.addAll(classCoverage.getProcessDefinitions());
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

        for (final ClassCoverage classCoverage : classCoverages) {
            decisionDefinitions.addAll(classCoverage.getDecisionDefinitions());
        }

        return decisionDefinitions;
    }

    @Override
    public double getCoveragePercentage() {

        final Set<ProcessDefinition> processDefinitions = this.getProcessDefinitions();

        final Set<FlowNode> definitionsFlowNodes = new HashSet<FlowNode>();
        final Set<SequenceFlow> definitionsSequenceFlows = new HashSet<SequenceFlow>();

        final Set<CoveredFlowNode> coveredFlowNodes = new TreeSet<CoveredFlowNode>(CoveredElementComparator.instance());
        final Set<CoveredSequenceFlow> coveredSequenceFlows = new TreeSet<CoveredSequenceFlow>(CoveredElementComparator.instance());

        for (final ProcessDefinition processDefinition : processDefinitions) {
            final String processDefinitionKey = processDefinition.getKey();

            final MethodCoverage deploymentWithProcessDefinition = this.getMethodCoverage(processDefinitionKey);

            definitionsFlowNodes.addAll(deploymentWithProcessDefinition.getProcessDefinitionsFlowNodes(processDefinitionKey));
            definitionsSequenceFlows.addAll(deploymentWithProcessDefinition.getProcessDefinitionsSequenceFlows(processDefinitionKey));

            coveredFlowNodes.addAll(this.getCoveredFlowNodes(processDefinitionKey));
            coveredSequenceFlows.addAll(this.getCoveredSequenceFlows(processDefinitionKey));
        }

        final double bpmnElementsCount = definitionsFlowNodes.size() + definitionsSequenceFlows.size();
        final double coveredElementsCount = coveredFlowNodes.size() + coveredSequenceFlows.size();

        return coveredElementsCount / bpmnElementsCount;
    }

    @Override
    public double getCoveragePercentage(final String processDefinitionKey) {
        final MethodCoverage deploymentWithProcessDefinition = this.getMethodCoverage(processDefinitionKey);

        final Set<FlowNode> definitionsFlowNodes = deploymentWithProcessDefinition.getProcessDefinitionsFlowNodes(processDefinitionKey);
        final Set<SequenceFlow> definitionsSequenceFlows = deploymentWithProcessDefinition.getProcessDefinitionsSequenceFlows(processDefinitionKey);

        final Set<CoveredFlowNode> coveredFlowNodes = this.getCoveredFlowNodes(processDefinitionKey);
        final Set<CoveredSequenceFlow> coveredSequenceFlows = this.getCoveredSequenceFlows(processDefinitionKey);

        final double bpmnElementsCount = definitionsFlowNodes.size() + definitionsSequenceFlows.size();
        final double coveredElementsCount = coveredFlowNodes.size() + coveredSequenceFlows.size();

        return coveredElementsCount / bpmnElementsCount;
    }

    private MethodCoverage getMethodCoverage(final String processDefinitionKey) {
        return classCoveragesByProcessDefinitionKey.get(processDefinitionKey).get(0).getAnyMethodCoverage();
    }
}
