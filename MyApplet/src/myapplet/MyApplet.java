package myapplet;

import javacard.framework.*;
import javacard.security.KeyBuilder;
import javacard.security.*;
import javacardx.crypto.*;

public class MyApplet extends Applet {

    private static byte[] name, birthDate, gender, address, pin, balance, point, avatar;
    private static short nameLen, birthDateLen, genderLen, addressLen, pinLen, balanceLen, pointLen, avatarLen;
    private static short counter;

    private static final byte INS_INIT_CUSTOMER = (byte) 0x10;
    private static final byte UNBLOCK_CARD = (byte) 0x11;
    private static final byte INS_RQPIN = (byte) 0x12;
    private static final byte INS_GETINFO = (byte) 0x13;
    private static final byte INS_GET_BALANCE = (byte) 0x14;
    private static final byte INS_GET_POINT = (byte) 0x15;
    private static final byte INS_SET_BALANCE = (byte) 0x16;
    private static final byte INS_SET_POINT = (byte) 0x17;
    private static final byte CHECK_CARD = (byte) 0x18;
    private static final byte CLEAR_CARD = (byte) 0x19;
    private static final byte CHECK_PIN = (byte) 0x20;
    private static final byte INS_INIT_CUSTOMER_AVATAR = (byte) 0x21;

