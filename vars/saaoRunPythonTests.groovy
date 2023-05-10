def call(Map config = [:] ) {
  def success = true
  def banditDirs = _dirs(config, "bandit")
  def flake8Dirs = _dirs(config, "flake8")
  echo banditDirs
}

def _dirs(Map config, String key) {
  if (config.containsKey(key)) {
    return config.get(key)
  }
  return []
}
