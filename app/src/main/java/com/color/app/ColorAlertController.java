package com.color.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.R;
import com.color.util.ColorContextUtil;
import com.color.util.ColorDialogUtil;
import java.lang.ref.WeakReference;

public class ColorAlertController {
    private static final float TEXT_SIZE_SCALE = 0.88f;
    protected ListAdapter mAdapter;
    protected int mAlertDialogLayout;
    private final OnClickListener mButtonHandler;
    protected Button mButtonNegative;
    protected Message mButtonNegativeMessage;
    protected CharSequence mButtonNegativeText;
    protected Button mButtonNeutral;
    protected Message mButtonNeutralMessage;
    protected CharSequence mButtonNeutralText;
    protected int mButtonPanelLayoutHint;
    protected int mButtonPanelSideLayout;
    protected Button mButtonPositive;
    protected Message mButtonPositiveMessage;
    protected CharSequence mButtonPositiveText;
    protected int mCheckedItem;
    protected final Context mContext;
    protected View mCustomTitleView;
    private int mDeleteDialogOption;
    protected final DialogInterface mDialogInterface;
    protected boolean mForceInverseBackground;
    private Handler mHandler;
    protected Drawable mIcon;
    protected int mIconId;
    protected ImageView mIconView;
    protected int mListItemLayout;
    protected int mListLayout;
    protected ListView mListView;
    protected CharSequence mMessage;
    protected TextView mMessageView;
    protected int mMultiChoiceItemLayout;
    protected ScrollView mScrollView;
    protected int mSingleChoiceItemLayout;
    protected CharSequence mTitle;
    protected TextView mTitleView;
    protected View mView;
    protected int mViewLayoutResId;
    protected int mViewSpacingBottom;
    protected int mViewSpacingLeft;
    protected int mViewSpacingRight;
    protected boolean mViewSpacingSpecified;
    protected int mViewSpacingTop;
    protected final Window mWindow;

    public static class AlertParams {
        public ListAdapter mAdapter;
        public boolean mCancelable;
        public int mCheckedItem = -1;
        public boolean[] mCheckedItems;
        public final Context mContext;
        public Cursor mCursor;
        public View mCustomTitleView;
        public boolean mForceInverseBackground;
        public Drawable mIcon;
        public int mIconAttrId = 0;
        public int mIconId = 0;
        public final LayoutInflater mInflater;
        public String mIsCheckedColumn;
        public boolean mIsMultiChoice;
        public boolean mIsSingleChoice;
        public boolean mIsTitle = false;
        public CharSequence[] mItems;
        public String mLabelColumn;
        public CharSequence mMessage;
        public DialogInterface.OnClickListener mNegativeButtonListener;
        public CharSequence mNegativeButtonText;
        public DialogInterface.OnClickListener mNeutralButtonListener;
        public CharSequence mNeutralButtonText;
        public OnCancelListener mOnCancelListener;
        public OnMultiChoiceClickListener mOnCheckboxClickListener;
        public DialogInterface.OnClickListener mOnClickListener;
        public OnDismissListener mOnDismissListener;
        public OnItemSelectedListener mOnItemSelectedListener;
        public OnKeyListener mOnKeyListener;
        public OnPrepareListViewListener mOnPrepareListViewListener;
        public DialogInterface.OnClickListener mPositiveButtonListener;
        public CharSequence mPositiveButtonText;
        public boolean mRecycleOnMeasure = true;
        public CharSequence mTitle;
        public View mView;
        public int mViewLayoutResId;
        public int mViewSpacingBottom;
        public int mViewSpacingLeft;
        public int mViewSpacingRight;
        public boolean mViewSpacingSpecified = false;
        public int mViewSpacingTop;
        public int[] textColor;

        /* renamed from: com.color.app.ColorAlertController$AlertParams$2 */
        class AnonymousClass2 extends CursorAdapter {
            private final int mIsCheckedIndex;
            private final int mLabelIndex;
            final /* synthetic */ ColorAlertController val$dialog;
            final /* synthetic */ RecycleListView val$listView;

            AnonymousClass2(Context $anonymous0, Cursor $anonymous1, boolean $anonymous2, RecycleListView recycleListView, ColorAlertController colorAlertController) {
                this.val$listView = recycleListView;
                this.val$dialog = colorAlertController;
                super($anonymous0, $anonymous1, $anonymous2);
                Cursor cursor = getCursor();
                this.mLabelIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mLabelColumn);
                this.mIsCheckedIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mIsCheckedColumn);
            }

