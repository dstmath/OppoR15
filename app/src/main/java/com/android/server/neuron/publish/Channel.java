package com.android.server.neuron.publish;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.neuron.publish.Response.NativeIndication;
import com.android.server.neuron.publish.Response.NativeResponse;
import com.oppo.neuron.NeuronSystemManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public final class Channel {
    private static final int CONNECT_INTERNAL = 2000;
    private static final int MAX_COMMAND_BYTES = 3072;
    private static final int MAX_CONNECT_COUNT = 32;
    private static final int MAX_QUEUE_CAPACITY = 256;
    private static final int RE_INIT = 1;
    private static final String SOCK_NAME = "/dev/socket/neosocket";
    static final String TAG = "NeuronSystem";
    private boolean mInited = false;
    private InputStream mInputStream = null;
    private ChannelEventListener mListener = null;
    private OutputStream mOutputStream = null;
    private ArrayList<Request> mPendingRequestQueue = new ArrayList();
    private int mReConnectCount = 0;
    private RequestSender mSender = new RequestSender();
    private LocalSocket mSocket = new LocalSocket();
    private LocalSocketAddress mSocketAddress = new LocalSocketAddress(SOCK_NAME, Namespace.FILESYSTEM);
    private boolean mStop = true;
    private SparseArray<Request> mWaitForReponse = new SparseArray();
    private Handler myHandler = new Handler() {
        public void handleMessage(Message message) {
            if (message.what == 1) {
                Channel.this.mInited = false;
                if (!Channel.this.init()) {
                    Channel.this.triggerInitDelay();
                }
            }
        }
    };

    public interface ChannelEventListener {
        void onConnection(RequestSender requestSender);

        void onError(int i);

        void onIndication(NativeIndication nativeIndication);

        void onResponse(Request request, NativeResponse nativeResponse);
    }

    private class ReceiverThread extends Thread {
        private static final int INT_SIZE = 4;
        private int end;
        private byte[] mRecvBuf;

        /* synthetic */ ReceiverThread(Channel this$0, ReceiverThread -this1) {
            this();
        }

        private ReceiverThread() {
            this.mRecvBuf = new byte[Channel.MAX_COMMAND_BYTES];
            this.end = 0;
        }

        public void run() {
            while (!Channel.this.mStop) {
                try {
                    int len = Channel.this.mInputStream.read(this.mRecvBuf, this.end, 3072 - this.end);
                    if (len < 0) {
                        Slog.e("NeuronSystem", "ReceiverThread socket err");
                        this.end = 0;
                        Channel.this.mStop = true;
                        synchronized (Channel.this.mWaitForReponse) {
                            Channel.this.mWaitForReponse.clear();
                        }
                        Channel.this.triggerInitDelay();
                        return;
                    }
                    int copyLen;
                    this.end += len;
                    int start = 0;
                    while (this.end - start > 4) {
                        Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(this.mRecvBuf, start, this.end);
                        parcel.setDataPosition(0);
                        int responseLen = parcel.readInt();
                        if (responseLen + 4 < Channel.MAX_COMMAND_BYTES) {
                            int targetPos = (start + responseLen) + 4;
                            if (this.end >= targetPos) {
                                Response resp = Response.makeReponse(parcel);
                                start = targetPos;
                                parcel.recycle();
                                if (resp != null) {
                                    processResponse(resp);
                                    if (this.end - targetPos <= 0) {
                                        break;
                                    }
                                } else {
                                    Slog.d("NeuronSystem", "recv illegal bit data");
                                    this.end = targetPos;
                                    break;
                                }
                            }
                            copyLen = this.end - start;
                            System.arraycopy(this.mRecvBuf, start, this.mRecvBuf, 0, copyLen);
                            parcel.recycle();
                            this.end = copyLen;
                            if (NeuronSystemManager.LOG_ON) {
                                Slog.d("NeuronSystem", "recv data less than packet len");
                            }
                        } else {
                            Slog.e("NeuronSystem", "recv illegal bit data, data len larger than buffer size");
                            resetConnect();
                            parcel.recycle();
                            return;
                        }
                    }
                    copyLen = this.end - start;
                    System.arraycopy(this.mRecvBuf, start, this.mRecvBuf, 0, copyLen);
                    this.end = copyLen;
                    if (NeuronSystemManager.LOG_ON) {
                        Slog.d("NeuronSystem", "recv data less than 4");
                    }
                    if (this.end == start) {
                        this.end = 0;
                    }
                } catch (IOException e) {
                    Slog.e("NeuronSystem", "socket read err: " + e);
                    Channel.this.mStop = true;
                    resetConnect();
                    return;
                }
            }
        }

        private void processResponse(Response resp) {
            if (Channel.this.mListener == null) {
                return;
            }
            if (resp.isIndication()) {
                Channel.this.mListener.onIndication(resp.getIndication());
                return;
            }
            NativeResponse respData = resp.getResponseData();
            synchronized (Channel.this.mWaitForReponse) {
                int index = Channel.this.mWaitForReponse.indexOfKey(respData.serial);
                if (index >= 0) {
                    Request req = (Request) Channel.this.mWaitForReponse.get(respData.serial);
                    Channel.this.mListener.onResponse((Request) Channel.this.mWaitForReponse.get(respData.serial), respData);
                    Channel.this.mWaitForReponse.removeAt(index);
                    req.release();
                } else {
                    Slog.e("NeuronSystem", "receive a unknown sequence number: " + respData.serial);
                }
            }
            if (respData.error != 1) {
                Channel.this.mListener.onError(respData.error);
            }
        }

        private void resetConnect() {
            synchronized (Channel.this.mWaitForReponse) {
                Channel.this.mWaitForReponse.clear();
            }
            Channel.this.triggerInitDelay();
        }
    }

    public class RequestSender {
        public void sendRequest(Request req) {
            if (Channel.this.mInited) {
                try {
                    synchronized (Channel.this.mWaitForReponse) {
                        Channel.this.mWaitForReponse.put(req.getSequenceNumber(), req);
                    }
                    if (!Channel.this.doSendRequest(req)) {
                        synchronized (Channel.this.mWaitForReponse) {
                            Channel.this.mWaitForReponse.remove(req.getSequenceNumber());
                        }
                        synchronized (Channel.this.mPendingRequestQueue) {
                            if (Channel.this.mPendingRequestQueue.size() > 256) {
                                Channel.this.mPendingRequestQueue.clear();
                            }
                            Channel.this.mPendingRequestQueue.add(req);
                        }
                    }
                } catch (Exception e) {
                    Slog.e("NeuronSystem", "RequestSender add request to queue err: " + e);
                }
            }
        }
    }

    public void triggerInit() {
        if (SystemProperties.getBoolean("persist.sys.neuron.channel", false) && !this.myHandler.hasMessages(1)) {
            this.myHandler.sendEmptyMessage(1);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void triggerInitDelay() {
        if (this.mReConnectCount <= 32) {
            this.mReConnectCount++;
            if (!this.myHandler.hasMessages(1)) {
                long interval = (long) (this.mReConnectCount * 2000);
                this.myHandler.sendEmptyMessageDelayed(1, interval);
                if (NeuronSystemManager.LOG_ON) {
                    Slog.d("NeuronSystem", "Channel will init again in " + (interval / 1000) + "s");
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean init() {
        if (this.mInited) {
            return true;
        }
        if (!initSocket()) {
            return false;
        }
        initRecvThread();
        Slog.i("NeuronSystem", "Channel init ok");
        this.mInited = true;
        if (this.mListener != null) {
            this.mListener.onConnection(this.mSender);
        }
    }

    public RequestSender getRequestSender() {
        return this.mSender;
    }

    public void setEventListener(ChannelEventListener listener) {
        this.mListener = listener;
    }

    private boolean initSocket() {
        try {
            if (this.mSocket.isConnected()) {
                try {
                    this.mSocket.close();
                } catch (IOException e) {
                }
                this.mSocket = null;
            }
            this.mSocket = new LocalSocket();
            this.mSocket.connect(this.mSocketAddress);
            Slog.d("NeuronSystem", "local socket connect ok");
            try {
                this.mInputStream = this.mSocket.getInputStream();
                this.mOutputStream = this.mSocket.getOutputStream();
                drainPendingRequestQueue();
                this.mReConnectCount = 0;
                return true;
            } catch (IOException e2) {
                this.mInputStream = null;
                this.mOutputStream = null;
                Slog.e("NeuronSystem", "socket get inputstream or outputstream err:" + e2);
                try {
                    this.mSocket.close();
                } catch (IOException e3) {
                }
                return false;
            }
        } catch (IOException e4) {
            try {
                this.mSocket.close();
            } catch (IOException e5) {
            }
            return false;
        }
    }

    private void initRecvThread() {
        this.mStop = false;
        new ReceiverThread().start();
    }

    private boolean doSendRequest(Request req) {
        try {
            if (NeuronSystemManager.LOG_ON) {
                Slog.d("NeuronSystem", "doSendRequest timestamp:" + (System.nanoTime() / 1000));
            }
            if (this.mOutputStream == null) {
                return false;
            }
            this.mOutputStream.write(req.getBytes());
            this.mOutputStream.flush();
            return true;
        } catch (IOException e) {
            Slog.e("NeuronSystem", "write to local socket err: " + e);
            this.mOutputStream = null;
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean drainPendingRequestQueue() {
        Throwable th;
        if (this.mPendingRequestQueue.size() == 0) {
            return true;
        }
        synchronized (this.mPendingRequestQueue) {
            try {
                ArrayList<Request> tempRequestQueue = new ArrayList(this.mPendingRequestQueue);
                try {
                    this.mPendingRequestQueue.clear();
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }
}
