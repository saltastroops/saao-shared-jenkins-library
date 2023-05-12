import SaaoUtil

def rrr = [
        'allure':[],
        'warningsNextGeneration': []
]

def runPythonTests(Map config = [:] ) {
  // Get the directories to test
  def banditDirs = _dirs(config, "bandit")
  def blackDirs = _dirs(config, "black")
  def flake8Dirs = _dirs(config, "flake8")
  def isortDirs = _dirs(config, "isort")
  def mypyDirs = _dirs(config, "mypy")
  def pytestDirs = _dirs(config, "pytest")

  // Get the Allure option
  def allureOption = ''
  if (!config.containsKey('allure') || config.allure) {
    allureOption = "--alluredir=reports/allure";
  }

  // Get the Warnings Next Generation report options
  def wngFlake8Options = ''
  def wngMypyRedirection = ''
  if (!config.containsKey('warningsNextGeneration') || config.warningsNextGeneration) {
    wngFlake8Options = "--format=pylint --output-file=reports/warnings-next-generation/flake8.txt"
    wngMypyRedirection = " | tee reports/warnings-next-generation/mypy.txt"
  }

  // Run bandit
  def success = true
  if (banditDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "bandit -r $banditDirs"
    if (returnValue != 0) {
      echo "bandit failed."
      success = false
    }
  }

  // Run black
  if (blackDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "black --check $blackDirs"
    if (returnValue != 0) {
      echo "black failed."
      success = false
    }
  }

  // Run flake8
  if (flake8Dirs.length() > 0) {
    if (wngFlake8Options != '') {
      rrr.warningsNextGeneration.add('flake8')
    }
    returnValue = sh 'returnStatus': true, 'script': "flake8 $wngFlake8Options $flake8Dirs"
    if (returnValue != 0) {
      echo "flake8 failed."
      success = false
    }
  }

  // Run isort
  if (isortDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "isort --check-only $isortDirs"
    if (returnValue != 0) {
      echo "isort failed."
      success = false
    }
  }

  // Run mypy
  if (mypyDirs.length() > 0) {
    if (wngMypyRedirection) {
      rrr.warningsNextGeneration.add('mypy')
    }
    returnValue = sh 'returnStatus': true, 'script': "mypy $mypyDirs $wngMypyRedirection"
    if (returnValue != 0) {
      echo "mypy failed."
      success = false
    }
  }

  // Run pytest
  if (pytestDirs.length() > 0) {
    of (allureOption != '') {
      rrr.allure.add('pytest');
    }
    returnValue = sh(
            'returnStatus': true,
            'script': "pytest $allureOption $pytestDirs"
    )
    if (returnValue != 0) {
      echo "pytest failed."
      success = false
    }
  }

  // Stash changes
  stash includes: 'reports/**', name: 'reports'

  return success
}

def createPythonTestReports() {
  echo rrr.toString()
}

def deployContainer(Map config = [:])
{
  def saaoUtil = new SaaoUtil(this)

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
    saaoUtil.loadScript 'prepare_docker_compose.sh'
    def compose_file = sh returnStdout: true, script: "./prepare_docker_compose.sh \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""
    writeFile file: '_docker-compose.yml', text: "$compose_file"

    // Get the deployment script
    saaoUtil.loadScript 'deployment.sh'

    // Create a file with the Docker registry password
    writeFile file: 'registry-password.txt', text: registryPassword

    // Copy the required files
    sshPut remote: remote, from: '_docker-compose.yml', into: "${config.imageName}/docker-compose.yml"
    sshPut remote: remote, from: 'deployment.sh', into: "${config.imageName}/deployment.sh"
    sshCommand remote: remote, command: "chmod u+x ${config.imageName}/deployment.sh"
    sshPut remote: remote, from: 'registry-password.txt', into: "${config.imageName}/registry-password.txt"
    sshCommand remote: remote, command: "chmod go-rwx ${config.imageName}/registry-password.txt"

    // Copy the secret files
    // Note: You cannot use a for loop here, as Jenkins would try to serialize a
    // non-serializable entry (see
    // https://stackoverflow.com/questions/60900302/jenkins-pipeline-how-do-i-use-the-sh-module-when-traversing-a-map)
    config.secretFiles.each { key, value ->
      withCredentials([file(credentialsId: key, variable: 'secretFile')]) {
        targetPath = "${config.imageName}/${value}"
        sshPut remote: remote, from: secretFile, into: targetPath
      }
    }

    // Execute the deployment script
    sshCommand remote: remote, command: "cd ${config.imageName}; ./deployment.sh \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""

    // Remove the remote files again, if they still exist
    sshRemove remote: remote, path: "${config.imageName}/registry-password.txt", failOnError: false
    sshRemove remote: remote, path: "${config.imageName}/deployment.sh", failOnError: false
  }
}

def _dirs(Map config, String key) {
  if (config.containsKey(key) && config.containsKey(key) != []) {
    return config.get(key).join(' ')
  }
  return ''
}
