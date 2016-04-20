package fr.marin.cyril.mapsapp.services;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.Collection;

/**
 * Created by CSCM6014 on 18/04/2016.
 */
public class Messages {

    public static final int MSG_REGISTER_CLIENT = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;

    public static final int MSG_SENSOR_UPDATE = 5;
    public static final int MSG_LOCATION_UPDATE = 6;
    public static final int MSG_REQUEST_LOCATION = 7;


    public static void sendNewMessageToAll(Collection<Messenger> mClients, int type, Bundle data, Messenger replyTo) {
        for (Messenger client : mClients)
            Messages.sendNewMessage(client, type, data, replyTo);
    }

    public static void sendNewMessage(Messenger client, int type, Bundle data, Messenger replyTo) {
        Message msg = Message.obtain(null, type);
        msg.replyTo = replyTo;
        msg.setData(data);

        try {
            client.send(msg);
        } catch (RemoteException ignore) {

        }
    }

    public static void sendMessage(Messenger client, Message message) {

        try {
            client.send(message);

        } catch (RemoteException ignore) {

        }
    }

    public static void sendMessageToAll(Collection<Messenger> clients, Message message) {

        try {
            for (Messenger client : clients)
                client.send(message);

        } catch (RemoteException ignore) {

        }
    }

}
