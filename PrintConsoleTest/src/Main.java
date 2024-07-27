import br.alphabt.pc.*;
import br.alphabt.pc.annotations.*;

public class Main {

    public static void main(String[] args) {
        ConsoleManager consoleManager = ConsoleManager.newConsoleManager(args);
        consoleManager.register(MainPc.class, PartConsole.MAIN);
        consoleManager.execute();
    }

    @Part
    public static class MainPc {

        @Self
        PartConsole part;

        @Cycle(State.CREATE)
        public void initialize() {
            System.out.println("Initialize...");
        }

        @Cycle(State.STOP)
        public void stop() {
            System.out.println("Stop");
            part.restart();
        }

        @Cycle(State.RESTART)
        public void rest() {
            System.out.println("restart0000");
        }

        @Print(flag = "default")
        public void test(PrintConsole pc) {
        }
    }

}
