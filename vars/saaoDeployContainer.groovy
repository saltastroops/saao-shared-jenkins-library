def call(Map config = [:]) {
  // Ensure all required arguments are given
  String[] arguments = ["host", "hostCredentials", "registryCredentialsId", "registryUrl"]
  for (String argument: arguments) {
    if (!config.containsKey(argument)) {
      error "The registryCredentialsId argument is missing."
    }
  }

  _deployToRegistry()
}

def _deployToRegistry() {
  echo 'Deploying...'
}
