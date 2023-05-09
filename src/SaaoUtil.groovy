class SaaoUtil {
  private final def script

  public SaaoUtil(def script) {
    this.script = script
  }

  public void loadScript(def name) {
    def scriptContent = this.script.libraryResource "za/ac/saao/scripts/${name}"
    this.script.writeFile file: name, text: scriptContent
    this.script.sh "chmod a+x ${name}"
  }
}
