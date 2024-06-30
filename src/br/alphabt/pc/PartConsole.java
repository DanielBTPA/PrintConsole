package br.alphabt.pc;

import java.io.BufferedReader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static br.alphabt.pc.PartConsole.State.*;

public abstract class PartConsole implements Questionable {

    public static final String MAIN = "main";

    public static final List<State> STATES_LIST_DEFAULT = Arrays.asList(CREATE, START, RUNNING, STOP, FINISH);

    private final String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartConsole that)) return false;
        return Objects.equals(getName(), that.getName()) && Objects.equals(cm, that.cm) && Objects.equals(pc, that.pc) && Objects.equals(bufReader, that.bufReader) && Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), cm, pc, bufReader, states);
    }

    private ConsoleManager cm;
    private PrintConsole pc;

    protected PrintConsole getPrintConsole() {
        return pc;
    }

    @DependsOn
    private BufferedReader bufReader;

    final Deque<State> states;

    public PartConsole(String name) {
        this.name = name;
        states = new ArrayDeque<>();
        orderFlags = new ArrayDeque<>();
    }

    public String getName() {
        return name;
    }

    protected void restart() {
        this.states.removeIf(e -> e.equals(FINISH));
        this.states.addLast(RESTART);
    }

    protected void finish() {
        if (!cm.oldPartConsole.isEmpty()) {
            this.callNext(cm.oldPartConsole);

            if (cm.getNamePartConsoleMain().equals(cm.oldPartConsole)) {
                cm.oldPartConsole = "";
            }
        }
    }

    protected <R> void finish(R result) {
        if (!cm.oldPartConsole.isEmpty()) {
            this.callNext(cm.oldPartConsole, result);

            if (cm.getNamePartConsoleMain().equals(cm.oldPartConsole)) {
                cm.oldPartConsole = "";
            }
        }
    }

    protected void callNext(String pcName) {
        cm.callNext(pcName).states.addAll(STATES_LIST_DEFAULT);
    }

    protected <T> void callNext(String pcName, T result) {
        PartConsole pc = cm.callNext(pcName);
        pc.states.addAll(STATES_LIST_DEFAULT);
        pc.result = result;
    }

    protected <T> void callNext(String pcName, T send, String flag) {
        PartConsole pc = cm.callNext(pcName);
        pc.states.addAll(STATES_LIST_DEFAULT);
        pc.requireNext(flag, send);
    }

    protected void exit() {
        cm.exit();
    }

    protected abstract void onInitialize();

    protected abstract void onPrint(PrintConsole pc, String flag);

    protected <E> E onRead(String read, String flag) {
        return null;
    }

    protected void onSimpleRead(String read, String flag) {
        pc.printMessage("Typed: " + read, PrintType.INFO);
    }

    protected <E> void onProcessRead(E element) {
    }

    protected void onError(Exception e, String flag) {
        throw new RuntimeException(e);
    }


    protected void onStop() {
        this.pc.clear();
    }

    protected void onFinish() {
    }

    protected void requireInput() {
        this.pushRequire(null, true);
    }
    protected void requireInput(String flag) {
        this.pushRequire(flag, true);
    }

    protected void requireNext(String flag) {
        this.requireNext(flag, null);
    }

    protected void requireNext(String flag, Object result) {
        this.pushRequire(flag, false);
        this.result = result;
    }

    protected void requireAll(String flag, Object result) {
        this.requireNext(flag);
        this.requireInput(flag);
        this.result = result;
    }

    protected void requireAll(String flag) {
       this.requireAll(flag, null);
    }

    Object result = null;

    protected Object getResult() {
        return result;
    }

    private <E extends Serializable> E implQuestions() {
        DependencyManager.getDependencyManager().injectInstance(false, questions);

        final Map<String, String> qMap = questions.createQuestions();

        if (qMap == null || qMap.isEmpty()) throw new NullPointerException("Map can't be null or empty.");

        E obj = null;
        final List<String> listKey = List.copyOf(qMap.keySet());

        for (int i = 0; i < listKey.size(); i++) {
            boolean rept;
            String result;
            do {
                if (i == 0) {
                    obj = (E) questions.createElement(obj);
                    if (obj == null) throw new NullPointerException("Initialize the element first.");
                }

                String k = listKey.get(i);
                String v = qMap.get(k);
                try {
                    questions.onPrintQuestion(pc, v, k);
                    result = bufReader.readLine();
                    rept = questions.onReadLine(obj, result, k);

                } catch (Exception e) {
                    questions.onMessageErr(this, e);
                    rept = true;
                }
            } while (rept);
        }

        questions.onProcessResult(obj);
        return obj;
    }

    private final ArrayDeque<String> orderFlags;

    void runOnConsole(ConsoleManager cm) {
        this.cm = cm;
        for (State state : states) {
            if (state == CREATE) {
                this.pc = new PrintConsole();
                this.onInitialize();
                if (orderFlags.isEmpty()) {
                    this.requireNext(null);
                }
            } else if (state == RUNNING) {
                while (!orderFlags.isEmpty()) {
                    String fkv = orderFlags.pollLast();
                    String fv = getRequireInfo(fkv, true);
                    String fk = getRequireInfo(fkv, false);

                    try {
                        if (fk.contentEquals("require")) {
                            this.onPrint(pc, fv);
                            this.callPartsPrintable(fv);
                        } else if (fk.contentEquals("requireInput")) {
                            String read = bufReader.readLine();
                            this.onSimpleRead(read, fv);
                            Object obj = this.onRead(read, fv);

                            if (obj != null) {
                                this.onProcessRead(obj);
                            }
                        }
                    } catch (Exception e) {
                        this.onError(e, fv);
                    }

                    orderFlags.remove(fkv);
                }
            } else if (state == STOP) {
                this.onStop();
            } else if (state == FINISH) {
                this.onFinish();
                this.orderFlags.clear();
                this.pc = null;
                this.bufReader = null;
                this.result = null;
            } else if (state == RESTART) {
                cm.startMills = 0;
                states.addAll(STATES_LIST_DEFAULT.subList(1, STATES_LIST_DEFAULT.size() - 1));
            }

            states.pollFirst();
        }

    }

    private void callPartsPrintable(String flag) {
        Class<?> pcClass = this.getClass();

        Method[] methods = pcClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Printable.class)) {
                if (flag.equals(method.getName()) || flag.equals(method.getAnnotation(Printable.class).name())) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] params = new Object[paramTypes.length];

                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i].equals(PrintConsole.class)) {
                            params[i] = pc;
                        } else if (paramTypes[i].equals(String.class)) {
                            params[i] = flag;
                        } else {
                            if (result != null) {
                                params[i] = paramTypes[i].cast(result);
                            }
                        }
                    }

                    try {
                        method.invoke(this, params);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


    public enum State {
        CREATE, START, RUNNING, RESTART, STOP, FINISH
    }

    private Questions<Serializable> questions;

    @Override
    public void setQuestions(Questions<? extends Serializable> questions) {
        this.questions = (Questions<Serializable>) questions;
    }

    @Override
    public <E extends Serializable> E runQuestions() {
        if (questions == null) {
            throw new NullPointerException("Implements Question first and use setQuestions().");
        }

        return this.implQuestions();
    }

    private void pushRequire(String flag, boolean input) {
        orderFlags.push((input ? "requireInput" : "require")
                .concat(":")
                .concat(flag == null || flag.isEmpty() ? "default" : flag));
    }

    private String getRequireInfo(String flag, boolean vok) {
        return flag.split(":")[vok ? 1 : 0];
    }

}
