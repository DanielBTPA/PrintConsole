package br.alphabt.pc;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public interface Questions<E extends Serializable> {

    int INDEX_PRINT_RESULT = 100;

    E createElement(E current);

    Map<String, String> createQuestions();

    void onPrintQuestion(PrintConsole pc, String ask, String key);

    boolean onReadLine(E element, String readResult, String key);

    void onProcessResult(E element);

    default void onMessageErr(PartConsole context, Exception e) {
        String msg = "Error type: " + e.getClass().getSimpleName() + "\n" +
                "Message: " + e.getMessage() + "\n" +
                "Cause: " + e.getCause();
        context.getPrintConsole().printMessage(msg, PrintType.ERR);
    }

    static Map<String, String> lazyCreateQuestions(String pairString) {
        if (pairString == null || pairString.isEmpty()) throw new NullPointerException("String is not be null or empty.");

        final LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();

        String[] splitPair = pairString.split(",");

        for (String sp : splitPair) {
            String[] splitKv = sp.split(":");
            if (splitKv.length > 1 && splitKv[1].matches("'[^']*'")) {
                linkedHashMap.put(splitKv[0].trim(), splitKv[1].replace("'", ""));
            }
        }

        return linkedHashMap;
    }

    static Map<String, String> lazyCreateQuestions(String... pairs) {
        if (pairs.length == 0) throw new NullPointerException("Array is not be null or empty.");

        final LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();

        for (int i = 0; i < pairs.length; i++) {
            if (i % 2 != 0) {
                linkedHashMap.put(pairs[i-1], pairs[i]);
            }
        }

        return linkedHashMap;
    }

    static <E extends Serializable> void lazyFillElement(E element, String res, String key) {
        Class<?> eClass = element.getClass();

        Field[] fields = eClass.getDeclaredFields();
        Method[] methods = eClass.getDeclaredMethods();

        for (Field field : fields) {
            if (field.isAnnotationPresent(QuestionResult.class)) {
                QuestionResult questionResult = field.getAnnotation(QuestionResult.class);
                String fName = field.getName();
                if (questionResult.name().equals(key) || fName.equals(key)) {
                    try {
                        Method method = Arrays.stream(methods)
                                .filter(m -> m.getName().equalsIgnoreCase("set" + fName))
                                .findFirst().
                                orElseThrow((Supplier<Throwable>) () ->
                                        new NoSuchElementException("The '" + fName + "' attribute of the '" + eClass.getSimpleName() + ".class' must have a set method."));

                        switch (field.getType().getTypeName()) {
                            case "Integer", "int":
                                method.invoke(element, Integer.parseInt(res));
                                break;
                            case "Float", "float":
                                method.invoke(element, Float.parseFloat(res));
                                break;
                            case "Long", "long":
                                method.invoke(element, Long.parseLong(res));
                                break;
                            case "Boolean", "boolean":
                                method.invoke(element, Boolean.parseBoolean(res));
                                break;
                            case "Short", "short":
                                method.invoke(element, Short.parseShort(res));
                                break;
                            case "Byte", "byte":
                                method.invoke(element, Byte.parseByte(res));
                                break;
                            case "Character", "char":
                                method.invoke(element, res.charAt(0));
                                break;
                            default:
                                method.invoke(element, res);
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