    //trạng thái thẻ
    private static boolean blockCard = false;
    //mảng để send ra apdu các offset logic
    private final static byte[] abc = {(byte) 0x3A, (byte) 0x00, (byte) 0x01};
    //mảng tạm, các mảng lưu giữ khóa
    final private byte[] tempBuffer, aesKey, rsaPubKey, rsaPriKey, keyrsa;
    //mấy thứ linh tinh khác
    private byte aesKeyLen;
    private Cipher aesEcbCipher, rsaCipher;
    private AESKey tempAesKey1;
    private short rsaPubKeyLen, rsaPriKeyLen;
    private static final short SW_REFERENCE_DATA_NOT_FOUND = (short) 0x6A88;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new MyApplet();
    }

    public MyApplet() {
        register();
        //CUSTOMER
        name = new byte[64];//vi su dung ma hoa 512 (64byte)
        birthDate = new byte[64];
        gender = new byte[64];
        address = new byte[64];
        pin = new byte[18];
        balance = new byte[64];
        point = new byte[64];

        //RSA
        tempBuffer = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);
        rsaPubKey = new byte[(short) 128];
        rsaPriKey = new byte[(short) 128];
        keyrsa = new byte[(short) 128];
        rsaPubKeyLen = 0;
        rsaPriKeyLen = 0;
        counter = 3;

        //Create a RSA(with pad) object instance
        rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);

        //AES
        aesKey = new byte[16];
        aesKeyLen = 0;
        JCSystem.requestObjectDeletion();
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        switch (buf[ISO7816.OFFSET_INS]) {
            case INS_INIT_CUSTOMER:
                init_customer(apdu, len);
                break;
            case INS_RQPIN:
                get_pin(apdu);
                break;
            case INS_GETINFO:
                get_customer_info(apdu);
                break;
            case INS_GET_BALANCE:
                get_balance(apdu);
                break;
            case INS_GET_POINT:
                get_point(apdu);
                break;
            case INS_SET_BALANCE:
                set_balance(apdu, len);
                break;
            case INS_SET_POINT:
                set_point(apdu, len);
                break;
            case CHECK_CARD:
                check_card(apdu);
                break;
            case CLEAR_CARD:
                clear_card(apdu);
                break;
            case CHECK_PIN://20
                check_pin(apdu, len);
                break;
            case UNBLOCK_CARD://11
                unblockCard(apdu);
                break;
            case INS_INIT_CUSTOMER_AVATAR:
                init_customer_avatar(apdu, len);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    //true la bi khoa, else binh thuong (OK)
    private void check_card(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 1);
        if (blockCard == true) {//return 1
            apdu.sendBytesLong(abc, (short) 2, (short) 1);
        } else {//return 0
            apdu.sendBytesLong(abc, (short) 1, (short) 1);
        }
    }

    private void check_pin(APDU apdu, short len) {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 1);
        //so sanh 2 mang, return 0 if = nhau
        if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, pin, (short) 0, len) == 0) {
            apdu.sendBytesLong(abc, (short) 1, (short) 1);//gui 0
        } else {
            counter--;
            if (counter == 0) {
                blockCard = true;
            }
            apdu.sendBytesLong(abc, (short) 2, (short) 1);//gui 1
        }
    }

    private void unblockCard(APDU apdu) {
        counter = 3;
        blockCard = false;
    }

    private void clear_card(APDU apdu) {
        balanceLen = (short) 0;
        genderLen = (short) 0;
        nameLen = (short) 0;
        birthDateLen = (short) 0;
        pinLen = (short) 0;
        addressLen = (short) 0;
        balanceLen = (short) 0;
        Util.arrayFillNonAtomic(name, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(birthDate, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(address, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(gender, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(pin, (short) 0, (short) 18, (byte) 0);
        Util.arrayFillNonAtomic(point, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(balance, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(name, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(name, (short) 0, (short) 64, (byte) 0);
        Util.arrayFillNonAtomic(rsaPriKey, (short) 0, (short) 128, (byte) 0);
        Util.arrayFillNonAtomic(rsaPubKey, (short) 0, (short) 128, (byte) 0);
        Util.arrayFillNonAtomic(aesKey, (short) 0, (short) 16, (byte) 0);
    }

    private void get_pin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(pin, (short) 0, buffer, (short) 0, (short) 18);
        apdu.setOutgoingAndSend((short) 0, (short) 18);
    }

    private void get_customer_info(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 200);
        buffer[0] = (byte) 0x3a;
        doAesCipher(apdu, rsaPriKey, (short) 128, (byte) 1, keyrsa);//ok 

        doRSACipher2(apdu, name, (short) 64);
        doRSACipher2(apdu, birthDate, (short) 64);
        doRSACipher2(apdu, address, (short) 64);
        doRSACipher2(apdu, gender, (short) 64);
        get_balance(apdu);
        get_point(apdu);
    }

    private void get_balance(APDU apdu) {
        if (balanceLen != 0) {
            byte[] buffer = apdu.getBuffer();
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 65);
            doAesCipher(apdu, rsaPriKey, (short) 128, (byte) 1, keyrsa);//ok 
            doRSACipher2(apdu, balance, (short) 64);
        }
    }

    private void get_point(APDU apdu) {
        if (balanceLen != 0) {
            byte[] buffer = apdu.getBuffer();
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 65);
            doAesCipher(apdu, rsaPriKey, (short) 128, (byte) 1, keyrsa);//ok 
            doRSACipher2(apdu, point, (short) 64);
        }
    }

    private void set_balance(APDU apdu, short len) {
        balanceLen = (short) len;
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 65);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, balance, (short) 0, len);
        doRSACipher(apdu, (short) 0, balance, balanceLen, (short) 0);
        doRSACipher2(apdu, balance, (short) 64);
    }

    private void set_point(APDU apdu, short len) {
        balanceLen = (short) len;
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 65);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, point, (short) 0, len);
        doRSACipher(apdu, (short) 0, point, balanceLen, (short) 0);
        doRSACipher2(apdu, point, (short) 64);
    }

    private void init_customer(APDU apdu, short len) {
        short tg1, tg2, tg3, tg4;
        tg1 = tg2 = tg3 = tg4 = 0;

        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);

        for (short i = 0; i < len; i++) {
            if (tempBuffer[i] == (byte) 0x2e) {
                if (tg1 == 0) {
                    tg1 = i;
                    nameLen = (short) tg1;
                } else {
                    if (tg2 == 0) {
                        tg2 = i;
                        birthDateLen = (short) (tg2 - tg1 - 1);
                    } else {
                        if (tg3 == 0) {
                            tg3 = i;
                            addressLen = (short) (tg3 - tg2 - 1);
                        } else {
                            if (tg4 == 0) {
                                tg4 = i;
                                genderLen = (short) (tg4 - tg3 - 1);
                                pinLen = (short) 18;
                            }
                        }
                    }
                }
            }
        }

        Util.arrayCopy(tempBuffer, (short) 0, name, (short) 0, nameLen);
        Util.arrayCopy(tempBuffer, (short) (tg1 + 1), birthDate, (short) 0, birthDateLen);
        Util.arrayCopy(tempBuffer, (short) (tg2 + 1), address, (short) 0, addressLen);
        Util.arrayCopy(tempBuffer, (short) (tg3 + 1), gender, (short) 0, genderLen);
        Util.arrayCopy(tempBuffer, (short) (tg4 + 1), pin, (short) 0, pinLen);
