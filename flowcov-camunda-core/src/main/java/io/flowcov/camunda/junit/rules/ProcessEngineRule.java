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

package io.flowcov.camunda.junit.rules;

import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.TestHelper;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ProcessEngineRule extends TestWatcher implements ProcessEngineServices {
    protected String configurationResource;
    protected String configurationResourceCompat;
    protected String deploymentId;
    protected List<String> additionalDeployments;
    protected boolean ensureCleanAfterTest;
    protected ProcessEngine processEngine;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected HistoryService historyService;
    protected IdentityService identityService;
    protected ManagementService managementService;
    protected FormService formService;
    protected FilterService filterService;
    protected AuthorizationService authorizationService;
    protected CaseService caseService;
    protected ExternalTaskService externalTaskService;
    protected DecisionService decisionService;

    public ProcessEngineRule() {
        this(false);
    }

    public ProcessEngineRule(final boolean ensureCleanAfterTest) {
        this.configurationResource = "camunda.cfg.xml";
        this.configurationResourceCompat = "activiti.cfg.xml";
        this.deploymentId = null;
        this.additionalDeployments = new ArrayList();
        this.ensureCleanAfterTest = false;
        this.ensureCleanAfterTest = ensureCleanAfterTest;
    }

    public ProcessEngineRule(final String configurationResource) {
        this(configurationResource, false);
    }

    public ProcessEngineRule(final String configurationResource, final boolean ensureCleanAfterTest) {
        this.configurationResource = "camunda.cfg.xml";
        this.configurationResourceCompat = "activiti.cfg.xml";
        this.deploymentId = null;
        this.additionalDeployments = new ArrayList();
        this.ensureCleanAfterTest = false;
        this.configurationResource = configurationResource;
        this.ensureCleanAfterTest = ensureCleanAfterTest;
    }

    public ProcessEngineRule(final ProcessEngine processEngine) {
        this(processEngine, false);
    }

    public ProcessEngineRule(final ProcessEngine processEngine, final boolean ensureCleanAfterTest) {
        this.configurationResource = "camunda.cfg.xml";
        this.configurationResourceCompat = "activiti.cfg.xml";
        this.deploymentId = null;
        this.additionalDeployments = new ArrayList();
        this.ensureCleanAfterTest = false;
        this.processEngine = processEngine;
        this.ensureCleanAfterTest = ensureCleanAfterTest;
    }

    @Override
    public void starting(final Description description) {
        this.deploymentId = TestHelper.annotationDeploymentSetUp(this.processEngine, description.getTestClass(), description.getMethodName(), (Deployment) description.getAnnotation(Deployment.class));
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return ProcessEngineRule.super.apply(base, description);

    }

    protected void initializeProcessEngine() {
        try {
            this.processEngine = TestHelper.getProcessEngine(this.configurationResource);
        } catch (final RuntimeException var2) {
            if (var2.getCause() == null || !(var2.getCause() instanceof FileNotFoundException)) {
                throw var2;
            }

            this.processEngine = TestHelper.getProcessEngine(this.configurationResourceCompat);
        }

    }

    protected void initializeServices() {
        this.processEngineConfiguration = ((ProcessEngineImpl) this.processEngine).getProcessEngineConfiguration();
        this.repositoryService = this.processEngine.getRepositoryService();
        this.runtimeService = this.processEngine.getRuntimeService();
        this.taskService = this.processEngine.getTaskService();
        this.historyService = this.processEngine.getHistoryService();
        this.identityService = this.processEngine.getIdentityService();
        this.managementService = this.processEngine.getManagementService();
        this.formService = this.processEngine.getFormService();
        this.authorizationService = this.processEngine.getAuthorizationService();
        this.caseService = this.processEngine.getCaseService();
        this.filterService = this.processEngine.getFilterService();
        this.externalTaskService = this.processEngine.getExternalTaskService();
        this.decisionService = this.processEngine.getDecisionService();
    }

    protected void clearServiceReferences() {
        this.processEngineConfiguration = null;
        this.repositoryService = null;
        this.runtimeService = null;
        this.taskService = null;
        this.formService = null;
        this.historyService = null;
        this.identityService = null;
        this.managementService = null;
        this.authorizationService = null;
        this.caseService = null;
        this.filterService = null;
        this.externalTaskService = null;
        this.decisionService = null;
    }

    @Override
    public void finished(final Description description) {
        this.identityService.clearAuthentication();
        this.processEngine.getProcessEngineConfiguration().setTenantCheckEnabled(true);
        TestHelper.annotationDeploymentTearDown(this.processEngine, this.deploymentId, description.getTestClass(), description.getMethodName());
        final Iterator var2 = this.additionalDeployments.iterator();

        while (var2.hasNext()) {
            final String additionalDeployment = (String) var2.next();
            TestHelper.deleteDeployment(this.processEngine, additionalDeployment);
        }

        if (this.ensureCleanAfterTest) {
            TestHelper.assertAndEnsureCleanDbAndCache(this.processEngine);
        }

        TestHelper.resetIdGenerator(this.processEngineConfiguration);
        ClockUtil.reset();
        this.clearServiceReferences();
    }

    public void setCurrentTime(final Date currentTime) {
        ClockUtil.setCurrentTime(currentTime);
    }

    public String getConfigurationResource() {
        return this.configurationResource;
    }

    public void setConfigurationResource(final String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public ProcessEngine getProcessEngine() {
        return this.processEngine;
    }

    public void setProcessEngine(final ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return this.processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(final ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return this.repositoryService;
    }

    public void setRepositoryService(final RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public RuntimeService getRuntimeService() {
        return this.runtimeService;
    }

    public void setRuntimeService(final RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public TaskService getTaskService() {
        return this.taskService;
    }

    public void setTaskService(final TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public HistoryService getHistoryService() {
        return this.historyService;
    }

    public void setHistoryService(final HistoryService historyService) {
        this.historyService = historyService;
    }

    public void setHistoricDataService(final HistoryService historicService) {
        this.setHistoryService(historicService);
    }

    @Override
    public IdentityService getIdentityService() {
        return this.identityService;
    }

    public void setIdentityService(final IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public ManagementService getManagementService() {
        return this.managementService;
    }

    @Override
    public AuthorizationService getAuthorizationService() {
        return this.authorizationService;
    }

    public void setAuthorizationService(final AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public CaseService getCaseService() {
        return this.caseService;
    }

    public void setCaseService(final CaseService caseService) {
        this.caseService = caseService;
    }

    @Override
    public FormService getFormService() {
        return this.formService;
    }

    public void setFormService(final FormService formService) {
        this.formService = formService;
    }

    public void setManagementService(final ManagementService managementService) {
        this.managementService = managementService;
    }

    @Override
    public FilterService getFilterService() {
        return this.filterService;
    }

    public void setFilterService(final FilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public ExternalTaskService getExternalTaskService() {
        return this.externalTaskService;
    }

    public void setExternalTaskService(final ExternalTaskService externalTaskService) {
        this.externalTaskService = externalTaskService;
    }

    @Override
    public DecisionService getDecisionService() {
        return this.decisionService;
    }

    public void setDecisionService(final DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    public void manageDeployment(final org.camunda.bpm.engine.repository.Deployment deployment) {
        this.additionalDeployments.add(deployment.getId());
    }
}
