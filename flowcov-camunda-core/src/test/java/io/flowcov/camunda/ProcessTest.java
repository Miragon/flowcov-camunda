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

package io.flowcov.camunda;

import io.flowcov.camunda.junit.FlowCovProcessEngineRuleBuilder;
import lombok.val;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

@Deployment(resources = {"bpmn/StartWithFlowCov.bpmn", "dmn/DecideOnUsage.dmn"})
public class ProcessTest {

    @Rule
    @ClassRule
    public static ProcessEngineRule rule = FlowCovProcessEngineRuleBuilder.create().build();

    private static String PROCESS_KEY = "StartWithFlowCov";

    private static String TASK_REGISTER = "Task_RegisterOnFlowCov";
    private static String TASK_INSTALL = "Task_InstallOnPremise";
    private static String TASK_CREATE_REPOSITORY = "Task_CreateRepository";
    private static String TASK_START_USING = "Task_StartUsingFlowCov";
    private static String TASK_DISCUSS_FEEDBACK = "Task_DiscussFeedback";

    private static String EVENT_FLOWCOV_USED = "EndEvent_FlowCovIsUsed";
    private static String EVENT_FEEDBACK_PROCESSED = "EndEvent_FeedbackProcessed";

    private static String MESSAGE_FEEDBACK_RECEIVED = "feedbackReceived";

    @Test
    public void deploy_process() {
        // just deployment
    }

    @Test
    public void start_process() {
        val instance = runtimeService().startProcessInstanceByKey(
                PROCESS_KEY,
                withVariables("cloudAccess", true, "openSource", true));
        assertThat(instance).isWaitingAt(TASK_REGISTER);
    }

    @Test
    public void register_and_use() {

        val instance = runtimeService().startProcessInstanceByKey(
                PROCESS_KEY,
                withVariables("cloudAccess", true, "openSource", true));

        assertThat(instance).isWaitingAt(TASK_REGISTER);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_CREATE_REPOSITORY);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_START_USING);
        complete(task());

        assertThat(instance).isEnded().hasPassed(EVENT_FLOWCOV_USED);
    }

    @Test
    public void install_and_use() {
        val instance = runtimeService().startProcessInstanceByKey(
                PROCESS_KEY,
                withVariables("cloudAccess", false, "openSource", false));

        assertThat(instance).isWaitingAt(TASK_INSTALL);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_CREATE_REPOSITORY);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_START_USING);
        complete(task());

        assertThat(instance).isEnded().hasPassed(EVENT_FLOWCOV_USED);
    }

    @Test
    public void register_and_give_feedback() {
        val instance = runtimeService().startProcessInstanceByKey(
                PROCESS_KEY,
                withVariables("cloudAccess", true, "openSource", true));

        assertThat(instance).isWaitingAt(TASK_REGISTER);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_CREATE_REPOSITORY);
        complete(task());

        assertThat(instance).isWaitingAt(TASK_START_USING);

        runtimeService().correlateMessage(MESSAGE_FEEDBACK_RECEIVED);
        assertThat(instance).isWaitingAt(TASK_DISCUSS_FEEDBACK, TASK_START_USING);
        complete(task(TASK_DISCUSS_FEEDBACK));

        assertThat(instance).hasPassed(EVENT_FEEDBACK_PROCESSED);

        complete(task(TASK_START_USING));
        assertThat(instance).isEnded().hasPassed(EVENT_FLOWCOV_USED);

    }

}
