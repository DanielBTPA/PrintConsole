package br.alphabt.pc;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ConsoleManager implements Runnable {

    private static final int TIME_EXEC_DEFAULT = 100;

    public static ConsoleManager newConsoleManager(String... args) {
        return new ConsoleManager(args);
    }

    static Thread cmThread;

    private boolean isRunning = true;

    private String[] args;

    private final Map<String, PartConsole> partConsoleHashMap;

    String oldPartConsole = "", currentPartConsole = "";

    private String namePartConsoleMain;

    private ConsoleManager(String[] args) {
        this.args = args;

        partConsoleHashMap = Collections.synchronizedMap(new HashMap<>());
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void exit() {
        this.isRunning = false;
    }

    public void register(Class<? extends PartConsole> pcClass, String name) {
        try {
            if (pcClass == null) {
                throw new NullPointerException("Not be null...");
            }

            PartConsole partConsole = (PartConsole) pcClass.getDeclaredConstructors()[0].newInstance(name);

            if (name == null || name.isEmpty()) {
                throw new NullPointerException("Name is not be null");
            }

            if (partConsoleHashMap.containsValue(partConsole)) {
                throw new RuntimeException("This pc is already registered");
            }

            partConsoleHashMap.put(name, partConsole);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | RuntimeException e) {
            if (e instanceof IllegalAccessException iae) {
                iae.initCause(new RuntimeException("The constructor of class " + pcClass.getName() + " is not public."));
            }

            throw new RuntimeException(e);
        }
    }

    PartConsole callNext(String pcName) {
        if (!partConsoleHashMap.containsKey(pcName)) {
            throw new RuntimeException("'".concat(pcName).concat("' not registered."));
        }

        if (!currentPartConsole.equals(pcName)) {
            this.oldPartConsole = this.currentPartConsole;
            this.currentPartConsole = pcName;
        } else {
            throw new IllegalCallerException("Method is already called.");
        }

        return partConsoleHashMap.get(pcName);
    }

    long startMills;

    long totalTime() {
        long endMills = System.currentTimeMillis();
        long totalMillis = (endMills - startMills);
        startMills = endMills;
        return totalMillis;
    }

    @Override
    public void run() {
        PartConsole pc;
        if (partConsoleHashMap.containsKey(PartConsole.MAIN)) {
            pc = this.callNext("main");
            pc.states.addAll(PartConsole.STATES_LIST_DEFAULT);
            this.namePartConsoleMain = pc.getName();
        }

        while (isRunning) {
            startMills = System.currentTimeMillis();
            pc = partConsoleHashMap.get(currentPartConsole);
            DependencyManager.getDependencyManager().injectInstance(true, pc);
            pc.runOnConsole(this);

            long totalTimeInMills = totalTime();

            if (totalTimeInMills <= TIME_EXEC_DEFAULT) {
                exit();
            }

        }
    }

    public String getNamePartConsoleMain() {
        return namePartConsoleMain;
    }

    public void execute() {
        if (cmThread == null) {
            cmThread = new Thread(this, "CM Thread");
        }

        if (!cmThread.isAlive()) {
            cmThread.start();
        }
    }

    public DependencyManager getDependencyManager() {
        return DependencyManager.getDependencyManager();
    }
}
