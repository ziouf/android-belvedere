package fr.marin.cyril.belvedere.services;

/**
 * Created by cyril on 12/12/17.
 */
public interface ILocationService {

    void resume();

    void pause();

    ILocationEventListener registerLocationEventListener(ILocationEventListener eventListener);

    void unRegisterLocationEventListener(ILocationEventListener eventListener);
}
