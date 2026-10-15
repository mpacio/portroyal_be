package com.matteopaciolla.prbe.security.callback;

import com.matteopaciolla.prbe.exceptions.callback.InvalidCallbackUrlException;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

@Component
public class CallbackUrlValidator {

    private static final List<Ipv4Range> DISALLOWED_IPV4_RANGES = List.of(
            range("0.0.0.0", 8),
            range("10.0.0.0", 8),
            range("100.64.0.0", 10),
            range("127.0.0.0", 8),
            range("169.254.0.0", 16),
            range("172.16.0.0", 12),
            range("192.0.0.0", 24),
            range("192.0.2.0", 24),
            range("192.31.196.0", 24),
            range("192.52.193.0", 24),
            range("192.88.99.0", 24),
            range("192.168.0.0", 16),
            range("192.175.48.0", 24),
            range("198.18.0.0", 15),
            range("198.51.100.0", 24),
            range("203.0.113.0", 24),
            range("224.0.0.0", 4),
            range("240.0.0.0", 4)
    );

    private final AddressResolver addressResolver;

    public CallbackUrlValidator() {
        this(InetAddress::getAllByName);
    }

    CallbackUrlValidator(AddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    public void validate(String rawUrl) {
        try {
            validateUri(rawUrl);
        } catch (InvalidCallbackUrlException e) {
            throw e;
        } catch (UnknownHostException | SecurityException | IllegalArgumentException e) {
            throw new InvalidCallbackUrlException();
        }
    }

    private void validateUri(String rawUrl) throws UnknownHostException {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidCallbackUrlException();
        }

        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new InvalidCallbackUrlException();
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !uri.isAbsolute()
                || uri.isOpaque()
                || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new InvalidCallbackUrlException();
        }

        String asciiHost = normalizeHost(uri.getHost());
        if (!isPublicHostname(asciiHost)) {
            throw new InvalidCallbackUrlException();
        }

        resolvePublicAddresses(asciiHost);
    }

    public InetAddress[] resolvePublicAddresses(String hostname) throws UnknownHostException {
        String asciiHost;
        try {
            asciiHost = normalizeHost(hostname);
        } catch (IllegalArgumentException exception) {
            throw new UnknownHostException("Invalid callback hostname.");
        }
        if (!isPublicHostname(asciiHost)) {
            throw new UnknownHostException("Callback hostname is not publicly routable.");
        }
        InetAddress[] addresses = addressResolver.resolve(asciiHost);
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException("Callback hostname has no addresses.");
        }
        for (InetAddress address : addresses) {
            if (address == null || isDisallowed(address)) {
                throw new UnknownHostException("Callback hostname resolves to a non-public address.");
            }
        }
        return addresses.clone();
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank() || host.indexOf(':') >= 0 || host.startsWith("[")) {
            throw new InvalidCallbackUrlException();
        }
        String asciiHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        return asciiHost.endsWith(".") ? asciiHost.substring(0, asciiHost.length() - 1) : asciiHost;
    }

    private static boolean isPublicHostname(String asciiHost) {
        return asciiHost.length() <= 253
                && !asciiHost.matches("[0-9.]+")
                && asciiHost.indexOf('.') >= 0
                && !asciiHost.endsWith(".")
                && !asciiHost.substring(asciiHost.lastIndexOf('.') + 1).matches("[0-9]+")
                && !asciiHost.equals("localhost")
                && !asciiHost.endsWith(".localhost")
                && !asciiHost.endsWith(".local")
                && !asciiHost.endsWith(".internal");
    }

    private static boolean isDisallowed(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            long value = ipv4Value(bytes);
            return DISALLOWED_IPV4_RANGES.stream().anyMatch(range -> range.contains(value))
                    || value == ipv4Value(new byte[]{(byte) 168, 63, (byte) 129, 16});
        }
        if (bytes.length != 16
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        boolean globalUnicast = (bytes[0] & 0xE0) == 0x20;
        boolean special2001Range = bytes[0] == 0x20
                && bytes[1] == 0x01
                && (bytes[2] & 0xFE) == 0;
        boolean documentationRange = bytes[0] == 0x20
                && bytes[1] == 0x01
                && (bytes[2] & 0xFF) == 0x0D
                && (bytes[3] & 0xFF) == 0xB8;
        boolean sixToFourRange = bytes[0] == 0x20 && bytes[1] == 0x02;
        boolean documentationV6Range = bytes[0] == 0x3F
                && bytes[1] == (byte) 0xFF
                && (bytes[2] & 0xF0) == 0;
        return !globalUnicast || special2001Range || documentationRange || sixToFourRange || documentationV6Range;
    }

    private static long ipv4Value(byte[] bytes) {
        return ((bytes[0] & 0xFFL) << 24)
                | ((bytes[1] & 0xFFL) << 16)
                | ((bytes[2] & 0xFFL) << 8)
                | (bytes[3] & 0xFFL);
    }

    private static Ipv4Range range(String address, int prefixLength) {
        String[] octets = address.split("\\.");
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(octets[i]);
        }
        return new Ipv4Range(ipv4Value(bytes), prefixLength);
    }

    @FunctionalInterface
    interface AddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private record Ipv4Range(long network, int prefixLength) {
        private boolean contains(long address) {
            long mask = prefixLength == 0 ? 0 : (0xFFFF_FFFFL << (32 - prefixLength)) & 0xFFFF_FFFFL;
            return (address & mask) == (network & mask);
        }
    }
}
