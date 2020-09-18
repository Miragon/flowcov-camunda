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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * An activity covered by a test.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CoveredFlowNode extends CoveredElement {

    private final String flowNodeId;

    private final String flowNodeInstanceId;

    private final String type;

    private Integer executionEndCoutner;

    public CoveredFlowNode(final String processDefinitionKey, final String flowNodeId, final String flowNodeInstanceId, final String type) {
        this.flowNodeId = flowNodeId;
        this.processDefinitionKey = processDefinitionKey;
        this.flowNodeInstanceId = flowNodeInstanceId;
        this.type = type;
    }

    @Override
    public String getElementId() {
        return flowNodeId;
    }

}
