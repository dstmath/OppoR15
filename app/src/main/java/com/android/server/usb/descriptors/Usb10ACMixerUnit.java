package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACMixerUnit extends UsbACMixerUnit {
    private static final String TAG = "Usb10ACMixerUnit";
    private byte mChanNameID;
    private int mChannelConfig;
    private byte[] mControls;
    private byte mNameID;

    public Usb10ACMixerUnit(int length, byte type, byte subtype, byte subClass) {
        super(length, type, subtype, subClass);
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChanNameID() {
        return this.mChanNameID;
    }

    public byte[] getControls() {
        return this.mControls;
    }

    public byte getNameID() {
        return this.mNameID;
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        this.mChannelConfig = stream.unpackUsbShort();
        this.mChanNameID = stream.getByte();
        int controlArraySize = UsbACMixerUnit.calcControlArraySize(this.mNumInputs, this.mNumOutputs);
        this.mControls = new byte[controlArraySize];
        for (int index = 0; index < controlArraySize; index++) {
            this.mControls[index] = stream.getByte();
        }
        this.mNameID = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeParagraph("Mixer Unit", false);
        canvas.openList();
        canvas.writeListItem("Unit ID: " + ReportCanvas.getHexString(getUnitID()));
        byte numInputs = getNumInputs();
        byte[] inputIDs = getInputIDs();
        canvas.openListItem();
        canvas.write("Num Inputs: " + numInputs + " [");
        for (byte input = (byte) 0; input < numInputs; input++) {
            canvas.write("" + ReportCanvas.getHexString(inputIDs[input]));
            if (input < numInputs - 1) {
                canvas.write(" ");
            }
        }
        canvas.write("]");
        canvas.closeListItem();
        canvas.writeListItem("Num Outputs: " + getNumOutputs());
        canvas.writeListItem("Channel Config: " + ReportCanvas.getHexString(getChannelConfig()));
        byte[] controls = getControls();
        canvas.openListItem();
        canvas.write("Controls: " + controls.length + " [");
        for (int ctrl = 0; ctrl < controls.length; ctrl++) {
            canvas.write("" + controls[ctrl]);
            if (ctrl < controls.length - 1) {
                canvas.write(" ");
            }
        }
        canvas.write("]");
        canvas.closeListItem();
        canvas.closeList();
    }
}
