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

## Available functions

### `saaoDeployContainer`

This function builds an image of the current directory, pushes the image to a registry and deploys a container to a server. More precisely, the following steps are carried out by the function.

1. An image is built using the Dockerfile in the root directory of the workspace.
2. The image is tagged with the short hashcode of the git head.
3. The image is pushed to a container registry.
4. On the deployment server, the image just pushed is pulled.
5. Still on the deployment server, docker compose is used to restart the services.

Various requirements must be met for this function.

* The public SSH key of the Jenkins user must be included in the `authorized_keys` file of the user on the deployment server.
* The username for the deployment server and the private SSH _of the Jenkins user_ must be stored as credentials of the type "SSH username with private key".
* The username and password for the container registry must be stored as credentials of the type "Username with password".
* The docker compose file must only define a single service, and that service must use the deployed image. The registry, image name and image tag should not be given explicitly, but in form of environment variables `$REGISTRY`, `$IMAGE_NAME` and `$TAG`.

The following is an example of what the docker compose file might look like.

```yaml
services:
  fcg:
    image: ${REGISTRY}/${REGISTRY_USERNAME}/${IMAGE_NAME}:${TAG}
    ports:
      - 6789:8000
    restart: always
```

The `saaoDeployContainer` function takes several arguments, all of which are required.

| Argument              | Explanation                                                                                                                                                                        | Example value |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| host                  | Address of the deployment server.                                                                                                                                                  | dev.example.com |
| hostCredentialsId     | Identifier of the credentials for the username on the deployment server and the private SSH key of the Jenkins user.                                                               | dev-server-credentials |
| imageName             | Name of the Docker image to deploy. This name is also used as the name of the deployment directory (on the deployment server), which will be created in the user's home directory. | my-great-webapp |
| registryUrl           | URL of the container registry.                                                                                                                                                     | https://registry.example.com |
| registryCredentialsId | Identifier of the credentials for the container registry username and password.                                                                                                    |

The credentials identifier is the ID you provide when creating new credentials, as highlighted in the following screenshot.

![Credentials ID](doc/images/credentials_identifier.png)
