package fr.marin.cyril.belvedere.tools;

import com.annimon.stream.Stream;

/**
 * Created by cyril on 24/11/17.
 */

public class Objects {
    public static boolean isNull(Object o) {
        return null == o;
    }

    public static boolean nonNull(Object o) {
        return !Objects.isNull(o);
    }

    public static boolean equals(Object... objects) {
        return !Stream.of(objects).anyMatch(Objects::isNull)
                && Stream.of(objects).allMatch(o -> o.equals(objects[0]));
    }
}
