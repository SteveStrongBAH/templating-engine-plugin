/*
   Copyright 2018 Booz Allen Hamilton

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.boozallen.plugins.jte.job

import hudson.Extension
import hudson.model.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint
import org.jenkinsci.plugins.workflow.flow.DurabilityHintProvider
import org.jenkinsci.plugins.workflow.flow.GlobalDefaultFlowDurabilityLevel
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn
import static org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext.*
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.Stapler
import org.kohsuke.stapler.StaplerRequest
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage

@PersistIn(JOB)
class TemplateFlowDefinition extends FlowDefinition {

    private final String template
    private final boolean sandbox
    private final String pipelineConfig

    @DataBoundConstructor
    public TemplateFlowDefinition(String template, boolean sandbox, String pipelineConfig){
        StaplerRequest req = Stapler.getCurrentRequest();
        this.template = sandbox ? template : ScriptApproval.get().configuring(template, GroovyLanguage.get(), ApprovalContext.create().withCurrentUser().withItemAsKey(req != null ? req.findAncestorObject(Item.class) : null));
        this.sandbox = sandbox
        this.pipelineConfig = pipelineConfig
    }

    private Object readResolve() {
        if (!sandbox) {
            ScriptApproval.get().configuring(template, GroovyLanguage.get(), ApprovalContext.create());
        }
        return this;
    }

    public String getTemplate() {
        return template
    }

    public boolean isSandbox() {
        return sandbox
    }

    public getPipelineConfig(){
        return pipelineConfig
    }

    @Override
    public FlowExecution create(FlowExecutionOwner handle, TaskListener listener, List<? extends Action> actions) throws Exception {
        Jenkins jenkins = Jenkins.getInstance()
        if (jenkins == null) {
            throw new IllegalStateException("inappropriate context")
        }
        Queue.Executable exec = handle.getExecutable()
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException("inappropriate context")
        }
        FlowDurabilityHint hint = (exec instanceof Item) ? DurabilityHintProvider.suggestedFor((Item)exec) : GlobalDefaultFlowDurabilityLevel.getDefaultDurabilityHint()
        
        String script = """
            template{
                ${ sandbox ? template : ScriptApproval.get().using(template, GroovyLanguage.get()) }
            }
        """
        return new CpsFlowExecution(script, sandbox, handle, hint);
    }

    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {

        @Override
        public String getDisplayName() {
            return "Jenkins Templating Engine"
        }

    }

    /**
     * Want to display this in the r/o configuration for a branch project, but not offer it on standalone jobs or in any other context.
     */
    @Extension
    public static class HideMeElsewhere extends DescriptorVisibilityFilter {

        @Override
        public boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl) {
                return context instanceof WorkflowJob && !(((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject)
            }
            return true
        }

    }
}
