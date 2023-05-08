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

    // Prepare the deployment script.
    saaoLoadScript 'deployment.sh'
    sh "./deployment.sh ${config.registryUrl} ${registryUsername} ${config.imageName} ${tag}"
  }
}
