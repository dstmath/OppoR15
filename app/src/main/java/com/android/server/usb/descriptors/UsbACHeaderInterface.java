package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public abstract class UsbACHeaderInterface extends UsbACInterface {
    private static final String TAG = "UsbACHeaderInterface";
    protected int mADCRelease;
    protected int mTotalLength;

    public UsbACHeaderInterface(int length, byte type, byte subtype, byte subclass, int adcRelease) {
        super(length, type, subtype, subclass);
        this.mADCRelease = adcRelease;
    }

    public int getADCRelease() {
        return this.mADCRelease;
    }

    public int getTotalLength() {
        return this.mTotalLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("Release: " + ReportCanvas.getBCDString(getADCRelease()));
        canvas.writeListItem("Total Length: " + getTotalLength());
        canvas.closeList();
    }
}
