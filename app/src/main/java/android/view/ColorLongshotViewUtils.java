package android.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.color.util.ColorLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ColorLongshotViewUtils {
    private static final RectComparator RECT_COMPARATOR = new RectComparator();
    private static final String TAG = "LongshotDump";
    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();
    private final Rect mTempRect3 = new Rect();

    private static class RectComparator implements Comparator<Rect> {
        /* synthetic */ RectComparator(RectComparator -this0) {
            this();
        }

        private RectComparator() {
        }

        public int compare(Rect rect1, Rect rect2) {
            int result = rect2.top - rect1.top;
            if (result != 0) {
                return rect2.left - rect1.left;
            }
            return result;
        }
    }

    public ColorLongshotViewUtils(Context context) {
    }

    private boolean needUpdateParent(Rect viewRect, Rect rootRect, ColorLongshotViewContent parent, boolean keepLargeRect) {
        if (isLargeCoverRect(viewRect, rootRect, keepLargeRect)) {
            return false;
        }
        return isLargeCoverRect(parent.getRect(), rootRect, keepLargeRect);
    }

    public void findCoverRect(int r26, android.view.ViewGroup r27, android.view.View r28, java.util.List<android.view.ColorLongshotViewContent> r29, java.util.List<android.view.ColorLongshotViewContent> r30, android.graphics.Rect r31, android.view.ColorLongshotViewContent r32, boolean r33) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r32_3 'parent' android.view.ColorLongshotViewContent) in PHI: PHI: (r32_4 'parent' android.view.ColorLongshotViewContent) = (r32_1 'parent' android.view.ColorLongshotViewContent), (r32_3 'parent' android.view.ColorLongshotViewContent) binds: {(r32_1 'parent' android.view.ColorLongshotViewContent)=B:33:0x00d2, (r32_3 'parent' android.view.ColorLongshotViewContent)=B:34:0x00d4}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r25 = this;
        r23 = new android.graphics.Rect;
        r23.<init>();
        r4 = 1;
        r0 = r27;
        r1 = r23;
        r0.getBoundsOnScreen(r1, r4);
        if (r31 != 0) goto L_0x0011;
    L_0x000f:
        r31 = r23;
    L_0x0011:
        if (r32 != 0) goto L_0x0091;
    L_0x0013:
        r19 = 1;
    L_0x0015:
        r21 = r25.getPrefix(r26);
        r4 = "LongshotDump";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r0 = r21;
        r5 = r5.append(r0);
        r6 = "findCoverRect : rootRect=";
        r5 = r5.append(r6);
        r0 = r31;
        r5 = r5.append(r0);
        r6 = ", srcRect=";
        r5 = r5.append(r6);
        r0 = r23;
        r5 = r5.append(r0);
        r6 = ", group=";
        r5 = r5.append(r6);
        r0 = r27;
        r5 = r5.append(r0);
        r6 = ", keepLargeRect=";
        r5 = r5.append(r6);
        r0 = r33;
        r5 = r5.append(r0);
        r5 = r5.toString();
        com.color.util.ColorLog.d(r4, r5);
        r16 = r27.getChildCount();
        r22 = r27.buildOrderedChildList();
        if (r22 != 0) goto L_0x0094;
    L_0x006c:
        r20 = 1;
    L_0x006e:
        if (r20 == 0) goto L_0x0097;
    L_0x0070:
        r17 = r27.isChildrenDrawingOrderEnabled();
    L_0x0074:
        r18 = r16 + -1;
    L_0x0076:
        if (r18 < 0) goto L_0x00aa;
    L_0x0078:
        if (r17 == 0) goto L_0x009a;
    L_0x007a:
        r0 = r27;
        r1 = r16;
        r2 = r18;
        r15 = r0.getChildDrawingOrder(r1, r2);
    L_0x0084:
        if (r20 == 0) goto L_0x009d;
    L_0x0086:
        r0 = r27;
        r11 = r0.getChildAt(r15);
    L_0x008c:
        if (r11 != 0) goto L_0x00a6;
    L_0x008e:
        r18 = r18 + -1;
        goto L_0x0076;
    L_0x0091:
        r19 = 0;
        goto L_0x0015;
    L_0x0094:
        r20 = 0;
        goto L_0x006e;
    L_0x0097:
        r17 = 0;
        goto L_0x0074;
    L_0x009a:
        r15 = r18;
        goto L_0x0084;
    L_0x009d:
        r0 = r22;
        r11 = r0.get(r15);
        r11 = (android.view.View) r11;
        goto L_0x008c;
    L_0x00a6:
        r0 = r28;
        if (r11 != r0) goto L_0x00b0;
    L_0x00aa:
        if (r22 == 0) goto L_0x00af;
    L_0x00ac:
        r22.clear();
    L_0x00af:
        return;
    L_0x00b0:
        r4 = r11.isVisibleToUser();
        if (r4 == 0) goto L_0x008e;
    L_0x00b6:
        r0 = r25;
        r4 = r0.mTempRect3;
        r5 = 1;
        r11.getBoundsOnScreen(r4, r5);
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r31;
        r4 = android.graphics.Rect.intersects(r4, r0);
        if (r4 == 0) goto L_0x008e;
    L_0x00ca:
        r0 = r25;
        r4 = r0.isTransparentGroup(r11);
        if (r4 == 0) goto L_0x01a2;
    L_0x00d2:
        if (r19 == 0) goto L_0x00e0;
    L_0x00d4:
        r32 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r5 = 0;
        r0 = r32;
        r0.<init>(r11, r4, r5);
    L_0x00e0:
        r4 = r11;
        r4 = (android.view.ViewGroup) r4;
        r0 = r25;
        r1 = r31;
        r4 = r0.isWaterMarkGroup(r1, r4);
        if (r4 == 0) goto L_0x011e;
    L_0x00ed:
        r24 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r24;
        r1 = r32;
        r0.<init>(r11, r4, r1);
        r4 = "LongshotDump";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r0 = r21;
        r5 = r5.append(r0);
        r6 = "  skipCoverRect : isWaterMarkGroup=";
        r5 = r5.append(r6);
        r0 = r24;
        r5 = r5.append(r0);
        r5 = r5.toString();
        com.color.util.ColorLog.d(r4, r5);
        goto L_0x008e;
    L_0x011e:
        r4 = r11;
        r4 = (android.view.ViewGroup) r4;
        r0 = r25;
        r1 = r31;
        r2 = r30;
        r4 = r0.isSideBarGroup(r1, r4, r2);
        if (r4 == 0) goto L_0x0165;
    L_0x012d:
        r24 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r24;
        r1 = r32;
        r0.<init>(r11, r4, r1);
        r4 = "LongshotDump";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r0 = r21;
        r5 = r5.append(r0);
        r6 = "  skipCoverRect : isSideBarGroup=";
        r5 = r5.append(r6);
        r0 = r24;
        r5 = r5.append(r0);
        r5 = r5.toString();
        com.color.util.ColorLog.d(r4, r5);
        r0 = r30;
        r1 = r24;
        r0.add(r1);
        goto L_0x008e;
    L_0x0165:
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r25;
        r1 = r31;
        r2 = r32;
        r3 = r33;
        r4 = r0.needUpdateParent(r4, r1, r2, r3);
        if (r4 == 0) goto L_0x0183;
    L_0x0177:
        r32 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r5 = 0;
        r0 = r32;
        r0.<init>(r11, r4, r5);
    L_0x0183:
        r8 = new java.util.ArrayList;
        r8.<init>();
        r5 = r26 + 1;
        r6 = r11;
        r6 = (android.view.ViewGroup) r6;
        r7 = 0;
        r4 = r25;
        r9 = r30;
        r10 = r31;
        r11 = r32;
        r12 = r33;
        r4.findCoverRect(r5, r6, r7, r8, r9, r10, r11, r12);
        r0 = r29;
        r0.addAll(r8);
        goto L_0x008e;
    L_0x01a2:
        r14 = "noCoverContent";
        r9 = r25;
        r10 = r21;
        r12 = r31;
        r13 = r33;
        r4 = r9.hasVisibleContent(r10, r11, r12, r13, r14);
        if (r4 == 0) goto L_0x008e;
    L_0x01b3:
        if (r19 == 0) goto L_0x01c1;
    L_0x01b5:
        r32 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r5 = 0;
        r0 = r32;
        r0.<init>(r11, r4, r5);
    L_0x01c1:
        r24 = new android.view.ColorLongshotViewContent;
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r24;
        r1 = r32;
        r0.<init>(r11, r4, r1);
        r0 = r25;
        r4 = r0.mTempRect3;
        r0 = r25;
        r1 = r31;
        r4 = r0.isSideBarRect(r4, r1);
        if (r4 == 0) goto L_0x0209;
    L_0x01dc:
        r4 = "LongshotDump";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r0 = r21;
        r5 = r5.append(r0);
        r6 = "  skipCoverRect : isSideBarView=";
        r5 = r5.append(r6);
        r0 = r24;
        r5 = r5.append(r0);
        r5 = r5.toString();
        com.color.util.ColorLog.d(r4, r5);
        if (r30 == 0) goto L_0x008e;
    L_0x0200:
        r0 = r30;
        r1 = r24;
        r0.add(r1);
        goto L_0x008e;
    L_0x0209:
        r4 = "LongshotDump";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r0 = r21;
        r5 = r5.append(r0);
        r6 = "  addCoverRect : ";
        r5 = r5.append(r6);
        r0 = r24;
        r5 = r5.append(r0);
        r5 = r5.toString();
        com.color.util.ColorLog.d(r4, r5);
        r0 = r29;
        r1 = r24;
        r0.add(r1);
        goto L_0x008e;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.view.ColorLongshotViewUtils.findCoverRect(int, android.view.ViewGroup, android.view.View, java.util.List, java.util.List, android.graphics.Rect, android.view.ColorLongshotViewContent, boolean):void");
    }

    public boolean isBottomBarRect(Rect viewRect, Rect rootRect) {
        if (viewRect.width() == rootRect.width() && viewRect.bottom == rootRect.bottom) {
            return true;
        }
        return false;
    }

    public boolean isLargeCoverRect(Rect viewRect, Rect rootRect, boolean keepLargeRect) {
        if (!keepLargeRect) {
            if (viewRect.contains(rootRect)) {
                return true;
            }
            Rect intRect = new Rect();
            return intRect.setIntersect(viewRect, rootRect) && intRect.width() >= (rootRect.width() * 3) / 4 && intRect.height() >= (rootRect.height() * 3) / 4;
        }
        return false;
    }

    private String getPrefix(int recursive) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < recursive; i++) {
            prefix.append("    ");
        }
        return prefix.toString();
    }

    private boolean isSmallCoverRect(Rect viewRect, Rect rootRect) {
        return viewRect.width() <= 1 && viewRect.height() <= 1;
    }

    private boolean isCenterCoverRect(Rect viewRect, Rect rootRect) {
        Rect centerRect = new Rect();
        centerRect.set(rootRect);
        centerRect.inset(rootRect.width() / 3, rootRect.height() / 3);
        return centerRect.contains(viewRect);
    }

    private boolean isTransparentDrawable(Drawable drawable) {
        boolean z = true;
        if (drawable == null) {
            return true;
        }
        if (-2 != drawable.getOpacity()) {
            z = false;
        }
        return z;
    }

    private boolean isTransparentGroup(View view) {
        if (!(view instanceof GridView) && (view instanceof ViewGroup)) {
            return isTransparentDrawable(view.getBackground());
        }
        return false;
    }

    private void initCenterRect(Rect centerRect, Rect rootRect) {
        centerRect.set(rootRect);
        centerRect.inset(rootRect.width() / 4, rootRect.height() / 4);
    }

    private void printNoContentLog(String prefix, String tag, String type, Rect rect, View view) {
        if (prefix != null && tag != null) {
            ColorLog.d("LongshotDump", prefix + "  " + tag + " : " + type + "=" + rect + SettingsStringUtil.DELIMITER + view);
        }
    }

    private boolean hasVisibleContent(String prefix, View view, Rect rootRect, boolean keepLargeRect, String tag) {
        view.getBoundsOnScreen(this.mTempRect1, true);
        if (isCenterCoverRect(this.mTempRect1, rootRect)) {
            printNoContentLog(prefix, tag, "CenterCover", this.mTempRect1, view);
            return false;
        } else if (!isTransparentDrawable(view.getBackground())) {
            return true;
        } else {
            if (view instanceof TextView) {
                TextView contentView = (TextView) view;
                for (Drawable drawable : contentView.getCompoundDrawables()) {
                    if (!isTransparentDrawable(drawable)) {
                        return true;
                    }
                }
                if (!TextUtils.isEmpty(contentView.getHint())) {
                    return true;
                }
                if (!TextUtils.isEmpty(contentView.getText())) {
                    return true;
                }
                printNoContentLog(prefix, tag, "TextView", this.mTempRect1, view);
                return false;
            } else if (view instanceof ImageView) {
                if (!isTransparentDrawable(((ImageView) view).getDrawable())) {
                    return true;
                }
                printNoContentLog(prefix, tag, "ImageView", this.mTempRect1, view);
                return false;
            } else if (isLargeCoverRect(this.mTempRect1, rootRect, keepLargeRect)) {
                printNoContentLog(prefix, tag, "LargeCover", this.mTempRect1, view);
                return false;
            } else if (!isSmallCoverRect(this.mTempRect1, rootRect)) {
                return true;
            } else {
                printNoContentLog(prefix, tag, "SmallCover", this.mTempRect1, view);
                return false;
            }
        }
    }

    private boolean isNeighboringRect(Rect rect1, Rect rect2) {
        return rect1.top == rect2.bottom;
    }

    private boolean isSameLineRect(Rect rect1, Rect rect2) {
        return rect1.top == rect2.top && rect1.bottom == rect2.bottom;
    }

    private boolean isSideBarRect(Rect coverRect, Rect rootRect) {
        if (coverRect.width() <= rootRect.width() / 3 && coverRect.height() >= (rootRect.height() * 2) / 5) {
            return true;
        }
        return false;
    }

    private boolean findSideBarContent(View view, Rect rootRect, Rect itemRect) {
        if (itemRect != null) {
            view.getBoundsOnScreen(this.mTempRect1, true);
            if (itemRect.isEmpty()) {
                itemRect.set(this.mTempRect1);
            }
        }
        if (isTransparentGroup(view)) {
            boolean result = false;
            ViewGroup group = (ViewGroup) view;
            int childrenCount = group.getChildCount();
            ArrayList<View> preorderedList = group.buildOrderedChildList();
            boolean noPreorder = preorderedList == null;
            boolean customOrder = noPreorder ? group.isChildrenDrawingOrderEnabled() : false;
            int i = childrenCount - 1;
            while (i >= 0) {
                int childIndex = customOrder ? group.getChildDrawingOrder(childrenCount, i) : i;
                View child = noPreorder ? group.getChildAt(childIndex) : (View) preorderedList.get(childIndex);
                if (child != null && child.isVisibleToUser() && findSideBarContent(child, rootRect, null)) {
                    result = true;
                    break;
                }
                i--;
            }
            if (preorderedList != null) {
                preorderedList.clear();
            }
            return result;
        } else if (hasVisibleContent(null, view, rootRect, false, null)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isWaterMarkGroup(Rect rootRect, ViewGroup group) {
        this.mTempRect2.setEmpty();
        boolean allTextView = true;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (!(child instanceof TextView)) {
                allTextView = false;
                break;
            }
            child.getBoundsOnScreen(this.mTempRect1, true);
            this.mTempRect2.union(this.mTempRect1);
        }
        initCenterRect(this.mTempRect1, rootRect);
        if (allTextView) {
            return Rect.intersects(this.mTempRect2, this.mTempRect1);
        }
        return false;
    }

    private boolean isLargeWidth(View view, Rect rect) {
        return view.getWidth() > (rect.width() * 2) / 3;
    }

    private boolean isSideBarGroup(Rect rootRect, ViewGroup group, List<ColorLongshotViewContent> sideContents) {
        if (sideContents == null) {
            return false;
        }
        if (isLargeWidth(group, rootRect)) {
            return false;
        }
        Rect itemRect = new Rect(null);
        List<Rect> rects = new ArrayList();
        int childrenCount = group.getChildCount();
        ArrayList<View> preorderedList = group.buildOrderedChildList();
        boolean noPreorder = preorderedList == null;
        boolean customOrder = noPreorder ? group.isChildrenDrawingOrderEnabled() : false;
        int i = childrenCount - 1;
        while (i >= 0) {
            int childIndex = customOrder ? group.getChildDrawingOrder(childrenCount, i) : i;
            View child = noPreorder ? group.getChildAt(childIndex) : (View) preorderedList.get(childIndex);
            if (child != null && child.isVisibleToUser() && !isLargeWidth(child, rootRect) && findSideBarContent(child, rootRect, itemRect)) {
                child.getBoundsOnScreen(this.mTempRect1, true);
                rects.add(new Rect(this.mTempRect1));
            }
            i--;
        }
        if (preorderedList != null) {
            preorderedList.clear();
        }
        Collections.sort(rects, RECT_COMPARATOR);
        this.mTempRect1.setEmpty();
        Rect last = new Rect(null);
        for (Rect rect : rects) {
            if (last.isEmpty() || isNeighboringRect(last, rect)) {
                this.mTempRect1.union(rect);
            }
            last.set(rect);
        }
        return isSideBarRect(this.mTempRect1, rootRect);
    }
}
