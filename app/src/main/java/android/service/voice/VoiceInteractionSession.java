package android.service.voice;

import android.app.Dialog;
import android.app.Instrumentation;
import android.app.VoiceInteractor.PickOptionRequest.Option;
import android.app.VoiceInteractor.Prompt;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.SoftInputWindow;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.SettingsStringUtil;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.KeyEvent.Callback;
import android.view.KeyEvent.DispatcherState;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import com.android.internal.R;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;
import com.android.internal.app.IVoiceInteractorRequest.Stub;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

public class VoiceInteractionSession implements Callback, ComponentCallbacks2 {
    static final boolean DEBUG = false;
    public static final String KEY_CONTENT = "content";
    public static final String KEY_DATA = "data";
    public static final String KEY_RECEIVER_EXTRAS = "receiverExtras";
    public static final String KEY_STRUCTURE = "structure";
    static final int MSG_CANCEL = 7;
    static final int MSG_CLOSE_SYSTEM_DIALOGS = 102;
    static final int MSG_DESTROY = 103;
    static final int MSG_HANDLE_ASSIST = 104;
    static final int MSG_HANDLE_SCREENSHOT = 105;
    static final int MSG_HIDE = 107;
    static final int MSG_ON_LOCKSCREEN_SHOWN = 108;
    static final int MSG_SHOW = 106;
    static final int MSG_START_ABORT_VOICE = 4;
    static final int MSG_START_COMMAND = 5;
    static final int MSG_START_COMPLETE_VOICE = 3;
    static final int MSG_START_CONFIRMATION = 1;
    static final int MSG_START_PICK_OPTION = 2;
    static final int MSG_SUPPORTS_COMMANDS = 6;
    static final int MSG_TASK_FINISHED = 101;
    static final int MSG_TASK_STARTED = 100;
    public static final int SHOW_SOURCE_ACTIVITY = 16;
    public static final int SHOW_SOURCE_APPLICATION = 8;
    public static final int SHOW_SOURCE_ASSIST_GESTURE = 4;
    public static final int SHOW_WITH_ASSIST = 1;
    public static final int SHOW_WITH_SCREENSHOT = 2;
    static final String TAG = "VoiceInteractionSession";
    final ArrayMap<IBinder, Request> mActiveRequests;
    final MyCallbacks mCallbacks;
    FrameLayout mContentFrame;
    final Context mContext;
    final DispatcherState mDispatcherState;
    final HandlerCaller mHandlerCaller;
    boolean mInShowWindow;
    LayoutInflater mInflater;
    boolean mInitialized;
    final OnComputeInternalInsetsListener mInsetsComputer;
    final IVoiceInteractor mInteractor;
    View mRootView;
    final IVoiceInteractionSession mSession;
    IVoiceInteractionManagerService mSystemService;
    int mTheme;
    TypedArray mThemeAttrs;
    final Insets mTmpInsets;
    IBinder mToken;
    boolean mUiEnabled;
    final WeakReference<VoiceInteractionSession> mWeakRef;
    SoftInputWindow mWindow;
    boolean mWindowAdded;
    boolean mWindowVisible;
    boolean mWindowWasVisible;

    public static class Request {
        final IVoiceInteractorCallback mCallback;
        final String mCallingPackage;
        final int mCallingUid;
        final Bundle mExtras;
        final IVoiceInteractorRequest mInterface = new Stub() {
            public void cancel() throws RemoteException {
                VoiceInteractionSession session = (VoiceInteractionSession) Request.this.mSession.get();
                if (session != null) {
                    session.mHandlerCaller.sendMessage(session.mHandlerCaller.obtainMessageO(7, Request.this));
                }
            }
        };
        final WeakReference<VoiceInteractionSession> mSession;

        Request(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Bundle extras) {
            this.mCallingPackage = packageName;
            this.mCallingUid = uid;
            this.mCallback = callback;
            this.mSession = session.mWeakRef;
            this.mExtras = extras;
        }

        public int getCallingUid() {
            return this.mCallingUid;
        }

