package commands;

public enum Commands {

    help("Hi there! Look down to command list:"),
    ;

    public String value;

    Commands(String value) {
        this.value = value;
    }
}
