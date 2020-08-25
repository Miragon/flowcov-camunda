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

/**
 * An activity covered by a test.
 */
public class CoveredFlowNode extends CoveredElement {

    /**
     * Element ID of the activity.
     */
    private final String flowNodeId;

    /**
     * A flow node object is created in the coverage once it has started
     * execution. This flag is set to true once the flow node has finished (end
     * event).
     */
    private boolean ended = false;

    public CoveredFlowNode(String processDefinitionKey, String flowNodeId) {
        this.flowNodeId = flowNodeId;
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public String getElementId() {
        return flowNodeId;
    }

    public boolean hasEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    @Override
    public String toString() {
        return "CoveredActivity [flowNodeId=" + flowNodeId + ", processDefinitionKey=" + processDefinitionKey + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((flowNodeId == null) ? 0 : flowNodeId.hashCode());
        result = prime * result + ((processDefinitionKey == null) ? 0 : processDefinitionKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoveredFlowNode other = (CoveredFlowNode) obj;
        if (flowNodeId == null) {
            if (other.flowNodeId != null)
                return false;
        } else if (!flowNodeId.equals(other.flowNodeId))
            return false;
        if (processDefinitionKey == null) {
            if (other.processDefinitionKey != null)
                return false;
        } else if (!processDefinitionKey.equals(other.processDefinitionKey))
            return false;
        return true;
    }

}
