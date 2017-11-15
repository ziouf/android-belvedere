package fr.marin.cyril.belvedere.async.interfaces;

/**
 * Created by cyril on 16/11/17.
 */

public interface OnProgressListener<T> {
    void onProgress(T... args);
}
