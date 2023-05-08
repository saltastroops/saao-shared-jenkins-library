def call(Map config = [:])
{
  // Ensure all required arguments are given
  String[] arguments = ["host",
                        "hostCredentialsId",
                        "imageName",
                        "registryCredentialsId",
                        "registryUrl"]
  for (String argument : arguments) {
    if (!config.containsKey(argument)) {
      error "The ${argument} argument is missing."
    }
  }

  // Use the short git hash as the image tag
  def tag = sh returnStdout: true, script: 'git rev-parse --short HEAD | tr -d "\n"'

  withCredentials([usernamePassword(credentialsId: config.registryCredentialsId,
                                    passwordVariable: 'registryPassword',
                                    usernameVariable: 'registryUsername'),
                   sshUserPrivateKey(credentialsId: config.hostCredentialsId,
                                     keyFileVariable: 'identity',
                                     usernameVariable: 'username')]) {
    // Build the Docker image
    dockerImage = docker.build("${registryUsername}/${config.imageName}:${tag}")

    // Push the image to the container registry
    docker.withRegistry(config.registryUrl, config.registryCredentialsId) {
      dockerImage.push()
    }

    // Define the remote connection parameters
    def remote = [:]
    remote.name = config.host
    remote.host = config.host
    remote.allowAnyHosts = true
    remote.user = username
    remote.identityFile = identity

    // Create the necessary remote directory. The -p flag is used to avoid an
    // error if the directory exists already.
    sshCommand remote: remote, command: "mkdir -p ${config.imageName}"

    // Prepare the docker compose file
    saaoLoadScript 'prepare_docker_compose.sh'
    def compose_file = sh returnStdout: true, script: "./prepare_docker_compose.sh \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""
    writeFile file: '_docker-compose.yml', text: "$compose_file"

    // Prepare the deployment script
    saaoLoadScript 'deployment.sh'
    writeFile file: '_deployment.sh', text: "./deployment.sh \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""

    // Create a file with the Docker registry password
    writeFile file: 'registry-password.txt', text: "$REGISTRY_CREDENTIALS_PSW"

    // Copy the required files
    sshPut remote: remote, from: '_docker-compose.yml', into: "${config.imageName}/docker-compose.yml"
    sshPut remote: remote, from: '_deployment.sh', into: "${config.imageName}/deployment.sh"
    sshCommand remote: remote, command: "chmod u+x ${config.imageName}/deployment.sh"
    sshPut remote: remote, from: 'registry-password.txt', into: "${config.imageName}/registry-password.txt"
    sshCommand remote: remote, command: "chmod go-rwx ${config.imageName}/registry-password.txt"

    // Execute the deployment script
    sshCommand remote: remote, command: "${config.imageName}/deployment.sh"

    // Remove the remote secret file again, if it still exists
    sshRemove remote: remote, path: 'finder-chart-generator/registry-password.txt', failOnError: false
  }
}
