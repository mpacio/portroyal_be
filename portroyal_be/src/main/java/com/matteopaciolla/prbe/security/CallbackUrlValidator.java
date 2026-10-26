package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class CallbackUrlValidator {

    private final boolean developmentProfile;

    public CallbackUrlValidator(Environment environment) {
        developmentProfile = environment.acceptsProfiles(Profiles.of("dev", "test"));
    }

    public void validate(String value) {
        if (value == null || value.isBlank()) {
            throw invalidCallbackUrl();
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalidCallbackUrl();
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !scheme.equals("https") && !(developmentProfile && scheme.equals("http"))) {
            throw invalidCallbackUrl();
        }
        if (!developmentProfile && uri.getPort() != -1 && uri.getPort() != 443) {
            throw invalidCallbackUrl();
        }
        if (!developmentProfile) {
            validatePublicHost(uri.getHost());
        }
    }

    private static void validatePublicHost(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw invalidCallbackUrl();
        }
        if (addresses.length == 0) {
            throw invalidCallbackUrl();
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw invalidCallbackUrl();
            }
        }
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first != 0
                    && first < 224
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 192 && second == 0 && third == 0)
                    && !(first == 192 && second == 0 && third == 2)
                    && !(first == 192 && second == 88 && third == 99)
                    && !(first == 198 && (second == 18 || second == 19
                            || second == 51 && third == 100))
                    && !(first == 203 && second == 0 && third == 113);
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            boolean globalUnicast = (first & 0xe0) == 0x20;
            boolean uniqueLocal = (first & 0xfe) == 0xfc;
            boolean documentation = first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0d
                    && Byte.toUnsignedInt(bytes[3]) == 0xb8;
            boolean teredo = first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x00
                    && Byte.toUnsignedInt(bytes[3]) == 0x00;
            boolean sixToFour = first == 0x20 && second == 0x02;
            return globalUnicast && !uniqueLocal && !documentation && !teredo && !sixToFour;
        }
        return false;
    }

    private static MandatoryParamException invalidCallbackUrl() {
        return new MandatoryParamException(
                "Callback URL must use HTTPS and resolve only to publicly routable addresses");
    }
}
