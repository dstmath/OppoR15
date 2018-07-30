package com.android.internal.app;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.internal.R;
import com.color.util.ColorContextUtil;
import com.oppo.util.OppoDialogUtil;
import java.lang.ref.WeakReference;

public class AlertController {
    public static final int MICRO = 1;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK : Add for the button of the delete dialog is scroll", property = OppoRomType.ROM)
    public static boolean mScroll = false;
    private ListAdapter mAdapter;
    private int mAlertDialogLayout;
    private final OnClickListener mButtonHandler;
    private Button mButtonNegative;
    private Message mButtonNegativeMessage;
    private CharSequence mButtonNegativeText;
    private Button mButtonNeutral;
    private Message mButtonNeutralMessage;
    private CharSequence mButtonNeutralText;
    private int mButtonPanelLayoutHint;
    private int mButtonPanelSideLayout;
    private Button mButtonPositive;
    private Message mButtonPositiveMessage;
    private CharSequence mButtonPositiveText;
    private int mCheckedItem;
    private final Context mContext;
    private View mCustomTitleView;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Shuai.Zhang@Plf.SDK, 2016-04-28 : Add for a new style AlertDialog", property = OppoRomType.ROM)
    private int mDeleteDialogOption;
    private final DialogInterface mDialogInterface;
    private boolean mForceInverseBackground;
    private Handler mHandler;
    private Drawable mIcon;
    private int mIconId;
    private ImageView mIconView;
    private int mListItemLayout;
    private int mListLayout;
    protected ListView mListView;
    protected CharSequence mMessage;
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK : Add for the message is scroll", property = OppoRomType.ROM)
    public boolean mMessageScroll;
    protected TextView mMessageView;
    private int mMultiChoiceItemLayout;
    protected ScrollView mScrollView;
    private boolean mShowTitle;
    private int mSingleChoiceItemLayout;
    private CharSequence mTitle;
    private TextView mTitleView;
    private View mView;
    private int mViewLayoutResId;
    private int mViewSpacingBottom;
    private int mViewSpacingLeft;
    private int mViewSpacingRight;
    private boolean mViewSpacingSpecified;
    private int mViewSpacingTop;
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
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Yujun.Feng@Plf.SDK, 2017-02-20 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public boolean mHasMessage = false;
        public Drawable mIcon;
        public int mIconAttrId = 0;
        public int mIconId = 0;
        public final LayoutInflater mInflater;
        public String mIsCheckedColumn;
        public boolean mIsMultiChoice;
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK, 2016-08-20 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public boolean mIsScroll;
        public boolean mIsSingleChoice;
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK, 2016-05-31 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public boolean mIsTitle = false;
        public CharSequence[] mItems;
        public String mLabelColumn;
        public CharSequence mMessage;
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK, 2017-01-23 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public boolean mMessageIsScroll;
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
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK, 2016-08-20 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public CharSequence[] mSummaryItems;
        public CharSequence mTitle;
        public View mView;
        public int mViewLayoutResId;
        public int mViewSpacingBottom;
        public int mViewSpacingLeft;
        public int mViewSpacingRight;
        public boolean mViewSpacingSpecified = false;
        public int mViewSpacingTop;
        @OppoHook(level = OppoHookType.NEW_FIELD, note = "Suying.You@Plf.SDK, 2016-05-31 : Add for 3.1 dialog", property = OppoRomType.ROM)
        public int[] textColor;

        public interface OnPrepareListViewListener {
            void onPrepareListView(ListView listView);
        }

