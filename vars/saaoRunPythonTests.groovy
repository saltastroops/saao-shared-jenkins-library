def call(Map config = [:] ) {
  // Get the directories to test
  def banditDirs = _dirs(config, "bandit")
  def blackDirs = _dirs(config, "black")
  def flake8Dirs = _dirs(config, "flake8")
  def isortDirs = _dirs(config, "isort")
  def mypyDirs = _dirs(config, "mypy")
  def pytestDirs = _dirs(config, "pytest")

  def junitReportOption = ''
  if (config.containsKey('junit') && config.junit.length() > 0) {
    junitReportOption = " --junitxml=${config.junit}/junit.xml"
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
    returnValue = sh 'returnStatus': true, 'script': "flake8 $flake8Dirs"
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
    returnValue = sh 'returnStatus': true, 'script': "mypy $mypyDirs"
    if (returnValue != 0) {
      echo "mypy failed."
      success = false
    }
  }

  // Run pytest
  if (pytestDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "pytest $junitReportOption $pytestDirs"
    if (returnValue != 0) {
      echo "pytest failed."
      success = false
    }
  }

  return success
}

def _dirs(Map config, String key) {
  if (config.containsKey(key) && config.containsKey(key) != []) {
    return config.get(key).join(' ')
  }
  return ''
}
