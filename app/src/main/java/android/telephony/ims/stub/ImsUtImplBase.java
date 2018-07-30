package android.telephony.ims.stub;

import android.os.Bundle;
import android.os.RemoteException;
import com.android.ims.internal.IImsUt.Stub;
import com.android.ims.internal.IImsUtListener;

public class ImsUtImplBase extends Stub {
    public void close() throws RemoteException {
    }

    public int queryCallBarring(int cbType) throws RemoteException {
        return -1;
    }

    public int queryCallBarringForServiceClass(int cbType, int serviceClass) throws RemoteException {
        return -1;
    }

    public int queryCallForward(int condition, String number) throws RemoteException {
        return -1;
    }

    public int queryCFForServiceClass(int condition, String number, int serviceClass) throws RemoteException {
        return -1;
    }

    public int queryCallWaiting() throws RemoteException {
        return -1;
    }

    public int queryCLIR() throws RemoteException {
        return -1;
    }

    public int queryCLIP() throws RemoteException {
        return -1;
    }

    public int queryCOLR() throws RemoteException {
        return -1;
    }

    public int queryCOLP() throws RemoteException {
        return -1;
    }

    public int transact(Bundle ssInfo) throws RemoteException {
        return -1;
    }

    public int updateCallBarring(int cbType, int action, String[] barrList) throws RemoteException {
        return -1;
    }

    public int updateCallBarringForServiceClass(int cbType, int action, int serviceClass, String[] barrList) throws RemoteException {
        return -1;
    }

    public int updateCallForward(int action, int condition, String number, int serviceClass, int timeSeconds) throws RemoteException {
        return 0;
    }

    public int updateCallWaiting(boolean enable, int serviceClass) throws RemoteException {
        return -1;
    }

    public int updateCLIR(int clirMode) throws RemoteException {
        return -1;
    }

    public int updateCLIP(boolean enable) throws RemoteException {
        return -1;
    }

    public int updateCOLR(int presentation) throws RemoteException {
        return -1;
    }

    public int updateCOLP(boolean enable) throws RemoteException {
        return -1;
    }

    public void setListener(IImsUtListener listener) throws RemoteException {
    }
}
