def call(Map config = [:]) {
  // Ensure all required arguments are given
  String[] arguments = ["host", "hostCredentials", "registryCredentialsId", "registryUrl"]
  for (String argument: arguments) {
    if (!config.containsKey(argument)) {
      error "The registryCredentialsId argument is missing."
    }
  }

  // Use the short git hash as the image tag
  def tag = sh returnStdout: true, script: 'git rev-parse --short HEAD | tr -d "\n"'

  // Get the credentials
  withCredentials([usernamePassword(credentialsId: config.registryCredentialsId, passwordVariable: 'registryPassword', usernameVariable: 'registryUsername')]) {
    dockerImage = docker.build("${registryUsername}/finder-chart-generator:${tag}")
  }
}
