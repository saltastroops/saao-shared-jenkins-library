class Script {
  private final def script

  public Script(def script) {
    this.script = script
  }

  public void sayHello() {
    this.script.echo "Hello"
  }
}
