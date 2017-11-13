package fr.marin.cyril.belvedere.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by cyril on 10/11/17.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {
    String value();
}
