package com.karim.examples.rabbitmq.connector.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Network Utility class for the network card on a gumstix
 * 
 * @author Marcello de Sales (marcello.sales@gmail.com)
 *
 */
public final class NetworkUtil {
    /**
     * The current host IP address is the IP address from the device.
     */
    private static String currentHostIpAddress;
    
    /**
     * @return	the current environment's IP address, taking into account the Internet connection to
     * 			any of the available machine's Network interfaces. Examples of the outputs can be in 
     * octats or in IPV6 format.
     * <pre>
     *         ==> wlan0
     *         
     *         fec0:0:0:9:213:e8ff:fef1:b717%4 
     *         siteLocal: true 
     *         isLoopback: false isIPV6: true
     *         130.212.150.216 <<<<<<<<<<<---------- This is the one we want to grab so that we can. 
     *         siteLocal: false                      address the DSP on the network. 
     *         isLoopback: false 
     *         isIPV6: false 
     *         
     *         ==> lo 
     *         0:0:0:0:0:0:0:1%1 
     *         siteLocal: false 
     *         isLoopback: true 
     *         isIPV6: true 
     *         127.0.0.1 
     *         siteLocal: false 
     *         isLoopback: true 
     *         isIPV6: false
     *  </pre>
     */
    public static String getCurrentEnvironmentNetworkIP() {
        if (currentHostIpAddress == null) {
            Enumeration<NetworkInterface> netInterfaces = null;
            try {
                netInterfaces = NetworkInterface.getNetworkInterfaces();

                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    Enumeration<InetAddress> address = ni.getInetAddresses();
                    while (address.hasMoreElements()) {
                        InetAddress addr = address.nextElement();
                        if (!addr.isLoopbackAddress() 
                        		&& addr.isSiteLocalAddress() 
                        		&& !addr.isLinkLocalAddress() 
                        		&& !(addr.getHostAddress().indexOf(":") > -1)) {
                            currentHostIpAddress = addr.getHostAddress();
                        }
                    }
                }
                if (currentHostIpAddress == null) {
                    currentHostIpAddress = InetAddress.getLocalHost().getHostAddress();
                }

            } catch (Exception e) {
                currentHostIpAddress = "127.0.0.1";
            }
        }
        return currentHostIpAddress;
    }
   
}