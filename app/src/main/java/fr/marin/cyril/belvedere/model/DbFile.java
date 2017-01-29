package fr.marin.cyril.belvedere.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by marin on 29/01/2017.
 */

public class DbFile extends RealmObject {

    @PrimaryKey
    private String fileName;

    private String hash;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
