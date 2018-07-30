package com.android.server.wifi.nano;

import com.android.framework.protobuf.nano.CodedInputByteBufferNano;
import com.android.framework.protobuf.nano.CodedOutputByteBufferNano;
import com.android.framework.protobuf.nano.InternalNano;
import com.android.framework.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.framework.protobuf.nano.WireFormatNano;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import java.io.IOException;

public interface WifiMetricsProto {

    public static final class AlertReasonCount extends MessageNano {
        private static volatile AlertReasonCount[] _emptyArray;
        public int count;
        public int reason;

        public static AlertReasonCount[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new AlertReasonCount[0];
                    }
                }
            }
            return _emptyArray;
        }

        public AlertReasonCount() {
            clear();
        }

        public AlertReasonCount clear() {
            this.reason = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.reason != 0) {
                output.writeInt32(1, this.reason);
            }
            if (this.count != 0) {
                output.writeInt32(2, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.reason != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.reason);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.count);
            }
            return size;
        }

        public AlertReasonCount mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.reason = input.readInt32();
                        break;
                    case 16:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static AlertReasonCount parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (AlertReasonCount) MessageNano.mergeFrom(new AlertReasonCount(), data);
        }

        public static AlertReasonCount parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new AlertReasonCount().mergeFrom(input);
        }
    }

    public static final class ConnectToNetworkNotificationAndActionCount extends MessageNano {
        public static final int ACTION_CONNECT_TO_NETWORK = 2;
        public static final int ACTION_PICK_WIFI_NETWORK = 3;
        public static final int ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE = 4;
        public static final int ACTION_UNKNOWN = 0;
        public static final int ACTION_USER_DISMISSED_NOTIFICATION = 1;
        public static final int NOTIFICATION_CONNECTED_TO_NETWORK = 3;
        public static final int NOTIFICATION_CONNECTING_TO_NETWORK = 2;
        public static final int NOTIFICATION_FAILED_TO_CONNECT = 4;
        public static final int NOTIFICATION_RECOMMEND_NETWORK = 1;
        public static final int NOTIFICATION_UNKNOWN = 0;
        public static final int RECOMMENDER_OPEN = 1;
        public static final int RECOMMENDER_UNKNOWN = 0;
        private static volatile ConnectToNetworkNotificationAndActionCount[] _emptyArray;
        public int action;
        public int count;
        public int notification;
        public int recommender;

        public static ConnectToNetworkNotificationAndActionCount[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ConnectToNetworkNotificationAndActionCount[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ConnectToNetworkNotificationAndActionCount() {
            clear();
        }

        public ConnectToNetworkNotificationAndActionCount clear() {
            this.notification = 0;
            this.action = 0;
            this.recommender = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.notification != 0) {
                output.writeInt32(1, this.notification);
            }
            if (this.action != 0) {
                output.writeInt32(2, this.action);
            }
            if (this.recommender != 0) {
                output.writeInt32(3, this.recommender);
            }
            if (this.count != 0) {
                output.writeInt32(4, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.notification != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.notification);
            }
            if (this.action != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.action);
            }
            if (this.recommender != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.recommender);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(4, this.count);
            }
            return size;
        }

        public ConnectToNetworkNotificationAndActionCount mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.notification = value;
                                break;
                            default:
                                break;
                        }
                    case 16:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.action = value;
                                break;
                            default:
                                break;
                        }
                    case 24:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                                this.recommender = value;
                                break;
                            default:
                                break;
                        }
                    case 32:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static ConnectToNetworkNotificationAndActionCount parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (ConnectToNetworkNotificationAndActionCount) MessageNano.mergeFrom(new ConnectToNetworkNotificationAndActionCount(), data);
        }

        public static ConnectToNetworkNotificationAndActionCount parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new ConnectToNetworkNotificationAndActionCount().mergeFrom(input);
        }
    }

    public static final class ConnectionEvent extends MessageNano {
        public static final int HLF_DHCP = 2;
        public static final int HLF_NONE = 1;
        public static final int HLF_NO_INTERNET = 3;
        public static final int HLF_UNKNOWN = 0;
        public static final int HLF_UNWANTED = 4;
        public static final int ROAM_DBDC = 2;
        public static final int ROAM_ENTERPRISE = 3;
        public static final int ROAM_NONE = 1;
        public static final int ROAM_UNKNOWN = 0;
        public static final int ROAM_UNRELATED = 5;
        public static final int ROAM_USER_SELECTED = 4;
        private static volatile ConnectionEvent[] _emptyArray;
        public boolean automaticBugReportTaken;
        public int connectionResult;
        public int connectivityLevelFailureCode;
        public int durationTakenToConnectMillis;
        public int level2FailureCode;
        public int roamType;
        public RouterFingerPrint routerFingerprint;
        public int signalStrength;
        public long startTimeMillis;

        public static ConnectionEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ConnectionEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ConnectionEvent() {
            clear();
        }

        public ConnectionEvent clear() {
            this.startTimeMillis = 0;
            this.durationTakenToConnectMillis = 0;
            this.routerFingerprint = null;
            this.signalStrength = 0;
            this.roamType = 0;
            this.connectionResult = 0;
            this.level2FailureCode = 0;
            this.connectivityLevelFailureCode = 0;
            this.automaticBugReportTaken = false;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.startTimeMillis != 0) {
                output.writeInt64(1, this.startTimeMillis);
            }
            if (this.durationTakenToConnectMillis != 0) {
                output.writeInt32(2, this.durationTakenToConnectMillis);
            }
            if (this.routerFingerprint != null) {
                output.writeMessage(3, this.routerFingerprint);
            }
            if (this.signalStrength != 0) {
                output.writeInt32(4, this.signalStrength);
            }
            if (this.roamType != 0) {
                output.writeInt32(5, this.roamType);
            }
            if (this.connectionResult != 0) {
                output.writeInt32(6, this.connectionResult);
            }
            if (this.level2FailureCode != 0) {
                output.writeInt32(7, this.level2FailureCode);
            }
            if (this.connectivityLevelFailureCode != 0) {
                output.writeInt32(8, this.connectivityLevelFailureCode);
            }
            if (this.automaticBugReportTaken) {
                output.writeBool(9, this.automaticBugReportTaken);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.startTimeMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(1, this.startTimeMillis);
            }
            if (this.durationTakenToConnectMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.durationTakenToConnectMillis);
            }
            if (this.routerFingerprint != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(3, this.routerFingerprint);
            }
            if (this.signalStrength != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.signalStrength);
            }
            if (this.roamType != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.roamType);
            }
            if (this.connectionResult != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.connectionResult);
            }
            if (this.level2FailureCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(7, this.level2FailureCode);
            }
            if (this.connectivityLevelFailureCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.connectivityLevelFailureCode);
            }
            if (this.automaticBugReportTaken) {
                return size + CodedOutputByteBufferNano.computeBoolSize(9, this.automaticBugReportTaken);
            }
            return size;
        }

        public ConnectionEvent mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.startTimeMillis = input.readInt64();
                        break;
                    case 16:
                        this.durationTakenToConnectMillis = input.readInt32();
                        break;
                    case 26:
                        if (this.routerFingerprint == null) {
                            this.routerFingerprint = new RouterFingerPrint();
                        }
                        input.readMessage(this.routerFingerprint);
                        break;
                    case 32:
                        this.signalStrength = input.readInt32();
                        break;
                    case 40:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                this.roamType = value;
                                break;
                            default:
                                break;
                        }
                    case 48:
                        this.connectionResult = input.readInt32();
                        break;
                    case 56:
                        this.level2FailureCode = input.readInt32();
                        break;
                    case 64:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.connectivityLevelFailureCode = value;
                                break;
                            default:
                                break;
                        }
                    case 72:
                        this.automaticBugReportTaken = input.readBool();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static ConnectionEvent parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (ConnectionEvent) MessageNano.mergeFrom(new ConnectionEvent(), data);
        }

        public static ConnectionEvent parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new ConnectionEvent().mergeFrom(input);
        }
    }

    public static final class NumConnectableNetworksBucket extends MessageNano {
        private static volatile NumConnectableNetworksBucket[] _emptyArray;
        public int count;
        public int numConnectableNetworks;

        public static NumConnectableNetworksBucket[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new NumConnectableNetworksBucket[0];
                    }
                }
            }
            return _emptyArray;
        }

        public NumConnectableNetworksBucket() {
            clear();
        }

        public NumConnectableNetworksBucket clear() {
            this.numConnectableNetworks = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.numConnectableNetworks != 0) {
                output.writeInt32(1, this.numConnectableNetworks);
            }
            if (this.count != 0) {
                output.writeInt32(2, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.numConnectableNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.numConnectableNetworks);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.count);
            }
            return size;
        }

        public NumConnectableNetworksBucket mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.numConnectableNetworks = input.readInt32();
                        break;
                    case 16:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static NumConnectableNetworksBucket parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (NumConnectableNetworksBucket) MessageNano.mergeFrom(new NumConnectableNetworksBucket(), data);
        }

        public static NumConnectableNetworksBucket parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new NumConnectableNetworksBucket().mergeFrom(input);
        }
    }

    public static final class PnoScanMetrics extends MessageNano {
        private static volatile PnoScanMetrics[] _emptyArray;
        public int numPnoFoundNetworkEvents;
        public int numPnoScanAttempts;
        public int numPnoScanFailed;
        public int numPnoScanFailedOverOffload;
        public int numPnoScanStartedOverOffload;

        public static PnoScanMetrics[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new PnoScanMetrics[0];
                    }
                }
            }
            return _emptyArray;
        }

        public PnoScanMetrics() {
            clear();
        }

        public PnoScanMetrics clear() {
            this.numPnoScanAttempts = 0;
            this.numPnoScanFailed = 0;
            this.numPnoScanStartedOverOffload = 0;
            this.numPnoScanFailedOverOffload = 0;
            this.numPnoFoundNetworkEvents = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.numPnoScanAttempts != 0) {
                output.writeInt32(1, this.numPnoScanAttempts);
            }
            if (this.numPnoScanFailed != 0) {
                output.writeInt32(2, this.numPnoScanFailed);
            }
            if (this.numPnoScanStartedOverOffload != 0) {
                output.writeInt32(3, this.numPnoScanStartedOverOffload);
            }
            if (this.numPnoScanFailedOverOffload != 0) {
                output.writeInt32(4, this.numPnoScanFailedOverOffload);
            }
            if (this.numPnoFoundNetworkEvents != 0) {
                output.writeInt32(5, this.numPnoFoundNetworkEvents);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.numPnoScanAttempts != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.numPnoScanAttempts);
            }
            if (this.numPnoScanFailed != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.numPnoScanFailed);
            }
            if (this.numPnoScanStartedOverOffload != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.numPnoScanStartedOverOffload);
            }
            if (this.numPnoScanFailedOverOffload != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.numPnoScanFailedOverOffload);
            }
            if (this.numPnoFoundNetworkEvents != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(5, this.numPnoFoundNetworkEvents);
            }
            return size;
        }

        public PnoScanMetrics mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.numPnoScanAttempts = input.readInt32();
                        break;
                    case 16:
                        this.numPnoScanFailed = input.readInt32();
                        break;
                    case 24:
                        this.numPnoScanStartedOverOffload = input.readInt32();
                        break;
                    case 32:
                        this.numPnoScanFailedOverOffload = input.readInt32();
                        break;
                    case 40:
                        this.numPnoFoundNetworkEvents = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static PnoScanMetrics parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (PnoScanMetrics) MessageNano.mergeFrom(new PnoScanMetrics(), data);
        }

        public static PnoScanMetrics parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new PnoScanMetrics().mergeFrom(input);
        }
    }

    public static final class RouterFingerPrint extends MessageNano {
        public static final int AUTH_ENTERPRISE = 3;
        public static final int AUTH_OPEN = 1;
        public static final int AUTH_PERSONAL = 2;
        public static final int AUTH_UNKNOWN = 0;
        public static final int ROAM_TYPE_DBDC = 3;
        public static final int ROAM_TYPE_ENTERPRISE = 2;
        public static final int ROAM_TYPE_NONE = 1;
        public static final int ROAM_TYPE_UNKNOWN = 0;
        public static final int ROUTER_TECH_A = 1;
        public static final int ROUTER_TECH_AC = 5;
        public static final int ROUTER_TECH_B = 2;
        public static final int ROUTER_TECH_G = 3;
        public static final int ROUTER_TECH_N = 4;
        public static final int ROUTER_TECH_OTHER = 6;
        public static final int ROUTER_TECH_UNKNOWN = 0;
        private static volatile RouterFingerPrint[] _emptyArray;
        public int authentication;
        public int channelInfo;
        public int dtim;
        public boolean hidden;
        public boolean passpoint;
        public int roamType;
        public int routerTechnology;
        public boolean supportsIpv6;

        public static RouterFingerPrint[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new RouterFingerPrint[0];
                    }
                }
            }
            return _emptyArray;
        }

        public RouterFingerPrint() {
            clear();
        }

        public RouterFingerPrint clear() {
            this.roamType = 0;
            this.channelInfo = 0;
            this.dtim = 0;
            this.authentication = 0;
            this.hidden = false;
            this.routerTechnology = 0;
            this.supportsIpv6 = false;
            this.passpoint = false;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.roamType != 0) {
                output.writeInt32(1, this.roamType);
            }
            if (this.channelInfo != 0) {
                output.writeInt32(2, this.channelInfo);
            }
            if (this.dtim != 0) {
                output.writeInt32(3, this.dtim);
            }
            if (this.authentication != 0) {
                output.writeInt32(4, this.authentication);
            }
            if (this.hidden) {
                output.writeBool(5, this.hidden);
            }
            if (this.routerTechnology != 0) {
                output.writeInt32(6, this.routerTechnology);
            }
            if (this.supportsIpv6) {
                output.writeBool(7, this.supportsIpv6);
            }
            if (this.passpoint) {
                output.writeBool(8, this.passpoint);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.roamType != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.roamType);
            }
            if (this.channelInfo != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.channelInfo);
            }
            if (this.dtim != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.dtim);
            }
            if (this.authentication != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.authentication);
            }
            if (this.hidden) {
                size += CodedOutputByteBufferNano.computeBoolSize(5, this.hidden);
            }
            if (this.routerTechnology != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.routerTechnology);
            }
            if (this.supportsIpv6) {
                size += CodedOutputByteBufferNano.computeBoolSize(7, this.supportsIpv6);
            }
            if (this.passpoint) {
                return size + CodedOutputByteBufferNano.computeBoolSize(8, this.passpoint);
            }
            return size;
        }

        public RouterFingerPrint mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.roamType = value;
                                break;
                            default:
                                break;
                        }
                    case 16:
                        this.channelInfo = input.readInt32();
                        break;
                    case 24:
                        this.dtim = input.readInt32();
                        break;
                    case 32:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.authentication = value;
                                break;
                            default:
                                break;
                        }
                    case 40:
                        this.hidden = input.readBool();
                        break;
                    case 48:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                                this.routerTechnology = value;
                                break;
                            default:
                                break;
                        }
                    case 56:
                        this.supportsIpv6 = input.readBool();
                        break;
                    case 64:
                        this.passpoint = input.readBool();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static RouterFingerPrint parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (RouterFingerPrint) MessageNano.mergeFrom(new RouterFingerPrint(), data);
        }

        public static RouterFingerPrint parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new RouterFingerPrint().mergeFrom(input);
        }
    }

    public static final class RssiPollCount extends MessageNano {
        private static volatile RssiPollCount[] _emptyArray;
        public int count;
        public int rssi;

        public static RssiPollCount[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new RssiPollCount[0];
                    }
                }
            }
            return _emptyArray;
        }

        public RssiPollCount() {
            clear();
        }

        public RssiPollCount clear() {
            this.rssi = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.rssi != 0) {
                output.writeInt32(1, this.rssi);
            }
            if (this.count != 0) {
                output.writeInt32(2, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.rssi != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.rssi);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.count);
            }
            return size;
        }

        public RssiPollCount mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.rssi = input.readInt32();
                        break;
                    case 16:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static RssiPollCount parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (RssiPollCount) MessageNano.mergeFrom(new RssiPollCount(), data);
        }

        public static RssiPollCount parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new RssiPollCount().mergeFrom(input);
        }
    }

    public static final class SoftApDurationBucket extends MessageNano {
        private static volatile SoftApDurationBucket[] _emptyArray;
        public int bucketSizeSec;
        public int count;
        public int durationSec;

        public static SoftApDurationBucket[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new SoftApDurationBucket[0];
                    }
                }
            }
            return _emptyArray;
        }

        public SoftApDurationBucket() {
            clear();
        }

        public SoftApDurationBucket clear() {
            this.durationSec = 0;
            this.bucketSizeSec = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.durationSec != 0) {
                output.writeInt32(1, this.durationSec);
            }
            if (this.bucketSizeSec != 0) {
                output.writeInt32(2, this.bucketSizeSec);
            }
            if (this.count != 0) {
                output.writeInt32(3, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.durationSec != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.durationSec);
            }
            if (this.bucketSizeSec != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.bucketSizeSec);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(3, this.count);
            }
            return size;
        }

        public SoftApDurationBucket mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.durationSec = input.readInt32();
                        break;
                    case 16:
                        this.bucketSizeSec = input.readInt32();
                        break;
                    case 24:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static SoftApDurationBucket parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (SoftApDurationBucket) MessageNano.mergeFrom(new SoftApDurationBucket(), data);
        }

        public static SoftApDurationBucket parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new SoftApDurationBucket().mergeFrom(input);
        }
    }

    public static final class SoftApReturnCodeCount extends MessageNano {
        public static final int SOFT_AP_FAILED_GENERAL_ERROR = 2;
        public static final int SOFT_AP_FAILED_NO_CHANNEL = 3;
        public static final int SOFT_AP_RETURN_CODE_UNKNOWN = 0;
        public static final int SOFT_AP_STARTED_SUCCESSFULLY = 1;
        private static volatile SoftApReturnCodeCount[] _emptyArray;
        public int count;
        public int returnCode;
        public int startResult;

        public static SoftApReturnCodeCount[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new SoftApReturnCodeCount[0];
                    }
                }
            }
            return _emptyArray;
        }

        public SoftApReturnCodeCount() {
            clear();
        }

        public SoftApReturnCodeCount clear() {
            this.returnCode = 0;
            this.count = 0;
            this.startResult = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.returnCode != 0) {
                output.writeInt32(1, this.returnCode);
            }
            if (this.count != 0) {
                output.writeInt32(2, this.count);
            }
            if (this.startResult != 0) {
                output.writeInt32(3, this.startResult);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.returnCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.returnCode);
            }
            if (this.count != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.count);
            }
            if (this.startResult != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(3, this.startResult);
            }
            return size;
        }

        public SoftApReturnCodeCount mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.returnCode = input.readInt32();
                        break;
                    case 16:
                        this.count = input.readInt32();
                        break;
                    case 24:
                        int value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.startResult = value;
                                break;
                            default:
                                break;
                        }
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static SoftApReturnCodeCount parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (SoftApReturnCodeCount) MessageNano.mergeFrom(new SoftApReturnCodeCount(), data);
        }

        public static SoftApReturnCodeCount parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new SoftApReturnCodeCount().mergeFrom(input);
        }
    }

    public static final class StaEvent extends MessageNano {
        public static final int AUTH_FAILURE_EAP_FAILURE = 4;
        public static final int AUTH_FAILURE_NONE = 1;
        public static final int AUTH_FAILURE_TIMEOUT = 2;
        public static final int AUTH_FAILURE_UNKNOWN = 0;
        public static final int AUTH_FAILURE_WRONG_PSWD = 3;
        public static final int DISCONNECT_API = 1;
        public static final int DISCONNECT_GENERIC = 2;
        public static final int DISCONNECT_P2P_DISCONNECT_WIFI_REQUEST = 5;
        public static final int DISCONNECT_RESET_SIM_NETWORKS = 6;
        public static final int DISCONNECT_ROAM_WATCHDOG_TIMER = 4;
        public static final int DISCONNECT_UNKNOWN = 0;
        public static final int DISCONNECT_UNWANTED = 3;
        public static final int STATE_ASSOCIATED = 6;
        public static final int STATE_ASSOCIATING = 5;
        public static final int STATE_AUTHENTICATING = 4;
        public static final int STATE_COMPLETED = 9;
        public static final int STATE_DISCONNECTED = 0;
        public static final int STATE_DORMANT = 10;
        public static final int STATE_FOUR_WAY_HANDSHAKE = 7;
        public static final int STATE_GROUP_HANDSHAKE = 8;
        public static final int STATE_INACTIVE = 2;
        public static final int STATE_INTERFACE_DISABLED = 1;
        public static final int STATE_INVALID = 12;
        public static final int STATE_SCANNING = 3;
        public static final int STATE_UNINITIALIZED = 11;
        public static final int TYPE_ASSOCIATION_REJECTION_EVENT = 1;
        public static final int TYPE_AUTHENTICATION_FAILURE_EVENT = 2;
        public static final int TYPE_CMD_ASSOCIATED_BSSID = 6;
        public static final int TYPE_CMD_IP_CONFIGURATION_LOST = 8;
        public static final int TYPE_CMD_IP_CONFIGURATION_SUCCESSFUL = 7;
        public static final int TYPE_CMD_IP_REACHABILITY_LOST = 9;
        public static final int TYPE_CMD_START_CONNECT = 11;
        public static final int TYPE_CMD_START_ROAM = 12;
        public static final int TYPE_CMD_TARGET_BSSID = 10;
        public static final int TYPE_CONNECT_NETWORK = 13;
        public static final int TYPE_FRAMEWORK_DISCONNECT = 15;
        public static final int TYPE_NETWORK_AGENT_VALID_NETWORK = 14;
        public static final int TYPE_NETWORK_CONNECTION_EVENT = 3;
        public static final int TYPE_NETWORK_DISCONNECTION_EVENT = 4;
        public static final int TYPE_SUPPLICANT_STATE_CHANGE_EVENT = 5;
        public static final int TYPE_UNKNOWN = 0;
        private static volatile StaEvent[] _emptyArray;
        public boolean associationTimedOut;
        public int authFailureReason;
        public ConfigInfo configInfo;
        public int frameworkDisconnectReason;
        public int lastFreq;
        public int lastLinkSpeed;
        public int lastRssi;
        public boolean localGen;
        public int reason;
        public long startTimeMillis;
        public int status;
        public int supplicantStateChangesBitmask;
        public int type;

        public static final class ConfigInfo extends MessageNano {
            private static volatile ConfigInfo[] _emptyArray;
            public int allowedAuthAlgorithms;
            public int allowedGroupCiphers;
            public int allowedKeyManagement;
            public int allowedPairwiseCiphers;
            public int allowedProtocols;
            public boolean hasEverConnected;
            public boolean hiddenSsid;
            public boolean isEphemeral;
            public boolean isPasspoint;
            public int scanFreq;
            public int scanRssi;

            public static ConfigInfo[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new ConfigInfo[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public ConfigInfo() {
                clear();
            }

            public ConfigInfo clear() {
                this.allowedKeyManagement = 0;
                this.allowedProtocols = 0;
                this.allowedAuthAlgorithms = 0;
                this.allowedPairwiseCiphers = 0;
                this.allowedGroupCiphers = 0;
                this.hiddenSsid = false;
                this.isPasspoint = false;
                this.isEphemeral = false;
                this.hasEverConnected = false;
                this.scanRssi = -127;
                this.scanFreq = -1;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.allowedKeyManagement != 0) {
                    output.writeUInt32(1, this.allowedKeyManagement);
                }
                if (this.allowedProtocols != 0) {
                    output.writeUInt32(2, this.allowedProtocols);
                }
                if (this.allowedAuthAlgorithms != 0) {
                    output.writeUInt32(3, this.allowedAuthAlgorithms);
                }
                if (this.allowedPairwiseCiphers != 0) {
                    output.writeUInt32(4, this.allowedPairwiseCiphers);
                }
                if (this.allowedGroupCiphers != 0) {
                    output.writeUInt32(5, this.allowedGroupCiphers);
                }
                if (this.hiddenSsid) {
                    output.writeBool(6, this.hiddenSsid);
                }
                if (this.isPasspoint) {
                    output.writeBool(7, this.isPasspoint);
                }
                if (this.isEphemeral) {
                    output.writeBool(8, this.isEphemeral);
                }
                if (this.hasEverConnected) {
                    output.writeBool(9, this.hasEverConnected);
                }
                if (this.scanRssi != -127) {
                    output.writeInt32(10, this.scanRssi);
                }
                if (this.scanFreq != -1) {
                    output.writeInt32(11, this.scanFreq);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.allowedKeyManagement != 0) {
                    size += CodedOutputByteBufferNano.computeUInt32Size(1, this.allowedKeyManagement);
                }
                if (this.allowedProtocols != 0) {
                    size += CodedOutputByteBufferNano.computeUInt32Size(2, this.allowedProtocols);
                }
                if (this.allowedAuthAlgorithms != 0) {
                    size += CodedOutputByteBufferNano.computeUInt32Size(3, this.allowedAuthAlgorithms);
                }
                if (this.allowedPairwiseCiphers != 0) {
                    size += CodedOutputByteBufferNano.computeUInt32Size(4, this.allowedPairwiseCiphers);
                }
                if (this.allowedGroupCiphers != 0) {
                    size += CodedOutputByteBufferNano.computeUInt32Size(5, this.allowedGroupCiphers);
                }
                if (this.hiddenSsid) {
                    size += CodedOutputByteBufferNano.computeBoolSize(6, this.hiddenSsid);
                }
                if (this.isPasspoint) {
                    size += CodedOutputByteBufferNano.computeBoolSize(7, this.isPasspoint);
                }
                if (this.isEphemeral) {
                    size += CodedOutputByteBufferNano.computeBoolSize(8, this.isEphemeral);
                }
                if (this.hasEverConnected) {
                    size += CodedOutputByteBufferNano.computeBoolSize(9, this.hasEverConnected);
                }
                if (this.scanRssi != -127) {
                    size += CodedOutputByteBufferNano.computeInt32Size(10, this.scanRssi);
                }
                if (this.scanFreq != -1) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(11, this.scanFreq);
                }
                return size;
            }

            public ConfigInfo mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            this.allowedKeyManagement = input.readUInt32();
                            break;
                        case 16:
                            this.allowedProtocols = input.readUInt32();
                            break;
                        case 24:
                            this.allowedAuthAlgorithms = input.readUInt32();
                            break;
                        case 32:
                            this.allowedPairwiseCiphers = input.readUInt32();
                            break;
                        case 40:
                            this.allowedGroupCiphers = input.readUInt32();
                            break;
                        case 48:
                            this.hiddenSsid = input.readBool();
                            break;
                        case 56:
                            this.isPasspoint = input.readBool();
                            break;
                        case 64:
                            this.isEphemeral = input.readBool();
                            break;
                        case 72:
                            this.hasEverConnected = input.readBool();
                            break;
                        case 80:
                            this.scanRssi = input.readInt32();
                            break;
                        case 88:
                            this.scanFreq = input.readInt32();
                            break;
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static ConfigInfo parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (ConfigInfo) MessageNano.mergeFrom(new ConfigInfo(), data);
            }

            public static ConfigInfo parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new ConfigInfo().mergeFrom(input);
            }
        }

        public static StaEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new StaEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public StaEvent() {
            clear();
        }

        public StaEvent clear() {
            this.type = 0;
            this.reason = -1;
            this.status = -1;
            this.localGen = false;
            this.configInfo = null;
            this.lastRssi = -127;
            this.lastLinkSpeed = -1;
            this.lastFreq = -1;
            this.supplicantStateChangesBitmask = 0;
            this.startTimeMillis = 0;
            this.frameworkDisconnectReason = 0;
            this.associationTimedOut = false;
            this.authFailureReason = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.type != 0) {
                output.writeInt32(1, this.type);
            }
            if (this.reason != -1) {
                output.writeInt32(2, this.reason);
            }
            if (this.status != -1) {
                output.writeInt32(3, this.status);
            }
            if (this.localGen) {
                output.writeBool(4, this.localGen);
            }
            if (this.configInfo != null) {
                output.writeMessage(5, this.configInfo);
            }
            if (this.lastRssi != -127) {
                output.writeInt32(6, this.lastRssi);
            }
            if (this.lastLinkSpeed != -1) {
                output.writeInt32(7, this.lastLinkSpeed);
            }
            if (this.lastFreq != -1) {
                output.writeInt32(8, this.lastFreq);
            }
            if (this.supplicantStateChangesBitmask != 0) {
                output.writeUInt32(9, this.supplicantStateChangesBitmask);
            }
            if (this.startTimeMillis != 0) {
                output.writeInt64(10, this.startTimeMillis);
            }
            if (this.frameworkDisconnectReason != 0) {
                output.writeInt32(11, this.frameworkDisconnectReason);
            }
            if (this.associationTimedOut) {
                output.writeBool(12, this.associationTimedOut);
            }
            if (this.authFailureReason != 0) {
                output.writeInt32(13, this.authFailureReason);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.type != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.type);
            }
            if (this.reason != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.reason);
            }
            if (this.status != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.status);
            }
            if (this.localGen) {
                size += CodedOutputByteBufferNano.computeBoolSize(4, this.localGen);
            }
            if (this.configInfo != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(5, this.configInfo);
            }
            if (this.lastRssi != -127) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.lastRssi);
            }
            if (this.lastLinkSpeed != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(7, this.lastLinkSpeed);
            }
            if (this.lastFreq != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.lastFreq);
            }
            if (this.supplicantStateChangesBitmask != 0) {
                size += CodedOutputByteBufferNano.computeUInt32Size(9, this.supplicantStateChangesBitmask);
            }
            if (this.startTimeMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(10, this.startTimeMillis);
            }
            if (this.frameworkDisconnectReason != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(11, this.frameworkDisconnectReason);
            }
            if (this.associationTimedOut) {
                size += CodedOutputByteBufferNano.computeBoolSize(12, this.associationTimedOut);
            }
            if (this.authFailureReason != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(13, this.authFailureReason);
            }
            return size;
        }

        public StaEvent mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                                this.type = value;
                                break;
                            default:
                                break;
                        }
                    case 16:
                        this.reason = input.readInt32();
                        break;
                    case 24:
                        this.status = input.readInt32();
                        break;
                    case 32:
                        this.localGen = input.readBool();
                        break;
                    case 42:
                        if (this.configInfo == null) {
                            this.configInfo = new ConfigInfo();
                        }
                        input.readMessage(this.configInfo);
                        break;
                    case 48:
                        this.lastRssi = input.readInt32();
                        break;
                    case 56:
                        this.lastLinkSpeed = input.readInt32();
                        break;
                    case 64:
                        this.lastFreq = input.readInt32();
                        break;
                    case 72:
                        this.supplicantStateChangesBitmask = input.readUInt32();
                        break;
                    case 80:
                        this.startTimeMillis = input.readInt64();
                        break;
                    case 88:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                                this.frameworkDisconnectReason = value;
                                break;
                            default:
                                break;
                        }
                    case 96:
                        this.associationTimedOut = input.readBool();
                        break;
                    case 104:
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.authFailureReason = value;
                                break;
                            default:
                                break;
                        }
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static StaEvent parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (StaEvent) MessageNano.mergeFrom(new StaEvent(), data);
        }

        public static StaEvent parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new StaEvent().mergeFrom(input);
        }
    }

    public static final class WifiAwareLog extends MessageNano {
        public static final int ALREADY_ENABLED = 11;
        public static final int FOLLOWUP_TX_QUEUE_FULL = 12;
        public static final int INTERNAL_FAILURE = 2;
        public static final int INVALID_ARGS = 6;
        public static final int INVALID_NDP_ID = 8;
        public static final int INVALID_PEER_ID = 7;
        public static final int INVALID_SESSION_ID = 4;
        public static final int NAN_NOT_ALLOWED = 9;
        public static final int NO_OTA_ACK = 10;
        public static final int NO_RESOURCES_AVAILABLE = 5;
        public static final int PROTOCOL_FAILURE = 3;
        public static final int SUCCESS = 1;
        public static final int UNKNOWN = 0;
        public static final int UNKNOWN_HAL_STATUS = 14;
        public static final int UNSUPPORTED_CONCURRENCY_NAN_DISABLED = 13;
        private static volatile WifiAwareLog[] _emptyArray;
        public long availableTimeMs;
        public long enabledTimeMs;
        public HistogramBucket[] histogramAttachDurationMs;
        public NanStatusHistogramBucket[] histogramAttachSessionStatus;
        public HistogramBucket[] histogramAwareAvailableDurationMs;
        public HistogramBucket[] histogramAwareEnabledDurationMs;
        public HistogramBucket[] histogramNdpCreationTimeMs;
        public HistogramBucket[] histogramNdpSessionDataUsageMb;
        public HistogramBucket[] histogramNdpSessionDurationMs;
        public HistogramBucket[] histogramPublishSessionDurationMs;
        public NanStatusHistogramBucket[] histogramPublishStatus;
        public NanStatusHistogramBucket[] histogramRequestNdpOobStatus;
        public NanStatusHistogramBucket[] histogramRequestNdpStatus;
        public HistogramBucket[] histogramSubscribeSessionDurationMs;
        public NanStatusHistogramBucket[] histogramSubscribeStatus;
        public int maxConcurrentAttachSessionsInApp;
        public int maxConcurrentDiscoverySessionsInApp;
        public int maxConcurrentDiscoverySessionsInSystem;
        public int maxConcurrentNdiInApp;
        public int maxConcurrentNdiInSystem;
        public int maxConcurrentNdpInApp;
        public int maxConcurrentNdpInSystem;
        public int maxConcurrentNdpPerNdi;
        public int maxConcurrentPublishInApp;
        public int maxConcurrentPublishInSystem;
        public int maxConcurrentSecureNdpInApp;
        public int maxConcurrentSecureNdpInSystem;
        public int maxConcurrentSubscribeInApp;
        public int maxConcurrentSubscribeInSystem;
        public long ndpCreationTimeMsMax;
        public long ndpCreationTimeMsMin;
        public long ndpCreationTimeMsNumSamples;
        public long ndpCreationTimeMsSum;
        public long ndpCreationTimeMsSumOfSq;
        public int numApps;
        public int numAppsUsingIdentityCallback;
        public int numAppsWithDiscoverySessionFailureOutOfResources;

        public static final class HistogramBucket extends MessageNano {
            private static volatile HistogramBucket[] _emptyArray;
            public int count;
            public long end;
            public long start;

            public static HistogramBucket[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new HistogramBucket[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public HistogramBucket() {
                clear();
            }

            public HistogramBucket clear() {
                this.start = 0;
                this.end = 0;
                this.count = 0;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.start != 0) {
                    output.writeInt64(1, this.start);
                }
                if (this.end != 0) {
                    output.writeInt64(2, this.end);
                }
                if (this.count != 0) {
                    output.writeInt32(3, this.count);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.start != 0) {
                    size += CodedOutputByteBufferNano.computeInt64Size(1, this.start);
                }
                if (this.end != 0) {
                    size += CodedOutputByteBufferNano.computeInt64Size(2, this.end);
                }
                if (this.count != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(3, this.count);
                }
                return size;
            }

            public HistogramBucket mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            this.start = input.readInt64();
                            break;
                        case 16:
                            this.end = input.readInt64();
                            break;
                        case 24:
                            this.count = input.readInt32();
                            break;
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static HistogramBucket parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (HistogramBucket) MessageNano.mergeFrom(new HistogramBucket(), data);
            }

            public static HistogramBucket parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new HistogramBucket().mergeFrom(input);
            }
        }

        public static final class NanStatusHistogramBucket extends MessageNano {
            private static volatile NanStatusHistogramBucket[] _emptyArray;
            public int count;
            public int nanStatusType;

            public static NanStatusHistogramBucket[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new NanStatusHistogramBucket[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public NanStatusHistogramBucket() {
                clear();
            }

            public NanStatusHistogramBucket clear() {
                this.nanStatusType = 0;
                this.count = 0;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.nanStatusType != 0) {
                    output.writeInt32(1, this.nanStatusType);
                }
                if (this.count != 0) {
                    output.writeInt32(2, this.count);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.nanStatusType != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.nanStatusType);
                }
                if (this.count != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(2, this.count);
                }
                return size;
            }

            public NanStatusHistogramBucket mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                case 9:
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                    this.nanStatusType = value;
                                    break;
                                default:
                                    break;
                            }
                        case 16:
                            this.count = input.readInt32();
                            break;
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static NanStatusHistogramBucket parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (NanStatusHistogramBucket) MessageNano.mergeFrom(new NanStatusHistogramBucket(), data);
            }

            public static NanStatusHistogramBucket parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new NanStatusHistogramBucket().mergeFrom(input);
            }
        }

        public static WifiAwareLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new WifiAwareLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public WifiAwareLog() {
            clear();
        }

        public WifiAwareLog clear() {
            this.numApps = 0;
            this.numAppsUsingIdentityCallback = 0;
            this.maxConcurrentAttachSessionsInApp = 0;
            this.histogramAttachSessionStatus = NanStatusHistogramBucket.emptyArray();
            this.maxConcurrentPublishInApp = 0;
            this.maxConcurrentSubscribeInApp = 0;
            this.maxConcurrentDiscoverySessionsInApp = 0;
            this.maxConcurrentPublishInSystem = 0;
            this.maxConcurrentSubscribeInSystem = 0;
            this.maxConcurrentDiscoverySessionsInSystem = 0;
            this.histogramPublishStatus = NanStatusHistogramBucket.emptyArray();
            this.histogramSubscribeStatus = NanStatusHistogramBucket.emptyArray();
            this.numAppsWithDiscoverySessionFailureOutOfResources = 0;
            this.histogramRequestNdpStatus = NanStatusHistogramBucket.emptyArray();
            this.histogramRequestNdpOobStatus = NanStatusHistogramBucket.emptyArray();
            this.maxConcurrentNdiInApp = 0;
            this.maxConcurrentNdiInSystem = 0;
            this.maxConcurrentNdpInApp = 0;
            this.maxConcurrentNdpInSystem = 0;
            this.maxConcurrentSecureNdpInApp = 0;
            this.maxConcurrentSecureNdpInSystem = 0;
            this.maxConcurrentNdpPerNdi = 0;
            this.histogramAwareAvailableDurationMs = HistogramBucket.emptyArray();
            this.histogramAwareEnabledDurationMs = HistogramBucket.emptyArray();
            this.histogramAttachDurationMs = HistogramBucket.emptyArray();
            this.histogramPublishSessionDurationMs = HistogramBucket.emptyArray();
            this.histogramSubscribeSessionDurationMs = HistogramBucket.emptyArray();
            this.histogramNdpSessionDurationMs = HistogramBucket.emptyArray();
            this.histogramNdpSessionDataUsageMb = HistogramBucket.emptyArray();
            this.histogramNdpCreationTimeMs = HistogramBucket.emptyArray();
            this.ndpCreationTimeMsMin = 0;
            this.ndpCreationTimeMsMax = 0;
            this.ndpCreationTimeMsSum = 0;
            this.ndpCreationTimeMsSumOfSq = 0;
            this.ndpCreationTimeMsNumSamples = 0;
            this.availableTimeMs = 0;
            this.enabledTimeMs = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.numApps != 0) {
                output.writeInt32(1, this.numApps);
            }
            if (this.numAppsUsingIdentityCallback != 0) {
                output.writeInt32(2, this.numAppsUsingIdentityCallback);
            }
            if (this.maxConcurrentAttachSessionsInApp != 0) {
                output.writeInt32(3, this.maxConcurrentAttachSessionsInApp);
            }
            if (this.histogramAttachSessionStatus != null && this.histogramAttachSessionStatus.length > 0) {
                for (NanStatusHistogramBucket element : this.histogramAttachSessionStatus) {
                    if (element != null) {
                        output.writeMessage(4, element);
                    }
                }
            }
            if (this.maxConcurrentPublishInApp != 0) {
                output.writeInt32(5, this.maxConcurrentPublishInApp);
            }
            if (this.maxConcurrentSubscribeInApp != 0) {
                output.writeInt32(6, this.maxConcurrentSubscribeInApp);
            }
            if (this.maxConcurrentDiscoverySessionsInApp != 0) {
                output.writeInt32(7, this.maxConcurrentDiscoverySessionsInApp);
            }
            if (this.maxConcurrentPublishInSystem != 0) {
                output.writeInt32(8, this.maxConcurrentPublishInSystem);
            }
            if (this.maxConcurrentSubscribeInSystem != 0) {
                output.writeInt32(9, this.maxConcurrentSubscribeInSystem);
            }
            if (this.maxConcurrentDiscoverySessionsInSystem != 0) {
                output.writeInt32(10, this.maxConcurrentDiscoverySessionsInSystem);
            }
            if (this.histogramPublishStatus != null && this.histogramPublishStatus.length > 0) {
                for (NanStatusHistogramBucket element2 : this.histogramPublishStatus) {
                    if (element2 != null) {
                        output.writeMessage(11, element2);
                    }
                }
            }
            if (this.histogramSubscribeStatus != null && this.histogramSubscribeStatus.length > 0) {
                for (NanStatusHistogramBucket element22 : this.histogramSubscribeStatus) {
                    if (element22 != null) {
                        output.writeMessage(12, element22);
                    }
                }
            }
            if (this.numAppsWithDiscoverySessionFailureOutOfResources != 0) {
                output.writeInt32(13, this.numAppsWithDiscoverySessionFailureOutOfResources);
            }
            if (this.histogramRequestNdpStatus != null && this.histogramRequestNdpStatus.length > 0) {
                for (NanStatusHistogramBucket element222 : this.histogramRequestNdpStatus) {
                    if (element222 != null) {
                        output.writeMessage(14, element222);
                    }
                }
            }
            if (this.histogramRequestNdpOobStatus != null && this.histogramRequestNdpOobStatus.length > 0) {
                for (NanStatusHistogramBucket element2222 : this.histogramRequestNdpOobStatus) {
                    if (element2222 != null) {
                        output.writeMessage(15, element2222);
                    }
                }
            }
            if (this.maxConcurrentNdiInApp != 0) {
                output.writeInt32(19, this.maxConcurrentNdiInApp);
            }
            if (this.maxConcurrentNdiInSystem != 0) {
                output.writeInt32(20, this.maxConcurrentNdiInSystem);
            }
            if (this.maxConcurrentNdpInApp != 0) {
                output.writeInt32(21, this.maxConcurrentNdpInApp);
            }
            if (this.maxConcurrentNdpInSystem != 0) {
                output.writeInt32(22, this.maxConcurrentNdpInSystem);
            }
            if (this.maxConcurrentSecureNdpInApp != 0) {
                output.writeInt32(23, this.maxConcurrentSecureNdpInApp);
            }
            if (this.maxConcurrentSecureNdpInSystem != 0) {
                output.writeInt32(24, this.maxConcurrentSecureNdpInSystem);
            }
            if (this.maxConcurrentNdpPerNdi != 0) {
                output.writeInt32(25, this.maxConcurrentNdpPerNdi);
            }
            if (this.histogramAwareAvailableDurationMs != null && this.histogramAwareAvailableDurationMs.length > 0) {
                for (HistogramBucket element3 : this.histogramAwareAvailableDurationMs) {
                    if (element3 != null) {
                        output.writeMessage(26, element3);
                    }
                }
            }
            if (this.histogramAwareEnabledDurationMs != null && this.histogramAwareEnabledDurationMs.length > 0) {
                for (HistogramBucket element32 : this.histogramAwareEnabledDurationMs) {
                    if (element32 != null) {
                        output.writeMessage(27, element32);
                    }
                }
            }
            if (this.histogramAttachDurationMs != null && this.histogramAttachDurationMs.length > 0) {
                for (HistogramBucket element322 : this.histogramAttachDurationMs) {
                    if (element322 != null) {
                        output.writeMessage(28, element322);
                    }
                }
            }
            if (this.histogramPublishSessionDurationMs != null && this.histogramPublishSessionDurationMs.length > 0) {
                for (HistogramBucket element3222 : this.histogramPublishSessionDurationMs) {
                    if (element3222 != null) {
                        output.writeMessage(29, element3222);
                    }
                }
            }
            if (this.histogramSubscribeSessionDurationMs != null && this.histogramSubscribeSessionDurationMs.length > 0) {
                for (HistogramBucket element32222 : this.histogramSubscribeSessionDurationMs) {
                    if (element32222 != null) {
                        output.writeMessage(30, element32222);
                    }
                }
            }
            if (this.histogramNdpSessionDurationMs != null && this.histogramNdpSessionDurationMs.length > 0) {
                for (HistogramBucket element322222 : this.histogramNdpSessionDurationMs) {
                    if (element322222 != null) {
                        output.writeMessage(31, element322222);
                    }
                }
            }
            if (this.histogramNdpSessionDataUsageMb != null && this.histogramNdpSessionDataUsageMb.length > 0) {
                for (HistogramBucket element3222222 : this.histogramNdpSessionDataUsageMb) {
                    if (element3222222 != null) {
                        output.writeMessage(32, element3222222);
                    }
                }
            }
            if (this.histogramNdpCreationTimeMs != null && this.histogramNdpCreationTimeMs.length > 0) {
                for (HistogramBucket element32222222 : this.histogramNdpCreationTimeMs) {
                    if (element32222222 != null) {
                        output.writeMessage(33, element32222222);
                    }
                }
            }
            if (this.ndpCreationTimeMsMin != 0) {
                output.writeInt64(34, this.ndpCreationTimeMsMin);
            }
            if (this.ndpCreationTimeMsMax != 0) {
                output.writeInt64(35, this.ndpCreationTimeMsMax);
            }
            if (this.ndpCreationTimeMsSum != 0) {
                output.writeInt64(36, this.ndpCreationTimeMsSum);
            }
            if (this.ndpCreationTimeMsSumOfSq != 0) {
                output.writeInt64(37, this.ndpCreationTimeMsSumOfSq);
            }
            if (this.ndpCreationTimeMsNumSamples != 0) {
                output.writeInt64(38, this.ndpCreationTimeMsNumSamples);
            }
            if (this.availableTimeMs != 0) {
                output.writeInt64(39, this.availableTimeMs);
            }
            if (this.enabledTimeMs != 0) {
                output.writeInt64(40, this.enabledTimeMs);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.numApps != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.numApps);
            }
            if (this.numAppsUsingIdentityCallback != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.numAppsUsingIdentityCallback);
            }
            if (this.maxConcurrentAttachSessionsInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.maxConcurrentAttachSessionsInApp);
            }
            if (this.histogramAttachSessionStatus != null && this.histogramAttachSessionStatus.length > 0) {
                for (NanStatusHistogramBucket element : this.histogramAttachSessionStatus) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(4, element);
                    }
                }
            }
            if (this.maxConcurrentPublishInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.maxConcurrentPublishInApp);
            }
            if (this.maxConcurrentSubscribeInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.maxConcurrentSubscribeInApp);
            }
            if (this.maxConcurrentDiscoverySessionsInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(7, this.maxConcurrentDiscoverySessionsInApp);
            }
            if (this.maxConcurrentPublishInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.maxConcurrentPublishInSystem);
            }
            if (this.maxConcurrentSubscribeInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(9, this.maxConcurrentSubscribeInSystem);
            }
            if (this.maxConcurrentDiscoverySessionsInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(10, this.maxConcurrentDiscoverySessionsInSystem);
            }
            if (this.histogramPublishStatus != null && this.histogramPublishStatus.length > 0) {
                for (NanStatusHistogramBucket element2 : this.histogramPublishStatus) {
                    if (element2 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(11, element2);
                    }
                }
            }
            if (this.histogramSubscribeStatus != null && this.histogramSubscribeStatus.length > 0) {
                for (NanStatusHistogramBucket element22 : this.histogramSubscribeStatus) {
                    if (element22 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(12, element22);
                    }
                }
            }
            if (this.numAppsWithDiscoverySessionFailureOutOfResources != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(13, this.numAppsWithDiscoverySessionFailureOutOfResources);
            }
            if (this.histogramRequestNdpStatus != null && this.histogramRequestNdpStatus.length > 0) {
                for (NanStatusHistogramBucket element222 : this.histogramRequestNdpStatus) {
                    if (element222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(14, element222);
                    }
                }
            }
            if (this.histogramRequestNdpOobStatus != null && this.histogramRequestNdpOobStatus.length > 0) {
                for (NanStatusHistogramBucket element2222 : this.histogramRequestNdpOobStatus) {
                    if (element2222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(15, element2222);
                    }
                }
            }
            if (this.maxConcurrentNdiInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(19, this.maxConcurrentNdiInApp);
            }
            if (this.maxConcurrentNdiInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(20, this.maxConcurrentNdiInSystem);
            }
            if (this.maxConcurrentNdpInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(21, this.maxConcurrentNdpInApp);
            }
            if (this.maxConcurrentNdpInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(22, this.maxConcurrentNdpInSystem);
            }
            if (this.maxConcurrentSecureNdpInApp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(23, this.maxConcurrentSecureNdpInApp);
            }
            if (this.maxConcurrentSecureNdpInSystem != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(24, this.maxConcurrentSecureNdpInSystem);
            }
            if (this.maxConcurrentNdpPerNdi != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(25, this.maxConcurrentNdpPerNdi);
            }
            if (this.histogramAwareAvailableDurationMs != null && this.histogramAwareAvailableDurationMs.length > 0) {
                for (HistogramBucket element3 : this.histogramAwareAvailableDurationMs) {
                    if (element3 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(26, element3);
                    }
                }
            }
            if (this.histogramAwareEnabledDurationMs != null && this.histogramAwareEnabledDurationMs.length > 0) {
                for (HistogramBucket element32 : this.histogramAwareEnabledDurationMs) {
                    if (element32 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(27, element32);
                    }
                }
            }
            if (this.histogramAttachDurationMs != null && this.histogramAttachDurationMs.length > 0) {
                for (HistogramBucket element322 : this.histogramAttachDurationMs) {
                    if (element322 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(28, element322);
                    }
                }
            }
            if (this.histogramPublishSessionDurationMs != null && this.histogramPublishSessionDurationMs.length > 0) {
                for (HistogramBucket element3222 : this.histogramPublishSessionDurationMs) {
                    if (element3222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(29, element3222);
                    }
                }
            }
            if (this.histogramSubscribeSessionDurationMs != null && this.histogramSubscribeSessionDurationMs.length > 0) {
                for (HistogramBucket element32222 : this.histogramSubscribeSessionDurationMs) {
                    if (element32222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(30, element32222);
                    }
                }
            }
            if (this.histogramNdpSessionDurationMs != null && this.histogramNdpSessionDurationMs.length > 0) {
                for (HistogramBucket element322222 : this.histogramNdpSessionDurationMs) {
                    if (element322222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(31, element322222);
                    }
                }
            }
            if (this.histogramNdpSessionDataUsageMb != null && this.histogramNdpSessionDataUsageMb.length > 0) {
                for (HistogramBucket element3222222 : this.histogramNdpSessionDataUsageMb) {
                    if (element3222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(32, element3222222);
                    }
                }
            }
            if (this.histogramNdpCreationTimeMs != null && this.histogramNdpCreationTimeMs.length > 0) {
                for (HistogramBucket element32222222 : this.histogramNdpCreationTimeMs) {
                    if (element32222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(33, element32222222);
                    }
                }
            }
            if (this.ndpCreationTimeMsMin != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(34, this.ndpCreationTimeMsMin);
            }
            if (this.ndpCreationTimeMsMax != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(35, this.ndpCreationTimeMsMax);
            }
            if (this.ndpCreationTimeMsSum != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(36, this.ndpCreationTimeMsSum);
            }
            if (this.ndpCreationTimeMsSumOfSq != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(37, this.ndpCreationTimeMsSumOfSq);
            }
            if (this.ndpCreationTimeMsNumSamples != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(38, this.ndpCreationTimeMsNumSamples);
            }
            if (this.availableTimeMs != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(39, this.availableTimeMs);
            }
            if (this.enabledTimeMs != 0) {
                return size + CodedOutputByteBufferNano.computeInt64Size(40, this.enabledTimeMs);
            }
            return size;
        }

        public WifiAwareLog mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int arrayLength;
                int i;
                NanStatusHistogramBucket[] newArray;
                HistogramBucket[] newArray2;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.numApps = input.readInt32();
                        break;
                    case 16:
                        this.numAppsUsingIdentityCallback = input.readInt32();
                        break;
                    case 24:
                        this.maxConcurrentAttachSessionsInApp = input.readInt32();
                        break;
                    case 34:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 34);
                        i = this.histogramAttachSessionStatus == null ? 0 : this.histogramAttachSessionStatus.length;
                        newArray = new NanStatusHistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramAttachSessionStatus, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new NanStatusHistogramBucket();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new NanStatusHistogramBucket();
                        input.readMessage(newArray[i]);
                        this.histogramAttachSessionStatus = newArray;
                        break;
                    case 40:
                        this.maxConcurrentPublishInApp = input.readInt32();
                        break;
                    case 48:
                        this.maxConcurrentSubscribeInApp = input.readInt32();
                        break;
                    case 56:
                        this.maxConcurrentDiscoverySessionsInApp = input.readInt32();
                        break;
                    case 64:
                        this.maxConcurrentPublishInSystem = input.readInt32();
                        break;
                    case 72:
                        this.maxConcurrentSubscribeInSystem = input.readInt32();
                        break;
                    case 80:
                        this.maxConcurrentDiscoverySessionsInSystem = input.readInt32();
                        break;
                    case 90:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 90);
                        i = this.histogramPublishStatus == null ? 0 : this.histogramPublishStatus.length;
                        newArray = new NanStatusHistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramPublishStatus, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new NanStatusHistogramBucket();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new NanStatusHistogramBucket();
                        input.readMessage(newArray[i]);
                        this.histogramPublishStatus = newArray;
                        break;
                    case 98:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 98);
                        i = this.histogramSubscribeStatus == null ? 0 : this.histogramSubscribeStatus.length;
                        newArray = new NanStatusHistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramSubscribeStatus, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new NanStatusHistogramBucket();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new NanStatusHistogramBucket();
                        input.readMessage(newArray[i]);
                        this.histogramSubscribeStatus = newArray;
                        break;
                    case 104:
                        this.numAppsWithDiscoverySessionFailureOutOfResources = input.readInt32();
                        break;
                    case 114:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 114);
                        i = this.histogramRequestNdpStatus == null ? 0 : this.histogramRequestNdpStatus.length;
                        newArray = new NanStatusHistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramRequestNdpStatus, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new NanStatusHistogramBucket();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new NanStatusHistogramBucket();
                        input.readMessage(newArray[i]);
                        this.histogramRequestNdpStatus = newArray;
                        break;
                    case 122:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 122);
                        i = this.histogramRequestNdpOobStatus == null ? 0 : this.histogramRequestNdpOobStatus.length;
                        newArray = new NanStatusHistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramRequestNdpOobStatus, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new NanStatusHistogramBucket();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new NanStatusHistogramBucket();
                        input.readMessage(newArray[i]);
                        this.histogramRequestNdpOobStatus = newArray;
                        break;
                    case 152:
                        this.maxConcurrentNdiInApp = input.readInt32();
                        break;
                    case 160:
                        this.maxConcurrentNdiInSystem = input.readInt32();
                        break;
                    case 168:
                        this.maxConcurrentNdpInApp = input.readInt32();
                        break;
                    case 176:
                        this.maxConcurrentNdpInSystem = input.readInt32();
                        break;
                    case 184:
                        this.maxConcurrentSecureNdpInApp = input.readInt32();
                        break;
                    case 192:
                        this.maxConcurrentSecureNdpInSystem = input.readInt32();
                        break;
                    case 200:
                        this.maxConcurrentNdpPerNdi = input.readInt32();
                        break;
                    case 210:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 210);
                        i = this.histogramAwareAvailableDurationMs == null ? 0 : this.histogramAwareAvailableDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramAwareAvailableDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramAwareAvailableDurationMs = newArray2;
                        break;
                    case 218:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 218);
                        i = this.histogramAwareEnabledDurationMs == null ? 0 : this.histogramAwareEnabledDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramAwareEnabledDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramAwareEnabledDurationMs = newArray2;
                        break;
                    case 226:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 226);
                        i = this.histogramAttachDurationMs == null ? 0 : this.histogramAttachDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramAttachDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramAttachDurationMs = newArray2;
                        break;
                    case 234:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 234);
                        i = this.histogramPublishSessionDurationMs == null ? 0 : this.histogramPublishSessionDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramPublishSessionDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramPublishSessionDurationMs = newArray2;
                        break;
                    case 242:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 242);
                        i = this.histogramSubscribeSessionDurationMs == null ? 0 : this.histogramSubscribeSessionDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramSubscribeSessionDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramSubscribeSessionDurationMs = newArray2;
                        break;
                    case 250:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 250);
                        i = this.histogramNdpSessionDurationMs == null ? 0 : this.histogramNdpSessionDurationMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramNdpSessionDurationMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramNdpSessionDurationMs = newArray2;
                        break;
                    case 258:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 258);
                        i = this.histogramNdpSessionDataUsageMb == null ? 0 : this.histogramNdpSessionDataUsageMb.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramNdpSessionDataUsageMb, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramNdpSessionDataUsageMb = newArray2;
                        break;
                    case 266:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 266);
                        i = this.histogramNdpCreationTimeMs == null ? 0 : this.histogramNdpCreationTimeMs.length;
                        newArray2 = new HistogramBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histogramNdpCreationTimeMs, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new HistogramBucket();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new HistogramBucket();
                        input.readMessage(newArray2[i]);
                        this.histogramNdpCreationTimeMs = newArray2;
                        break;
                    case 272:
                        this.ndpCreationTimeMsMin = input.readInt64();
                        break;
                    case 280:
                        this.ndpCreationTimeMsMax = input.readInt64();
                        break;
                    case 288:
                        this.ndpCreationTimeMsSum = input.readInt64();
                        break;
                    case 296:
                        this.ndpCreationTimeMsSumOfSq = input.readInt64();
                        break;
                    case 304:
                        this.ndpCreationTimeMsNumSamples = input.readInt64();
                        break;
                    case 312:
                        this.availableTimeMs = input.readInt64();
                        break;
                    case 320:
                        this.enabledTimeMs = input.readInt64();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static WifiAwareLog parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (WifiAwareLog) MessageNano.mergeFrom(new WifiAwareLog(), data);
        }

        public static WifiAwareLog parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new WifiAwareLog().mergeFrom(input);
        }
    }

    public static final class WifiLog extends MessageNano {
        public static final int FAILURE_WIFI_DISABLED = 4;
        public static final int SCAN_FAILURE_INTERRUPTED = 2;
        public static final int SCAN_FAILURE_INVALID_CONFIGURATION = 3;
        public static final int SCAN_SUCCESS = 1;
        public static final int SCAN_UNKNOWN = 0;
        public static final int WIFI_ASSOCIATED = 3;
        public static final int WIFI_DISABLED = 1;
        public static final int WIFI_DISCONNECTED = 2;
        public static final int WIFI_UNKNOWN = 0;
        private static volatile WifiLog[] _emptyArray;
        public AlertReasonCount[] alertReasonCount;
        public NumConnectableNetworksBucket[] availableOpenBssidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableOpenOrSavedBssidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableOpenOrSavedSsidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableOpenSsidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableSavedBssidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableSavedPasspointProviderBssidsInScanHistogram;
        public NumConnectableNetworksBucket[] availableSavedPasspointProviderProfilesInScanHistogram;
        public NumConnectableNetworksBucket[] availableSavedSsidsInScanHistogram;
        public WifiSystemStateEntry[] backgroundScanRequestState;
        public ScanReturnEntry[] backgroundScanReturnEntries;
        public ConnectToNetworkNotificationAndActionCount[] connectToNetworkNotificationActionCount;
        public ConnectToNetworkNotificationAndActionCount[] connectToNetworkNotificationCount;
        public ConnectionEvent[] connectionEvent;
        public int fullBandAllSingleScanListenerResults;
        public boolean isLocationEnabled;
        public boolean isScanningAlwaysEnabled;
        public boolean isWifiNetworksAvailableNotificationOn;
        public int numBackgroundScans;
        public int numConnectivityWatchdogBackgroundBad;
        public int numConnectivityWatchdogBackgroundGood;
        public int numConnectivityWatchdogPnoBad;
        public int numConnectivityWatchdogPnoGood;
        public int numEmptyScanResults;
        public int numEnterpriseNetworkScanResults;
        public int numEnterpriseNetworks;
        public int numHalCrashes;
        public int numHiddenNetworkScanResults;
        public int numHiddenNetworks;
        public int numHotspot2R1NetworkScanResults;
        public int numHotspot2R2NetworkScanResults;
        public int numLastResortWatchdogAvailableNetworksTotal;
        public int numLastResortWatchdogBadAssociationNetworksTotal;
        public int numLastResortWatchdogBadAuthenticationNetworksTotal;
        public int numLastResortWatchdogBadDhcpNetworksTotal;
        public int numLastResortWatchdogBadOtherNetworksTotal;
        public int numLastResortWatchdogSuccesses;
        public int numLastResortWatchdogTriggers;
        public int numLastResortWatchdogTriggersWithBadAssociation;
        public int numLastResortWatchdogTriggersWithBadAuthentication;
        public int numLastResortWatchdogTriggersWithBadDhcp;
        public int numLastResortWatchdogTriggersWithBadOther;
        public int numNetworksAddedByApps;
        public int numNetworksAddedByUser;
        public int numNonEmptyScanResults;
        public int numOneshotScans;
        public int numOpenNetworkConnectMessageFailedToSend;
        public int numOpenNetworkRecommendationUpdates;
        public int numOpenNetworkScanResults;
        public int numOpenNetworks;
        public int numPasspointNetworks;
        public int numPasspointProviderInstallSuccess;
        public int numPasspointProviderInstallation;
        public int numPasspointProviderUninstallSuccess;
        public int numPasspointProviderUninstallation;
        public int numPasspointProviders;
        public int numPasspointProvidersSuccessfullyConnected;
        public int numPersonalNetworkScanResults;
        public int numPersonalNetworks;
        public int numSavedNetworks;
        public int numScans;
        public int numTotalScanResults;
        public int numWifiOnFailureDueToHal;
        public int numWifiOnFailureDueToWificond;
        public int numWifiToggledViaAirplane;
        public int numWifiToggledViaSettings;
        public int numWificondCrashes;
        public int openNetworkRecommenderBlacklistSize;
        public int partialAllSingleScanListenerResults;
        public PnoScanMetrics pnoScanMetrics;
        public int recordDurationSec;
        public RssiPollCount[] rssiPollDeltaCount;
        public RssiPollCount[] rssiPollRssiCount;
        public ScanReturnEntry[] scanReturnEntries;
        public SoftApDurationBucket[] softApDuration;
        public SoftApReturnCodeCount[] softApReturnCode;
        public StaEvent[] staEventList;
        public NumConnectableNetworksBucket[] totalBssidsInScanHistogram;
        public NumConnectableNetworksBucket[] totalSsidsInScanHistogram;
        public WifiAwareLog wifiAwareLog;
        public WifiScoreCount[] wifiScoreCount;
        public WifiSystemStateEntry[] wifiSystemStateEntries;

        public static final class ScanReturnEntry extends MessageNano {
            private static volatile ScanReturnEntry[] _emptyArray;
            public int scanResultsCount;
            public int scanReturnCode;

            public static ScanReturnEntry[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new ScanReturnEntry[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public ScanReturnEntry() {
                clear();
            }

            public ScanReturnEntry clear() {
                this.scanReturnCode = 0;
                this.scanResultsCount = 0;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.scanReturnCode != 0) {
                    output.writeInt32(1, this.scanReturnCode);
                }
                if (this.scanResultsCount != 0) {
                    output.writeInt32(2, this.scanResultsCount);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.scanReturnCode != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.scanReturnCode);
                }
                if (this.scanResultsCount != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(2, this.scanResultsCount);
                }
                return size;
            }

            public ScanReturnEntry mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                    this.scanReturnCode = value;
                                    break;
                                default:
                                    break;
                            }
                        case 16:
                            this.scanResultsCount = input.readInt32();
                            break;
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static ScanReturnEntry parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (ScanReturnEntry) MessageNano.mergeFrom(new ScanReturnEntry(), data);
            }

            public static ScanReturnEntry parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new ScanReturnEntry().mergeFrom(input);
            }
        }

        public static final class WifiSystemStateEntry extends MessageNano {
            private static volatile WifiSystemStateEntry[] _emptyArray;
            public boolean isScreenOn;
            public int wifiState;
            public int wifiStateCount;

            public static WifiSystemStateEntry[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new WifiSystemStateEntry[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public WifiSystemStateEntry() {
                clear();
            }

            public WifiSystemStateEntry clear() {
                this.wifiState = 0;
                this.wifiStateCount = 0;
                this.isScreenOn = false;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.wifiState != 0) {
                    output.writeInt32(1, this.wifiState);
                }
                if (this.wifiStateCount != 0) {
                    output.writeInt32(2, this.wifiStateCount);
                }
                if (this.isScreenOn) {
                    output.writeBool(3, this.isScreenOn);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.wifiState != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.wifiState);
                }
                if (this.wifiStateCount != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.wifiStateCount);
                }
                if (this.isScreenOn) {
                    return size + CodedOutputByteBufferNano.computeBoolSize(3, this.isScreenOn);
                }
                return size;
            }

            public WifiSystemStateEntry mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.wifiState = value;
                                    break;
                                default:
                                    break;
                            }
                        case 16:
                            this.wifiStateCount = input.readInt32();
                            break;
                        case 24:
                            this.isScreenOn = input.readBool();
                            break;
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static WifiSystemStateEntry parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (WifiSystemStateEntry) MessageNano.mergeFrom(new WifiSystemStateEntry(), data);
            }

            public static WifiSystemStateEntry parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new WifiSystemStateEntry().mergeFrom(input);
            }
        }

        public static WifiLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new WifiLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public WifiLog() {
            clear();
        }

        public WifiLog clear() {
            this.connectionEvent = ConnectionEvent.emptyArray();
            this.numSavedNetworks = 0;
            this.numOpenNetworks = 0;
            this.numPersonalNetworks = 0;
            this.numEnterpriseNetworks = 0;
            this.isLocationEnabled = false;
            this.isScanningAlwaysEnabled = false;
            this.numWifiToggledViaSettings = 0;
            this.numWifiToggledViaAirplane = 0;
            this.numNetworksAddedByUser = 0;
            this.numNetworksAddedByApps = 0;
            this.numEmptyScanResults = 0;
            this.numNonEmptyScanResults = 0;
            this.numOneshotScans = 0;
            this.numBackgroundScans = 0;
            this.scanReturnEntries = ScanReturnEntry.emptyArray();
            this.wifiSystemStateEntries = WifiSystemStateEntry.emptyArray();
            this.backgroundScanReturnEntries = ScanReturnEntry.emptyArray();
            this.backgroundScanRequestState = WifiSystemStateEntry.emptyArray();
            this.numLastResortWatchdogTriggers = 0;
            this.numLastResortWatchdogBadAssociationNetworksTotal = 0;
            this.numLastResortWatchdogBadAuthenticationNetworksTotal = 0;
            this.numLastResortWatchdogBadDhcpNetworksTotal = 0;
            this.numLastResortWatchdogBadOtherNetworksTotal = 0;
            this.numLastResortWatchdogAvailableNetworksTotal = 0;
            this.numLastResortWatchdogTriggersWithBadAssociation = 0;
            this.numLastResortWatchdogTriggersWithBadAuthentication = 0;
            this.numLastResortWatchdogTriggersWithBadDhcp = 0;
            this.numLastResortWatchdogTriggersWithBadOther = 0;
            this.numConnectivityWatchdogPnoGood = 0;
            this.numConnectivityWatchdogPnoBad = 0;
            this.numConnectivityWatchdogBackgroundGood = 0;
            this.numConnectivityWatchdogBackgroundBad = 0;
            this.recordDurationSec = 0;
            this.rssiPollRssiCount = RssiPollCount.emptyArray();
            this.numLastResortWatchdogSuccesses = 0;
            this.numHiddenNetworks = 0;
            this.numPasspointNetworks = 0;
            this.numTotalScanResults = 0;
            this.numOpenNetworkScanResults = 0;
            this.numPersonalNetworkScanResults = 0;
            this.numEnterpriseNetworkScanResults = 0;
            this.numHiddenNetworkScanResults = 0;
            this.numHotspot2R1NetworkScanResults = 0;
            this.numHotspot2R2NetworkScanResults = 0;
            this.numScans = 0;
            this.alertReasonCount = AlertReasonCount.emptyArray();
            this.wifiScoreCount = WifiScoreCount.emptyArray();
            this.softApDuration = SoftApDurationBucket.emptyArray();
            this.softApReturnCode = SoftApReturnCodeCount.emptyArray();
            this.rssiPollDeltaCount = RssiPollCount.emptyArray();
            this.staEventList = StaEvent.emptyArray();
            this.numHalCrashes = 0;
            this.numWificondCrashes = 0;
            this.numWifiOnFailureDueToHal = 0;
            this.numWifiOnFailureDueToWificond = 0;
            this.wifiAwareLog = null;
            this.numPasspointProviders = 0;
            this.numPasspointProviderInstallation = 0;
            this.numPasspointProviderInstallSuccess = 0;
            this.numPasspointProviderUninstallation = 0;
            this.numPasspointProviderUninstallSuccess = 0;
            this.numPasspointProvidersSuccessfullyConnected = 0;
            this.totalSsidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.totalBssidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableOpenSsidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableOpenBssidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableSavedSsidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableSavedBssidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableOpenOrSavedSsidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableOpenOrSavedBssidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableSavedPasspointProviderProfilesInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.availableSavedPasspointProviderBssidsInScanHistogram = NumConnectableNetworksBucket.emptyArray();
            this.fullBandAllSingleScanListenerResults = 0;
            this.partialAllSingleScanListenerResults = 0;
            this.pnoScanMetrics = null;
            this.connectToNetworkNotificationCount = ConnectToNetworkNotificationAndActionCount.emptyArray();
            this.connectToNetworkNotificationActionCount = ConnectToNetworkNotificationAndActionCount.emptyArray();
            this.openNetworkRecommenderBlacklistSize = 0;
            this.isWifiNetworksAvailableNotificationOn = false;
            this.numOpenNetworkRecommendationUpdates = 0;
            this.numOpenNetworkConnectMessageFailedToSend = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.connectionEvent != null && this.connectionEvent.length > 0) {
                for (ConnectionEvent element : this.connectionEvent) {
                    if (element != null) {
                        output.writeMessage(1, element);
                    }
                }
            }
            if (this.numSavedNetworks != 0) {
                output.writeInt32(2, this.numSavedNetworks);
            }
            if (this.numOpenNetworks != 0) {
                output.writeInt32(3, this.numOpenNetworks);
            }
            if (this.numPersonalNetworks != 0) {
                output.writeInt32(4, this.numPersonalNetworks);
            }
            if (this.numEnterpriseNetworks != 0) {
                output.writeInt32(5, this.numEnterpriseNetworks);
            }
            if (this.isLocationEnabled) {
                output.writeBool(6, this.isLocationEnabled);
            }
            if (this.isScanningAlwaysEnabled) {
                output.writeBool(7, this.isScanningAlwaysEnabled);
            }
            if (this.numWifiToggledViaSettings != 0) {
                output.writeInt32(8, this.numWifiToggledViaSettings);
            }
            if (this.numWifiToggledViaAirplane != 0) {
                output.writeInt32(9, this.numWifiToggledViaAirplane);
            }
            if (this.numNetworksAddedByUser != 0) {
                output.writeInt32(10, this.numNetworksAddedByUser);
            }
            if (this.numNetworksAddedByApps != 0) {
                output.writeInt32(11, this.numNetworksAddedByApps);
            }
            if (this.numEmptyScanResults != 0) {
                output.writeInt32(12, this.numEmptyScanResults);
            }
            if (this.numNonEmptyScanResults != 0) {
                output.writeInt32(13, this.numNonEmptyScanResults);
            }
            if (this.numOneshotScans != 0) {
                output.writeInt32(14, this.numOneshotScans);
            }
            if (this.numBackgroundScans != 0) {
                output.writeInt32(15, this.numBackgroundScans);
            }
            if (this.scanReturnEntries != null && this.scanReturnEntries.length > 0) {
                for (ScanReturnEntry element2 : this.scanReturnEntries) {
                    if (element2 != null) {
                        output.writeMessage(16, element2);
                    }
                }
            }
            if (this.wifiSystemStateEntries != null && this.wifiSystemStateEntries.length > 0) {
                for (WifiSystemStateEntry element3 : this.wifiSystemStateEntries) {
                    if (element3 != null) {
                        output.writeMessage(17, element3);
                    }
                }
            }
            if (this.backgroundScanReturnEntries != null && this.backgroundScanReturnEntries.length > 0) {
                for (ScanReturnEntry element22 : this.backgroundScanReturnEntries) {
                    if (element22 != null) {
                        output.writeMessage(18, element22);
                    }
                }
            }
            if (this.backgroundScanRequestState != null && this.backgroundScanRequestState.length > 0) {
                for (WifiSystemStateEntry element32 : this.backgroundScanRequestState) {
                    if (element32 != null) {
                        output.writeMessage(19, element32);
                    }
                }
            }
            if (this.numLastResortWatchdogTriggers != 0) {
                output.writeInt32(20, this.numLastResortWatchdogTriggers);
            }
            if (this.numLastResortWatchdogBadAssociationNetworksTotal != 0) {
                output.writeInt32(21, this.numLastResortWatchdogBadAssociationNetworksTotal);
            }
            if (this.numLastResortWatchdogBadAuthenticationNetworksTotal != 0) {
                output.writeInt32(22, this.numLastResortWatchdogBadAuthenticationNetworksTotal);
            }
            if (this.numLastResortWatchdogBadDhcpNetworksTotal != 0) {
                output.writeInt32(23, this.numLastResortWatchdogBadDhcpNetworksTotal);
            }
            if (this.numLastResortWatchdogBadOtherNetworksTotal != 0) {
                output.writeInt32(24, this.numLastResortWatchdogBadOtherNetworksTotal);
            }
            if (this.numLastResortWatchdogAvailableNetworksTotal != 0) {
                output.writeInt32(25, this.numLastResortWatchdogAvailableNetworksTotal);
            }
            if (this.numLastResortWatchdogTriggersWithBadAssociation != 0) {
                output.writeInt32(26, this.numLastResortWatchdogTriggersWithBadAssociation);
            }
            if (this.numLastResortWatchdogTriggersWithBadAuthentication != 0) {
                output.writeInt32(27, this.numLastResortWatchdogTriggersWithBadAuthentication);
            }
            if (this.numLastResortWatchdogTriggersWithBadDhcp != 0) {
                output.writeInt32(28, this.numLastResortWatchdogTriggersWithBadDhcp);
            }
            if (this.numLastResortWatchdogTriggersWithBadOther != 0) {
                output.writeInt32(29, this.numLastResortWatchdogTriggersWithBadOther);
            }
            if (this.numConnectivityWatchdogPnoGood != 0) {
                output.writeInt32(30, this.numConnectivityWatchdogPnoGood);
            }
            if (this.numConnectivityWatchdogPnoBad != 0) {
                output.writeInt32(31, this.numConnectivityWatchdogPnoBad);
            }
            if (this.numConnectivityWatchdogBackgroundGood != 0) {
                output.writeInt32(32, this.numConnectivityWatchdogBackgroundGood);
            }
            if (this.numConnectivityWatchdogBackgroundBad != 0) {
                output.writeInt32(33, this.numConnectivityWatchdogBackgroundBad);
            }
            if (this.recordDurationSec != 0) {
                output.writeInt32(34, this.recordDurationSec);
            }
            if (this.rssiPollRssiCount != null && this.rssiPollRssiCount.length > 0) {
                for (RssiPollCount element4 : this.rssiPollRssiCount) {
                    if (element4 != null) {
                        output.writeMessage(35, element4);
                    }
                }
            }
            if (this.numLastResortWatchdogSuccesses != 0) {
                output.writeInt32(36, this.numLastResortWatchdogSuccesses);
            }
            if (this.numHiddenNetworks != 0) {
                output.writeInt32(37, this.numHiddenNetworks);
            }
            if (this.numPasspointNetworks != 0) {
                output.writeInt32(38, this.numPasspointNetworks);
            }
            if (this.numTotalScanResults != 0) {
                output.writeInt32(39, this.numTotalScanResults);
            }
            if (this.numOpenNetworkScanResults != 0) {
                output.writeInt32(40, this.numOpenNetworkScanResults);
            }
            if (this.numPersonalNetworkScanResults != 0) {
                output.writeInt32(41, this.numPersonalNetworkScanResults);
            }
            if (this.numEnterpriseNetworkScanResults != 0) {
                output.writeInt32(42, this.numEnterpriseNetworkScanResults);
            }
            if (this.numHiddenNetworkScanResults != 0) {
                output.writeInt32(43, this.numHiddenNetworkScanResults);
            }
            if (this.numHotspot2R1NetworkScanResults != 0) {
                output.writeInt32(44, this.numHotspot2R1NetworkScanResults);
            }
            if (this.numHotspot2R2NetworkScanResults != 0) {
                output.writeInt32(45, this.numHotspot2R2NetworkScanResults);
            }
            if (this.numScans != 0) {
                output.writeInt32(46, this.numScans);
            }
            if (this.alertReasonCount != null && this.alertReasonCount.length > 0) {
                for (AlertReasonCount element5 : this.alertReasonCount) {
                    if (element5 != null) {
                        output.writeMessage(47, element5);
                    }
                }
            }
            if (this.wifiScoreCount != null && this.wifiScoreCount.length > 0) {
                for (WifiScoreCount element6 : this.wifiScoreCount) {
                    if (element6 != null) {
                        output.writeMessage(48, element6);
                    }
                }
            }
            if (this.softApDuration != null && this.softApDuration.length > 0) {
                for (SoftApDurationBucket element7 : this.softApDuration) {
                    if (element7 != null) {
                        output.writeMessage(49, element7);
                    }
                }
            }
            if (this.softApReturnCode != null && this.softApReturnCode.length > 0) {
                for (SoftApReturnCodeCount element8 : this.softApReturnCode) {
                    if (element8 != null) {
                        output.writeMessage(50, element8);
                    }
                }
            }
            if (this.rssiPollDeltaCount != null && this.rssiPollDeltaCount.length > 0) {
                for (RssiPollCount element42 : this.rssiPollDeltaCount) {
                    if (element42 != null) {
                        output.writeMessage(51, element42);
                    }
                }
            }
            if (this.staEventList != null && this.staEventList.length > 0) {
                for (StaEvent element9 : this.staEventList) {
                    if (element9 != null) {
                        output.writeMessage(52, element9);
                    }
                }
            }
            if (this.numHalCrashes != 0) {
                output.writeInt32(53, this.numHalCrashes);
            }
            if (this.numWificondCrashes != 0) {
                output.writeInt32(54, this.numWificondCrashes);
            }
            if (this.numWifiOnFailureDueToHal != 0) {
                output.writeInt32(55, this.numWifiOnFailureDueToHal);
            }
            if (this.numWifiOnFailureDueToWificond != 0) {
                output.writeInt32(56, this.numWifiOnFailureDueToWificond);
            }
            if (this.wifiAwareLog != null) {
                output.writeMessage(57, this.wifiAwareLog);
            }
            if (this.numPasspointProviders != 0) {
                output.writeInt32(58, this.numPasspointProviders);
            }
            if (this.numPasspointProviderInstallation != 0) {
                output.writeInt32(59, this.numPasspointProviderInstallation);
            }
            if (this.numPasspointProviderInstallSuccess != 0) {
                output.writeInt32(60, this.numPasspointProviderInstallSuccess);
            }
            if (this.numPasspointProviderUninstallation != 0) {
                output.writeInt32(61, this.numPasspointProviderUninstallation);
            }
            if (this.numPasspointProviderUninstallSuccess != 0) {
                output.writeInt32(62, this.numPasspointProviderUninstallSuccess);
            }
            if (this.numPasspointProvidersSuccessfullyConnected != 0) {
                output.writeInt32(63, this.numPasspointProvidersSuccessfullyConnected);
            }
            if (this.totalSsidsInScanHistogram != null && this.totalSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10 : this.totalSsidsInScanHistogram) {
                    if (element10 != null) {
                        output.writeMessage(64, element10);
                    }
                }
            }
            if (this.totalBssidsInScanHistogram != null && this.totalBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102 : this.totalBssidsInScanHistogram) {
                    if (element102 != null) {
                        output.writeMessage(65, element102);
                    }
                }
            }
            if (this.availableOpenSsidsInScanHistogram != null && this.availableOpenSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022 : this.availableOpenSsidsInScanHistogram) {
                    if (element1022 != null) {
                        output.writeMessage(66, element1022);
                    }
                }
            }
            if (this.availableOpenBssidsInScanHistogram != null && this.availableOpenBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222 : this.availableOpenBssidsInScanHistogram) {
                    if (element10222 != null) {
                        output.writeMessage(67, element10222);
                    }
                }
            }
            if (this.availableSavedSsidsInScanHistogram != null && this.availableSavedSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102222 : this.availableSavedSsidsInScanHistogram) {
                    if (element102222 != null) {
                        output.writeMessage(68, element102222);
                    }
                }
            }
            if (this.availableSavedBssidsInScanHistogram != null && this.availableSavedBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022222 : this.availableSavedBssidsInScanHistogram) {
                    if (element1022222 != null) {
                        output.writeMessage(69, element1022222);
                    }
                }
            }
            if (this.availableOpenOrSavedSsidsInScanHistogram != null && this.availableOpenOrSavedSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222222 : this.availableOpenOrSavedSsidsInScanHistogram) {
                    if (element10222222 != null) {
                        output.writeMessage(70, element10222222);
                    }
                }
            }
            if (this.availableOpenOrSavedBssidsInScanHistogram != null && this.availableOpenOrSavedBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102222222 : this.availableOpenOrSavedBssidsInScanHistogram) {
                    if (element102222222 != null) {
                        output.writeMessage(71, element102222222);
                    }
                }
            }
            if (this.availableSavedPasspointProviderProfilesInScanHistogram != null && this.availableSavedPasspointProviderProfilesInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022222222 : this.availableSavedPasspointProviderProfilesInScanHistogram) {
                    if (element1022222222 != null) {
                        output.writeMessage(72, element1022222222);
                    }
                }
            }
            if (this.availableSavedPasspointProviderBssidsInScanHistogram != null && this.availableSavedPasspointProviderBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222222222 : this.availableSavedPasspointProviderBssidsInScanHistogram) {
                    if (element10222222222 != null) {
                        output.writeMessage(73, element10222222222);
                    }
                }
            }
            if (this.fullBandAllSingleScanListenerResults != 0) {
                output.writeInt32(74, this.fullBandAllSingleScanListenerResults);
            }
            if (this.partialAllSingleScanListenerResults != 0) {
                output.writeInt32(75, this.partialAllSingleScanListenerResults);
            }
            if (this.pnoScanMetrics != null) {
                output.writeMessage(76, this.pnoScanMetrics);
            }
            if (this.connectToNetworkNotificationCount != null && this.connectToNetworkNotificationCount.length > 0) {
                for (ConnectToNetworkNotificationAndActionCount element11 : this.connectToNetworkNotificationCount) {
                    if (element11 != null) {
                        output.writeMessage(77, element11);
                    }
                }
            }
            if (this.connectToNetworkNotificationActionCount != null && this.connectToNetworkNotificationActionCount.length > 0) {
                for (ConnectToNetworkNotificationAndActionCount element112 : this.connectToNetworkNotificationActionCount) {
                    if (element112 != null) {
                        output.writeMessage(78, element112);
                    }
                }
            }
            if (this.openNetworkRecommenderBlacklistSize != 0) {
                output.writeInt32(79, this.openNetworkRecommenderBlacklistSize);
            }
            if (this.isWifiNetworksAvailableNotificationOn) {
                output.writeBool(80, this.isWifiNetworksAvailableNotificationOn);
            }
            if (this.numOpenNetworkRecommendationUpdates != 0) {
                output.writeInt32(81, this.numOpenNetworkRecommendationUpdates);
            }
            if (this.numOpenNetworkConnectMessageFailedToSend != 0) {
                output.writeInt32(82, this.numOpenNetworkConnectMessageFailedToSend);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.connectionEvent != null && this.connectionEvent.length > 0) {
                for (ConnectionEvent element : this.connectionEvent) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(1, element);
                    }
                }
            }
            if (this.numSavedNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.numSavedNetworks);
            }
            if (this.numOpenNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.numOpenNetworks);
            }
            if (this.numPersonalNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.numPersonalNetworks);
            }
            if (this.numEnterpriseNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.numEnterpriseNetworks);
            }
            if (this.isLocationEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(6, this.isLocationEnabled);
            }
            if (this.isScanningAlwaysEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(7, this.isScanningAlwaysEnabled);
            }
            if (this.numWifiToggledViaSettings != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.numWifiToggledViaSettings);
            }
            if (this.numWifiToggledViaAirplane != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(9, this.numWifiToggledViaAirplane);
            }
            if (this.numNetworksAddedByUser != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(10, this.numNetworksAddedByUser);
            }
            if (this.numNetworksAddedByApps != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(11, this.numNetworksAddedByApps);
            }
            if (this.numEmptyScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(12, this.numEmptyScanResults);
            }
            if (this.numNonEmptyScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(13, this.numNonEmptyScanResults);
            }
            if (this.numOneshotScans != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(14, this.numOneshotScans);
            }
            if (this.numBackgroundScans != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(15, this.numBackgroundScans);
            }
            if (this.scanReturnEntries != null && this.scanReturnEntries.length > 0) {
                for (ScanReturnEntry element2 : this.scanReturnEntries) {
                    if (element2 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(16, element2);
                    }
                }
            }
            if (this.wifiSystemStateEntries != null && this.wifiSystemStateEntries.length > 0) {
                for (WifiSystemStateEntry element3 : this.wifiSystemStateEntries) {
                    if (element3 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(17, element3);
                    }
                }
            }
            if (this.backgroundScanReturnEntries != null && this.backgroundScanReturnEntries.length > 0) {
                for (ScanReturnEntry element22 : this.backgroundScanReturnEntries) {
                    if (element22 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(18, element22);
                    }
                }
            }
            if (this.backgroundScanRequestState != null && this.backgroundScanRequestState.length > 0) {
                for (WifiSystemStateEntry element32 : this.backgroundScanRequestState) {
                    if (element32 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(19, element32);
                    }
                }
            }
            if (this.numLastResortWatchdogTriggers != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(20, this.numLastResortWatchdogTriggers);
            }
            if (this.numLastResortWatchdogBadAssociationNetworksTotal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(21, this.numLastResortWatchdogBadAssociationNetworksTotal);
            }
            if (this.numLastResortWatchdogBadAuthenticationNetworksTotal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(22, this.numLastResortWatchdogBadAuthenticationNetworksTotal);
            }
            if (this.numLastResortWatchdogBadDhcpNetworksTotal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(23, this.numLastResortWatchdogBadDhcpNetworksTotal);
            }
            if (this.numLastResortWatchdogBadOtherNetworksTotal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(24, this.numLastResortWatchdogBadOtherNetworksTotal);
            }
            if (this.numLastResortWatchdogAvailableNetworksTotal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(25, this.numLastResortWatchdogAvailableNetworksTotal);
            }
            if (this.numLastResortWatchdogTriggersWithBadAssociation != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(26, this.numLastResortWatchdogTriggersWithBadAssociation);
            }
            if (this.numLastResortWatchdogTriggersWithBadAuthentication != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(27, this.numLastResortWatchdogTriggersWithBadAuthentication);
            }
            if (this.numLastResortWatchdogTriggersWithBadDhcp != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(28, this.numLastResortWatchdogTriggersWithBadDhcp);
            }
            if (this.numLastResortWatchdogTriggersWithBadOther != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(29, this.numLastResortWatchdogTriggersWithBadOther);
            }
            if (this.numConnectivityWatchdogPnoGood != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(30, this.numConnectivityWatchdogPnoGood);
            }
            if (this.numConnectivityWatchdogPnoBad != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(31, this.numConnectivityWatchdogPnoBad);
            }
            if (this.numConnectivityWatchdogBackgroundGood != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(32, this.numConnectivityWatchdogBackgroundGood);
            }
            if (this.numConnectivityWatchdogBackgroundBad != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(33, this.numConnectivityWatchdogBackgroundBad);
            }
            if (this.recordDurationSec != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(34, this.recordDurationSec);
            }
            if (this.rssiPollRssiCount != null && this.rssiPollRssiCount.length > 0) {
                for (RssiPollCount element4 : this.rssiPollRssiCount) {
                    if (element4 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(35, element4);
                    }
                }
            }
            if (this.numLastResortWatchdogSuccesses != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(36, this.numLastResortWatchdogSuccesses);
            }
            if (this.numHiddenNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(37, this.numHiddenNetworks);
            }
            if (this.numPasspointNetworks != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(38, this.numPasspointNetworks);
            }
            if (this.numTotalScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(39, this.numTotalScanResults);
            }
            if (this.numOpenNetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(40, this.numOpenNetworkScanResults);
            }
            if (this.numPersonalNetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(41, this.numPersonalNetworkScanResults);
            }
            if (this.numEnterpriseNetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(42, this.numEnterpriseNetworkScanResults);
            }
            if (this.numHiddenNetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(43, this.numHiddenNetworkScanResults);
            }
            if (this.numHotspot2R1NetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(44, this.numHotspot2R1NetworkScanResults);
            }
            if (this.numHotspot2R2NetworkScanResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(45, this.numHotspot2R2NetworkScanResults);
            }
            if (this.numScans != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(46, this.numScans);
            }
            if (this.alertReasonCount != null && this.alertReasonCount.length > 0) {
                for (AlertReasonCount element5 : this.alertReasonCount) {
                    if (element5 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(47, element5);
                    }
                }
            }
            if (this.wifiScoreCount != null && this.wifiScoreCount.length > 0) {
                for (WifiScoreCount element6 : this.wifiScoreCount) {
                    if (element6 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(48, element6);
                    }
                }
            }
            if (this.softApDuration != null && this.softApDuration.length > 0) {
                for (SoftApDurationBucket element7 : this.softApDuration) {
                    if (element7 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(49, element7);
                    }
                }
            }
            if (this.softApReturnCode != null && this.softApReturnCode.length > 0) {
                for (SoftApReturnCodeCount element8 : this.softApReturnCode) {
                    if (element8 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(50, element8);
                    }
                }
            }
            if (this.rssiPollDeltaCount != null && this.rssiPollDeltaCount.length > 0) {
                for (RssiPollCount element42 : this.rssiPollDeltaCount) {
                    if (element42 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(51, element42);
                    }
                }
            }
            if (this.staEventList != null && this.staEventList.length > 0) {
                for (StaEvent element9 : this.staEventList) {
                    if (element9 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(52, element9);
                    }
                }
            }
            if (this.numHalCrashes != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(53, this.numHalCrashes);
            }
            if (this.numWificondCrashes != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(54, this.numWificondCrashes);
            }
            if (this.numWifiOnFailureDueToHal != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(55, this.numWifiOnFailureDueToHal);
            }
            if (this.numWifiOnFailureDueToWificond != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(56, this.numWifiOnFailureDueToWificond);
            }
            if (this.wifiAwareLog != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(57, this.wifiAwareLog);
            }
            if (this.numPasspointProviders != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(58, this.numPasspointProviders);
            }
            if (this.numPasspointProviderInstallation != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(59, this.numPasspointProviderInstallation);
            }
            if (this.numPasspointProviderInstallSuccess != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(60, this.numPasspointProviderInstallSuccess);
            }
            if (this.numPasspointProviderUninstallation != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(61, this.numPasspointProviderUninstallation);
            }
            if (this.numPasspointProviderUninstallSuccess != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(62, this.numPasspointProviderUninstallSuccess);
            }
            if (this.numPasspointProvidersSuccessfullyConnected != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(63, this.numPasspointProvidersSuccessfullyConnected);
            }
            if (this.totalSsidsInScanHistogram != null && this.totalSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10 : this.totalSsidsInScanHistogram) {
                    if (element10 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(64, element10);
                    }
                }
            }
            if (this.totalBssidsInScanHistogram != null && this.totalBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102 : this.totalBssidsInScanHistogram) {
                    if (element102 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(65, element102);
                    }
                }
            }
            if (this.availableOpenSsidsInScanHistogram != null && this.availableOpenSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022 : this.availableOpenSsidsInScanHistogram) {
                    if (element1022 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(66, element1022);
                    }
                }
            }
            if (this.availableOpenBssidsInScanHistogram != null && this.availableOpenBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222 : this.availableOpenBssidsInScanHistogram) {
                    if (element10222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(67, element10222);
                    }
                }
            }
            if (this.availableSavedSsidsInScanHistogram != null && this.availableSavedSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102222 : this.availableSavedSsidsInScanHistogram) {
                    if (element102222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(68, element102222);
                    }
                }
            }
            if (this.availableSavedBssidsInScanHistogram != null && this.availableSavedBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022222 : this.availableSavedBssidsInScanHistogram) {
                    if (element1022222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(69, element1022222);
                    }
                }
            }
            if (this.availableOpenOrSavedSsidsInScanHistogram != null && this.availableOpenOrSavedSsidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222222 : this.availableOpenOrSavedSsidsInScanHistogram) {
                    if (element10222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(70, element10222222);
                    }
                }
            }
            if (this.availableOpenOrSavedBssidsInScanHistogram != null && this.availableOpenOrSavedBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element102222222 : this.availableOpenOrSavedBssidsInScanHistogram) {
                    if (element102222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(71, element102222222);
                    }
                }
            }
            if (this.availableSavedPasspointProviderProfilesInScanHistogram != null && this.availableSavedPasspointProviderProfilesInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element1022222222 : this.availableSavedPasspointProviderProfilesInScanHistogram) {
                    if (element1022222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(72, element1022222222);
                    }
                }
            }
            if (this.availableSavedPasspointProviderBssidsInScanHistogram != null && this.availableSavedPasspointProviderBssidsInScanHistogram.length > 0) {
                for (NumConnectableNetworksBucket element10222222222 : this.availableSavedPasspointProviderBssidsInScanHistogram) {
                    if (element10222222222 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(73, element10222222222);
                    }
                }
            }
            if (this.fullBandAllSingleScanListenerResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(74, this.fullBandAllSingleScanListenerResults);
            }
            if (this.partialAllSingleScanListenerResults != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(75, this.partialAllSingleScanListenerResults);
            }
            if (this.pnoScanMetrics != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(76, this.pnoScanMetrics);
            }
            if (this.connectToNetworkNotificationCount != null && this.connectToNetworkNotificationCount.length > 0) {
                for (ConnectToNetworkNotificationAndActionCount element11 : this.connectToNetworkNotificationCount) {
                    if (element11 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(77, element11);
                    }
                }
            }
            if (this.connectToNetworkNotificationActionCount != null && this.connectToNetworkNotificationActionCount.length > 0) {
                for (ConnectToNetworkNotificationAndActionCount element112 : this.connectToNetworkNotificationActionCount) {
                    if (element112 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(78, element112);
                    }
                }
            }
            if (this.openNetworkRecommenderBlacklistSize != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(79, this.openNetworkRecommenderBlacklistSize);
            }
            if (this.isWifiNetworksAvailableNotificationOn) {
                size += CodedOutputByteBufferNano.computeBoolSize(80, this.isWifiNetworksAvailableNotificationOn);
            }
            if (this.numOpenNetworkRecommendationUpdates != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(81, this.numOpenNetworkRecommendationUpdates);
            }
            if (this.numOpenNetworkConnectMessageFailedToSend != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(82, this.numOpenNetworkConnectMessageFailedToSend);
            }
            return size;
        }

        public WifiLog mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int arrayLength;
                int i;
                ScanReturnEntry[] newArray;
                WifiSystemStateEntry[] newArray2;
                RssiPollCount[] newArray3;
                NumConnectableNetworksBucket[] newArray4;
                ConnectToNetworkNotificationAndActionCount[] newArray5;
                switch (tag) {
                    case 0:
                        return this;
                    case 10:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 10);
                        i = this.connectionEvent == null ? 0 : this.connectionEvent.length;
                        ConnectionEvent[] newArray6 = new ConnectionEvent[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.connectionEvent, 0, newArray6, 0, i);
                        }
                        while (i < newArray6.length - 1) {
                            newArray6[i] = new ConnectionEvent();
                            input.readMessage(newArray6[i]);
                            input.readTag();
                            i++;
                        }
                        newArray6[i] = new ConnectionEvent();
                        input.readMessage(newArray6[i]);
                        this.connectionEvent = newArray6;
                        break;
                    case 16:
                        this.numSavedNetworks = input.readInt32();
                        break;
                    case 24:
                        this.numOpenNetworks = input.readInt32();
                        break;
                    case 32:
                        this.numPersonalNetworks = input.readInt32();
                        break;
                    case 40:
                        this.numEnterpriseNetworks = input.readInt32();
                        break;
                    case 48:
                        this.isLocationEnabled = input.readBool();
                        break;
                    case 56:
                        this.isScanningAlwaysEnabled = input.readBool();
                        break;
                    case 64:
                        this.numWifiToggledViaSettings = input.readInt32();
                        break;
                    case 72:
                        this.numWifiToggledViaAirplane = input.readInt32();
                        break;
                    case 80:
                        this.numNetworksAddedByUser = input.readInt32();
                        break;
                    case 88:
                        this.numNetworksAddedByApps = input.readInt32();
                        break;
                    case 96:
                        this.numEmptyScanResults = input.readInt32();
                        break;
                    case 104:
                        this.numNonEmptyScanResults = input.readInt32();
                        break;
                    case 112:
                        this.numOneshotScans = input.readInt32();
                        break;
                    case 120:
                        this.numBackgroundScans = input.readInt32();
                        break;
                    case 130:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 130);
                        i = this.scanReturnEntries == null ? 0 : this.scanReturnEntries.length;
                        newArray = new ScanReturnEntry[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.scanReturnEntries, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new ScanReturnEntry();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new ScanReturnEntry();
                        input.readMessage(newArray[i]);
                        this.scanReturnEntries = newArray;
                        break;
                    case 138:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 138);
                        i = this.wifiSystemStateEntries == null ? 0 : this.wifiSystemStateEntries.length;
                        newArray2 = new WifiSystemStateEntry[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.wifiSystemStateEntries, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new WifiSystemStateEntry();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new WifiSystemStateEntry();
                        input.readMessage(newArray2[i]);
                        this.wifiSystemStateEntries = newArray2;
                        break;
                    case 146:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 146);
                        i = this.backgroundScanReturnEntries == null ? 0 : this.backgroundScanReturnEntries.length;
                        newArray = new ScanReturnEntry[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.backgroundScanReturnEntries, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new ScanReturnEntry();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new ScanReturnEntry();
                        input.readMessage(newArray[i]);
                        this.backgroundScanReturnEntries = newArray;
                        break;
                    case 154:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 154);
                        i = this.backgroundScanRequestState == null ? 0 : this.backgroundScanRequestState.length;
                        newArray2 = new WifiSystemStateEntry[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.backgroundScanRequestState, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new WifiSystemStateEntry();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new WifiSystemStateEntry();
                        input.readMessage(newArray2[i]);
                        this.backgroundScanRequestState = newArray2;
                        break;
                    case 160:
                        this.numLastResortWatchdogTriggers = input.readInt32();
                        break;
                    case 168:
                        this.numLastResortWatchdogBadAssociationNetworksTotal = input.readInt32();
                        break;
                    case 176:
                        this.numLastResortWatchdogBadAuthenticationNetworksTotal = input.readInt32();
                        break;
                    case 184:
                        this.numLastResortWatchdogBadDhcpNetworksTotal = input.readInt32();
                        break;
                    case 192:
                        this.numLastResortWatchdogBadOtherNetworksTotal = input.readInt32();
                        break;
                    case 200:
                        this.numLastResortWatchdogAvailableNetworksTotal = input.readInt32();
                        break;
                    case 208:
                        this.numLastResortWatchdogTriggersWithBadAssociation = input.readInt32();
                        break;
                    case 216:
                        this.numLastResortWatchdogTriggersWithBadAuthentication = input.readInt32();
                        break;
                    case 224:
                        this.numLastResortWatchdogTriggersWithBadDhcp = input.readInt32();
                        break;
                    case 232:
                        this.numLastResortWatchdogTriggersWithBadOther = input.readInt32();
                        break;
                    case 240:
                        this.numConnectivityWatchdogPnoGood = input.readInt32();
                        break;
                    case 248:
                        this.numConnectivityWatchdogPnoBad = input.readInt32();
                        break;
                    case 256:
                        this.numConnectivityWatchdogBackgroundGood = input.readInt32();
                        break;
                    case 264:
                        this.numConnectivityWatchdogBackgroundBad = input.readInt32();
                        break;
                    case 272:
                        this.recordDurationSec = input.readInt32();
                        break;
                    case 282:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 282);
                        i = this.rssiPollRssiCount == null ? 0 : this.rssiPollRssiCount.length;
                        newArray3 = new RssiPollCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.rssiPollRssiCount, 0, newArray3, 0, i);
                        }
                        while (i < newArray3.length - 1) {
                            newArray3[i] = new RssiPollCount();
                            input.readMessage(newArray3[i]);
                            input.readTag();
                            i++;
                        }
                        newArray3[i] = new RssiPollCount();
                        input.readMessage(newArray3[i]);
                        this.rssiPollRssiCount = newArray3;
                        break;
                    case 288:
                        this.numLastResortWatchdogSuccesses = input.readInt32();
                        break;
                    case 296:
                        this.numHiddenNetworks = input.readInt32();
                        break;
                    case 304:
                        this.numPasspointNetworks = input.readInt32();
                        break;
                    case 312:
                        this.numTotalScanResults = input.readInt32();
                        break;
                    case 320:
                        this.numOpenNetworkScanResults = input.readInt32();
                        break;
                    case 328:
                        this.numPersonalNetworkScanResults = input.readInt32();
                        break;
                    case 336:
                        this.numEnterpriseNetworkScanResults = input.readInt32();
                        break;
                    case MetricsEvent.USER_LOCALE_LIST /*344*/:
                        this.numHiddenNetworkScanResults = input.readInt32();
                        break;
                    case 352:
                        this.numHotspot2R1NetworkScanResults = input.readInt32();
                        break;
                    case 360:
                        this.numHotspot2R2NetworkScanResults = input.readInt32();
                        break;
                    case MetricsEvent.SUW_ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION /*368*/:
                        this.numScans = input.readInt32();
                        break;
                    case MetricsEvent.SETTINGS_CONDITION_BACKGROUND_DATA /*378*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.SETTINGS_CONDITION_BACKGROUND_DATA);
                        i = this.alertReasonCount == null ? 0 : this.alertReasonCount.length;
                        AlertReasonCount[] newArray7 = new AlertReasonCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.alertReasonCount, 0, newArray7, 0, i);
                        }
                        while (i < newArray7.length - 1) {
                            newArray7[i] = new AlertReasonCount();
                            input.readMessage(newArray7[i]);
                            input.readTag();
                            i++;
                        }
                        newArray7[i] = new AlertReasonCount();
                        input.readMessage(newArray7[i]);
                        this.alertReasonCount = newArray7;
                        break;
                    case MetricsEvent.ACTION_SETTINGS_SUGGESTION /*386*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.ACTION_SETTINGS_SUGGESTION);
                        i = this.wifiScoreCount == null ? 0 : this.wifiScoreCount.length;
                        WifiScoreCount[] newArray8 = new WifiScoreCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.wifiScoreCount, 0, newArray8, 0, i);
                        }
                        while (i < newArray8.length - 1) {
                            newArray8[i] = new WifiScoreCount();
                            input.readMessage(newArray8[i]);
                            input.readTag();
                            i++;
                        }
                        newArray8[i] = new WifiScoreCount();
                        input.readMessage(newArray8[i]);
                        this.wifiScoreCount = newArray8;
                        break;
                    case MetricsEvent.ACTION_DATA_SAVER_MODE /*394*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.ACTION_DATA_SAVER_MODE);
                        i = this.softApDuration == null ? 0 : this.softApDuration.length;
                        SoftApDurationBucket[] newArray9 = new SoftApDurationBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.softApDuration, 0, newArray9, 0, i);
                        }
                        while (i < newArray9.length - 1) {
                            newArray9[i] = new SoftApDurationBucket();
                            input.readMessage(newArray9[i]);
                            input.readTag();
                            i++;
                        }
                        newArray9[i] = new SoftApDurationBucket();
                        input.readMessage(newArray9[i]);
                        this.softApDuration = newArray9;
                        break;
                    case 402:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 402);
                        i = this.softApReturnCode == null ? 0 : this.softApReturnCode.length;
                        SoftApReturnCodeCount[] newArray10 = new SoftApReturnCodeCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.softApReturnCode, 0, newArray10, 0, i);
                        }
                        while (i < newArray10.length - 1) {
                            newArray10[i] = new SoftApReturnCodeCount();
                            input.readMessage(newArray10[i]);
                            input.readTag();
                            i++;
                        }
                        newArray10[i] = new SoftApReturnCodeCount();
                        input.readMessage(newArray10[i]);
                        this.softApReturnCode = newArray10;
                        break;
                    case MetricsEvent.ACTION_NOTIFICATION_GROUP_GESTURE_EXPANDER /*410*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.ACTION_NOTIFICATION_GROUP_GESTURE_EXPANDER);
                        i = this.rssiPollDeltaCount == null ? 0 : this.rssiPollDeltaCount.length;
                        newArray3 = new RssiPollCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.rssiPollDeltaCount, 0, newArray3, 0, i);
                        }
                        while (i < newArray3.length - 1) {
                            newArray3[i] = new RssiPollCount();
                            input.readMessage(newArray3[i]);
                            input.readTag();
                            i++;
                        }
                        newArray3[i] = new RssiPollCount();
                        input.readMessage(newArray3[i]);
                        this.rssiPollDeltaCount = newArray3;
                        break;
                    case 418:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 418);
                        i = this.staEventList == null ? 0 : this.staEventList.length;
                        StaEvent[] newArray11 = new StaEvent[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.staEventList, 0, newArray11, 0, i);
                        }
                        while (i < newArray11.length - 1) {
                            newArray11[i] = new StaEvent();
                            input.readMessage(newArray11[i]);
                            input.readTag();
                            i++;
                        }
                        newArray11[i] = new StaEvent();
                        input.readMessage(newArray11[i]);
                        this.staEventList = newArray11;
                        break;
                    case 424:
                        this.numHalCrashes = input.readInt32();
                        break;
                    case 432:
                        this.numWificondCrashes = input.readInt32();
                        break;
                    case 440:
                        this.numWifiOnFailureDueToHal = input.readInt32();
                        break;
                    case 448:
                        this.numWifiOnFailureDueToWificond = input.readInt32();
                        break;
                    case MetricsEvent.STORAGE_MANAGER_SETTINGS /*458*/:
                        if (this.wifiAwareLog == null) {
                            this.wifiAwareLog = new WifiAwareLog();
                        }
                        input.readMessage(this.wifiAwareLog);
                        break;
                    case MetricsEvent.ACTION_DELETION_APPS_COLLAPSED /*464*/:
                        this.numPasspointProviders = input.readInt32();
                        break;
                    case MetricsEvent.ACTION_DELETION_HELPER_DOWNLOADS_DELETION_FAIL /*472*/:
                        this.numPasspointProviderInstallation = input.readInt32();
                        break;
                    case 480:
                        this.numPasspointProviderInstallSuccess = input.readInt32();
                        break;
                    case 488:
                        this.numPasspointProviderUninstallation = input.readInt32();
                        break;
                    case 496:
                        this.numPasspointProviderUninstallSuccess = input.readInt32();
                        break;
                    case 504:
                        this.numPasspointProvidersSuccessfullyConnected = input.readInt32();
                        break;
                    case 514:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 514);
                        i = this.totalSsidsInScanHistogram == null ? 0 : this.totalSsidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.totalSsidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.totalSsidsInScanHistogram = newArray4;
                        break;
                    case 522:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 522);
                        i = this.totalBssidsInScanHistogram == null ? 0 : this.totalBssidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.totalBssidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.totalBssidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_APN_EDITOR_ERROR /*530*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_APN_EDITOR_ERROR);
                        i = this.availableOpenSsidsInScanHistogram == null ? 0 : this.availableOpenSsidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableOpenSsidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableOpenSsidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_BLUETOOTH_RENAME /*538*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_BLUETOOTH_RENAME);
                        i = this.availableOpenBssidsInScanHistogram == null ? 0 : this.availableOpenBssidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableOpenBssidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableOpenBssidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_VPN_APP_CONFIG /*546*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_VPN_APP_CONFIG);
                        i = this.availableSavedSsidsInScanHistogram == null ? 0 : this.availableSavedSsidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableSavedSsidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableSavedSsidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_ZEN_ACCESS_GRANT /*554*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_ZEN_ACCESS_GRANT);
                        i = this.availableSavedBssidsInScanHistogram == null ? 0 : this.availableSavedBssidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableSavedBssidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableSavedBssidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_VOLUME_UNMOUNT /*562*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_VOLUME_UNMOUNT);
                        i = this.availableOpenOrSavedSsidsInScanHistogram == null ? 0 : this.availableOpenOrSavedSsidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableOpenOrSavedSsidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableOpenOrSavedSsidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_FINGERPINT_EDIT /*570*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_FINGERPINT_EDIT);
                        i = this.availableOpenOrSavedBssidsInScanHistogram == null ? 0 : this.availableOpenOrSavedBssidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableOpenOrSavedBssidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableOpenOrSavedBssidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_WIFI_P2P_DELETE_GROUP /*578*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_WIFI_P2P_DELETE_GROUP);
                        i = this.availableSavedPasspointProviderProfilesInScanHistogram == null ? 0 : this.availableSavedPasspointProviderProfilesInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableSavedPasspointProviderProfilesInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableSavedPasspointProviderProfilesInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_ACCOUNT_SYNC_FAILED_REMOVAL /*586*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.DIALOG_ACCOUNT_SYNC_FAILED_REMOVAL);
                        i = this.availableSavedPasspointProviderBssidsInScanHistogram == null ? 0 : this.availableSavedPasspointProviderBssidsInScanHistogram.length;
                        newArray4 = new NumConnectableNetworksBucket[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.availableSavedPasspointProviderBssidsInScanHistogram, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new NumConnectableNetworksBucket();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new NumConnectableNetworksBucket();
                        input.readMessage(newArray4[i]);
                        this.availableSavedPasspointProviderBssidsInScanHistogram = newArray4;
                        break;
                    case MetricsEvent.DIALOG_USER_ENABLE_CALLING /*592*/:
                        this.fullBandAllSingleScanListenerResults = input.readInt32();
                        break;
                    case 600:
                        this.partialAllSingleScanListenerResults = input.readInt32();
                        break;
                    case MetricsEvent.PROVISIONING_NETWORK_TYPE /*610*/:
                        if (this.pnoScanMetrics == null) {
                            this.pnoScanMetrics = new PnoScanMetrics();
                        }
                        input.readMessage(this.pnoScanMetrics);
                        break;
                    case MetricsEvent.PROVISIONING_ENTRY_POINT_TRUSTED_SOURCE /*618*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.PROVISIONING_ENTRY_POINT_TRUSTED_SOURCE);
                        i = this.connectToNetworkNotificationCount == null ? 0 : this.connectToNetworkNotificationCount.length;
                        newArray5 = new ConnectToNetworkNotificationAndActionCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.connectToNetworkNotificationCount, 0, newArray5, 0, i);
                        }
                        while (i < newArray5.length - 1) {
                            newArray5[i] = new ConnectToNetworkNotificationAndActionCount();
                            input.readMessage(newArray5[i]);
                            input.readTag();
                            i++;
                        }
                        newArray5[i] = new ConnectToNetworkNotificationAndActionCount();
                        input.readMessage(newArray5[i]);
                        this.connectToNetworkNotificationCount = newArray5;
                        break;
                    case MetricsEvent.PROVISIONING_COPY_ACCOUNT_STATUS /*626*/:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, MetricsEvent.PROVISIONING_COPY_ACCOUNT_STATUS);
                        i = this.connectToNetworkNotificationActionCount == null ? 0 : this.connectToNetworkNotificationActionCount.length;
                        newArray5 = new ConnectToNetworkNotificationAndActionCount[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.connectToNetworkNotificationActionCount, 0, newArray5, 0, i);
                        }
                        while (i < newArray5.length - 1) {
                            newArray5[i] = new ConnectToNetworkNotificationAndActionCount();
                            input.readMessage(newArray5[i]);
                            input.readTag();
                            i++;
                        }
                        newArray5[i] = new ConnectToNetworkNotificationAndActionCount();
                        input.readMessage(newArray5[i]);
                        this.connectToNetworkNotificationActionCount = newArray5;
                        break;
                    case MetricsEvent.ACTION_PERMISSION_DENIED_UNKNOWN /*632*/:
                        this.openNetworkRecommenderBlacklistSize = input.readInt32();
                        break;
                    case 640:
                        this.isWifiNetworksAvailableNotificationOn = input.readBool();
                        break;
                    case MetricsEvent.ACTION_PERMISSION_DENIED_READ_CONTACTS /*648*/:
                        this.numOpenNetworkRecommendationUpdates = input.readInt32();
                        break;
                    case MetricsEvent.ACTION_PERMISSION_DENIED_GET_ACCOUNTS /*656*/:
                        this.numOpenNetworkConnectMessageFailedToSend = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static WifiLog parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (WifiLog) MessageNano.mergeFrom(new WifiLog(), data);
        }

        public static WifiLog parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new WifiLog().mergeFrom(input);
        }
    }

    public static final class WifiScoreCount extends MessageNano {
        private static volatile WifiScoreCount[] _emptyArray;
        public int count;
        public int score;

        public static WifiScoreCount[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new WifiScoreCount[0];
                    }
                }
            }
            return _emptyArray;
        }

        public WifiScoreCount() {
            clear();
        }

        public WifiScoreCount clear() {
            this.score = 0;
            this.count = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.score != 0) {
                output.writeInt32(1, this.score);
            }
            if (this.count != 0) {
                output.writeInt32(2, this.count);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.score != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.score);
            }
            if (this.count != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.count);
            }
            return size;
        }

        public WifiScoreCount mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.score = input.readInt32();
                        break;
                    case 16:
                        this.count = input.readInt32();
                        break;
                    default:
                        if (WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static WifiScoreCount parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (WifiScoreCount) MessageNano.mergeFrom(new WifiScoreCount(), data);
        }

        public static WifiScoreCount parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new WifiScoreCount().mergeFrom(input);
        }
    }
}
