# SAAO Shared Jenkins Library

This shared library contains various global variables (steps) which you can use in Jenkins pipelines for SAAO projects.

## Installation

You first have to configure Jenkins to use the library. Go to the Jenkins dashboard and select the "Manage Jenkins" option from the sidebar menu.

![Manage Jenkins menu item](doc/images/manage_jenkins.png)

Select the "Configure System" option.

![Configure System option](doc/images/configure_system.png)

The system configuration page is quite long, but if you scroll down, you will eventually find the section for adding global pipeline libraries.

![Global Pipeline Libraries section](doc/images/global_pipeline_libraries.png)

Click on the Add button and define the library details as shown below. Configure the library as follows.

* Choose `saao-shared-library` as the name.
* Use `main` as the default version.
* Choose [https://github.com/saltastroops/saao-shared-jenkins-library.git](https://github.com/saltastroops/saao-shared-jenkins-library.git) as the project repository.
* Leave the other settings unchanged.

The screenshots below highlight the settings you have to change.

![Library settings (part 1)](doc/images/library_settings_1.png)

![Library settings (part 2)](doc/images/library_settings_2.png)

Finally, Click the Save button at the bottom of the page to save your changes.

The library is now available. However, to make the documentation more readable, you need to enable HTML formatting. To do so, click on the Manage Jenkins link in the breadcrumbs at the top of the page and select the Configure Global Security option.

![Global Security option](doc/images/global_security.png)

Scroll down to the Markup Formatter section and select Safe HTML as the formatter option.

![Markup Formatter section](doc/images/markup_formatter.png)

Remember to click the Save button at the bottom of the page to save your changes.

## Using the library

Once you have installed the library, you can use it in any of your pipelines by importing it at the top of the pipeline script and then calling its functions, as shown in the following example.

## Viewing the documentation

For convenience, documentation about the .library functions is provided in Jenkins' pipeline syntax documentation. To access it, select the Pipeline Syntax item from the sidebar menu of your pipeline page.

![Pipeline Syntax menu item](doc/images/pipeline_syntax.png)

Then choose the Global Variables Reference item from the sidebar menu.

![Global Variables Reference menu item](doc/images/global_variables_reference.png)

You can now scroll to the documentation for the function you need.
