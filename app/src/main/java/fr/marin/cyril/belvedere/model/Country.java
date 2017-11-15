package fr.marin.cyril.belvedere.model;

import fr.marin.cyril.belvedere.annotations.JsonField;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

/**
 * Created by cyril on 15/11/17.
 */
@RealmClass
public class Country implements RealmModel {

    @Required
    @PrimaryKey
    @JsonField("pays")
    private String id;

    @Required
    @JsonField("paysLabel")
    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Country{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
