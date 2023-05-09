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
  if (!config.containsKey('secretFiles')) {
    config.secretFiles = []
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

    // Get the deployment script
    saaoLoadScript 'deployment.sh'

    // Create a file with the Docker registry password
    writeFile file: 'registry-password.txt', text: "$REGISTRY_CREDENTIALS_PSW"

    // Copy the required files
    sshPut remote: remote, from: '_docker-compose.yml', into: "${config.imageName}/docker-compose.yml"
    sshPut remote: remote, from: 'deployment.sh', into: "${config.imageName}/deployment.sh"
    sshCommand remote: remote, command: "chmod u+x ${config.imageName}/deployment.sh"
    sshPut remote: remote, from: 'registry-password.txt', into: "${config.imageName}/registry-password.txt"
    sshCommand remote: remote, command: "chmod go-rwx ${config.imageName}/registry-password.txt"

    // Copy the secret files
    for (entry in config.secretFiles) {
      withCredentials([file(credentialsId: entry.key, variable: 'secretFile')]) {
        print("${entry.key} -- ${entry.value} -- ${secretFile.toString()}")
//        targetPath = "${config.imageName}/${entry.value}"
//        echo "${secretFile} -------"
//        // sshPut remote: remote, from: secretFile, into: targetPath
      }
    }

    // Execute the deployment script
    sshCommand remote: remote, command: "cd ${config.imageName}; ./deployment.sh \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""

    // Remove the remote files again, if they still exist
    sshRemove remote: remote, path: "${config.imageName}/registry-password.txt", failOnError: false
    sshRemove remote: remote, path: "${config.imageName}/deployment.sh", failOnError: false
  }
}
