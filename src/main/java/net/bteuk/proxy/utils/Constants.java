package net.bteuk.proxy.utils;

import net.bteuk.proxy.Proxy;

public class Constants {

    public static final String JOIN_MESSAGE = Proxy.getInstance().getConfig().getString("custom_messages.leave");
    public static final String WELCOME_MESSAGE = Proxy.getInstance().getConfig().getString("custom_messages.leave");
    public static final String RECONNECT_MESSAGE = Proxy.getInstance().getConfig().getString("custom_messages.leave");
    public static final String LEAVE_MESSAGE = Proxy.getInstance().getConfig().getString("custom_messages.leave");

}
