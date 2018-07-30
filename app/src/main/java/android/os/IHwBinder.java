package android.os;

public interface IHwBinder {
    public static final int FIRST_CALL_TRANSACTION = 1;
    public static final int FLAG_ONEWAY = 1;

    public interface DeathRecipient {
        void serviceDied(long j);
    }

    boolean linkToDeath(DeathRecipient deathRecipient, long j);

    IHwInterface queryLocalInterface(String str);

    void transact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient);
}
