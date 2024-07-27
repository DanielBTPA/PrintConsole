package br.alphabt.pc;

import br.alphabt.pc.annotations.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static br.alphabt.pc.State.*;

public abstract class PartConsole implements Questionable {

    public static final String MAIN = "main";
    public static final List<State> STATES_LIST_DEFAULT = Arrays.asList(CREATE, START, RUNNING, STOP, FINISH);
    protected String name;
    private ConsoleManager cm;
    protected PrintConsole pc;

    public PrintConsole getPrintConsole() {
        return pc;
    }

    private BufferedReader bufReader;
    final Deque<State> states;

    public PartConsole() {
        states = new ArrayDeque<>();
        orderFlags = new ArrayDeque<>();
    }

    public String getName() {
        return name;
    }

    public void restart() {
        this.states.removeIf(e -> e.equals(FINISH));
        this.states.addLast(RESTART);
    }

    public void finish() {
        if (!cm.oldPartConsole.isEmpty()) {
            this.callNext(cm.oldPartConsole);

            if (cm.getNamePartConsoleMain().equals(cm.oldPartConsole)) {
                cm.oldPartConsole = "";
            }
        }
    }

    public <R> void finish(R result) {
        if (!cm.oldPartConsole.isEmpty()) {
            this.callNext(cm.oldPartConsole, result);

            if (cm.getNamePartConsoleMain().equals(cm.oldPartConsole)) {
                cm.oldPartConsole = "";
            }
        }
    }

    public void callNext(String pcName) {
        cm.callNext(pcName).states.addAll(STATES_LIST_DEFAULT);
    }

    public <T> void callNext(String pcName, T result) {
        PartConsole pc = cm.callNext(pcName);
        pc.states.addAll(STATES_LIST_DEFAULT);
        pc.result = result;
    }

    public <T> void callNext(String pcName, T send, String flag) {
        PartConsole pc = cm.callNext(pcName);
        pc.states.addAll(STATES_LIST_DEFAULT);
        pc.requireNext(flag, send);
    }

    public void exit() {
        cm.exit();
    }


    /* Life Cycle */
    protected void onInitialize() {
    }

    protected void onStart() {
    }

    protected void onRestart() {
    }

    protected void onRunning() {
    }

    protected void onStop() {
    }

    protected void onFinish() {
    }
    /* ---------------------------- */

    protected abstract void onPrint(PrintConsole pc, String flag);

    protected void onRead(Object read, String flag) {
        pc.printMessage("Typed: " + read, PrintType.INFO);
    }

    protected void onError(Exception e, String flag) {
        throw new RuntimeException(e);
    }

    public void requireInput() {
        this.pushRequire(null, true);
    }

    public void requireInput(String flag) {
        this.pushRequire(flag, true);
    }

    public void requireNext(String flag) {
        this.requireNext(flag, null);
    }

    public void requireNext(String flag, Object result) {
        this.pushRequire(flag, false);
        this.result = result;
    }

    public void requireAll(String flag, Object result) {
        this.requireNext(flag);
        this.requireInput(flag);
        this.result = result;
    }

    public void requireAll(String flag) {
        this.requireAll(flag, null);
    }

    Object result = null;

    public Object getResult() {
        return result;
    }

