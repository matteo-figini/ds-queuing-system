package misc;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class AddressHandler {
    public static String retrieveCorrectIPAddress () {
        try
        {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface) e.nextElement();
                Enumeration ee = ni.getInetAddresses();
                while(ee.hasMoreElements())
                {
                    InetAddress ia = (InetAddress) ee.nextElement();

                    String stringa = ia.toString();
                    if(stringa.startsWith("/192.168"))
                        return ia.getHostAddress();
                }
            }
        }
        catch (Exception e) {}
        return null;
    }
}
