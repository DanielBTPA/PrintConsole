package br.alphabt.pc;

public final class InvalidOptionException extends RuntimeException {

    public InvalidOptionException() {
        super();
    }
    public InvalidOptionException(String msg) {
        super(msg);
    }
}
