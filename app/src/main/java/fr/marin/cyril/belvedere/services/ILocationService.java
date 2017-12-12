package fr.marin.cyril.belvedere.services;

import fr.marin.cyril.belvedere.services.impl.AbstractLocationEventListener;

/**
 * Created by cyril on 12/12/17.
 */
public interface ILocationService {

    void resume();

    void pause();

    AbstractLocationEventListener registerLocationEventListener(AbstractLocationEventListener eventListener);

    void unRegisterLocationEventListener(AbstractLocationEventListener eventListener);
}
