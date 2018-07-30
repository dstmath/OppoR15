package com.oppo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView.BufferType;
import com.android.internal.widget.ColorViewExplorerByTouchHelper;
import com.android.internal.widget.ColorViewExplorerByTouchHelper.ColorViewTalkBalkInteraction;
import oppo.R;

public class OppoEditText extends EditText {
    private static final int LARGEVIEWID = 1;
    private static final boolean LOG_DBG = false;
    private static final int NOID = -1;
    private static final int SMALLVIEWID = 0;
    private static final String TAG = "OppoEditText";
    private static final int TOTALVIEWID = 2;
    private int mArea;
    private ColorViewTalkBalkInteraction mColorViewTalkBalkInteraction;
    private Context mContext;
    private boolean mDeletable;
    private String mDeleteButton;
    private Drawable mDeleteNormal;
    private Drawable mDeletePressed;
    private int mDrawablePadding;
    private int mDrawableSizeRight;
    private boolean mForceFinishDetach;
    private OppoTextWatcher mOppoTextWatcher;
    private OnPasswordDeletedListener mPasswordDeleteListener;
    private boolean mQuickDelete;
    boolean mShouldHandleDelete;
    private OnTextDeletedListener mTextDeleteListener;
    private final ColorViewExplorerByTouchHelper mTouchHelper;

    public interface OnPasswordDeletedListener {
        boolean onPasswordDeleted();
    }

    public interface OnTextDeletedListener {
        boolean onTextDeleted();
    }

    private class OppoTextWatcher implements TextWatcher {
        /* synthetic */ OppoTextWatcher(OppoEditText this$0, OppoTextWatcher -this1) {
            this();
        }

        private OppoTextWatcher() {
        }

