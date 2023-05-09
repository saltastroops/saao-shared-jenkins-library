class Script {
  public Script(def script) {
    this.script = script
  }

  public void sayHello() {
    this.script.echo "Hello"
  }
}
