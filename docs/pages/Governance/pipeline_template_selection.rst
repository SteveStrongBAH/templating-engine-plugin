.. _pipeline-template-selection:

---------------------------
Pipeline Template Selection
---------------------------

Pipeline Templates can either come from the application's source code repository, from the default 
template in a governance tier, or from a named template in a governance tier.  

^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
1. Application Source Code Repository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, if there is a Jenkinsfile within the application's source code repository it will be used 
as the pipeline template. 

Governance Tier's can choose to disable this flexibility to enforce the use of consolidated templates by 
setting ``allow_scm_jenkinsfile = false`` at the root of their configuration file. 

^^^^^^^^^^^^^^^^^^^^^^^^^^
2. Named Pipeline Template 
^^^^^^^^^^^^^^^^^^^^^^^^^^
Within a Governance Tier, you can create a ``pipeline_templates`` directory alongside the ``pipeline_config.groovy``
configuration file. Within this directory you can have as many different templates as you'd like and they will be
referenceable via their filename from the configuration file. 

To select a named pipeline template, you can specify ``pipeline_template = <template name>`` at the root 
of your configuration file. 

If a named pipeline template is specified, JTE will recursively search the ``pipeline_templates`` directories 
of each Governance Tier going up the hierarchy from most specific all the way to the Global Governance Tier 
defined in ``Manage Jenkins > Configure System``. 

.. important::

    If a named pipeline template is specified and the template is not found in any Governance Tier, JTE 
    will throw a Template Not Found Exception and fail the build. 

^^^^^^^^^^^^^^^^^^^^^^^^^^^^
3. Default Pipeline Template 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
If there is not an application repository ``Jenkinsfile`` or one is not permitted, and a named pipeline template 
has not been specified, JTE will look for the first Governance Tier Default Pipeline Template in ascending order. 

To create a Default Pipeline Template, simply create a ``Jenkinsfile`` alongside the ``pipeline_config.groovy`` 
configuration file in the Governance Tier. 

.. important:: 

    If no Default Pipeline Template can be found, then no pipeline template has been defined and JTE 
    will fail the build. 