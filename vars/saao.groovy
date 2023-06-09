import SaaoUtil

def runPythonTests(Map config = [:] ) {
  generatedReportFiles = ''

  // Get the directories to test
  banditDirs = _dirs(config, 'bandit')
  blackDirs = _dirs(config, 'black')
  ruffDirs = _dirs(config, 'ruff')
  mypyDirs = _dirs(config, 'mypy')
  pytestDirs = _dirs(config, 'pytest')

  // Get the base directory for storing report-related files
  reportsDir = "reports"
  if (config.containsKey('reportsDir')) {
    // Replace multiple slashes with a single one, and remove trailing slashes
    reportsDir = config.reportsDir.replaceAll(/\/+/, "/").replaceAll(/\/$/, "")
  }

  // Create the directories for reports if necessary
  sh "mkdir -p ${reportsDir}/allure"
  sh "mkdir -p ${reportsDir}/warnings-next-generation"

  // Get the Allure option
  allureOption = ''
  if (!config.containsKey('allure') || config.allure) {
    allureOption = "--alluredir=${reportsDir}/allure"
  }

  // Get the Warnings Next Generation report options
  wngRuffOption = ''
  wngRuffFile = ''
  wngMypyFile = ''
  if (!config.containsKey('warningsNextGeneration') || config.warningsNextGeneration) {
    wngRuffOption = "--format=pylint"
    wngRuffFile = "${reportsDir}/warnings-next-generation/ruff.txt"
    wngMypyFile = "${reportsDir}/warnings-next-generation/mypy.txt"
  }

  // Should Poetry be used?
  poetryPrefix = ''
  if (config.containsKey('usePoetry') && config.usePoetry) {
    poetryPrefix = 'poetry run '
  }

  // Run Bandit
  success = true
  if (banditDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "${poetryPrefix}bandit -r $banditDirs"
    if (returnValue != 0) {
      echo 'bandit failed.'
      success = false
    }
  }

  // Run Black
  if (blackDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "${poetryPrefix}black --check $blackDirs"
    if (returnValue != 0) {
      echo 'black failed.'
      success = false
    }
  }

  // Run Ruff
  if (ruffDirs.length() > 0) {
    if (wngRuffFile != '') {
      generatedReportFiles += 'warningsNextGeneration--ruff|'
    }
    script = "${poetryPrefix}ruff $wngRuffOption $ruffDirs"
    if (wngRuffFile != '') {
      script += "> $wngRuffFile"
    }
    returnValue = sh returnStatus: true, script: script
    if (wngRuffFile != '') {
      // As the output was redirected, the warnings weren't shown in the log when Ruff
      // ran; but it might be helpful to show them
      sh "cat $wngRuffFile"
    }
     if (returnValue != 0) {
      echo 'Ruff failed.'
      success = false
    }
  }

  // Run Mypy
  if (mypyDirs.length() > 0) {
    if (wngMypyFile != '') {
      generatedReportFiles += 'warningsNextGeneration--mypy|'
    }
    script = "${poetryPrefix}mypy $mypyDirs"
    if (wngMypyFile != '') {
      script += "> $wngMypyFile"
    }
    returnValue = sh returnStatus: true, script: script
    if (wngMypyFile != '') {
      // As the output was redirected, the warnings weren't shown in the log when Mypy
      // ran; but it might be helpful to show them
      sh "cat $wngMypyFile"
    }
    if (returnValue != 0) {
      echo 'Mypy failed.'
      success = false
    }
  }

  // Run pytest
  if (pytestDirs.length() > 0) {
    if (allureOption != '') {
      generatedReportFiles += 'allure--pytest|'
    }
    returnValue = sh(
            'returnStatus': true,
            'script': "${poetryPrefix}pytest $allureOption $pytestDirs"
    )
    if (returnValue != 0) {
      echo 'pytest failed.'
      success = false
    }
  }

  // Stash data needed later
  stash includes: "$reportsDir/**", name: 'reports'
  env.saaoGeneratedReportedFiles = generatedReportFiles
  env.saaoReportsDir = reportsDir
  env.saaoRunPythonTestsRun = 'yes'

  return success
}

def generatePythonTestReports() {
  // Sanity check
  if (env.saaoRunPythonTestsRun == null) {
    error 'The generatePythonTestReports step can only be executed after the runPytonTests step.'
  }

  // Unstash the previously stashed report-related files
  unstash 'reports'

  // Generate the requested reports
  reportsDir = env.saaoReportsDir
  if (env.saaoGeneratedReportedFiles.contains('allure--pytest')) {
    allure includeProperties: false, jdk: '', results: [[path: "${reportsDir}/allure"]]
  }
  if (env.saaoGeneratedReportedFiles.contains('warningsNextGeneration--ruff')) {
    recordIssues(tools: [flake8(name: 'Ruff', pattern: "${reportsDir}/warnings-next-generation/ruff.txt")])
  }
  if (env.saaoGeneratedReportedFiles.contains('warningsNextGeneration--mypy')) {
    recordIssues(tools: [myPy(pattern: "${reportsDir}/warnings-next-generation/mypy.txt")])
  }
}

def deployContainer(Map config = [:])
{
  saaoUtil = new SaaoUtil(this)

  // Ensure all required arguments are given
  String[] requiredArguments = ['host',
                        'hostCredentialsId',
                        'imageName',
                        'registryCredentialsId',
                        'registryUrl']
  for (String argument : requiredArguments) {
    if (!config.containsKey(argument)) {
      error "The ${argument} argument is missing."
    }
  }
  if (!config.containsKey('dockerFile')) {
    config.dockerFile = 'Dockerfile'
  }
  if (!config.containsKey('dockerComposeFile')) {
    config.dockerComposeFile = 'docker-compose.yml'
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
    dockerImage = docker.build(
            "${registryUsername}/${config.imageName}:${tag}",
            "-f ${config.dockerFile} ."
    )

    // Push the image to the container registry
    docker.withRegistry(config.registryUrl, config.registryCredentialsId) {
      dockerImage.push()
    }

    // Define the remote connection parameters
    remote = [:]
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
    def compose_file = sh(
            returnStdout: true,
            script: "./prepare_docker_compose.sh \"${config.dockerComposeFile}\" \"${config.registryUrl}\" \"${registryUsername}\" \"${config.imageName}\" \"${tag}\""
    )
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
