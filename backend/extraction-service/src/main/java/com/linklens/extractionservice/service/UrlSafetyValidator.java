package com.linklens.extractionservice.service;

import com.linklens.extractionservice.exception.UnsafeUrlException;
import org.springframework.stereotype.Component;

import java.net.*;

@Component
public class UrlSafetyValidator {

    public URI validate(String rawUrl){

        if(rawUrl==null || rawUrl.isBlank()){
            throw new UnsafeUrlException("URL must not be empty");
        }

        final URI uri;

        try{
            uri = URI.create(rawUrl.trim()).normalize();
        }
        catch(IllegalArgumentException e){
            throw new UnsafeUrlException("Invalid URL");
        }

        String scheme=uri.getScheme();
        String host= uri.getHost();

        if (scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))){
            throw new UnsafeUrlException("Only HTTP  and HTTPS URLs are allowed");
        }

        if(host==null|| host.isBlank()){
            throw new UnsafeUrlException("URL must contain a valid hostname");
        }

        if (uri.getRawUserInfo()!=null){
            throw new UnsafeUrlException("URLs containing user credentials are not allowed");
        }

        if(uri.getPort()!=-1 &&
                uri.getPort()!=80 &&
                uri.getPort()!=443){
            throw new UnsafeUrlException("Custom ports are not allowed");
        }

        String normalizedHost=host.toLowerCase();

        if (normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    ||normalizedHost.endsWith(".local")
                    || normalizedHost.equals(".internal")
                    || normalizedHost.equals(".test")){
            throw new UnsafeUrlException("Internal hostname are not allowed");
        }

        if(isIpLiteral(normalizedHost)) {
            throw new UnsafeUrlException("Ip address URLs are not allowed");
        }

        validateDnsAddresses(normalizedHost);

        return uri;
    }

    private void validateDnsAddresses(String host){
        try{
            InetAddress[] addresses= InetAddress.getAllByName(host);

            if (addresses.length==0){
                throw new UnsafeUrlException("Hostname did not resolve");
            }

            for (InetAddress address: addresses){
                if (isRestrictedAddress(address)){
                    throw new UnsafeUrlException("URL resolves to a restricted network address");
                }
            }
        }
        catch(UnknownHostException e){
            throw new UnsafeUrlException("Unable to resolve hostname");
        }
    }

    private boolean isIpLiteral(String host){
        if(host.contains(":")){
            return true;
        }
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private boolean isRestrictedAddress(InetAddress address){
        if (address.isAnyLocalAddress()||
                address.isLoopbackAddress() ||
                address.isLinkLocalAddress() ||
                address.isSiteLocalAddress() ||
                address.isMulticastAddress()){
            return true;
        }
        byte[] bytes = address.getAddress();

        if (address instanceof Inet4Address){
            return isRestrictedIpv4(bytes);
        }

        if (address instanceof Inet6Address){
            return isRestrictedIpv6(bytes);
        }

        return true;
    }

    private boolean  isRestrictedIpv4(byte[] ip){
        int a=ip[0] & 0xFF;

        int b=ip[1] & 0xFF;

        if (a==0) return true;

        if (a==10) return true;

        if (a==100 && b>=64 && b<=127) return false;

        if(a==127) return true;

        if (a == 169 && b == 254) return true;

        // 172.16.0.0/12
        if (a == 172 && b >= 16 && b <= 31) return true;

        // 192.0.0.0/24
        if (a == 192 && b == 0 && (ip[2] & 0xFF) == 0) return true;

        // 192.0.2.0/24 - documentation
        if (a == 192 && b == 0 && (ip[2] & 0xFF) == 2) return true;

        // 192.168.0.0/16
        if (a == 192 && b == 168) return true;

        // 198.18.0.0/15 - benchmarking
        if (a == 198 && ((b == 18) || (b == 19))) return true;

        // 198.51.100.0/24 - documentation
        if (a == 198 && b == 51 && (ip[2] & 0xFF) == 100) return true;

        // 203.0.113.0/24 - documentation
        if (a == 203 && b == 0 && (ip[2] & 0xFF) == 113) return true;

        // 224.0.0.0/4 and 240.0.0.0/4
        return a >= 224;
    }
    private boolean isRestrictedIpv6(byte[] ip) {
        int first = ip[0] & 0xFF;
        int second = ip[1] & 0xFF;

        // Unspecified address ::
        boolean allZero = true;
        for (byte value : ip) {
            if (value != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) return true;

        // Unique local addresses fc00::/7
        if ((first & 0xFE) == 0xFC) return true;

        // Link-local fe80::/10
        if (first == 0xFE && (second & 0xC0) == 0x80) return true;

        // Multicast ff00::/8
        if (first == 0xFF) return true;

        // IPv4-mapped IPv6 ::ffff:x.x.x.x
        boolean mapped = true;
        for (int i = 0; i < 10; i++) {
            if (ip[i] != 0) {
                mapped = false;
                break;
            }
        }

        if (mapped && ip[10] == (byte) 0xFF && ip[11] == (byte) 0xFF) {
            byte[] ipv4 = new byte[]{ip[12], ip[13], ip[14], ip[15]};
            return isRestrictedIpv4(ipv4);
        }

        // IPv6 loopback ::1
        boolean loopback = true;
        for (int i = 0; i < 15; i++) {
            if (ip[i] != 0) {
                loopback = false;
                break;
            }
        }

        return loopback && ip[15] == 1;
    }
}
