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
import lombok.val;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

/**
 * Adds a PathCoverageExecutionListener to every transition.
 */
public class PathCoverageParseListener extends AbstractBpmnParseListener {

    /**
     * The state of the coverage test run.
     */
    private CoverageTestRunState coverageTestRunState;

    @Override
    public void parseSequenceFlow(Element sequenceFlowElement, ScopeImpl scopeElement,
                                  org.camunda.bpm.engine.impl.pvm.process.TransitionImpl transition) {

        val pathCoverageExecutionListener = new PathCoverageExecutionListener(
                coverageTestRunState);
        transition.addListener(ExecutionListener.EVENTNAME_TAKE, pathCoverageExecutionListener);

    }

    @Override
    public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope,
                                            org.camunda.bpm.engine.impl.pvm.process.ActivityImpl activity) {

        val startListener = new IntermediateEventExecutionListener(
                coverageTestRunState);
        activity.addListener(ExecutionListener.EVENTNAME_START, startListener);

        val endListener = new IntermediateEventExecutionListener(
                coverageTestRunState);
        activity.addListener(ExecutionListener.EVENTNAME_END, endListener);
    }

    public void setCoverageTestRunState(CoverageTestRunState coverageTestRunState) {
        this.coverageTestRunState = coverageTestRunState;
    }
}