//        Util.arrayCopy(tempBuffer, (short) (tg5 + 1), pin, (short) 0, pinLen);

        //can tao ra cap khoa truoc
        genRsaKeyPair(apdu);
        //ma hoa private key
        encrypt_private_key(apdu);
        //giai ma private key
        decrypt_private_key(apdu);
        //tiep theo can ma hoa thong tin cua khach hang
        doRSACipher(apdu, (short) 0, name, nameLen, (short) 0);
        doRSACipher(apdu, (short) 0, birthDate, birthDateLen, (short) 0);
        doRSACipher(apdu, (short) 0, address, addressLen, (short) 0);
        doRSACipher(apdu, (short) 0, gender, genderLen, (short) 0);
        //giai ma va gui du lieu ve apdu
        doRSACipher(apdu, (short) 1, name, nameLen, (short) 0);
        Util.arrayFillNonAtomic(tempBuffer, nameLen, (short) 1, (byte) 0x3A);//dau :
        doRSACipher(apdu, (short) 1, birthDate, birthDateLen, (short) (nameLen + 1));
        Util.arrayFillNonAtomic(tempBuffer, (short) (nameLen + birthDateLen + 1), (short) 1, (byte) 0x3A);
        doRSACipher(apdu, (short) 1, address, addressLen, (short) (nameLen + birthDateLen + 2));
        Util.arrayFillNonAtomic(tempBuffer, (short) (nameLen + birthDateLen + addressLen + 2), (short) 1, (byte) 0x3A);
        doRSACipher(apdu, (short) 1, gender, genderLen, (short) (nameLen + birthDateLen + addressLen + 3));
        Util.arrayFillNonAtomic(tempBuffer, (short) (nameLen + birthDateLen + addressLen + genderLen + 3), (short) 1, (byte) 0x3A);

        Util.arrayCopy(tempBuffer, (short) 0, buffer, (short) 0, len);
        apdu.setOutgoingAndSend((short) 0, len);//sau khi ma hoa, do dai la 64 byte
    }

    private void init_customer_avatar(APDU apdu, short len) {
        byte[] buffer = apdu.getBuffer();
        short dataLen = (short) (buffer[ISO7816.OFFSET_LC] & 0xff);
        avatarLen = dataLen;
        short pointer = 0;
        while (dataLen > 0) {
            Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, avatar, pointer, avatarLen);
            pointer += len;
            dataLen -= len;
            len = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        apdu.setOutgoingAndSend((short) 0, len);

    }

    private void encrypt_private_key(APDU apdu) {
        setAesKey(apdu, pinLen);//ok
        doAesCipher(apdu, rsaPriKey, (short) 128, (byte) 0, rsaPriKey);//ok   
    }

    private void decrypt_private_key(APDU apdu) {
        setAesKey(apdu, pinLen);//ok
        doAesCipher(apdu, rsaPriKey, (short) 128, (byte) 1, keyrsa);//ok 
    }

    //RSA algorithm encrypt and decrypt, tham so P2 co dinh 00, P1 tuy chon (xem them ben duoi)
    private void doRSACipher(APDU apdu, short mode, byte[] arr, short len, short off) {//mode la che do ma hoa hay giai ma
        byte[] buffer = apdu.getBuffer();
        short keyLen = KeyBuilder.LENGTH_RSA_512;
        short offset = (short) 64;
        if (len <= 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        //RSA encrypt, Public Key will be used
        if (mode == (short) 0) {
            //Create uninitialized public key for signature and cipher algorithms.
            RSAPublicKey pubKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, keyLen, false);//fasle la k ma hoa key
            pubKey.setModulus(rsaPubKey, (short) 0, offset);
            pubKey.setExponent(rsaPubKey, offset, (short) 3);
            //In multiple-part encryption/decryption operations, only the fist APDU command will be used.
            rsaCipher.init(pubKey, Cipher.MODE_ENCRYPT);
            //tao ra dau ra duoc ma hoa tu tat ca du lieu dau vao
            //tham so(array_input,offset_in,len,array_out,offset_out)
            short outlen = rsaCipher.doFinal(arr, (short) 0, len, buffer, (short) 0);
            //apdu.setOutgoingAndSend((short) 0, outlen);
            //ma hoa xong save lai vao mang goc
            Util.arrayCopy(buffer, (short) 0, arr, (short) 0, outlen);
        } else//RSA decrypt, Private Key will be used
        {
            RSAPrivateKey priKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, keyLen, false);
            priKey.setModulus(keyrsa, (short) 0, offset);
            priKey.setExponent(keyrsa, offset, offset);
            rsaCipher.init(priKey, Cipher.MODE_DECRYPT);
            short outlen = rsaCipher.doFinal(arr, (short) 0, (short) 64, tempBuffer, off);
            //apdu.setOutgoingAndSend((short) 0, outlen);//ok            
        }
    }

    private void doRSACipher2(APDU apdu, byte[] arr, short len) {
        byte[] buffer = apdu.getBuffer();
        short keyLen = KeyBuilder.LENGTH_RSA_512;
        short offset = (short) 64;
        RSAPrivateKey priKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, keyLen, false);
        priKey.setModulus(keyrsa, (short) 0, offset);
        priKey.setExponent(keyrsa, offset, offset);
        rsaCipher.init(priKey, Cipher.MODE_DECRYPT);
        short outlen = rsaCipher.doFinal(arr, (short) 0, len, buffer, (short) 0);
        apdu.sendBytes((short) 0, outlen);
        apdu.sendBytesLong(abc, (short) 0, (short) 1);
    }

    //Get the value of RSA Public Key from the global variable 'rsaPubKey', p1=0 lay ra N, =1 lay ra E
    private void getRsaPubKey(APDU apdu, short len) {
        byte[] buffer = apdu.getBuffer();
        if (rsaPubKeyLen == 0) {
            ISOException.throwIt(SW_REFERENCE_DATA_NOT_FOUND);
        }
        short modLen = (short) 64;
        switch (buffer[ISO7816.OFFSET_P1]) {
            case 0:
                //get puclic key N
                Util.arrayCopyNonAtomic(rsaPubKey, (short) 0, buffer, (short) 0, modLen);
                apdu.setOutgoingAndSend((short) 0, modLen);
                break;
            case 1:
                //get public key E
                short eLen = (short) (rsaPubKeyLen - modLen);
                Util.arrayCopyNonAtomic(rsaPubKey, modLen, buffer, (short) 0, eLen);
                apdu.setOutgoingAndSend((short) 0, eLen);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
                break;
        }
    }

    //According to the different ID, returns the value/length of RSA Private component
    private short getRsaPriKeyComponent(byte id, byte[] outBuff, short outOff)//id truyen vao la tham so P1
    {
        if (rsaPriKeyLen == 0) {
            return (short) 0;
        }
        short modLen = (short) 64;// do dai cua thanh phan module N trong khoa
        short readOff;//read offset; doc tu dau
        short readLen;//do dai doc

        switch (id) {
            case (byte) 0:
                //RSA private key N
                readOff = (short) 0;
                readLen = modLen;
                break;
            case (byte) 1:
                //RSA private key D
                readOff = modLen;
                readLen = modLen;
                break;
            default:
                return 0;
        }
        Util.arrayCopyNonAtomic(rsaPriKey, readOff, outBuff, outOff, readLen);
        return readLen;
    }

    //lay ra dau, va lay ra gi
    private void getRsaPriKey(byte[] arr, byte mode) {//mode la lay ra N (0)  hay D (1)
        //byte[] buffer = apdu.getBuffer();
        short ret = getRsaPriKeyComponent(mode, arr, (short) 0);//mode, outbuf, offbuf
        if (ret == 0) {
            ISOException.throwIt(SW_REFERENCE_DATA_NOT_FOUND);
        }
        //apdu.setOutgoingAndSend((short) 0, ret);
    }

    private void genRsaKeyPair(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        try {
            KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_512);
            keyPair.genKeyPair();
            JCSystem.beginTransaction();
            rsaPubKeyLen = 0;
            rsaPriKeyLen = 0;
            JCSystem.commitTransaction();
            //Get a reference to the public key component of this 'keyPair' object.
            RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
            short pubKeyLen = 0;
            //Store the RSA public key value in the global variable 'rsaPubKey', the public key contains modulo N and Exponent E
            pubKeyLen += pubKey.getModulus(rsaPubKey, pubKeyLen);//N
            pubKeyLen += pubKey.getExponent(rsaPubKey, pubKeyLen);//E

            short priKeyLen = 0;
            //Returns a reference to the private key component of this KeyPair object.
            RSAPrivateKey priKey = (RSAPrivateKey) keyPair.getPrivate();
            //RSA Algorithm,  the Private Key contains N and D, and store these parameters value in global variable 'rsaPriKey'.
            priKeyLen += priKey.getModulus(rsaPriKey, priKeyLen);//N
            priKeyLen += priKey.getExponent(rsaPriKey, priKeyLen);//D

            JCSystem.beginTransaction();
            rsaPubKeyLen = pubKeyLen;
            rsaPriKeyLen = priKeyLen;
            JCSystem.commitTransaction();
        } catch (CryptoException e) {
            short reason = e.getReason();
            ISOException.throwIt(reason);
        }

        JCSystem.requestObjectDeletion();
    }

    //set AES key 128bit (16 byte)
    private void setAesKey(APDU apdu, short len) {
        byte[] buffer = apdu.getBuffer();
        byte keyLen = 16;
        if (len < 16) // The length of key is 16 bytes
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        //Copy the incoming AES Key value to the global variable 'aesKey'
        JCSystem.beginTransaction();
        Util.arrayCopy(pin, (short) 0, aesKey, (short) 0, (short) 16);
        aesKeyLen = keyLen;
        JCSystem.commitTransaction();
    }

    //AES algorithm encrypt and decrypt, p1==00 encrypt else decrypt, p2=00 ecb
    private void doAesCipher(APDU apdu, byte[] arr, short len, byte mod, byte[] arrb) {
        try {
            byte[] buffer = apdu.getBuffer();
            aesEcbCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);//externalAccess  =false
            tempAesKey1 = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
            if (len <= 0 || len % 16 != 0) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            tempAesKey1.setKey(aesKey, (short) 0);
            byte mode = mod == (byte) 0x00 ? Cipher.MODE_ENCRYPT : Cipher.MODE_DECRYPT;
            Cipher cipher = aesEcbCipher;
            cipher.init(tempAesKey1, mode);

            if (mode == 0) {
                cipher.doFinal(arr, (short) 0, len, buffer, (short) 0);
                Util.arrayCopy(buffer, (short) 0, arr, (short) 0, len);
            } else {
                cipher.doFinal(arr, (short) 0, len, buffer, (short) 0);
                Util.arrayCopy(buffer, (short) 0, arrb, (short) 0, len);
            }
            //apdu.setOutgoingAndSend((short) 0, len);
        } catch (CryptoException e) {
            short reason = e.getReason();
            ISOException.throwIt(reason);
        }
    }
}
