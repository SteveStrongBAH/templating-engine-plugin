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


import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.hooks.*
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.TemplateEntryPointVariable
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import com.cloudbees.groovy.cps.impl.CpsClosure 

def call(CpsClosure body = null){
    // get template to be executed if missing closure param
    // this is placed at the top to consolidate JTE initialization
    // logging before the logs generated by createWorkspaceStash()
    String template
    if(!body){
        template = TemplateEntryPointVariable.getTemplate(pipelineConfig)
    }
    // checkout SCM and stash "workspace"
    createWorkspaceStash()

    // archive the current configuration
    archiveConfig()

    // otherwise currentBuild.result defaults to null 
    currentBuild.result = "SUCCESS"
    Map context = [
        step: null, 
        library: null, 
        status: currentBuild.result 
    ]

    try{
        // execute methods in steps annotated @Validate
        Hooks.invoke(Validate, getBinding(), context)

        // execute methods in steps annotated @Init
        Hooks.invoke(Init, getBinding(), context)
        
        /*
          exists if JTE invoked via:
          template{
              // do things 
          } <-- the closure parameter is variable "body"
        */
        if (body){ 
            body()
        } else{
            TemplateScriptEngine.parse(template, getBinding()).run() 
        }
    }catch(any){
        currentBuild.result = "FAILURE" 
        context.status = currentBuild.result 
        throw any 
    }finally{
        /*
          execute methods in steps annotated with @CleanUp
          followed by @Notify
        */
        Hooks.invoke(CleanUp, getBinding(), context)
        Hooks.invoke(Notify, getBinding(), context)
    }
}

void createWorkspaceStash(){
    try{
        if (scm){
            node{
                cleanWs()
                checkout scm 
                stash name: 'workspace', allowEmpty: true, useDefaultExcludes: false
            }
        }
    }catch(any){

    }
}

void archiveConfig(){
    node{
        // templateConfigObject variable injected from TemplateEntryPointVariable.groovy
        writeFile text: TemplateConfigDsl.serialize(templateConfigObject), file: "pipeline_config.groovy"
        archiveArtifacts "pipeline_config.groovy"
    }
}