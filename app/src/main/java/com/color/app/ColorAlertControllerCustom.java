package com.color.app;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.color.app.ColorAlertDialogCustom.ListItemAttr;

public class ColorAlertControllerCustom extends ColorAlertController {
    private static final String TAG = "ColorAlertControllerCustom";
    private boolean mIsDynaChange = false;
    private ListItemAttr[] mItemsAttrs;

    public static class AlertParams extends com.color.app.ColorAlertController.AlertParams {
        public ListItemAttr[] mItemsAttrs;

        public AlertParams(Context context) {
            super(context);
        }

        protected void createListView(final com.color.app.ColorAlertController r13) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r0_3 'adapter' android.widget.ListAdapter) in PHI: PHI: (r0_2 'adapter' android.widget.ListAdapter) = (r0_0 'adapter' android.widget.ListAdapter), (r0_1 'adapter' android.widget.ListAdapter), (r0_3 'adapter' android.widget.ListAdapter) binds: {(r0_0 'adapter' android.widget.ListAdapter)=B:4:0x0023, (r0_1 'adapter' android.widget.ListAdapter)=B:16:0x0056, (r0_3 'adapter' android.widget.ListAdapter)=B:17:0x0062}
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
            r12 = this;
            r4 = 16908308; // 0x1020014 float:2.3877285E-38 double:8.353814E-317;
            r9 = 1;
            r11 = 0;
            r6 = 0;
            r1 = r13;
            r1 = (com.color.app.ColorAlertControllerCustom) r1;
            r2 = 201917561; // 0xc090479 float:1.0555442E-31 double:9.976053E-316;
            r13.mListLayout = r2;
            r2 = r12.mInflater;
            r5 = r13.mListLayout;
            r10 = r2.inflate(r5, r6);
            r10 = (com.color.app.ColorAlertController.RecycleListView) r10;
            r3 = 201917553; // 0xc090471 float:1.0555433E-31 double:9.9760526E-316;
            r2 = r12.mCursor;
            if (r2 != 0) goto L_0x0062;
        L_0x001f:
            r2 = r12.mAdapter;
            if (r2 == 0) goto L_0x0056;
        L_0x0023:
            r0 = r12.mAdapter;
        L_0x0025:
            r2 = r12.mOnPrepareListViewListener;
            if (r2 == 0) goto L_0x002e;
        L_0x0029:
            r2 = r12.mOnPrepareListViewListener;
            r2.onPrepareListView(r10);
        L_0x002e:
            r13.mAdapter = r0;
            r2 = r12.mCheckedItem;
            r13.mCheckedItem = r2;
            r2 = r12.mOnClickListener;
            if (r2 == 0) goto L_0x0040;
        L_0x0038:
            r2 = new com.color.app.ColorAlertControllerCustom$AlertParams$1;
            r2.<init>(r13, r1);
            r10.setOnItemClickListener(r2);
        L_0x0040:
            r2 = r12.mOnItemSelectedListener;
            if (r2 == 0) goto L_0x0049;
        L_0x0044:
            r2 = r12.mOnItemSelectedListener;
            r10.setOnItemSelectedListener(r2);
        L_0x0049:
            r2 = r12.mRecycleOnMeasure;
            r10.mRecycleOnMeasure = r2;
            r2 = 201720878; // 0xc06042e float:1.0324242E-31 double:9.9663356E-316;
            r10.setSelector(r2);
            r13.mListView = r10;
            return;
        L_0x0056:
            r0 = new com.color.app.ColorAlertControllerCustom$CheckedItemAdapter;
            r2 = r12.mContext;
            r5 = r12.mItems;
            r6 = r12.mItemsAttrs;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            goto L_0x0025;
        L_0x0062:
            r0 = new android.widget.SimpleCursorAdapter;
            r5 = r12.mContext;
            r7 = r12.mCursor;
            r8 = new java.lang.String[r9];
            r2 = r12.mLabelColumn;
            r8[r11] = r2;
            r9 = new int[r9];
            r9[r11] = r4;
            r4 = r0;
            r6 = r3;
            r4.<init>(r5, r6, r7, r8, r9);
            goto L_0x0025;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.color.app.ColorAlertControllerCustom.AlertParams.createListView(com.color.app.ColorAlertController):void");
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        private Context context;
        private ColorAlertControllerCustom dialog;
        private ListItemAttr[] mItemsAttrs;
        private boolean textBold;
        private int textColor;
        private int textId;

