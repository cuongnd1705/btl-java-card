package thethongminh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.sun.javacard.apduio.*;

public class TheThongMinh {

    private Apdu apdu;
    private Socket sock;
    private OutputStream os;
    private InputStream is;
    private CadClientInterface cad;

    public TheThongMinh() {
        apdu = new Apdu();
    }

    //thiet lap ket noi voi applet thong qua java card runtime voi port 9025
    public void establishConnectionToSimulator() {
        try {
            sock = new Socket("localhost", 9027);
            os = sock.getOutputStream();
            is = sock.getInputStream();
            //khoi dau 1 thuc the card chap nhan the
            cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //dong ket noi voi JCR
    public void closeConnection() {
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //cung cap nguon
    public void pwrUp() {
        try {
            if (cad != null) {
                cad.powerUp();
            }
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
        }
    }

    //tat nguon
    public void pwrDown() {
        try {
            if (cad != null) {
                cad.powerDown(true);
            }
            if (sock!=null) {
                sock.close();
            }
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
        }
    }

    //set APDU cmd (HEADER)
    public void setTheAPDUCommands(byte[] cmnds) {
        if (cmnds.length > 4 || cmnds.length == 0) {
            System.err.println("inavlid commands");
        } else {
            apdu.command = cmnds;
            System.out.println("CLA: " + atrToHex(cmnds[0]));
            System.out.println("INS: " + atrToHex(cmnds[1]));
            System.out.println("P1: " + atrToHex(cmnds[2]));
            System.out.println("P2: " + atrToHex(cmnds[3]));
        }
    }

    //set Lc
    public void setTheDataLength(byte len) {
        apdu.Lc = len;
        System.out.println("Lc: " + atrToHex(len));
    }
    public void setTheDataLengthShort(short len) {
        apdu.Lc = len;
        System.out.println("Lc: " + shorttoHex(len));
    }

    //gui du lieu den applet (tu mang data)
    public void setTheDataIn(byte[] data) {
        if (data.length != apdu.Lc) {
            System.err.println("The number of data in the array are more than expected");
        } else {
            //set the data to be sent to the applets
            apdu.dataIn = data;
            for (int i = 0; i < data.length; i++) {
                System.out.println("dataIndex" + i + ": " + atrToHex(data[i]));
            }
        }
    }

    //Le
    public void setExpctdByteLength(byte len) {
        apdu.Le = len;
        System.out.println("Le: " + atrToHex(len));
    }
    
    public void setExpctShortLength(short len) {
        apdu.Le = len;
        System.out.println("Le: " + shorttoHex(len));
    }

    //trao doi du lieu (apdu nhan du lieu tu applet)
    public void exchangeTheAPDUWithSimulator() {
        try {
            apdu.setDataIn(apdu.dataIn, apdu.Lc);
            cad.exchangeApdu(apdu);//cau lenh thuc hien trao doi du lieu giua apdu va applet
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
        }
    }

    //convert data respone to hex
    public byte[] decodeDataOut() {
        byte[] dout = apdu.dataOut;
        for (int i = 0; i < dout.length; i++) {
            System.out.println("dataOut" + i + ": " + atrToHex(dout[i]));
        }
        return dout;
    }

    //get sw1 sw2, convert to hex
    public byte[] decodeStatus() {
        byte[] statByte = apdu.getSw1Sw2();
        System.out.println("SW1: " + atrToHex(statByte[0]));
        System.out.println("SW2: " + atrToHex(statByte[1]));
        return statByte;
    }

    //convert byte to hex
    public String atrToHex(byte atCode) {
        StringBuilder result = new StringBuilder();
            result.append(String.format("%02x", atCode));
        return result.toString();
    }
    public String shorttoHex(short atCode) {
        StringBuilder result = new StringBuilder();
            result.append(String.format("%02x", atCode));
        return result.toString();
    }
}
