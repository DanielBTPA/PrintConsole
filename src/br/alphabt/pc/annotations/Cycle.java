package br.alphabt.pc.annotations;

import br.alphabt.pc.State;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface Cycle {

    State value();

}
