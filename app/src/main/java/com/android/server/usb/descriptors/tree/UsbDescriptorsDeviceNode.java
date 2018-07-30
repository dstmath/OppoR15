package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;

public final class UsbDescriptorsDeviceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsDeviceNode";
    private final ArrayList<UsbDescriptorsConfigNode> mConfigNodes = new ArrayList();
    private final UsbDeviceDescriptor mDeviceDescriptor;

    public UsbDescriptorsDeviceNode(UsbDeviceDescriptor deviceDescriptor) {
        this.mDeviceDescriptor = deviceDescriptor;
    }

    public void addConfigDescriptorNode(UsbDescriptorsConfigNode configNode) {
        this.mConfigNodes.add(configNode);
    }

    public void report(ReportCanvas canvas) {
        this.mDeviceDescriptor.report(canvas);
        for (UsbDescriptorsConfigNode node : this.mConfigNodes) {
            node.report(canvas);
        }
    }
}
