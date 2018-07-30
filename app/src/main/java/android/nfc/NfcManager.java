package android.nfc;

import android.content.Context;

public final class NfcManager {
    private NfcAdapter mAdapter;
    private final Context mContext;

    public NfcManager(Context context) {
        this.mContext = context.getApplicationContext();
        init();
    }

    private void init() {
        if (this.mContext == null) {
            throw new IllegalArgumentException("context not associated with any application (using a mock context?)");
        }
        NfcAdapter adapter;
        try {
            adapter = NfcAdapter.getNfcAdapter(this.mContext);
        } catch (UnsupportedOperationException e) {
            adapter = null;
        }
        this.mAdapter = adapter;
    }

    public NfcAdapter getDefaultAdapter() {
        if (this.mAdapter == null) {
            init();
        }
        return this.mAdapter;
    }
}