    private <E extends Serializable> E implQuestions() {
        DependencyManager.getDependencyManager().injectInstance(questions);

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
                this.bufReader = new BufferedReader(new InputStreamReader(System.in));
                this.invokeDependencyInjection();
                this.onInitialize();
                if (orderFlags.isEmpty()) {
                    this.requireNext(null);
                }
            } else if (state == START) {
                this.onStart();
            } else if (state == RUNNING) {
                this.onRunning();
                while (!orderFlags.isEmpty()) {
                    String fkv = orderFlags.pollLast();
                    String fv = getRequireInfo(fkv, true);
                    String fk = getRequireInfo(fkv, false);

                    try {
                        if (fk.contentEquals("require")) {
                            this.invokePrint(pc, fv);
                        } else if (fk.contentEquals("requireInput")) {
                            String read = bufReader.readLine();
                            this.invokeReadInput(read, fv);
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
                this.pc.clear();
                this.pc = null;
                this.bufReader = null;
                this.result = null;
            } else if (state == RESTART) {
                this.onRestart();
                cm.startMills = 0;
                states.addAll(STATES_LIST_DEFAULT.subList(1, STATES_LIST_DEFAULT.size() - 1));
            }

            states.pollFirst();
        }

    }

    protected void invokePrint(PrintConsole pc, String flag) {
        this.onPrint(pc, flag);
        this.invokeMethodsAnnotatedWithPrint(flag, this);
    }

    protected void invokeReadInput(Object read, String flag) {
        this.onRead(read, flag);
        this.invokeMethodsAnnotatedWithInput(flag, this, read);
    }

    protected void invokeMethodsAnnotatedWithPrint(String flag, Object container) {
        Class<?> pcClass = container.getClass();

        Method[] methods = pcClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Print.class)) {
                if (flag.equals(method.getName()) || flag.equals(method.getAnnotation(Print.class).flag())) {
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
                        method.invoke(container, params);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void invokeMethodsAnnotatedWithInput(String flag, Object container, Object read) {
        Class<?> pcClass = container.getClass();
        Method[] methods = pcClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Input.class)) {
                if (flag.equals(method.getName()) || flag.equals(method.getAnnotation(Input.class).flag())) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] params = new Object[paramTypes.length];

                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i].equals(String.class)) {
                            params[i] = flag;
                        } else if (paramTypes[i].equals(Object.class)) {
                            params[i] = read;
                        }
                    }

                    try {
                        method.invoke(container, params);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void invokeDependencyInjection() {
        DependencyManager.getDependencyManager().injectInstance(this);
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

    protected static class PartConsoleRpc<C> extends PartConsole {

        protected C rpc;

        @DependsOn
        private HashMap<State, Method> cycleCalls;

        protected PartConsoleRpc(C rpc) {
            this.rpc = rpc;
        }

        private void initCycleCalls() {
            Method[] methods = rpc.getClass().getMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(Cycle.class)) {
                    Cycle cycle = method.getAnnotation(Cycle.class);

                    if (cycle.value() != null) {
                        cycleCalls.put(cycle.value(), method);
                    } else {
                        throw new NullPointerException("Initialize with State parameter");
                    }
                }
            }
        }

        private void invokeMethodCycle(State state) {
            try {
                Method method = cycleCalls.get(state);

                if (method != null) {
                    method.invoke(rpc, (Object[]) null);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onInitialize() {
            initCycleCalls();
            invokeMethodCycle(CREATE);
        }

        @Override
        protected void onStart() {
            invokeMethodCycle(START);
        }

        @Override
        protected void onRunning() {
            invokeMethodCycle(RUNNING);
        }

        @Override
        protected void onStop() {
            invokeMethodCycle(STOP);
        }

        @Override
        protected void onFinish() {
            invokeMethodCycle(FINISH);
        }

        @Override
        protected void onRestart() {
            invokeMethodCycle(RESTART);
        }

        @Override
        protected void invokePrint(PrintConsole pc, String flag) {
            super.invokeMethodsAnnotatedWithPrint(flag, rpc);
        }

        @Override
        protected void invokeDependencyInjection() {
            // Pt1
            Field[] fields = rpc.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(PartConsole.class) && field.isAnnotationPresent(Self.class)) {
                    field.setAccessible(true);
                    try {
                        field.set(rpc, this);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    field.setAccessible(false);
                    break;
                }
            }

            //Pt2
            DependencyManager.getDependencyManager().injectInstance(rpc, this);
        }

        @Override
        protected void invokeReadInput(Object read, String flag) {
            super.invokeMethodsAnnotatedWithInput(flag, rpc, read);
        }

        @Override
        protected void onPrint(PrintConsole pc, String flag) {
        }
    }

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

}
