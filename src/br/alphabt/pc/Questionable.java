package br.alphabt.pc;

import java.io.Serializable;

interface Questionable {

    void setQuestions(Questions<? extends Serializable> questions);

    <E extends Serializable> E runQuestions();

}