        public void afterTextChanged(Editable arg0) {
            OppoEditText.this.updateDeletableStatus(OppoEditText.this.hasFocus());
        }

        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }
    }

    public OppoEditText(Context context) {
        this(context, null);
    }

    public OppoEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 16842862);
    }

    public OppoEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public OppoEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mShouldHandleDelete = LOG_DBG;
        this.mDeleteButton = null;
        this.mQuickDelete = LOG_DBG;
        this.mDeletable = LOG_DBG;
        this.mTextDeleteListener = null;
        this.mPasswordDeleteListener = null;
        this.mOppoTextWatcher = null;
        this.mForceFinishDetach = LOG_DBG;
        this.mColorViewTalkBalkInteraction = new ColorViewTalkBalkInteraction() {
            private Rect mRect = null;
            private int mVirtualViewAt = -1;

            public void getItemBounds(int position, Rect rect) {
                int drawableSizeRight = 0;
                if (OppoEditText.this.mDeleteNormal != null) {
                    drawableSizeRight = OppoEditText.this.mDeleteNormal.getIntrinsicWidth();
                }
                int deltX = ((OppoEditText.this.mRight - OppoEditText.this.mLeft) - OppoEditText.this.mPaddingRight) - drawableSizeRight;
                this.mRect = rect;
                if (position == 0) {
                    rect.set(0, 0, deltX, OppoEditText.this.getHeight());
                } else if (position == 1) {
                    rect.set(deltX, 0, OppoEditText.this.getWidth(), OppoEditText.this.getHeight());
                }
            }

            public void performAction(int virtualViewId, int actiontype, boolean resolvePara) {
                if (virtualViewId == 1) {
                    OppoEditText.this.onFastDelete();
                }
                if (virtualViewId == 0) {
                    OppoEditText.this.setFocusable(true);
                    OppoEditText.this.setFocusableInTouchMode(true);
                    OppoEditText.this.requestFocus();
                }
            }

            public int getCurrentPosition() {
                return -1;
            }

            public int getItemCounts() {
                return 2;
            }

            public int getVirtualViewAt(float x, float y) {
                int drawableSizeRight = 0;
                if (OppoEditText.this.mDeleteNormal != null) {
                    drawableSizeRight = OppoEditText.this.mDeleteNormal.getIntrinsicWidth();
                }
                int deltX = ((OppoEditText.this.mRight - OppoEditText.this.mLeft) - OppoEditText.this.mPaddingRight) - drawableSizeRight;
                if (x < ((float) deltX)) {
                    return 0;
                }
                if (x <= ((float) deltX) || !OppoEditText.this.mQuickDelete || (OppoEditText.this.isEmpty(OppoEditText.this.getText().toString()) ^ 1) == 0 || !OppoEditText.this.hasFocus()) {
                    return -1;
                }
                return 1;
            }

            public CharSequence getItemDescription(int virtualViewId) {
                this.mVirtualViewAt = virtualViewId;
                if (virtualViewId == 0) {
                    if (!TextUtils.isEmpty(OppoEditText.this.getText())) {
                        return OppoEditText.this.getText();
                    }
                    if (!TextUtils.isEmpty(OppoEditText.this.getHint())) {
                        return OppoEditText.this.getHint();
                    }
                } else if (virtualViewId == 1) {
                    return OppoEditText.this.mDeleteButton;
                }
                return getClass().getSimpleName();
            }

            public CharSequence getClassName() {
                if (this.mVirtualViewAt == 0) {
                    return EditText.class.getName();
                }
                if (this.mVirtualViewAt == 1) {
                    return Button.class.getName();
                }
                return EditText.class.getName();
            }

            public int getDisablePosition() {
                return -1;
            }
        };
        this.mContext = context;
        boolean quickDelete = LOG_DBG;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OppoEditText, 0, 0);
        if (a != null) {
            int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    case 1:
                        quickDelete = a.getBoolean(attr, LOG_DBG);
                        break;
                    default:
                        break;
                }
            }
            a.recycle();
        }
        setFastDeletable(quickDelete);
        this.mDeleteNormal = context.getDrawable(201852137);
        if (this.mDeleteNormal != null) {
            this.mArea = this.mDeleteNormal.getIntrinsicWidth();
            this.mDeleteNormal.setBounds(0, 0, this.mArea, this.mArea);
        }
        this.mDeletePressed = context.getDrawable(201852136);
        if (this.mDeletePressed != null) {
            this.mDeletePressed.setBounds(0, 0, this.mArea, this.mArea);
        }
        this.mTouchHelper = new ColorViewExplorerByTouchHelper(this);
        this.mTouchHelper.setColorViewTalkBalkInteraction(this.mColorViewTalkBalkInteraction);
        setAccessibilityDelegate(this.mTouchHelper);
        setImportantForAccessibility(1);
        this.mDeleteButton = this.mContext.getString(201590171);
        this.mTouchHelper.invalidateRoot();
    }

    public void setFastDeletable(boolean quickDelete) {
        if (this.mQuickDelete != quickDelete) {
            this.mQuickDelete = quickDelete;
            if (this.mQuickDelete) {
                if (this.mOppoTextWatcher == null) {
                    this.mOppoTextWatcher = new OppoTextWatcher();
                    addTextChangedListener(this.mOppoTextWatcher);
                }
                this.mDrawablePadding = this.mContext.getResources().getDimensionPixelSize(201655514);
                setCompoundDrawablePadding(this.mDrawablePadding);
            }
        }
    }

    public boolean isFastDeletable() {
        return this.mQuickDelete;
    }

    private void updateDeletableStatus(boolean foucus) {
        if (TextUtils.isEmpty(getText().toString())) {
            setCompoundDrawables(null, null, null, null);
            this.mDeletable = LOG_DBG;
        } else if (foucus) {
            if (this.mDeleteNormal != null && (this.mDeletable ^ 1) != 0) {
                setCompoundDrawables(null, null, this.mDeleteNormal, null);
                this.mDeletable = true;
            }
        } else if (this.mDeletable) {
            setCompoundDrawables(null, null, null, null);
            this.mDeletable = LOG_DBG;
        }
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (this.mQuickDelete) {
            updateDeletableStatus(gainFocus);
        }
    }

    public void setOnTextDeletedListener(OnTextDeletedListener textDeleteListener) {
        this.mTextDeleteListener = textDeleteListener;
    }

    public void setTextDeletedListener(OnPasswordDeletedListener passwordDeletedListener) {
        this.mPasswordDeleteListener = passwordDeletedListener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mQuickDelete && (isEmpty(getText().toString()) ^ 1) != 0 && hasFocus()) {
            int deltX = ((this.mRight - this.mLeft) - this.mPaddingRight) - this.mDrawableSizeRight;
            if (getWidth() >= (this.mDrawableSizeRight + this.mPaddingRight) + this.mPaddingLeft) {
                int curX = (int) event.getX();
                int curY = (int) event.getY();
                boolean touchOnQuickDelete = getLayoutDirection() == 1 ? curX < (this.mLeft + this.mPaddingLeft) + this.mDrawableSizeRight ? true : LOG_DBG : curX > deltX ? true : LOG_DBG;
                switch (event.getAction()) {
                    case 0:
                        this.mTouchHelper.sendEventForVirtualView(0, 16);
                        if (touchOnQuickDelete && this.mDeletable) {
                            this.mShouldHandleDelete = true;
                            if (this.mDeletePressed != null) {
                                setCompoundDrawables(null, null, this.mDeletePressed, null);
                            }
                            return true;
                        }
                    case 1:
                        if (touchOnQuickDelete && this.mDeletable && this.mShouldHandleDelete) {
                            if (this.mDeleteNormal != null) {
                                setCompoundDrawables(null, null, this.mDeleteNormal, null);
                            }
                            if (this.mTextDeleteListener == null || !this.mTextDeleteListener.onTextDeleted()) {
                                this.mTouchHelper.sendEventForVirtualView(1, 1);
                                onFastDelete();
                                this.mShouldHandleDelete = LOG_DBG;
                                return true;
                            }
                        }
                    case 2:
                        if ((!touchOnQuickDelete || curY < 0 || curY > getHeight()) && this.mDeleteNormal != null) {
                            setCompoundDrawables(null, null, this.mDeleteNormal, null);
                            break;
                        }
                    case 3:
                    case 4:
                        if (this.mDeleteNormal != null) {
                            setCompoundDrawables(null, null, this.mDeleteNormal, null);
                            break;
                        }
                        break;
                }
            }
            return LOG_DBG;
        }
        return super.onTouchEvent(event);
    }

    private void onFastDelete() {
        CharSequence mText = getText();
        ((Editable) mText).delete(0, mText.length());
        setText("");
    }

    private boolean isEmpty(String currentText) {
        if (currentText == null) {
            return LOG_DBG;
        }
        return TextUtils.isEmpty(currentText);
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        setCompoundDrawablesRelative(left, top, right, bottom);
        if (right != null) {
            this.mDrawableSizeRight = right.getBounds().width();
        } else {
            this.mDrawableSizeRight = 0;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!this.mQuickDelete || keyCode != 67) {
            return super.onKeyDown(keyCode, event);
        }
        super.onKeyDown(keyCode, event);
        if (this.mPasswordDeleteListener != null) {
            this.mPasswordDeleteListener.onPasswordDeleted();
        }
        return true;
    }

    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        Selection.setSelection(getText(), length());
    }

    public void forceFinishDetach() {
        this.mForceFinishDetach = true;
    }

    public void dispatchStartTemporaryDetach() {
        if (this.mForceFinishDetach) {
            onStartTemporaryDetach();
        } else {
            super.dispatchStartTemporaryDetach();
        }
    }

    public void clearAccessibilityFocus() {
        if (this.mTouchHelper != null) {
            this.mTouchHelper.clearFocusedVirtualView();
        }
    }

    boolean restoreAccessibilityFocus(int position) {
        if (this.mTouchHelper != null) {
            this.mTouchHelper.setFocusedVirtualView(position);
        }
        return true;
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mTouchHelper == null || !this.mTouchHelper.dispatchHoverEvent(event)) {
            return super.dispatchHoverEvent(event);
        }
        return true;
    }
}