            public void bindView(View view, Context context, Cursor cursor) {
                boolean z = true;
                ((CheckedTextView) view.findViewById(201458950)).setText(cursor.getString(this.mLabelIndex));
                RecycleListView recycleListView = this.val$listView;
                int position = cursor.getPosition();
                if (cursor.getInt(this.mIsCheckedIndex) != 1) {
                    z = false;
                }
                recycleListView.setItemChecked(position, z);
            }

            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return AlertParams.this.mInflater.inflate(this.val$dialog.mMultiChoiceItemLayout, parent, false);
            }
        }

        /* renamed from: com.color.app.ColorAlertController$AlertParams$4 */
        class AnonymousClass4 implements OnItemClickListener {
            final /* synthetic */ ColorAlertController val$dialog;
            final /* synthetic */ RecycleListView val$listView;

            AnonymousClass4(RecycleListView recycleListView, ColorAlertController colorAlertController) {
                this.val$listView = recycleListView;
                this.val$dialog = colorAlertController;
            }

            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                if (AlertParams.this.mCheckedItems != null) {
                    AlertParams.this.mCheckedItems[position] = this.val$listView.isItemChecked(position);
                }
                AlertParams.this.mOnCheckboxClickListener.onClick(this.val$dialog.mDialogInterface, position, this.val$listView.isItemChecked(position));
            }
        }

        public interface OnPrepareListViewListener {
            void onPrepareListView(ListView listView);
        }

        public AlertParams(Context context) {
            this.mContext = context;
            this.mCancelable = true;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        public void apply(ColorAlertController dialog) {
            if (this.mCustomTitleView != null) {
                dialog.setCustomTitle(this.mCustomTitleView);
            } else {
                if (this.mTitle != null) {
                    dialog.setTitle(this.mTitle);
                }
                if (this.mIcon != null) {
                    dialog.setIcon(this.mIcon);
                }
                if (this.mIconId != 0) {
                    dialog.setIcon(this.mIconId);
                }
                if (this.mIconAttrId != 0) {
                    dialog.setIcon(dialog.getIconAttributeResId(this.mIconAttrId));
                }
            }
            if (this.mMessage != null) {
                dialog.setMessage(this.mMessage);
            }
            if (this.mPositiveButtonText != null) {
                dialog.setButton(-1, this.mPositiveButtonText, this.mPositiveButtonListener, null);
            }
            if (this.mNegativeButtonText != null) {
                dialog.setButton(-2, this.mNegativeButtonText, this.mNegativeButtonListener, null);
            }
            if (this.mNeutralButtonText != null) {
                dialog.setButton(-3, this.mNeutralButtonText, this.mNeutralButtonListener, null);
            }
            if (this.mForceInverseBackground) {
                dialog.setInverseBackgroundForced(true);
            }
            if (!(this.mItems == null && this.mCursor == null && this.mAdapter == null)) {
                createListView(dialog);
            }
            if (this.mView != null) {
                if (this.mViewSpacingSpecified) {
                    dialog.setView(this.mView, this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
                    return;
                }
                dialog.setView(this.mView);
            } else if (this.mViewLayoutResId != 0) {
                dialog.setView(this.mViewLayoutResId);
            }
        }

        protected void createListView(com.color.app.ColorAlertController r19) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r2_1 'adapter' android.widget.ListAdapter) in PHI: PHI: (r2_2 'adapter' android.widget.ListAdapter) = (r2_0 'adapter' android.widget.ListAdapter), (r2_1 'adapter' android.widget.ListAdapter), (r2_3 'adapter' android.widget.ListAdapter), (r2_4 'adapter' android.widget.ListAdapter), (r2_5 'adapter' android.widget.ListAdapter), (r2_6 'adapter' android.widget.ListAdapter), (r2_7 'adapter' android.widget.ListAdapter) binds: {(r2_0 'adapter' android.widget.ListAdapter)=B:23:0x0078, (r2_1 'adapter' android.widget.ListAdapter)=B:39:0x00e1, (r2_3 'adapter' android.widget.ListAdapter)=B:54:0x0129, (r2_4 'adapter' android.widget.ListAdapter)=B:56:0x0134, (r2_5 'adapter' android.widget.ListAdapter)=B:59:0x015d, (r2_6 'adapter' android.widget.ListAdapter)=B:60:0x0163, (r2_7 'adapter' android.widget.ListAdapter)=B:61:0x0175}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
            /*
            r18 = this;
            r0 = r18;
            r3 = r0.mContext;
            r3 = com.color.util.ColorContextUtil.isOppoStyle(r3);
            if (r3 == 0) goto L_0x0018;
        L_0x000a:
            r3 = r19.getDeleteDialogOption();
            r4 = 3;
            if (r3 != r4) goto L_0x0018;
        L_0x0011:
            r3 = 201917545; // 0xc090469 float:1.0555423E-31 double:9.97605223E-316;
            r0 = r19;
            r0.mListLayout = r3;
        L_0x0018:
            r0 = r18;
            r3 = r0.mContext;
            r3 = com.color.util.ColorContextUtil.isOppoStyle(r3);
            if (r3 == 0) goto L_0x0030;
        L_0x0022:
            r3 = r19.getDeleteDialogOption();
            r4 = 2;
            if (r3 != r4) goto L_0x0030;
        L_0x0029:
            r3 = 201917547; // 0xc09046b float:1.0555426E-31 double:9.97605233E-316;
            r0 = r19;
            r0.mListLayout = r3;
        L_0x0030:
            r0 = r18;
            r3 = r0.mInflater;
            r0 = r19;
            r4 = r0.mListLayout;
            r5 = 0;
            r8 = r3.inflate(r4, r5);
            r8 = (com.color.app.ColorAlertController.RecycleListView) r8;
            r0 = r18;
            r3 = r0.mContext;
            r3 = com.color.util.ColorContextUtil.isOppoStyle(r3);
            if (r3 == 0) goto L_0x006c;
        L_0x0049:
            r3 = r19.getDeleteDialogOption();
            r4 = 2;
            if (r3 == r4) goto L_0x0057;
        L_0x0050:
            r3 = r19.getDeleteDialogOption();
            r4 = 3;
            if (r3 != r4) goto L_0x006c;
        L_0x0057:
            r3 = 201720878; // 0xc06042e float:1.0324242E-31 double:9.9663356E-316;
            r8.setSelector(r3);
            r0 = r18;
            r3 = r0.mTitle;
            r3 = android.text.TextUtils.isEmpty(r3);
            if (r3 != 0) goto L_0x00db;
        L_0x0067:
            r3 = 1;
            r0 = r18;
            r0.mIsTitle = r3;
        L_0x006c:
            r0 = r18;
            r3 = r0.mIsMultiChoice;
            if (r3 == 0) goto L_0x00f5;
        L_0x0072:
            r0 = r18;
            r3 = r0.mCursor;
            if (r3 != 0) goto L_0x00e1;
        L_0x0078:
            r2 = new com.color.app.ColorAlertController$AlertParams$1;
            r0 = r18;
            r4 = r0.mContext;
            r0 = r19;
            r5 = r0.mMultiChoiceItemLayout;
            r0 = r18;
            r7 = r0.mItems;
            r6 = 201458950; // 0xc020506 float:1.0016347E-31 double:9.9533946E-316;
            r3 = r18;
            r2.<init>(r4, r5, r6, r7, r8);
        L_0x008e:
            r0 = r18;
            r3 = r0.mOnPrepareListViewListener;
            if (r3 == 0) goto L_0x009b;
        L_0x0094:
            r0 = r18;
            r3 = r0.mOnPrepareListViewListener;
            r3.onPrepareListView(r8);
        L_0x009b:
            r0 = r19;
            r0.mAdapter = r2;
            r0 = r18;
            r3 = r0.mCheckedItem;
            r0 = r19;
            r0.mCheckedItem = r3;
            r0 = r18;
            r3 = r0.mOnClickListener;
            if (r3 == 0) goto L_0x0198;
        L_0x00ad:
            r3 = new com.color.app.ColorAlertController$AlertParams$3;
            r0 = r18;
            r1 = r19;
            r3.<init>(r1);
            r8.setOnItemClickListener(r3);
        L_0x00b9:
            r0 = r18;
            r3 = r0.mOnItemSelectedListener;
            if (r3 == 0) goto L_0x00c6;
        L_0x00bf:
            r0 = r18;
            r3 = r0.mOnItemSelectedListener;
            r8.setOnItemSelectedListener(r3);
        L_0x00c6:
            r0 = r18;
            r3 = r0.mIsSingleChoice;
            if (r3 == 0) goto L_0x01ac;
        L_0x00cc:
            r3 = 1;
            r8.setChoiceMode(r3);
        L_0x00d0:
            r0 = r18;
            r3 = r0.mRecycleOnMeasure;
            r8.mRecycleOnMeasure = r3;
            r0 = r19;
            r0.mListView = r8;
            return;
        L_0x00db:
            r3 = 0;
            r0 = r18;
            r0.mIsTitle = r3;
            goto L_0x006c;
        L_0x00e1:
            r2 = new com.color.app.ColorAlertController$AlertParams$2;
            r0 = r18;
            r5 = r0.mContext;
            r0 = r18;
            r6 = r0.mCursor;
            r7 = 0;
            r3 = r2;
            r4 = r18;
            r9 = r19;
            r3.<init>(r5, r6, r7, r8, r9);
            goto L_0x008e;
        L_0x00f5:
            r0 = r18;
            r3 = r0.mIsSingleChoice;
            if (r3 == 0) goto L_0x012f;
        L_0x00fb:
            r0 = r19;
            r11 = r0.mSingleChoiceItemLayout;
        L_0x00ff:
            r0 = r18;
            r3 = r0.mContext;
            r3 = com.color.util.ColorContextUtil.isOppoStyle(r3);
            if (r3 == 0) goto L_0x0113;
        L_0x0109:
            r3 = r19.getDeleteDialogOption();
            r4 = 3;
            if (r3 != r4) goto L_0x0113;
        L_0x0110:
            r11 = 201917546; // 0xc09046a float:1.0555424E-31 double:9.9760523E-316;
        L_0x0113:
            r0 = r18;
            r3 = r0.mCursor;
            if (r3 != 0) goto L_0x0175;
        L_0x0119:
            r0 = r18;
            r3 = r0.mContext;
            r3 = com.color.util.ColorContextUtil.isOppoStyle(r3);
            if (r3 == 0) goto L_0x0157;
        L_0x0123:
            r0 = r18;
            r3 = r0.mAdapter;
            if (r3 == 0) goto L_0x0134;
        L_0x0129:
            r0 = r18;
            r2 = r0.mAdapter;
            goto L_0x008e;
        L_0x012f:
            r0 = r19;
            r11 = r0.mListItemLayout;
            goto L_0x00ff;
        L_0x0134:
            r2 = new com.color.app.ColorAlertController$CheckedItemAdapter;
            r0 = r18;
            r10 = r0.mContext;
            r0 = r18;
            r13 = r0.mItems;
            r0 = r18;
            r14 = r0.mIsTitle;
            r15 = r19.getDeleteDialogOption();
            r0 = r18;
            r0 = r0.textColor;
            r16 = r0;
            r17 = com.color.app.ColorAlertDialog.LIST_TEXT_COLOR;
            r12 = 201458950; // 0xc020506 float:1.0016347E-31 double:9.9533946E-316;
            r9 = r2;
            r9.<init>(r10, r11, r12, r13, r14, r15, r16, r17);
            goto L_0x008e;
        L_0x0157:
            r0 = r18;
            r3 = r0.mAdapter;
            if (r3 == 0) goto L_0x0163;
        L_0x015d:
            r0 = r18;
            r2 = r0.mAdapter;
            goto L_0x008e;
        L_0x0163:
            r2 = new com.color.app.ColorAlertController$CheckedItemAdapter;
            r0 = r18;
            r3 = r0.mContext;
            r0 = r18;
            r4 = r0.mItems;
            r5 = 201458950; // 0xc020506 float:1.0016347E-31 double:9.9533946E-316;
            r2.<init>(r3, r11, r5, r4);
            goto L_0x008e;
        L_0x0175:
            r2 = new android.widget.SimpleCursorAdapter;
            r0 = r18;
            r3 = r0.mContext;
            r0 = r18;
            r5 = r0.mCursor;
            r4 = 1;
            r6 = new java.lang.String[r4];
            r0 = r18;
            r4 = r0.mLabelColumn;
            r7 = 0;
            r6[r7] = r4;
            r4 = 1;
            r7 = new int[r4];
            r4 = 201458950; // 0xc020506 float:1.0016347E-31 double:9.9533946E-316;
            r9 = 0;
            r7[r9] = r4;
            r4 = r11;
            r2.<init>(r3, r4, r5, r6, r7);
            goto L_0x008e;
        L_0x0198:
            r0 = r18;
            r3 = r0.mOnCheckboxClickListener;
            if (r3 == 0) goto L_0x00b9;
        L_0x019e:
            r3 = new com.color.app.ColorAlertController$AlertParams$4;
            r0 = r18;
            r1 = r19;
            r3.<init>(r8, r1);
            r8.setOnItemClickListener(r3);
            goto L_0x00b9;
        L_0x01ac:
            r0 = r18;
            r3 = r0.mIsMultiChoice;
            if (r3 == 0) goto L_0x00d0;
        L_0x01b2:
            r3 = 2;
            r8.setChoiceMode(r3);
            goto L_0x00d0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.color.app.ColorAlertController.AlertParams.createListView(com.color.app.ColorAlertController):void");
        }
    }

    private static final class ButtonHandler extends Handler {
        private static final int MSG_DISMISS_DIALOG = 1;
        private WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialog) {
            this.mDialog = new WeakReference(dialog);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case -3:
                case -2:
                case -1:
                    ((DialogInterface.OnClickListener) msg.obj).onClick((DialogInterface) this.mDialog.get(), msg.what);
                    return;
                case 1:
                    ((DialogInterface) msg.obj).dismiss();
                    return;
                default:
                    return;
            }
        }
    }

    protected static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        private Context context;
        private boolean isTitle;
        private int[] listColor;
        private int mWarningColor;
        private int option;
        private int[] textColor;
        private int textId;

        public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects, boolean isTitle, int option, int[] textColor, int[] listColor) {
            super(context, resource, textViewResourceId, objects);
            this.isTitle = isTitle;
            this.option = option;
            this.textId = textViewResourceId;
            this.textColor = textColor;
            this.listColor = listColor;
            this.context = context;
            this.mWarningColor = ColorContextUtil.getAttrColor(context, 201392720);
        }

        public boolean hasStableIds() {
            return true;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View item = super.getView(position, convertView, parent);
            TextView textView = null;
            if (item != null) {
                textView = (TextView) item.findViewById(this.textId);
            }
            if (ColorContextUtil.isOppoStyle(this.context) && (this.option == 2 || this.option == 3)) {
                if (this.textColor != null && position >= 0 && position < this.textColor.length && this.listColor != null) {
                    if (this.textColor[position] == this.listColor[0]) {
                        textView.setTextColor(this.context.getColorStateList(201720844));
                    }
                    if (this.textColor[position] == this.listColor[1]) {
                        textView.setTextColor(this.mWarningColor);
                    }
                }
                if (getCount() > 1) {
                    if (position == 0 && (this.isTitle ^ 1) != 0) {
                        item.setBackgroundResource(201852165);
                    } else if (position == getCount() - 1) {
                        item.setBackgroundResource(201852167);
                    } else {
                        item.setBackgroundResource(201852166);
                    }
                } else if (this.isTitle || position != 0) {
                    item.setBackgroundResource(201852167);
                } else {
                    item.setBackgroundResource(201852160);
                }
            }
            return item;
        }
    }

    public static class RecycleListView extends ListView {
        boolean mRecycleOnMeasure = true;

        public RecycleListView(Context context) {
            super(context);
        }

        public RecycleListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public RecycleListView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public RecycleListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        protected boolean recycleOnMeasure() {
            return this.mRecycleOnMeasure;
        }
    }

    private static boolean shouldCenterSingleButton(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(17891338, outValue, true);
        if (outValue.data != 0) {
            return true;
        }
        return false;
    }

    public ColorAlertController(Context context, DialogInterface di, Window window) {
        this.mViewSpacingSpecified = false;
        this.mIconId = 0;
        this.mCheckedItem = -1;
        this.mButtonPanelLayoutHint = 0;
        this.mDeleteDialogOption = 0;
        this.mButtonHandler = new OnClickListener() {
            public void onClick(View v) {
                Message m;
                if (v == ColorAlertController.this.mButtonPositive && ColorAlertController.this.mButtonPositiveMessage != null) {
                    m = Message.obtain(ColorAlertController.this.mButtonPositiveMessage);
                } else if (v == ColorAlertController.this.mButtonNegative && ColorAlertController.this.mButtonNegativeMessage != null) {
                    m = Message.obtain(ColorAlertController.this.mButtonNegativeMessage);
                } else if (v != ColorAlertController.this.mButtonNeutral || ColorAlertController.this.mButtonNeutralMessage == null) {
                    m = null;
                } else {
                    m = Message.obtain(ColorAlertController.this.mButtonNeutralMessage);
                }
                if (m != null) {
                    m.sendToTarget();
                }
                ColorAlertController.this.mHandler.obtainMessage(1, ColorAlertController.this.mDialogInterface).sendToTarget();
            }
        };
        this.mContext = context;
        this.mDialogInterface = di;
        this.mWindow = window;
        this.mHandler = new ButtonHandler(di);
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        this.mAlertDialogLayout = a.getResourceId(10, 201917559);
        this.mButtonPanelSideLayout = a.getResourceId(11, 0);
        this.mListLayout = a.getResourceId(15, 201917555);
        this.mMultiChoiceItemLayout = a.getResourceId(16, 201917556);
        this.mSingleChoiceItemLayout = a.getResourceId(21, 201917557);
        this.mListItemLayout = a.getResourceId(14, 201917558);
        a.recycle();
        this.mWindow.requestFeature(1);
    }

    static boolean canTextInput(View v) {
        if (v.onCheckIsTextEditor()) {
            return true;
        }
        if (!(v instanceof ViewGroup)) {
            return false;
        }
        ViewGroup vg = (ViewGroup) v;
        int i = vg.getChildCount();
        while (i > 0) {
            i--;
            if (canTextInput(vg.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void installContent() {
        this.mWindow.requestFeature(1);
        this.mWindow.setContentView(selectContentView());
        AlertController.updateWindow(this, this.mContext, this.mWindow);
        setupView();
        setupDecor();
        AlertController.setDialogListener(this, this.mContext, this.mWindow, this.mDeleteDialogOption);
    }

    public int selectContentView() {
        if (this.mButtonPanelSideLayout == 0) {
            return this.mAlertDialogLayout;
        }
        if (this.mButtonPanelLayoutHint == 1) {
            return this.mButtonPanelSideLayout;
        }
        return this.mAlertDialogLayout;
    }

    public void setTitle(CharSequence title) {
        this.mTitle = title;
        if (this.mTitleView != null) {
            this.mTitleView.setText(title);
        }
    }

    public void setCustomTitle(View customTitleView) {
        this.mCustomTitleView = customTitleView;
    }

    public void setMessage(CharSequence message) {
        this.mMessage = message;
        if (this.mMessageView != null) {
            this.mMessageView.setText(message);
        }
    }

    public void setView(int layoutResId) {
        this.mView = null;
        this.mViewLayoutResId = layoutResId;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = true;
        this.mViewSpacingLeft = viewSpacingLeft;
        this.mViewSpacingTop = viewSpacingTop;
        this.mViewSpacingRight = viewSpacingRight;
        this.mViewSpacingBottom = viewSpacingBottom;
    }

    public void setButtonPanelLayoutHint(int layoutHint) {
        this.mButtonPanelLayoutHint = layoutHint;
    }

    public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener, Message msg) {
        if (msg == null && listener != null) {
            msg = this.mHandler.obtainMessage(whichButton, listener);
        }
        switch (whichButton) {
            case -3:
                this.mButtonNeutralText = text;
                this.mButtonNeutralMessage = msg;
                return;
            case -2:
                this.mButtonNegativeText = text;
                this.mButtonNegativeMessage = msg;
                return;
            case -1:
                this.mButtonPositiveText = text;
                this.mButtonPositiveMessage = msg;
                return;
            default:
                throw new IllegalArgumentException("Button does not exist");
        }
    }

    public void setIcon(int resId) {
        this.mIcon = null;
        this.mIconId = resId;
        if (this.mIconView == null) {
            return;
        }
        if (resId != 0) {
            this.mIconView.setImageResource(this.mIconId);
        } else {
            this.mIconView.setVisibility(8);
        }
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
        this.mIconId = 0;
        if (this.mIconView == null) {
            return;
        }
        if (icon != null) {
            this.mIconView.setImageDrawable(icon);
        } else {
            this.mIconView.setVisibility(8);
        }
    }

    public int getIconAttributeResId(int attrId) {
        TypedValue out = new TypedValue();
        this.mContext.getTheme().resolveAttribute(attrId, out, true);
        return out.resourceId;
    }

    public void setInverseBackgroundForced(boolean forceInverseBackground) {
        this.mForceInverseBackground = forceInverseBackground;
    }

    public ListView getListView() {
        return this.mListView;
    }

    public Button getButton(int whichButton) {
        switch (whichButton) {
            case -3:
                return this.mButtonNeutral;
            case -2:
                return this.mButtonNegative;
            case -1:
                return this.mButtonPositive;
            default:
                return null;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mScrollView != null ? this.mScrollView.executeKeyEvent(event) : false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mScrollView != null ? this.mScrollView.executeKeyEvent(event) : false;
    }

    private void setupDecor() {
        View decor = this.mWindow.getDecorView();
        final View parent = this.mWindow.findViewById(201458852);
        if (parent != null && decor != null) {
            decor.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    if (insets.isRound()) {
                        int roundOffset = ColorAlertController.this.mContext.getResources().getDimensionPixelOffset(17104932);
                        parent.setPadding(roundOffset, roundOffset, roundOffset, roundOffset);
                    }
                    return insets.consumeSystemWindowInsets();
                }
            });
            decor.setFitsSystemWindows(true);
            decor.requestApplyInsets();
        }
    }

    protected void setupView() {
        View customView;
        ViewGroup contentPanel = (ViewGroup) this.mWindow.findViewById(201458938);
        setupContent(contentPanel);
        boolean hasButtons = setupButtons();
        ViewGroup topPanel = (ViewGroup) this.mWindow.findViewById(201458939);
        TypedArray a = this.mContext.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        boolean hasTitle = setupTitle(topPanel);
        View buttonPanel = this.mWindow.findViewById(201458927);
        if (!hasButtons) {
            buttonPanel.setVisibility(8);
            View spacer = this.mWindow.findViewById(16909355);
            if (spacer != null) {
                spacer.setVisibility(0);
            }
            this.mWindow.setCloseOnTouchOutsideIfNotSet(true);
        }
        FrameLayout customPanel = (FrameLayout) this.mWindow.findViewById(201458942);
        if (this.mView != null) {
            customView = this.mView;
        } else if (this.mViewLayoutResId != 0) {
            customView = LayoutInflater.from(this.mContext).inflate(this.mViewLayoutResId, customPanel, false);
        } else {
            customView = null;
        }
        boolean hasCustomView = customView != null;
        if (!(hasCustomView && (canTextInput(customView) ^ 1) == 0)) {
            this.mWindow.setFlags(131072, 131072);
        }
        if (hasCustomView) {
            FrameLayout custom = (FrameLayout) this.mWindow.findViewById(201458944);
            custom.addView(customView, new LayoutParams(-1, -1));
            if (this.mViewSpacingSpecified) {
                custom.setPadding(this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
            }
            if (this.mListView != null) {
                ((LinearLayout.LayoutParams) customPanel.getLayoutParams()).weight = 0.0f;
            }
        } else {
            customPanel.setVisibility(8);
        }
        if (hasTitle) {
            View divider;
            if (this.mMessage == null && customView == null && this.mListView == null) {
                divider = this.mWindow.findViewById(16909374);
            } else {
                divider = this.mWindow.findViewById(201458779);
            }
            if (divider != null) {
                divider.setVisibility(0);
            }
        }
        setBackground(a, topPanel, contentPanel, customPanel, buttonPanel, hasTitle, hasCustomView, hasButtons);
        a.recycle();
    }

    protected boolean setupTitle(ViewGroup topPanel) {
        if (this.mCustomTitleView != null) {
            topPanel.addView(this.mCustomTitleView, 0, new LayoutParams(-1, -2));
            this.mWindow.findViewById(201458940).setVisibility(8);
            return true;
        }
        this.mIconView = (ImageView) this.mWindow.findViewById(201458740);
        if (TextUtils.isEmpty(this.mTitle) ^ 1) {
            this.mTitleView = (TextView) this.mWindow.findViewById(201458776);
            this.mTitleView.setText(this.mTitle);
            if (this.mIconId != 0) {
                this.mIconView.setImageResource(this.mIconId);
                return true;
            } else if (this.mIcon != null) {
                this.mIconView.setImageDrawable(this.mIcon);
                return true;
            } else {
                this.mTitleView.setPadding(this.mIconView.getPaddingLeft(), this.mIconView.getPaddingTop(), this.mIconView.getPaddingRight(), this.mIconView.getPaddingBottom());
                this.mIconView.setVisibility(8);
                return true;
            }
        }
        this.mWindow.findViewById(201458940).setVisibility(8);
        this.mIconView.setVisibility(8);
        topPanel.setVisibility(8);
        return false;
    }

    protected void setupContent(ViewGroup contentPanel) {
        this.mScrollView = (ScrollView) this.mWindow.findViewById(201458941);
        this.mScrollView.setFocusable(false);
        this.mMessageView = (TextView) this.mWindow.findViewById(201458841);
        if (this.mMessageView != null) {
            if (this.mMessage != null) {
                this.mMessageView.setText(this.mMessage);
            } else {
                this.mMessageView.setVisibility(8);
                this.mScrollView.removeView(this.mMessageView);
                if (this.mListView != null) {
                    ViewGroup scrollParent = (ViewGroup) this.mScrollView.getParent();
                    int childIndex = scrollParent.indexOfChild(this.mScrollView);
                    scrollParent.removeViewAt(childIndex);
                    scrollParent.addView(this.mListView, childIndex, new LayoutParams(-1, -1));
                } else {
                    contentPanel.setVisibility(8);
                }
            }
        }
    }

    private static void manageScrollIndicators(View v, View upIndicator, View downIndicator) {
        int i = 0;
        if (upIndicator != null) {
            upIndicator.setVisibility(v.canScrollVertically(-1) ? 0 : 4);
        }
        if (downIndicator != null) {
            if (!v.canScrollVertically(1)) {
                i = 4;
            }
            downIndicator.setVisibility(i);
        }
    }

    protected boolean setupButtons() {
        int whichButtons = 0;
        this.mButtonPositive = (Button) this.mWindow.findViewById(201458947);
        this.mButtonPositive.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonPositiveText)) {
            this.mButtonPositive.setVisibility(8);
        } else {
            this.mButtonPositive.setText(this.mButtonPositiveText);
            this.mButtonPositive.setVisibility(0);
            whichButtons = 1;
        }
        this.mButtonNegative = (Button) this.mWindow.findViewById(201458945);
        this.mButtonNegative.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNegativeText)) {
            this.mButtonNegative.setVisibility(8);
        } else {
            this.mButtonNegative.setText(this.mButtonNegativeText);
            this.mButtonNegative.setVisibility(0);
            whichButtons |= 2;
        }
        this.mButtonNeutral = (Button) this.mWindow.findViewById(201458946);
        this.mButtonNeutral.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNeutralText)) {
            this.mButtonNeutral.setVisibility(8);
        } else {
            this.mButtonNeutral.setText(this.mButtonNeutralText);
            this.mButtonNeutral.setVisibility(0);
            whichButtons |= 4;
        }
        ColorStateList textFousedColor = this.mContext.getColorStateList(201720844);
        if (!shouldCenterSingleButton(this.mContext)) {
            if (whichButtons == 1) {
                this.mButtonPositive.setTextColor(textFousedColor);
                this.mButtonPositive.setTypeface(null, 0);
            } else if (whichButtons == 2) {
                this.mButtonNegative.setTextColor(textFousedColor);
            } else if (whichButtons == 4) {
                this.mButtonNeutral.setTextColor(textFousedColor);
            }
        }
        if (this.mContext.isOppoStyle()) {
            if (whichButtons == 3) {
                this.mButtonPositive.setTextColor(textFousedColor);
                if (this.mDeleteDialogOption == 0) {
                    this.mButtonPositive.setBackgroundResource(201852175);
                    this.mButtonNegative.setBackgroundResource(201852176);
                }
            } else if (whichButtons == 5) {
                this.mButtonPositive.setTextColor(textFousedColor);
                if (this.mDeleteDialogOption == 0) {
                    this.mButtonPositive.setBackgroundResource(201852175);
                    this.mButtonNeutral.setBackgroundResource(201852176);
                }
            } else if (whichButtons == 6) {
                if (this.mDeleteDialogOption == 0) {
                    this.mButtonNegative.setBackgroundResource(201852176);
                    this.mButtonNeutral.setBackgroundResource(201852175);
                }
            } else if (whichButtons == 7) {
                this.mButtonPositive.setTextColor(textFousedColor);
                if (this.mDeleteDialogOption == 0) {
                    this.mButtonNegative.setBackgroundResource(201852176);
                    this.mButtonNeutral.setBackgroundResource(201851127);
                    this.mButtonPositive.setBackgroundResource(201852175);
                }
            }
        }
        if (this.mContext.isOppoStyle()) {
            final int wButton = whichButtons;
            this.mButtonPositive.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(1, 2, 4) {
                public void onGlobalLayout() {
                    if (ColorAlertController.this.mButtonPositive.getVisibility() == 0) {
                        int buttonPositiveWidth = ColorAlertController.this.mButtonPositive.getWidth() - (ColorAlertController.this.mButtonPositive.getPaddingLeft() + ColorAlertController.this.mButtonPositive.getPaddingRight());
                        int positiveTextWidth = (int) ColorAlertController.this.mButtonPositive.getPaint().measureText(ColorAlertController.this.mButtonPositiveText.toString());
                    }
                    if (ColorAlertController.this.mButtonNegative.getVisibility() == 0) {
                        int buttonNegativeWidth = ColorAlertController.this.mButtonNegative.getWidth() - (ColorAlertController.this.mButtonNegative.getPaddingLeft() + ColorAlertController.this.mButtonNegative.getPaddingRight());
                        int negativeTextWidth = (int) ColorAlertController.this.mButtonNegative.getPaint().measureText(ColorAlertController.this.mButtonNegativeText.toString());
                    }
                    if (ColorAlertController.this.mButtonNeutral.getVisibility() == 0) {
                        int buttonNeutralWidth = ColorAlertController.this.mButtonNeutral.getWidth() - (ColorAlertController.this.mButtonNeutral.getPaddingLeft() + ColorAlertController.this.mButtonNeutral.getPaddingRight());
                        int neutralTextWidth = (int) ColorAlertController.this.mButtonNeutral.getPaint().measureText(ColorAlertController.this.mButtonNeutralText.toString());
                    }
                    ColorAlertController.this.mButtonPositive.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    if (ColorAlertController.this.mDeleteDialogOption <= 0 && ColorAlertController.this.mDeleteDialogOption == 0 && wButton != 1 && wButton != 2) {
                        int i = wButton;
                        int i2 = 4;
                    }
                }
            });
        }
        if (whichButtons != 0) {
            return true;
        }
        return false;
    }

    protected void centerButton(Button button) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.gravity = 1;
        params.weight = 0.5f;
        button.setLayoutParams(params);
        View leftSpacer = this.mWindow.findViewById(201458948);
        if (leftSpacer != null) {
            leftSpacer.setVisibility(0);
        }
        View rightSpacer = this.mWindow.findViewById(201458949);
        if (rightSpacer != null) {
            rightSpacer.setVisibility(0);
        }
    }

    protected void setBackground(TypedArray a, View topPanel, View contentPanel, View customPanel, View buttonPanel, boolean hasTitle, boolean hasCustomView, boolean hasButtons) {
        View view;
        int i;
        int fullDark = 0;
        int topDark = 0;
        int centerDark = 0;
        int bottomDark = 0;
        int fullBright = 0;
        int topBright = 0;
        int centerBright = 0;
        int bottomBright = 0;
        int bottomMedium = 0;
        if (a.getBoolean(17, true)) {
            fullDark = 17303169;
            topDark = 17303183;
            centerDark = 17303166;
            bottomDark = 17303163;
            fullBright = 17303168;
            topBright = 17303182;
            centerBright = 17303165;
            bottomBright = 17303162;
            bottomMedium = 17303164;
        }
        topBright = a.getResourceId(5, topBright);
        topDark = a.getResourceId(1, topDark);
        centerBright = a.getResourceId(6, centerBright);
        centerDark = a.getResourceId(2, centerDark);
        View[] views = new View[4];
        boolean[] light = new boolean[4];
        View lastView = null;
        boolean lastLight = false;
        int pos = 0;
        if (hasTitle) {
            views[0] = topPanel;
            light[0] = false;
            pos = 1;
        }
        if (contentPanel.getVisibility() == 8) {
            view = null;
        } else {
            view = contentPanel;
        }
        views[pos] = view;
        light[pos] = this.mListView != null;
        pos++;
        if (hasCustomView) {
            views[pos] = customPanel;
            light[pos] = this.mForceInverseBackground;
            pos++;
        }
        if (hasButtons) {
            views[pos] = buttonPanel;
            light[pos] = true;
        }
        boolean setView = false;
        for (pos = 0; pos < views.length; pos++) {
            View v = views[pos];
            if (v != null) {
                if (lastView != null) {
                    if (setView) {
                        lastView.setBackgroundResource(lastLight ? centerBright : centerDark);
                    } else {
                        if (lastLight) {
                            i = topBright;
                        } else {
                            i = topDark;
                        }
                        lastView.setBackgroundResource(i);
                    }
                    setView = true;
                }
                lastView = v;
                lastLight = light[pos];
            }
        }
        if (lastView != null) {
            if (setView) {
                bottomBright = a.getResourceId(7, bottomBright);
                if (this.mDeleteDialogOption == 0) {
                    bottomMedium = a.getResourceId(8, bottomMedium);
                } else {
                    bottomMedium = 201850909;
                }
                i = lastLight ? hasButtons ? bottomMedium : bottomBright : a.getResourceId(3, bottomDark);
                lastView.setBackgroundResource(i);
            } else {
                lastView.setBackgroundResource(lastLight ? a.getResourceId(4, fullBright) : a.getResourceId(0, fullDark));
            }
        }
        ListView listView = this.mListView;
        if (!(listView == null || this.mAdapter == null)) {
            listView.setAdapter(this.mAdapter);
            int checkedItem = this.mCheckedItem;
            if (checkedItem > -1) {
                listView.setItemChecked(checkedItem, true);
                listView.setSelection(checkedItem);
            }
        }
        if (this.mDeleteDialogOption == 2 || this.mDeleteDialogOption == 3) {
            View view2;
            if (buttonPanel != null) {
                buttonPanel.setBackground(null);
            }
            boolean hasTop = topPanel != null && topPanel.getVisibility() == 0;
            if (contentPanel == null || contentPanel.getVisibility() != 0) {
            }
            if (customPanel == null || customPanel.getVisibility() != 0) {
            }
            if (this.mButtonPositive == null || this.mButtonPositive.getVisibility() != 0) {
            }
            if (this.mButtonNeutral == null || this.mButtonNeutral.getVisibility() != 0) {
            }
            View[] allViews = new View[]{topPanel, contentPanel, customPanel, this.mButtonPositive, this.mButtonNeutral};
            int bgTop = hasTop ? 201852168 : 201852161;
            int positionTop = -1;
            int positionBottom = -1;
            for (int i2 = 0; i2 < allViews.length; i2++) {
                view2 = allViews[i2];
                if (view2 != null && view2.getVisibility() == 0) {
                    positionTop = i2;
                    break;
                }
            }
            for (int j = allViews.length - 1; j >= 0; j--) {
                view2 = allViews[j];
                if (view2 != null && view2.getVisibility() == 0) {
                    positionBottom = j;
                    break;
                }
            }
            if (positionTop == positionBottom && positionTop != -1) {
                setBackgoundButKeepPadding(allViews[positionTop], 201852160);
            } else if (!(positionTop == positionBottom && positionTop == -1)) {
                setBackgoundButKeepPadding(allViews[positionTop], bgTop);
                setBackgoundButKeepPadding(allViews[positionBottom], 201852163);
                for (int k = positionTop + 1; k < positionBottom; k++) {
                    setBackgoundButKeepPadding(allViews[k], 201852162);
                }
            }
            if (contentPanel != null && this.mDeleteDialogOption == 3) {
                allViews[positionBottom].setPadding(0, 0, 0, 0);
            }
        }
    }

    public void setDeleteDialogOption(int delete) {
        this.mDeleteDialogOption = delete;
        ColorDialogUtil.setDeleteDialogOption(delete);
    }

    public int getDeleteDialogOption() {
        return this.mDeleteDialogOption;
    }

    public ColorAlertController(Context context, DialogInterface di, Window window, int deleteDialogOption) {
        this(context, di, window);
        setDeleteDialogOption(deleteDialogOption);
        if (this.mDeleteDialogOption == 1) {
            this.mAlertDialogLayout = 201917542;
        } else if (this.mDeleteDialogOption == 2) {
            this.mAlertDialogLayout = 201917543;
        } else if (this.mDeleteDialogOption == 3) {
            this.mAlertDialogLayout = 201917544;
        }
    }

    public void initializeDialog() {
        this.mWindow.setContentView(this.mAlertDialogLayout);
        setupView();
        new ColorDialogUtil(this.mContext).setDialogButtonFlag(this.mWindow, 8, this.mDeleteDialogOption);
    }

    private void setBackgoundButKeepPadding(View view, int bg) {
        if (view != null) {
            int pddingLeft = view.getPaddingLeft();
            int paddingTop = view.getPaddingTop();
            int paddingRight = view.getPaddingRight();
            int paddingBottom = view.getPaddingBottom();
            view.setBackgroundResource(bg);
            view.setPadding(pddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }

    private static void changeButtonTextSize(Button btn, int maxWidth, CharSequence text) {
    }
}
