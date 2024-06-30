package br.alphabt.pc;


import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DependsOn {

    boolean keepState() default true;

    String subClassType() default "";

}
