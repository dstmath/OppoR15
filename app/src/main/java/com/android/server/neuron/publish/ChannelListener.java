package com.android.server.neuron.publish;

import android.net.ConnectivityManager;
import android.os.Parcel;
import android.util.Slog;
import com.android.server.neuron.publish.Channel.ChannelEventListener;
import com.android.server.neuron.publish.Channel.RequestSender;
import com.android.server.neuron.publish.Response.NativeIndication;
import com.android.server.neuron.publish.Response.NativeResponse;
import com.oppo.neuron.NeuronSystemManager;

public final class ChannelListener implements ChannelEventListener {
    private final String TAG = "NeuronSystem";
    private IndicationHandler mIndicationHandler;

    public void setIndicationHandler(IndicationHandler h) {
        this.mIndicationHandler = h;
    }

    public void onConnection(RequestSender sender) {
        Request req;
        Parcel parcel;
        String app = NeuronContext.getSystemStatus().getForegroundApp();
        if (app != null) {
            req = Request.obtain();
            parcel = req.prepare();
            parcel.writeInt(1);
            parcel.writeString(app);
            parcel.writeInt(1);
            parcel.writeString("version");
            req.commit();
            sender.sendRequest(req);
        }
        int type = NeuronContext.getSystemStatus().getNetworkType();
        if (ConnectivityManager.isNetworkTypeValid(type)) {
            req = Request.obtain();
            parcel = req.prepare();
            parcel.writeInt(7);
            parcel.writeInt(type);
            parcel.writeInt(1);
            parcel.writeString(NeuronContext.getSystemStatus().getIfaceName());
            if (type == 1) {
                String ssid = NeuronContext.getSystemStatus().getWifissid();
                String bssid = NeuronContext.getSystemStatus().getWifiBssid();
                parcel.writeString(ssid);
                parcel.writeString(bssid);
                parcel.writeInt(0);
            }
            req.commit();
            sender.sendRequest(req);
        }
    }

    public void onError(int error) {
        Slog.e("NeuronSystem", "ChannelListener  onError err:" + error);
    }

    public void onResponse(Request req, NativeResponse resp) {
        if (NeuronSystemManager.LOG_ON) {
            Slog.d("NeuronSystem", "ChannelListener  onResponse resp:" + resp.toString());
            long us = (System.nanoTime() / 1000) - req.getTimeStamp();
            long ms = us / 1000;
            Slog.d("NeuronSystem", "ChannelListener  request-> response consume time:" + ms + "." + (us % 1000) + "ms");
        }
    }

    public void onIndication(NativeIndication indication) {
        if (NeuronSystemManager.LOG_ON) {
            Slog.d("NeuronSystem", "ChannelListener  onIndication indication:" + indication.toString());
        }
        if (this.mIndicationHandler != null) {
            this.mIndicationHandler.handle(indication);
        }
    }
}