        public AlertParams(Context context) {
            this.mContext = context;
            this.mCancelable = true;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        public void apply(AlertController dialog) {
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

        @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Suying.You@Plf.SDK, 2016-05-31 : Modify for 3.1 dialog", property = OppoRomType.ROM)
        private void createListView(AlertController dialog) {
            ListAdapter adapter;
            boolean isOppoStyle = ColorContextUtil.isOppoStyle(this.mContext);
            dialog.mListLayout = AlertController.initListLayout(dialog, isOppoStyle, this.mContext, dialog.mListLayout);
            this.mHasMessage = this.mMessage != null;
            final RecycleListView listView = (RecycleListView) this.mInflater.inflate(dialog.mListLayout, null);
            this.mIsTitle = AlertController.initRecycleListView(dialog, isOppoStyle, listView, this.mTitle, this.mIsScroll, this.mMessageIsScroll);
            if (!this.mIsMultiChoice) {
                int layout;
                if (this.mIsSingleChoice) {
                    layout = dialog.mSingleChoiceItemLayout;
                } else {
                    layout = dialog.mListItemLayout;
                }
                boolean isDialogThree = AlertController.isDialogThree(dialog);
                if (AlertController.isCustomAdapter(isOppoStyle) && isDialogThree) {
                    adapter = AlertController.initCheckedItemAdapter(dialog, this.mLabelColumn, this.mCursor, this.mContext, this.mItems, this.mSummaryItems, this.mIsTitle, this.textColor, layout, this.mAdapter, this.mHasMessage);
                } else if (this.mCursor != null) {
                    adapter = new SimpleCursorAdapter(this.mContext, layout, this.mCursor, new String[]{this.mLabelColumn}, new int[]{R.id.text1});
                } else if (this.mAdapter != null) {
                    adapter = this.mAdapter;
                } else {
                    adapter = new CheckedItemAdapter(this.mContext, layout, R.id.text1, this.mItems);
                }
            } else if (this.mCursor == null) {
                final RecycleListView recycleListView = listView;
                adapter = new ArrayAdapter<CharSequence>(this.mContext, dialog.mMultiChoiceItemLayout, R.id.text1, this.mItems) {
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        if (AlertParams.this.mCheckedItems != null && AlertParams.this.mCheckedItems[position]) {
                            recycleListView.setItemChecked(position, true);
                        }
                        return view;
                    }
                };
            } else {
                final RecycleListView recycleListView2 = listView;
                final AlertController alertController = dialog;
                adapter = new CursorAdapter(this.mContext, this.mCursor, false) {
                    private final int mIsCheckedIndex;
                    private final int mLabelIndex;

                    public void bindView(View view, Context context, Cursor cursor) {
                        boolean z = true;
                        ((CheckedTextView) view.findViewById(R.id.text1)).setText(cursor.getString(this.mLabelIndex));
                        RecycleListView recycleListView = recycleListView2;
                        int position = cursor.getPosition();
                        if (cursor.getInt(this.mIsCheckedIndex) != 1) {
                            z = false;
                        }
                        recycleListView.setItemChecked(position, z);
                    }

                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        return AlertParams.this.mInflater.inflate(alertController.mMultiChoiceItemLayout, parent, false);
                    }
                };
            }
            if (this.mOnPrepareListViewListener != null) {
                this.mOnPrepareListViewListener.onPrepareListView(listView);
            }
            dialog.mAdapter = adapter;
            dialog.mCheckedItem = this.mCheckedItem;
            final AlertController alertController2;
            if (this.mOnClickListener != null) {
                alertController2 = dialog;
                listView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                        AlertParams.this.mOnClickListener.onClick(alertController2.mDialogInterface, position);
                        if (!AlertParams.this.mIsSingleChoice) {
                            alertController2.mDialogInterface.dismiss();
                        }
                    }
                });
            } else if (this.mOnCheckboxClickListener != null) {
                alertController2 = dialog;
                listView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                        if (AlertParams.this.mCheckedItems != null) {
                            AlertParams.this.mCheckedItems[position] = listView.isItemChecked(position);
                        }
                        AlertParams.this.mOnCheckboxClickListener.onClick(alertController2.mDialogInterface, position, listView.isItemChecked(position));
                    }
                });
            }
            if (this.mOnItemSelectedListener != null) {
                listView.setOnItemSelectedListener(this.mOnItemSelectedListener);
            }
            if (this.mIsSingleChoice) {
                listView.setChoiceMode(1);
            } else if (this.mIsMultiChoice) {
                listView.setChoiceMode(2);
            }
            listView.mRecycleOnMeasure = this.mRecycleOnMeasure;
            dialog.mListView = listView;
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

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects) {
            super(context, resource, textViewResourceId, (Object[]) objects);
        }

        public boolean hasStableIds() {
            return true;
        }

        public long getItemId(int position) {
            return (long) position;
        }
    }

    public static class RecycleListView extends ListView {
        private boolean mIsOppoStyle;
        private final int mPaddingBottomNoButtons;
        private final int mPaddingTopNoTitle;
        private Path mPath1;
        private Path mPathArc;
        private int mRadius;
        boolean mRecycleOnMeasure;

        public RecycleListView(Context context) {
            this(context, null);
        }

        public RecycleListView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mRecycleOnMeasure = true;
            this.mIsOppoStyle = false;
            this.mRadius = 24;
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RecycleListView);
            this.mPaddingBottomNoButtons = ta.getDimensionPixelOffset(0, -1);
            this.mPaddingTopNoTitle = ta.getDimensionPixelOffset(1, -1);
            this.mRadius = (int) (((float) this.mRadius) * context.getResources().getDisplayMetrics().density);
            this.mIsOppoStyle = ColorContextUtil.isOppoStyle(context);
        }

        public void setHasDecor(boolean hasTitle, boolean hasButtons) {
            if (!hasButtons || (hasTitle ^ 1) != 0) {
                setPadding(getPaddingLeft(), hasTitle ? getPaddingTop() : this.mPaddingTopNoTitle, getPaddingRight(), hasButtons ? getPaddingBottom() : this.mPaddingBottomNoButtons);
            }
        }

        protected boolean recycleOnMeasure() {
            return this.mRecycleOnMeasure;
        }

        public void draw(Canvas canvas) {
            canvas.save();
            clipBottomRound(canvas);
            super.draw(canvas);
            canvas.restore();
        }

        private void clipBottomRound(Canvas canvas) {
            if (this.mIsOppoStyle && (AlertController.mScroll ^ 1) == 0) {
                if (this.mPath1 == null) {
                    this.mPath1 = new Path();
                } else {
                    this.mPath1.reset();
                }
                int offY = getScrollY();
                int startY = (getBottom() - (this.mRadius / 2)) + offY;
                int endY = getBottom() + offY;
                int startX = getLeft();
                int endX = this.mRadius / 2;
                this.mPath1.moveTo((float) startX, (float) startY);
                this.mPath1.lineTo((float) startX, (float) endY);
                this.mPath1.lineTo((float) endX, (float) endY);
                this.mPath1.close();
                canvas.clipPath(this.mPath1, Op.DIFFERENCE);
                this.mPath1.reset();
                startX = getRight();
                endX = getRight() - (this.mRadius / 2);
                this.mPath1.moveTo((float) startX, (float) startY);
                this.mPath1.lineTo((float) startX, (float) endY);
                this.mPath1.lineTo((float) endX, (float) endY);
                this.mPath1.close();
                canvas.clipPath(this.mPath1, Op.DIFFERENCE);
                if (this.mPathArc == null) {
                    this.mPathArc = new Path();
                } else {
                    this.mPathArc.reset();
                }
                this.mPathArc.addArc(new RectF(0.0f, (float) ((getBottom() - this.mRadius) + offY), (float) this.mRadius, (float) (getBottom() + offY)), 90.0f, 180.0f);
                canvas.clipPath(this.mPathArc, Op.UNION);
                this.mPathArc.reset();
                this.mPathArc.addArc(new RectF((float) (getRight() - this.mRadius), (float) ((getBottom() - this.mRadius) + offY), (float) getRight(), (float) (getBottom() + offY)), 0.0f, 90.0f);
                canvas.clipPath(this.mPathArc, Op.UNION);
            }
        }
    }

    private static boolean shouldCenterSingleButton(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogCenterButtons, outValue, true);
        if (outValue.data != 0) {
            return true;
        }
        return false;
    }

    public static final AlertController create(Context context, DialogInterface di, Window window) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.AlertDialog, R.attr.alertDialogStyle, 0);
        int controllerType = a.getInt(12, 0);
        a.recycle();
        switch (controllerType) {
            case 1:
                return new MicroAlertController(context, di, window);
            default:
                return new AlertController(context, di, window);
        }
    }

    protected AlertController(Context context, DialogInterface di, Window window) {
        this.mViewSpacingSpecified = false;
        this.mIconId = 0;
        this.mCheckedItem = -1;
        this.mButtonPanelLayoutHint = 0;
        this.mButtonHandler = new OnClickListener() {
            public void onClick(View v) {
                Message m;
                if (v == AlertController.this.mButtonPositive && AlertController.this.mButtonPositiveMessage != null) {
                    m = Message.obtain(AlertController.this.mButtonPositiveMessage);
                } else if (v == AlertController.this.mButtonNegative && AlertController.this.mButtonNegativeMessage != null) {
                    m = Message.obtain(AlertController.this.mButtonNegativeMessage);
                } else if (v != AlertController.this.mButtonNeutral || AlertController.this.mButtonNeutralMessage == null) {
                    m = null;
                } else {
                    m = Message.obtain(AlertController.this.mButtonNeutralMessage);
                }
                if (m != null) {
                    m.sendToTarget();
                }
                AlertController.this.mHandler.obtainMessage(1, AlertController.this.mDialogInterface).sendToTarget();
            }
        };
        this.mDeleteDialogOption = 0;
        this.mMessageScroll = true;
        this.mContext = context;
        this.mDialogInterface = di;
        this.mWindow = window;
        this.mHandler = new ButtonHandler(di);
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.AlertDialog, R.attr.alertDialogStyle, 0);
        this.mAlertDialogLayout = a.getResourceId(10, R.layout.alert_dialog);
        this.mButtonPanelSideLayout = a.getResourceId(11, 0);
        this.mListLayout = a.getResourceId(15, R.layout.select_dialog);
        this.mMultiChoiceItemLayout = a.getResourceId(16, R.layout.select_dialog_multichoice);
        this.mSingleChoiceItemLayout = a.getResourceId(21, R.layout.select_dialog_singlechoice);
        this.mListItemLayout = a.getResourceId(14, R.layout.select_dialog_item);
        this.mShowTitle = a.getBoolean(20, true);
        a.recycle();
        window.requestFeature(1);
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

    public void installContent(AlertParams params) {
        params.apply(this);
        installContent();
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "JianHua.Lin@Plf.SDK : Modify for Change the display position of dialog; Shuai.Zhang@Plf.SDK, 2015-05-19 : Modify for ColorOS 3.0 Dialog", property = OppoRomType.ROM)
    public void installContent() {
        this.mWindow.setContentView(selectContentView());
        AlertController.updateWindow(this, this.mContext, this.mWindow);
        setupView();
        AlertController.setDialogListener(this, this.mContext, this.mWindow, this.mDeleteDialogOption);
    }

    private int selectContentView() {
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
            this.mIconView.setVisibility(0);
            this.mIconView.setImageResource(this.mIconId);
            return;
        }
        this.mIconView.setVisibility(8);
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
        this.mIconId = 0;
        if (this.mIconView == null) {
            return;
        }
        if (icon != null) {
            this.mIconView.setVisibility(0);
            this.mIconView.setImageDrawable(icon);
            return;
        }
        this.mIconView.setVisibility(8);
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

    private ViewGroup resolvePanel(View customPanel, View defaultPanel) {
        if (customPanel == null) {
            if (defaultPanel instanceof ViewStub) {
                defaultPanel = ((ViewStub) defaultPanel).inflate();
            }
            return (ViewGroup) defaultPanel;
        }
        if (defaultPanel != null) {
            ViewParent parent = defaultPanel.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(defaultPanel);
            }
        }
        if (customPanel instanceof ViewStub) {
            customPanel = ((ViewStub) customPanel).inflate();
        }
        return (ViewGroup) customPanel;
    }

    private void setupView() {
        View spacer;
        View parentPanel = this.mWindow.findViewById(R.id.parentPanel);
        View defaultTopPanel = parentPanel.findViewById(R.id.topPanel);
        View defaultContentPanel = parentPanel.findViewById(R.id.contentPanel);
        View defaultButtonPanel = parentPanel.findViewById(R.id.buttonPanel);
        ViewGroup customPanel = (ViewGroup) parentPanel.findViewById(R.id.customPanel);
        setupCustomContent(customPanel);
        View customTopPanel = customPanel.findViewById(R.id.topPanel);
        View customContentPanel = customPanel.findViewById(R.id.contentPanel);
        View customButtonPanel = customPanel.findViewById(R.id.buttonPanel);
        ViewGroup topPanel = resolvePanel(customTopPanel, defaultTopPanel);
        ViewGroup contentPanel = resolvePanel(customContentPanel, defaultContentPanel);
        ViewGroup buttonPanel = resolvePanel(customButtonPanel, defaultButtonPanel);
        setupContent(contentPanel);
        setupButtons(buttonPanel);
        setupTitle(topPanel);
        boolean hasCustomPanel = customPanel != null ? customPanel.getVisibility() != 8 : false;
        boolean hasTopPanel = topPanel != null ? topPanel.getVisibility() != 8 : false;
        boolean hasButtonPanel = buttonPanel != null ? buttonPanel.getVisibility() != 8 : false;
        if (!hasButtonPanel) {
            if (contentPanel != null) {
                spacer = contentPanel.findViewById(R.id.textSpacerNoButtons);
                if (spacer != null) {
                    spacer.setVisibility(0);
                }
            }
            this.mWindow.setCloseOnTouchOutsideIfNotSet(true);
        }
        if (hasTopPanel) {
            if (this.mScrollView != null) {
                this.mScrollView.setClipToPadding(true);
            }
            View divider = null;
            if (this.mMessage == null && this.mListView == null && !hasCustomPanel) {
                divider = topPanel.findViewById(R.id.titleDividerTop);
            } else {
                if (!hasCustomPanel) {
                    divider = topPanel.findViewById(R.id.titleDividerNoCustom);
                }
                if (divider == null) {
                    divider = topPanel.findViewById(R.id.titleDivider);
                }
            }
            if (divider != null) {
                divider.setVisibility(0);
            }
        } else if (contentPanel != null) {
            spacer = contentPanel.findViewById(R.id.textSpacerNoTitle);
            if (spacer != null) {
                spacer.setVisibility(0);
            }
        }
        if (this.mListView instanceof RecycleListView) {
            ((RecycleListView) this.mListView).setHasDecor(hasTopPanel, hasButtonPanel);
        }
        if (!hasCustomPanel) {
            View content = this.mListView != null ? this.mListView : this.mScrollView;
            if (content != null) {
                content.setScrollIndicators((hasTopPanel ? 1 : 0) | (hasButtonPanel ? 2 : 0), 3);
            }
        }
        TypedArray a = this.mContext.obtainStyledAttributes(null, R.styleable.AlertDialog, R.attr.alertDialogStyle, 0);
        setBackground(a, topPanel, contentPanel, customPanel, buttonPanel, hasTopPanel, hasCustomPanel, hasButtonPanel);
        a.recycle();
    }

    private void setupCustomContent(ViewGroup customPanel) {
        View customView;
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
            FrameLayout custom = (FrameLayout) this.mWindow.findViewById(R.id.custom);
            custom.addView(customView, new LayoutParams(-1, -1));
            if (this.mViewSpacingSpecified) {
                custom.setPadding(this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
            }
            if (this.mListView != null) {
                ((LinearLayout.LayoutParams) customPanel.getLayoutParams()).weight = 0.0f;
                return;
            }
            return;
        }
        customPanel.setVisibility(8);
    }

    protected void setupTitle(ViewGroup topPanel) {
        if (this.mCustomTitleView == null || !this.mShowTitle) {
            this.mIconView = (ImageView) this.mWindow.findViewById(R.id.icon);
            if ((TextUtils.isEmpty(this.mTitle) ^ 1) && this.mShowTitle) {
                this.mTitleView = (TextView) this.mWindow.findViewById(R.id.alertTitle);
                this.mTitleView.setText(this.mTitle);
                if (this.mIconId != 0) {
                    this.mIconView.setImageResource(this.mIconId);
                    return;
                } else if (this.mIcon != null) {
                    this.mIconView.setImageDrawable(this.mIcon);
                    return;
                } else {
                    this.mTitleView.setPadding(this.mIconView.getPaddingLeft(), this.mIconView.getPaddingTop(), this.mIconView.getPaddingRight(), this.mIconView.getPaddingBottom());
                    this.mIconView.setVisibility(8);
                    return;
                }
            }
            this.mWindow.findViewById(R.id.title_template).setVisibility(8);
            this.mIconView.setVisibility(8);
            topPanel.setVisibility(8);
            return;
        }
        topPanel.addView(this.mCustomTitleView, 0, new LayoutParams(-1, -2));
        this.mWindow.findViewById(R.id.title_template).setVisibility(8);
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Suying.You@Plf.SDK : Modify for when the dialog is DELETE_ALERT_DIALOG_THREE, and has the message", property = OppoRomType.ROM)
    protected void setupContent(ViewGroup contentPanel) {
        this.mScrollView = (ScrollView) contentPanel.findViewById(R.id.scrollView);
        this.mScrollView.setFocusable(false);
        this.mMessageView = (TextView) contentPanel.findViewById(R.id.message);
        if (this.mMessageView != null) {
            if (ColorContextUtil.isOppoStyle(this.mContext) && this.mDeleteDialogOption == 3) {
                AlertController.addListView(this.mListView, this.mMessage, this.mMessageView, this.mScrollView, (ImageView) contentPanel.findViewById(201458936), (LinearLayout) contentPanel.findViewById(201458934), contentPanel, mScroll, this.mMessageScroll);
            } else if (this.mMessage != null) {
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

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Shuai.Zhang@Plf.SDK, 2015-05-19 : Modify for ColorOS 3.0 Dialog", property = OppoRomType.ROM)
    protected void setupButtons(ViewGroup buttonPanel) {
        int whichButtons = 0;
        this.mButtonPositive = (Button) buttonPanel.findViewById(R.id.button1);
        this.mButtonPositive.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonPositiveText)) {
            this.mButtonPositive.setVisibility(8);
        } else {
            this.mButtonPositive.setText(this.mButtonPositiveText);
            this.mButtonPositive.setVisibility(0);
            whichButtons = 1;
        }
        this.mButtonNegative = (Button) buttonPanel.findViewById(R.id.button2);
        this.mButtonNegative.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNegativeText)) {
            this.mButtonNegative.setVisibility(8);
        } else {
            this.mButtonNegative.setText(this.mButtonNegativeText);
            this.mButtonNegative.setVisibility(0);
            whichButtons |= 2;
        }
        this.mButtonNeutral = (Button) buttonPanel.findViewById(R.id.button3);
        this.mButtonNeutral.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNeutralText)) {
            this.mButtonNeutral.setVisibility(8);
        } else {
            this.mButtonNeutral.setText(this.mButtonNeutralText);
            this.mButtonNeutral.setVisibility(0);
            whichButtons |= 4;
        }
        ColorStateList textFousedColor = null;
        boolean shouldCenterSingleButton = shouldCenterSingleButton(this.mContext);
        boolean isOppoStyle = ColorContextUtil.isOppoStyle(this.mContext);
        if (AlertController.isCustomButtons(isOppoStyle)) {
            textFousedColor = AlertController.setupButtons(this.mContext, shouldCenterSingleButton, this.mDeleteDialogOption, whichButtons, this.mButtonPositive, this.mButtonNegative, this.mButtonNeutral);
        } else if (shouldCenterSingleButton) {
            if (whichButtons == 1) {
                centerButton(this.mButtonPositive);
            } else if (whichButtons == 2) {
                centerButton(this.mButtonNegative);
            } else if (whichButtons == 4) {
                centerButton(this.mButtonNeutral);
            }
        }
        if (isOppoStyle) {
            AlertController.changeButtonArrangeStyles(this.mContext, buttonPanel, this.mDeleteDialogOption, whichButtons, this.mButtonPositive, this.mButtonNegative, this.mButtonNeutral, this.mButtonPositiveText, this.mButtonNegativeText, this.mButtonNeutralText);
        }
        if (!(whichButtons != 0)) {
            buttonPanel.setVisibility(8);
        }
        AlertController.updateButtonsBackground(isOppoStyle, textFousedColor, this.mDeleteDialogOption, whichButtons, this.mButtonPositive, this.mButtonNegative, this.mButtonNeutral);
    }

    private void centerButton(Button button) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.gravity = 1;
        params.weight = 0.5f;
        button.setLayoutParams(params);
        View leftSpacer = this.mWindow.findViewById(R.id.leftSpacer);
        if (leftSpacer != null) {
            leftSpacer.setVisibility(0);
        }
        View rightSpacer = this.mWindow.findViewById(R.id.rightSpacer);
        if (rightSpacer != null) {
            rightSpacer.setVisibility(0);
        }
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Shuai.Zhang@Plf.SDK, 2016-04-28 : Modify for a new style AlertDialog; Changwei.Li@Plf.SDK, 2016-05-25 : Modify for delete dailog two", property = OppoRomType.ROM)
    private void setBackground(TypedArray a, View topPanel, View contentPanel, View customPanel, View buttonPanel, boolean hasTitle, boolean hasCustomView, boolean hasButtons) {
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
            fullDark = R.drawable.popup_full_dark;
            topDark = R.drawable.popup_top_dark;
            centerDark = R.drawable.popup_center_dark;
            bottomDark = R.drawable.popup_bottom_dark;
            fullBright = R.drawable.popup_full_bright;
            topBright = R.drawable.popup_top_bright;
            centerBright = R.drawable.popup_center_bright;
            bottomBright = R.drawable.popup_bottom_bright;
            bottomMedium = R.drawable.popup_bottom_medium;
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
                i = lastLight ? hasButtons ? AlertController.updateBottomMediumDrawable(this.mDeleteDialogOption, a.getResourceId(8, bottomMedium)) : a.getResourceId(7, bottomBright) : a.getResourceId(3, bottomDark);
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
                listView.setSelectionFromTop(checkedItem, a.getDimensionPixelSize(19, 0));
            }
        }
        AlertController.updatePanelsBackground(this.mDeleteDialogOption, buttonPanel, topPanel, contentPanel, customPanel, this.mButtonPositive, this.mButtonNeutral);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Shuai.Zhang@Plf.SDK, 2016-04-28 : Add for a new style AlertDialog", property = OppoRomType.ROM)
    public void setDeleteDialogOption(int delete) {
        this.mDeleteDialogOption = delete;
        OppoDialogUtil.setDeleteDialogOption(delete);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "JianHui.Yu@Plf.SDK, 2017-09-30 : Add for a new style AlertDialog", property = OppoRomType.ROM)
    private void initDeleteDialogOption(int deleteDialogOption) {
        setDeleteDialogOption(deleteDialogOption);
        this.mAlertDialogLayout = AlertController.initLayout(deleteDialogOption, this.mAlertDialogLayout);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "JianHui.Yu@Plf.SDK, 2017-09-30 : Add for a new style AlertDialog", property = OppoRomType.ROM)
    public static final AlertController create(Context context, DialogInterface di, Window window, int deleteDialogOption) {
        AlertController controller = create(context, di, window);
        controller.initDeleteDialogOption(deleteDialogOption);
        return controller;
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Shuai.Zhang@Plf.SDK, 2016-04-28 : Add for a new style AlertDialog", property = OppoRomType.ROM)
    protected AlertController(Context context, DialogInterface di, Window window, int deleteDialogOption) {
        this(context, di, window);
        initDeleteDialogOption(deleteDialogOption);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Changwei.Li@Plf.SDK, 2016-05-25 : Add for delete dialog type three", property = OppoRomType.ROM)
    public int getDeleteDialogOption() {
        return this.mDeleteDialogOption;
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "JianHua.Lin@Plf.SDK : Add for Change the display position of dialog", property = OppoRomType.ROM)
    public void initializeDialog() {
        this.mWindow.setContentView(this.mAlertDialogLayout);
        setupView();
        new OppoDialogUtil(this.mContext).setDialogButtonFlag(this.mWindow, 8, this.mDeleteDialogOption);
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "Suying.You@Plf.SDK, 2016-08-18 : Add for it sets whether the button is bold", property = OppoRomType.ROM)
    public void setButtonIsBold(int positive, int negative, int neutral) {
        AlertController.setButtonBold(positive, negative, neutral, this.mButtonPositive, this.mButtonNegative, this.mButtonNeutral, this.mContext);
    }
}
