def call(Map config = [:])
{
  // Ensure all required arguments are given
  String[] arguments = ["host", "hostCredentialsId", "registryCredentialsId", "registryUrl"]
  for (String argument : arguments) {
    if (!config.containsKey(argument)) {
      error "The ${argument} argument is missing."
    }
  }

  // Use the short git hash as the image tag
  def tag = sh returnStdout: true, script: 'git rev-parse --short HEAD | tr -d "\n"'

  withCredentials([usernamePassword(credentialsId: config.registryCredentialsId, passwordVariable: 'registryPassword',
                                    usernameVariable: 'registryUsername')]) {
    // Build the Docker image
    dockerImage = docker.build("${registryUsername}/finder-chart-generator:${tag}")

    // Push the image to the container registry
    docker.withRegistry(config.registryUrl, config.registryCredentialsId) {
      dockerImage.push()
    }
  }
}
