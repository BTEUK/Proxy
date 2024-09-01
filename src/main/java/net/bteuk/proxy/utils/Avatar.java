package net.bteuk.proxy.utils;

import net.bteuk.proxy.Proxy;

import java.util.Objects;

public class Avatar {

    public static String getAvatarUrl(String uuid, String texture) {
        return constructAvatarUrl(uuid, texture);
    }

    private static String constructAvatarUrl(String uuid, String texture) {

        String url = "https://crafatar.com/avatars/{uuid}.png?size={size}&overlay#{texture}";

        url = url
                .replace("{texture}", texture != null ? texture : "")
                .replace("{uuid}", uuid != null ? uuid.replace("-", "") : "")
                .replace("{size}", "128");

        return url;
    }
}