        public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public CheckedItemAdapter(ColorAlertControllerCustom dialog, Context context, int resource, int textViewResourceId, CharSequence[] objects, ListItemAttr[] itemsAttrs) {
            super(context, resource, textViewResourceId, objects);
            this.mItemsAttrs = itemsAttrs;
            this.textId = textViewResourceId;
            this.context = context;
            this.dialog = dialog;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View item = super.getView(position, convertView, parent);
            if (this.mItemsAttrs == null) {
                this.mItemsAttrs = this.dialog.mItemsAttrs;
            }
            if (this.mItemsAttrs != null && this.dialog.mIsDynaChange) {
                this.mItemsAttrs = this.dialog.mItemsAttrs;
            }
            if (item != null) {
                TextView textView = (TextView) item.findViewById(this.textId);
                if (this.mItemsAttrs == null || this.mItemsAttrs[position] == null) {
                    textView.setTextColor(this.context.getResources().getColorStateList(201719832));
                    textView.getPaint().setFakeBoldText(false);
                    item.setEnabled(true);
                } else {
                    if (this.mItemsAttrs[position].getItemColor() != null) {
                        textView.setTextColor(this.mItemsAttrs[position].getItemColor().intValue());
                    }
                    if (this.mItemsAttrs[position].getItemBold() != null) {
                        textView.getPaint().setFakeBoldText(this.mItemsAttrs[position].getItemBold().booleanValue());
                    }
                    if (this.mItemsAttrs[position].getItemEnable() != null) {
                        item.setEnabled(this.mItemsAttrs[position].getItemEnable().booleanValue());
                    }
                    if (!this.mItemsAttrs[position].getItemEnable().booleanValue() && this.mItemsAttrs[position].getItemColor() == null) {
                        textView.setTextColor(this.context.getResources().getColorStateList(201720883));
                    } else if (this.mItemsAttrs[position].getItemEnable().booleanValue() && this.mItemsAttrs[position].getItemColor() == null) {
                        textView.setTextColor(this.context.getResources().getColorStateList(201719832));
                    }
                }
            }
            int count = getCount();
            if (count > 1) {
                if (position == count - 1) {
                    item.setBackgroundResource(201852167);
                } else {
                    item.setBackgroundResource(201852166);
                }
            }
            return item;
        }

        public boolean hasStableIds() {
            return true;
        }

        public long getItemId(int position) {
            return (long) position;
        }
    }

    public ColorAlertControllerCustom(Context context, DialogInterface di, Window window) {
        super(context, di, window);
    }

    public int selectContentView() {
        this.mAlertDialogLayout = 201917560;
        return this.mAlertDialogLayout;
    }

    protected void setupContent(ViewGroup contentPanel) {
        this.mScrollView = (ScrollView) this.mWindow.findViewById(201458941);
        this.mScrollView.setFocusable(false);
        ImageView divider = (ImageView) this.mWindow.findViewById(201458951);
        LinearLayout linearyLayout = (LinearLayout) this.mWindow.findViewById(201458952);
        this.mMessageView = (TextView) this.mWindow.findViewById(201458841);
        if (this.mMessageView != null) {
            if (this.mMessage != null) {
                this.mMessageView.setText(this.mMessage);
            } else {
                this.mMessageView.setVisibility(8);
                this.mScrollView.removeView(this.mMessageView);
            }
            if (this.mListView != null) {
                divider.setVisibility(0);
                if (this.mMessage == null) {
                    ViewGroup scrollParent = (ViewGroup) this.mScrollView.getParent();
                    scrollParent.removeViewAt(scrollParent.indexOfChild(this.mScrollView));
                    scrollParent.removeViewAt(scrollParent.indexOfChild(linearyLayout));
                    scrollParent.addView(this.mListView, new LayoutParams(-1, -1));
                } else if (this.mMessage != null) {
                    linearyLayout.addView(this.mListView, new LayoutParams(-1, -1));
                }
            }
            if (this.mMessage == null && this.mListView == null) {
                contentPanel.setVisibility(8);
            }
        }
    }

    public void setListItemState(ListItemAttr[] itemsAttrs, boolean isDynaChange) {
        this.mItemsAttrs = itemsAttrs;
        this.mIsDynaChange = isDynaChange;
    }

    public void setButtonIsBold(int positive, int negative, int neutral) {
        if (positive == -1) {
            this.mButtonPositive.setTextAppearance(this.mContext, 201524241);
        }
        if (negative == -2) {
            this.mButtonNegative.getPaint().setFakeBoldText(true);
        }
        if (neutral == -3) {
            this.mButtonNeutral.getPaint().setFakeBoldText(true);
        }
    }
}
