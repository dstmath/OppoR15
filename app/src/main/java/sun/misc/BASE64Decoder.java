package sun.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Locale;

public class BASE64Decoder extends CharacterDecoder {
    private static final char[] pem_array = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private static final byte[] pem_convert_array = new byte[256];
    byte[] decode_buffer = new byte[4];

    protected int bytesPerAtom() {
        return 4;
    }

    protected int bytesPerLine() {
        return 72;
    }

    static {
        int i;
        for (i = 0; i < 255; i++) {
            pem_convert_array[i] = (byte) -1;
        }
        for (i = 0; i < pem_array.length; i++) {
            pem_convert_array[pem_array[i]] = (byte) i;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void decodeAtom(PushbackInputStream inStream, OutputStream outStream, int rem) throws IOException {
        int a = -1;
        int b = -1;
        int c = -1;
        int d = -1;
        if (rem >= 2) {
            while (true) {
                int i = inStream.read();
                if (i != -1) {
                    if (i != 10 && i != 13) {
                        this.decode_buffer[0] = (byte) i;
                        if (readFully(inStream, this.decode_buffer, 1, rem - 1) != -1) {
                            if (rem > 3 && this.decode_buffer[3] == (byte) 61) {
                                rem = 3;
                            }
                            if (rem > 2 && this.decode_buffer[2] == (byte) 61) {
                                rem = 2;
                            }
                            switch (rem) {
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    d = pem_convert_array[this.decode_buffer[3] & 255];
                                    break;
                            }
                        }
                        throw new CEStreamExhausted();
                    }
                } else {
                    throw new CEStreamExhausted();
                }
            }
        }
        throw new CEFormatException("BASE64Decoder: Not enough bytes for an atom.");
    }
}
