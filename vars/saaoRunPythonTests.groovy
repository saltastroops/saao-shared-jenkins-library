def call(Map config = [:] ) {
  // Get the directories to test
  def banditDirs = _dirs(config, "bandit")
  def blackDirs = _dirs(config, "black")
  def flake8Dirs = _dirs(config, "flake8")
  def isortDirs = _dirs(config, "isort")
  def mypyDirs = _dirs(config, "mypy")
  def pytestDirs = _dirs(config, "pytest")

  // Run the various tests
  def success = false
  if (banditDirs.length() > 0) {
    returnValue = sh 'returnStatus': true, 'script': "bandit -r $banditDirs"
    if (returnValue != 0) {
      echo "bandit failed."
      success = false
    }
  }
  return success
}

def _dirs(Map config, String key) {
  if (config.containsKey(key)) {
    return config.get(key).join(' ')
  }
  return ''
}
