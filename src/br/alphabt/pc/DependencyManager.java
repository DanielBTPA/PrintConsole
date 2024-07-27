package br.alphabt.pc;

import br.alphabt.pc.annotations.DependsOn;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DependencyManager {

    private static DependencyManager dependencyManager;

    public static DependencyManager getDependencyManager() {
        if (dependencyManager == null) {
            dependencyManager = new DependencyManager();
        }

        return dependencyManager;
    }

    private final HashMap<Class<?>, Object[]> allowClasses;
    private final List<Class<?>> blackList;

    DependencyManager() {
        allowClasses = new LinkedHashMap<>();
        blackList = new ArrayList<>();
        initAllowedClasses();
    }

    private void initAllowedClasses() {
        this.register(ArrayList.class);
        this.register(LinkedList.class);
        this.register(HashMap.class);
        this.register(LinkedHashMap.class);
        this.register(ArrayDeque.class);
        this.register(HashSet.class);
        this.register(LinkedHashSet.class);

        blackList.add(PrintConsole.class);
        blackList.add(PartConsole.class);
        blackList.add(ConsoleManager.class);
    }

    public void putOnBlacklist(Class<?> clazz) {
        blackList.add(clazz);
    }

    public void register(Class<?> theClass, Object... params) {
        allowClasses.put(theClass, params);
    }

    public void injectInstance(Object... containers) {
        for (Object o : containers) {
            this.injectInstance(o);
        }
    }

    public void injectInstance(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(DependsOn.class)) {
                Class<?> cf = field.getType();
                DependsOn dep = field.getDeclaredAnnotation(DependsOn.class);
                Object instance;
                try {
                    field.setAccessible(true);
                    instance = field.get(obj);
                    instance = dep.keepState() ? instance : null;

                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (instance == null) {
                    if (!cf.isPrimitive()) {
                        if (!blackList.contains(cf)) {
                            try {
                                Class<?>[] constructorParams = null;
                                Object[] params = null;
                                Class<?> classField = allowClasses.keySet().stream().filter(c ->
                                        cf.isAssignableFrom(c) && dep.subClassType().isEmpty() ||
                                                c.getSimpleName().equals(dep.subClassType()) ||
                                                c.getName().contentEquals(dep.subClassType())).findFirst().orElse(cf);

                                for (Class<?> supp : allowClasses.keySet()) {
                                    if (supp.isAssignableFrom(classField)) {
                                        params = allowClasses.get(supp);
                                        int sz = params.length;
                                        Class<?>[] suppParams = Arrays.stream(params).map(Object::getClass).toArray(e -> new Class<?>[sz]);
                                        for (Constructor<?> c : classField.getConstructors()) {
                                            for (var sp : suppParams) {
                                                if (Arrays.stream(c.getParameterTypes()).anyMatch(cls -> suppParams.length == c.getParameterCount() && cls.isAssignableFrom(sp))) {
                                                    constructorParams = c.getParameterTypes();
                                                } else {
                                                    constructorParams = null;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (constructorParams != null) {
                                    instance = classField.getConstructor(constructorParams).newInstance(params);
                                    field.set(obj, instance);
                                } else {
                                    try {
                                        instance = classField.getDeclaredConstructor().newInstance();
                                        field.set(obj, instance);
                                    } catch (NoSuchMethodException e) {
                                        if (classField.isInterface()) {
                                            throw new NoSuchMethodException("'" + classField.getSimpleName() + ".class' is an interface, add an implementation of it in the method 'getDependencyManager().register()'. ");
                                        } else {
                                            throw new NoSuchMethodException("'" + classField.getSimpleName() + ".class' without default constructor or parameterized class.");
                                        }
                                    }
                                }
                            } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                                     NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            throw new RuntimeException("The class '" + cf.getSimpleName() + ".class' can't be used with dependence injections.");
                        }
                    } else {
                        throw new RuntimeException("Intents are not applied to primitive types.");
                    }
                }

            }
        }
    }
}
