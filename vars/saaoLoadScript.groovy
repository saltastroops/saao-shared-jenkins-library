/*
 * Load a script from the resources folder and make it executable.
 *
 * Adapted from https://github.com/darinpope/github-api-global-lib/blob/main/vars/loadLinuxScript.groovy.
 */
def call(String name) {
  def scriptContent = libraryResource "za/ac/saao/scripts/${name}"
  writeFile file: name, text: scriptContent
  sh "chmod a+x ${name}"
}
