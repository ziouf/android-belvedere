package fr.marin.cyril.belvedere.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

/**
 * Created by cyril on 15/11/17.
 */
@RealmClass
public class Country implements RealmModel, Parcelable {

    public static final Creator<Country> CREATOR = new Creator<Country>() {
        @Override
        public Country createFromParcel(Parcel in) {
            return new Country(in);
        }

        @Override
        public Country[] newArray(int size) {
            return new Country[size];
        }
    };
    @Required
    @PrimaryKey
    @JsonAlias("country")
    private String id;
    @Required
    @JsonAlias("countryLabel")
    private String label;

    public Country() {
    }

    private Country(Parcel in) {
        id = in.readString();
        label = in.readString();
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(label);
    }
}