        public String getCallingPackage() {
            return this.mCallingPackage;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public boolean isActive() {
            VoiceInteractionSession session = (VoiceInteractionSession) this.mSession.get();
            if (session == null) {
                return false;
            }
            return session.isRequestActive(this.mInterface.asBinder());
        }

        void finishRequest() {
            VoiceInteractionSession session = (VoiceInteractionSession) this.mSession.get();
            if (session == null) {
                throw new IllegalStateException("VoiceInteractionSession has been destroyed");
            }
            Request req = session.removeRequest(this.mInterface.asBinder());
            if (req == null) {
                throw new IllegalStateException("Request not active: " + this);
            } else if (req != this) {
                throw new IllegalStateException("Current active request " + req + " not same as calling request " + this);
            }
        }

        public void cancel() {
            try {
                finishRequest();
                this.mCallback.deliverCancel(this.mInterface);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            DebugUtils.buildShortClassTag(this, sb);
            sb.append(" ");
            sb.append(this.mInterface.asBinder());
            sb.append(" pkg=");
            sb.append(this.mCallingPackage);
            sb.append(" uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append('}');
            return sb.toString();
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix);
            writer.print("mInterface=");
            writer.println(this.mInterface.asBinder());
            writer.print(prefix);
            writer.print("mCallingPackage=");
            writer.print(this.mCallingPackage);
            writer.print(" mCallingUid=");
            UserHandle.formatUid(writer, this.mCallingUid);
            writer.println();
            writer.print(prefix);
            writer.print("mCallback=");
            writer.println(this.mCallback.asBinder());
            if (this.mExtras != null) {
                writer.print(prefix);
                writer.print("mExtras=");
                writer.println(this.mExtras);
            }
        }
    }

    public static final class AbortVoiceRequest extends Request {
        final Prompt mPrompt;

        AbortVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        @Deprecated
        public CharSequence getMessage() {
            return this.mPrompt != null ? this.mPrompt.getVoicePromptAt(0) : null;
        }

        public void sendAbortResult(Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverAbortVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class CommandRequest extends Request {
        final String mCommand;

        CommandRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, String command, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mCommand = command;
        }

        public String getCommand() {
            return this.mCommand;
        }

        void sendCommandResult(boolean finished, Bundle result) {
            if (finished) {
                try {
                    finishRequest();
                } catch (RemoteException e) {
                    return;
                }
            }
            this.mCallback.deliverCommandResult(this.mInterface, finished, result);
        }

        public void sendIntermediateResult(Bundle result) {
            sendCommandResult(false, result);
        }

        public void sendResult(Bundle result) {
            sendCommandResult(true, result);
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mCommand=");
            writer.println(this.mCommand);
        }
    }

    public static final class CompleteVoiceRequest extends Request {
        final Prompt mPrompt;

        CompleteVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        @Deprecated
        public CharSequence getMessage() {
            return this.mPrompt != null ? this.mPrompt.getVoicePromptAt(0) : null;
        }

        public void sendCompleteResult(Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverCompleteVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class ConfirmationRequest extends Request {
        final Prompt mPrompt;

        ConfirmationRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        @Deprecated
        public CharSequence getPrompt() {
            return this.mPrompt != null ? this.mPrompt.getVoicePromptAt(0) : null;
        }

        public void sendConfirmationResult(boolean confirmed, Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverConfirmationResult(this.mInterface, confirmed, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class Insets {
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_REGION = 3;
        public final Rect contentInsets = new Rect();
        public int touchableInsets;
        public final Region touchableRegion = new Region();
    }

    class MyCallbacks implements HandlerCaller.Callback, SoftInputWindow.Callback {
        MyCallbacks() {
        }

        public void executeMessage(Message msg) {
            SomeArgs args = null;
            switch (msg.what) {
                case 1:
                    VoiceInteractionSession.this.onRequestConfirmation((ConfirmationRequest) msg.obj);
                    break;
                case 2:
                    VoiceInteractionSession.this.onRequestPickOption((PickOptionRequest) msg.obj);
                    break;
                case 3:
                    VoiceInteractionSession.this.onRequestCompleteVoice((CompleteVoiceRequest) msg.obj);
                    break;
                case 4:
                    VoiceInteractionSession.this.onRequestAbortVoice((AbortVoiceRequest) msg.obj);
                    break;
                case 5:
                    VoiceInteractionSession.this.onRequestCommand((CommandRequest) msg.obj);
                    break;
                case 6:
                    args = msg.obj;
                    args.arg1 = VoiceInteractionSession.this.onGetSupportedCommands((String[]) args.arg1);
                    args.complete();
                    args = null;
                    break;
                case 7:
                    VoiceInteractionSession.this.onCancelRequest((Request) msg.obj);
                    break;
                case 100:
                    VoiceInteractionSession.this.onTaskStarted((Intent) msg.obj, msg.arg1);
                    break;
                case 101:
                    VoiceInteractionSession.this.onTaskFinished((Intent) msg.obj, msg.arg1);
                    break;
                case 102:
                    VoiceInteractionSession.this.onCloseSystemDialogs();
                    break;
                case 103:
                    VoiceInteractionSession.this.doDestroy();
                    break;
                case 104:
                    args = msg.obj;
                    if (args.argi5 != 0) {
                        VoiceInteractionSession.this.doOnHandleAssistSecondary((Bundle) args.arg1, (AssistStructure) args.arg2, (Throwable) args.arg3, (AssistContent) args.arg4, args.argi5, args.argi6);
                        break;
                    } else {
                        VoiceInteractionSession.this.doOnHandleAssist((Bundle) args.arg1, (AssistStructure) args.arg2, (Throwable) args.arg3, (AssistContent) args.arg4);
                        break;
                    }
                case 105:
                    VoiceInteractionSession.this.onHandleScreenshot((Bitmap) msg.obj);
                    break;
                case 106:
                    args = msg.obj;
                    VoiceInteractionSession.this.doShow((Bundle) args.arg1, msg.arg1, (IVoiceInteractionSessionShowCallback) args.arg2);
                    break;
                case 107:
                    VoiceInteractionSession.this.doHide();
                    break;
                case 108:
                    VoiceInteractionSession.this.onLockscreenShown();
                    break;
            }
            if (args != null) {
                args.recycle();
            }
        }

        public void onBackPressed() {
            VoiceInteractionSession.this.onBackPressed();
        }
    }

    public static final class PickOptionRequest extends Request {
        final Option[] mOptions;
        final Prompt mPrompt;

        PickOptionRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Prompt prompt, Option[] options, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
            this.mOptions = options;
        }

        public Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        @Deprecated
        public CharSequence getPrompt() {
            return this.mPrompt != null ? this.mPrompt.getVoicePromptAt(0) : null;
        }

        public Option[] getOptions() {
            return this.mOptions;
        }

        void sendPickOptionResult(boolean finished, Option[] selections, Bundle result) {
            if (finished) {
                try {
                    finishRequest();
                } catch (RemoteException e) {
                    return;
                }
            }
            this.mCallback.deliverPickOptionResult(this.mInterface, finished, selections, result);
        }

        public void sendIntermediatePickOptionResult(Option[] selections, Bundle result) {
            sendPickOptionResult(false, selections, result);
        }

        public void sendPickOptionResult(Option[] selections, Bundle result) {
            sendPickOptionResult(true, selections, result);
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
            if (this.mOptions != null) {
                writer.print(prefix);
                writer.println("Options:");
                for (int i = 0; i < this.mOptions.length; i++) {
                    Option op = this.mOptions[i];
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.println(SettingsStringUtil.DELIMITER);
                    writer.print(prefix);
                    writer.print("    mLabel=");
                    writer.println(op.getLabel());
                    writer.print(prefix);
                    writer.print("    mIndex=");
                    writer.println(op.getIndex());
                    if (op.countSynonyms() > 0) {
                        writer.print(prefix);
                        writer.println("    Synonyms:");
                        for (int j = 0; j < op.countSynonyms(); j++) {
                            writer.print(prefix);
                            writer.print("      #");
                            writer.print(j);
                            writer.print(": ");
                            writer.println(op.getSynonymAt(j));
                        }
                    }
                    if (op.getExtras() != null) {
                        writer.print(prefix);
                        writer.print("    mExtras=");
                        writer.println(op.getExtras());
                    }
                }
            }
        }
    }

    public VoiceInteractionSession(Context context) {
        this(context, new Handler());
    }

    public VoiceInteractionSession(Context context, Handler handler) {
        this.mDispatcherState = new DispatcherState();
        this.mTheme = 0;
        this.mUiEnabled = true;
        this.mActiveRequests = new ArrayMap();
        this.mTmpInsets = new Insets();
        this.mWeakRef = new WeakReference(this);
        this.mInteractor = new IVoiceInteractor.Stub() {
            public IVoiceInteractorRequest startConfirmation(String callingPackage, IVoiceInteractorCallback callback, Prompt prompt, Bundle extras) {
                ConfirmationRequest request = new ConfirmationRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, prompt, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(1, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startPickOption(String callingPackage, IVoiceInteractorCallback callback, Prompt prompt, Option[] options, Bundle extras) {
                PickOptionRequest request = new PickOptionRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, prompt, options, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(2, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startCompleteVoice(String callingPackage, IVoiceInteractorCallback callback, Prompt message, Bundle extras) {
                CompleteVoiceRequest request = new CompleteVoiceRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, message, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(3, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startAbortVoice(String callingPackage, IVoiceInteractorCallback callback, Prompt message, Bundle extras) {
                AbortVoiceRequest request = new AbortVoiceRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, message, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(4, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startCommand(String callingPackage, IVoiceInteractorCallback callback, String command, Bundle extras) {
                CommandRequest request = new CommandRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, command, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(5, request));
                return request.mInterface;
            }

            public boolean[] supportsCommands(String callingPackage, String[] commands) {
                SomeArgs args = VoiceInteractionSession.this.mHandlerCaller.sendMessageAndWait(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIOO(6, 0, commands, null));
                if (args == null) {
                    return new boolean[commands.length];
                }
                boolean[] res = args.arg1;
                args.recycle();
                return res;
            }
        };
        this.mSession = new IVoiceInteractionSession.Stub() {
            public void show(Bundle sessionArgs, int flags, IVoiceInteractionSessionShowCallback showCallback) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIOO(106, flags, sessionArgs, showCallback));
            }

            public void hide() {
                VoiceInteractionSession.this.mHandlerCaller.removeMessages(106);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(107));
            }

            public void handleAssist(Bundle data, AssistStructure structure, AssistContent content, int index, int count) {
                final AssistStructure assistStructure = structure;
                final Bundle bundle = data;
                final AssistContent assistContent = content;
                final int i = index;
                final int i2 = count;
                new Thread("AssistStructure retriever") {
                    public void run() {
                        Object obj = null;
                        Object failure = null;
                        if (assistStructure != null) {
                            try {
                                assistStructure.ensureData();
                            } catch (Throwable e) {
                                Log.w(VoiceInteractionSession.TAG, "Failure retrieving AssistStructure", e);
                                Throwable failure2 = e;
                            }
                        }
                        HandlerCaller handlerCaller = VoiceInteractionSession.this.mHandlerCaller;
                        HandlerCaller handlerCaller2 = VoiceInteractionSession.this.mHandlerCaller;
                        Bundle bundle = bundle;
                        if (failure2 == null) {
                            obj = assistStructure;
                        }
                        handlerCaller.sendMessage(handlerCaller2.obtainMessageOOOOII(104, bundle, obj, failure2, assistContent, i, i2));
                    }
                }.start();
            }

            public void handleScreenshot(Bitmap screenshot) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(105, screenshot));
            }

            public void taskStarted(Intent intent, int taskId) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIO(100, taskId, intent));
            }

            public void taskFinished(Intent intent, int taskId) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIO(101, taskId, intent));
            }

            public void closeSystemDialogs() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(102));
            }

            public void onLockscreenShown() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(108));
            }

            public void destroy() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(103));
            }
        };
        this.mCallbacks = new MyCallbacks();
        this.mInsetsComputer = new OnComputeInternalInsetsListener() {
            public void onComputeInternalInsets(InternalInsetsInfo info) {
                VoiceInteractionSession.this.onComputeInsets(VoiceInteractionSession.this.mTmpInsets);
                info.contentInsets.set(VoiceInteractionSession.this.mTmpInsets.contentInsets);
                info.visibleInsets.set(VoiceInteractionSession.this.mTmpInsets.contentInsets);
                info.touchableRegion.set(VoiceInteractionSession.this.mTmpInsets.touchableRegion);
                info.setTouchableInsets(VoiceInteractionSession.this.mTmpInsets.touchableInsets);
            }
        };
        this.mContext = context;
        this.mHandlerCaller = new HandlerCaller(context, handler.getLooper(), this.mCallbacks, true);
    }

    public Context getContext() {
        return this.mContext;
    }

    void addRequest(Request req) {
        synchronized (this) {
            this.mActiveRequests.put(req.mInterface.asBinder(), req);
        }
    }

    boolean isRequestActive(IBinder reqInterface) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mActiveRequests.containsKey(reqInterface);
        }
        return containsKey;
    }

    Request removeRequest(IBinder reqInterface) {
        Request request;
        synchronized (this) {
            request = (Request) this.mActiveRequests.remove(reqInterface);
        }
        return request;
    }

    void doCreate(IVoiceInteractionManagerService service, IBinder token) {
        this.mSystemService = service;
        this.mToken = token;
        onCreate();
    }

    void doShow(Bundle args, int flags, final IVoiceInteractionSessionShowCallback showCallback) {
        if (this.mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        try {
            this.mInShowWindow = true;
            onPrepareShow(args, flags);
            if (!this.mWindowVisible) {
                ensureWindowAdded();
            }
            onShow(args, flags);
            if (!this.mWindowVisible) {
                this.mWindowVisible = true;
                if (this.mUiEnabled) {
                    this.mWindow.show();
                }
            }
            if (showCallback != null) {
                if (this.mUiEnabled) {
                    this.mRootView.invalidate();
                    this.mRootView.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                        public boolean onPreDraw() {
                            VoiceInteractionSession.this.mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                            try {
                                showCallback.onShown();
                            } catch (RemoteException e) {
                                Log.w(VoiceInteractionSession.TAG, "Error calling onShown", e);
                            }
                            return true;
                        }
                    });
                } else {
                    showCallback.onShown();
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Error calling onShown", e);
        } catch (Throwable th) {
            this.mWindowWasVisible = true;
            this.mInShowWindow = false;
        }
        this.mWindowWasVisible = true;
        this.mInShowWindow = false;
    }

    void doHide() {
        if (this.mWindowVisible) {
            ensureWindowHidden();
            this.mWindowVisible = false;
            onHide();
        }
    }

    void doDestroy() {
        onDestroy();
        if (this.mInitialized) {
            this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
            if (this.mWindowAdded) {
                this.mWindow.dismiss();
                this.mWindowAdded = false;
            }
            this.mInitialized = false;
        }
    }

    void ensureWindowCreated() {
        if (!this.mInitialized) {
            if (this.mUiEnabled) {
                this.mInitialized = true;
                this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
                this.mWindow = new SoftInputWindow(this.mContext, TAG, this.mTheme, this.mCallbacks, this, this.mDispatcherState, LayoutParams.TYPE_VOICE_INTERACTION, 80, true);
                this.mWindow.getWindow().addFlags(R.attr.transcriptMode);
                this.mThemeAttrs = this.mContext.obtainStyledAttributes(android.R.styleable.VoiceInteractionSession);
                this.mRootView = this.mInflater.inflate((int) R.layout.voice_interaction_session, null);
                this.mRootView.setSystemUiVisibility(1792);
                this.mWindow.setContentView(this.mRootView);
                this.mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsComputer);
                this.mContentFrame = (FrameLayout) this.mRootView.findViewById(16908290);
                this.mWindow.getWindow().setLayout(-1, -1);
                this.mWindow.setToken(this.mToken);
                return;
            }
            throw new IllegalStateException("setUiEnabled is false");
        }
    }

    void ensureWindowAdded() {
        if (this.mUiEnabled && (this.mWindowAdded ^ 1) != 0) {
            this.mWindowAdded = true;
            ensureWindowCreated();
            View v = onCreateContentView();
            if (v != null) {
                setContentView(v);
            }
        }
    }

    void ensureWindowHidden() {
        if (this.mWindow != null) {
            this.mWindow.hide();
        }
    }

    public void setDisabledShowContext(int flags) {
        try {
            this.mSystemService.setDisabledShowContext(flags);
        } catch (RemoteException e) {
        }
    }

    public int getDisabledShowContext() {
        try {
            return this.mSystemService.getDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getUserDisabledShowContext() {
        try {
            return this.mSystemService.getUserDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void show(Bundle args, int flags) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.showSessionFromSession(this.mToken, args, flags);
        } catch (RemoteException e) {
        }
    }

    public void hide() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.hideSessionFromSession(this.mToken);
        } catch (RemoteException e) {
        }
    }

    public void setUiEnabled(boolean enabled) {
        if (this.mUiEnabled != enabled) {
            this.mUiEnabled = enabled;
            if (!this.mWindowVisible) {
                return;
            }
            if (enabled) {
                ensureWindowAdded();
                this.mWindow.show();
                return;
            }
            ensureWindowHidden();
        }
    }

    public void setTheme(int theme) {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        this.mTheme = theme;
    }

    public void startVoiceActivity(Intent intent) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(this.mContext);
            Instrumentation.checkStartActivityResult(this.mSystemService.startVoiceActivity(this.mToken, intent, intent.resolveType(this.mContext.getContentResolver())), intent);
        } catch (RemoteException e) {
        }
    }

    public void startAssistantActivity(Intent intent) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(this.mContext);
            Instrumentation.checkStartActivityResult(this.mSystemService.startAssistantActivity(this.mToken, intent, intent.resolveType(this.mContext.getContentResolver())), intent);
        } catch (RemoteException e) {
        }
    }

    public void setKeepAwake(boolean keepAwake) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.setKeepAwake(this.mToken, keepAwake);
        } catch (RemoteException e) {
        }
    }

    public void closeSystemDialogs() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.closeSystemDialogs(this.mToken);
        } catch (RemoteException e) {
        }
    }

    public LayoutInflater getLayoutInflater() {
        ensureWindowCreated();
        return this.mInflater;
    }

    public Dialog getWindow() {
        ensureWindowCreated();
        return this.mWindow;
    }

    public void finish() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.finish(this.mToken);
        } catch (RemoteException e) {
        }
    }

    public void onCreate() {
        doOnCreate();
    }

    private void doOnCreate() {
        int i;
        if (this.mTheme != 0) {
            i = this.mTheme;
        } else {
            i = R.style.Theme_DeviceDefault_VoiceInteractionSession;
        }
        this.mTheme = i;
    }

    public void onPrepareShow(Bundle args, int showFlags) {
    }

    public void onShow(Bundle args, int showFlags) {
    }

    public void onHide() {
    }

    public void onDestroy() {
    }

    public View onCreateContentView() {
        return null;
    }

    public void setContentView(View view) {
        ensureWindowCreated();
        this.mContentFrame.removeAllViews();
        this.mContentFrame.addView(view, new FrameLayout.LayoutParams(-1, -1));
        this.mContentFrame.requestApplyInsets();
    }

    void doOnHandleAssist(Bundle data, AssistStructure structure, Throwable failure, AssistContent content) {
        if (failure != null) {
            onAssistStructureFailure(failure);
        }
        onHandleAssist(data, structure, content);
    }

    void doOnHandleAssistSecondary(Bundle data, AssistStructure structure, Throwable failure, AssistContent content, int index, int count) {
        if (failure != null) {
            onAssistStructureFailure(failure);
        }
        onHandleAssistSecondary(data, structure, content, index, count);
    }

    public void onAssistStructureFailure(Throwable failure) {
    }

    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
    }

    public void onHandleAssistSecondary(Bundle data, AssistStructure structure, AssistContent content, int index, int count) {
    }

    public void onHandleScreenshot(Bitmap screenshot) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    public void onBackPressed() {
        hide();
    }

    public void onCloseSystemDialogs() {
        hide();
    }

    public void onLockscreenShown() {
        hide();
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public void onComputeInsets(Insets outInsets) {
        outInsets.contentInsets.left = 0;
        outInsets.contentInsets.bottom = 0;
        outInsets.contentInsets.right = 0;
        View decor = getWindow().getWindow().getDecorView();
        outInsets.contentInsets.top = decor.getHeight();
        outInsets.touchableInsets = 0;
        outInsets.touchableRegion.setEmpty();
    }

    public void onTaskStarted(Intent intent, int taskId) {
    }

    public void onTaskFinished(Intent intent, int taskId) {
        hide();
    }

    public boolean[] onGetSupportedCommands(String[] commands) {
        return new boolean[commands.length];
    }

    public void onRequestConfirmation(ConfirmationRequest request) {
    }

    public void onRequestPickOption(PickOptionRequest request) {
    }

    public void onRequestCompleteVoice(CompleteVoiceRequest request) {
    }

    public void onRequestAbortVoice(AbortVoiceRequest request) {
    }

    public void onRequestCommand(CommandRequest request) {
    }

    public void onCancelRequest(Request request) {
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix);
        writer.print("mToken=");
        writer.println(this.mToken);
        writer.print(prefix);
        writer.print("mTheme=#");
        writer.println(Integer.toHexString(this.mTheme));
        writer.print(prefix);
        writer.print("mUiEnabled=");
        writer.println(this.mUiEnabled);
        writer.print(" mInitialized=");
        writer.println(this.mInitialized);
        writer.print(prefix);
        writer.print("mWindowAdded=");
        writer.print(this.mWindowAdded);
        writer.print(" mWindowVisible=");
        writer.println(this.mWindowVisible);
        writer.print(prefix);
        writer.print("mWindowWasVisible=");
        writer.print(this.mWindowWasVisible);
        writer.print(" mInShowWindow=");
        writer.println(this.mInShowWindow);
        if (this.mActiveRequests.size() > 0) {
            writer.print(prefix);
            writer.println("Active requests:");
            String innerPrefix = prefix + "    ";
            for (int i = 0; i < this.mActiveRequests.size(); i++) {
                Request req = (Request) this.mActiveRequests.valueAt(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(req);
                req.dump(innerPrefix, fd, writer, args);
            }
        }
    }
}
