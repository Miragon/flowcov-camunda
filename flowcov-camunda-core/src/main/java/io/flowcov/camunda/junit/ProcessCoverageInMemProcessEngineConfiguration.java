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

package io.flowcov.camunda.junit;

import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;

/**
 * Standalone in memory process engine configuration additionally configuring
 * flow node, sequence flow and compensation listeners for process coverage
 * testing.
 */
public class ProcessCoverageInMemProcessEngineConfiguration extends StandaloneInMemProcessEngineConfiguration {

    @Override
    protected void init() {
        ProcessCoverageConfigurator.initializeProcessCoverageExtensions(this);
        super.init();
    }

}
