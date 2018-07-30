package android.provider;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.provider.DocumentsContract.Path;
import android.provider.DocumentsContract.Root;
import android.util.Log;
import java.io.FileNotFoundException;
import java.util.Objects;
import libcore.io.IoUtils;

public abstract class DocumentsProvider extends ContentProvider {
    private static final int MATCH_CHILDREN = 6;
    private static final int MATCH_CHILDREN_TREE = 8;
    private static final int MATCH_DOCUMENT = 5;
    private static final int MATCH_DOCUMENT_TREE = 7;
    private static final int MATCH_RECENT = 3;
    private static final int MATCH_ROOT = 2;
    private static final int MATCH_ROOTS = 1;
    private static final int MATCH_SEARCH = 4;
    private static final String TAG = "DocumentsProvider";
    private String mAuthority;
    private UriMatcher mMatcher;

    public abstract ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException;

    public abstract Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException;

    public abstract Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException;

    public abstract Cursor queryRoots(String[] strArr) throws FileNotFoundException;

    public void attachInfo(Context context, ProviderInfo info) {
        registerAuthority(info.authority);
        if (!info.exported) {
            throw new SecurityException("Provider must be exported");
        } else if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grantUriPermissions");
        } else if ("android.permission.MANAGE_DOCUMENTS".equals(info.readPermission) && ("android.permission.MANAGE_DOCUMENTS".equals(info.writePermission) ^ 1) == 0) {
            super.attachInfo(context, info);
        } else {
            throw new SecurityException("Provider must be protected by MANAGE_DOCUMENTS");
        }
    }

    public void attachInfoForTesting(Context context, ProviderInfo info) {
        registerAuthority(info.authority);
        super.attachInfoForTesting(context, info);
    }

    private void registerAuthority(String authority) {
        this.mAuthority = authority;
        this.mMatcher = new UriMatcher(-1);
        this.mMatcher.addURI(this.mAuthority, "root", 1);
        this.mMatcher.addURI(this.mAuthority, "root/*", 2);
        this.mMatcher.addURI(this.mAuthority, "root/*/recent", 3);
        this.mMatcher.addURI(this.mAuthority, "root/*/search", 4);
        this.mMatcher.addURI(this.mAuthority, "document/*", 5);
        this.mMatcher.addURI(this.mAuthority, "document/*/children", 6);
        this.mMatcher.addURI(this.mAuthority, "tree/*/document/*", 7);
        this.mMatcher.addURI(this.mAuthority, "tree/*/document/*/children", 8);
    }

    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return false;
    }

    private void enforceTree(Uri documentUri) {
        if (DocumentsContract.isTreeUri(documentUri)) {
            String parent = DocumentsContract.getTreeDocumentId(documentUri);
            String child = DocumentsContract.getDocumentId(documentUri);
            if (!(Objects.equals(parent, child) || isChildDocument(parent, child))) {
                throw new SecurityException("Document " + child + " is not a descendant of " + parent);
            }
        }
    }

    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        throw new UnsupportedOperationException("Create not supported");
    }

    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        throw new UnsupportedOperationException("Rename not supported");
    }

    public void deleteDocument(String documentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Delete not supported");
    }

    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Copy not supported");
    }

    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Move not supported");
    }

    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Remove not supported");
    }

    public Path findDocumentPath(String parentDocumentId, String childDocumentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("findDocumentPath not supported.");
    }

    public IntentSender createWebLinkIntent(String documentId, Bundle options) throws FileNotFoundException {
        throw new UnsupportedOperationException("createWebLink is not supported.");
    }

    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        throw new UnsupportedOperationException("Recent not supported");
    }

    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, Bundle queryArgs) throws FileNotFoundException {
        return queryChildDocuments(parentDocumentId, projection, getSortClause(queryArgs));
    }

    public Cursor queryChildDocumentsForManage(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        throw new UnsupportedOperationException("Manage not supported");
    }

    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        throw new UnsupportedOperationException("Search not supported");
    }

    public void ejectRoot(String rootId) {
        throw new UnsupportedOperationException("Eject not supported");
    }

    public Bundle getDocumentMetadata(String documentId, String[] tags) throws FileNotFoundException {
        throw new UnsupportedOperationException("Metadata not supported");
    }

    public String getDocumentType(String documentId) throws FileNotFoundException {
        Cursor cursor = queryDocument(documentId, null);
        try {
            if (cursor.moveToFirst()) {
                String string = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
                return string;
            }
            IoUtils.closeQuietly(cursor);
            return null;
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        throw new UnsupportedOperationException("Thumbnails not supported");
    }

    public AssetFileDescriptor openTypedDocument(String documentId, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        throw new FileNotFoundException("The requested MIME type is not supported.");
    }

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Pre-Android-O query format not supported.");
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        throw new UnsupportedOperationException("Pre-Android-O query format not supported.");
    }

    public final Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
        try {
            switch (this.mMatcher.match(uri)) {
                case 1:
                    return queryRoots(projection);
                case 3:
                    return queryRecentDocuments(DocumentsContract.getRootId(uri), projection);
                case 4:
                    return querySearchDocuments(DocumentsContract.getRootId(uri), DocumentsContract.getSearchDocumentsQuery(uri), projection);
                case 5:
                case 7:
                    enforceTree(uri);
                    return queryDocument(DocumentsContract.getDocumentId(uri), projection);
                case 6:
                case 8:
                    enforceTree(uri);
                    if (DocumentsContract.isManageMode(uri)) {
                        return queryChildDocumentsForManage(DocumentsContract.getDocumentId(uri), projection, getSortClause(queryArgs));
                    }
                    return queryChildDocuments(DocumentsContract.getDocumentId(uri), projection, queryArgs);
                default:
                    throw new UnsupportedOperationException("Unsupported Uri " + uri);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during query", e);
            return null;
        }
        Log.w(TAG, "Failed during query", e);
        return null;
    }

    private static String getSortClause(Bundle queryArgs) {
        if (queryArgs == null) {
            queryArgs = Bundle.EMPTY;
        }
        String sortClause = queryArgs.getString("android:query-arg-sql-sort-order");
        if (sortClause == null && queryArgs.containsKey("android:query-arg-sort-columns")) {
            return ContentResolver.createSqlSortClause(queryArgs);
        }
        return sortClause;
    }

    public final String getType(Uri uri) {
        try {
            switch (this.mMatcher.match(uri)) {
                case 2:
                    return Root.MIME_TYPE_ITEM;
                case 5:
                case 7:
                    enforceTree(uri);
                    return getDocumentType(DocumentsContract.getDocumentId(uri));
                default:
                    return null;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during getType", e);
            return null;
        }
    }

    public Uri canonicalize(Uri uri) {
        Context context = getContext();
        switch (this.mMatcher.match(uri)) {
            case 7:
                enforceTree(uri);
                Uri narrowUri = DocumentsContract.buildDocumentUri(uri.getAuthority(), DocumentsContract.getDocumentId(uri));
                context.grantUriPermission(getCallingPackage(), narrowUri, getCallingOrSelfUriPermissionModeFlags(context, uri));
                return narrowUri;
            default:
                return null;
        }
    }

    private static int getCallingOrSelfUriPermissionModeFlags(Context context, Uri uri) {
        int modeFlags = 0;
        if (context.checkCallingOrSelfUriPermission(uri, 1) == 0) {
            modeFlags = 1;
        }
        if (context.checkCallingOrSelfUriPermission(uri, 2) == 0) {
            modeFlags |= 2;
        }
        if (context.checkCallingOrSelfUriPermission(uri, 65) == 0) {
            return modeFlags | 64;
        }
        return modeFlags;
    }

    public final Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert not supported");
    }

    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete not supported");
    }

    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }

    public Bundle call(String method, String arg, Bundle extras) {
        if (!method.startsWith("android:")) {
            return super.call(method, arg, extras);
        }
        try {
            return callUnchecked(method, arg, extras);
        } catch (FileNotFoundException e) {
            throw new ParcelableException(e);
        }
    }

    private android.os.Bundle callUnchecked(java.lang.String r35, java.lang.String r36, android.os.Bundle r37) throws java.io.FileNotFoundException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r24_1 'path' android.provider.DocumentsContract$Path) in PHI: PHI: (r24_2 'path' android.provider.DocumentsContract$Path) = (r24_0 'path' android.provider.DocumentsContract$Path), (r24_1 'path' android.provider.DocumentsContract$Path) binds: {(r24_0 'path' android.provider.DocumentsContract$Path)=B:62:0x038c, (r24_1 'path' android.provider.DocumentsContract$Path)=B:71:0x0414}
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
        r34 = this;
        r8 = r34.getContext();
        r20 = new android.os.Bundle;
        r20.<init>();
        r30 = "android:ejectRoot";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x0040;
    L_0x0016:
        r30 = "uri";
        r0 = r37;
        r1 = r30;
        r27 = r0.getParcelable(r1);
        r27 = (android.net.Uri) r27;
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r27;
        r2 = r30;
        r3 = r31;
        r0.enforceWritePermissionInner(r1, r2, r3);
        r26 = android.provider.DocumentsContract.getRootId(r27);
        r0 = r34;
        r1 = r26;
        r0.ejectRoot(r1);
        return r20;
    L_0x0040:
        r30 = "uri";
        r0 = r37;
        r1 = r30;
        r12 = r0.getParcelable(r1);
        r12 = (android.net.Uri) r12;
        r4 = r12.getAuthority();
        r11 = android.provider.DocumentsContract.getDocumentId(r12);
        r0 = r34;
        r0 = r0.mAuthority;
        r30 = r0;
        r0 = r30;
        r30 = r0.equals(r4);
        if (r30 != 0) goto L_0x0090;
    L_0x0063:
        r30 = new java.lang.SecurityException;
        r31 = new java.lang.StringBuilder;
        r31.<init>();
        r32 = "Requested authority ";
        r31 = r31.append(r32);
        r0 = r31;
        r31 = r0.append(r4);
        r32 = " doesn't match provider ";
        r31 = r31.append(r32);
        r0 = r34;
        r0 = r0.mAuthority;
        r32 = r0;
        r31 = r31.append(r32);
        r31 = r31.toString();
        r30.<init>(r31);
        throw r30;
    L_0x0090:
        r0 = r34;
        r0.enforceTree(r12);
        r30 = "android:isChildDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x00ea;
    L_0x00a2:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceReadPermissionInner(r12, r1, r2);
        r30 = "android.content.extra.TARGET_URI";
        r0 = r37;
        r1 = r30;
        r7 = r0.getParcelable(r1);
        r7 = (android.net.Uri) r7;
        r5 = r7.getAuthority();
        r6 = android.provider.DocumentsContract.getDocumentId(r7);
        r31 = "result";
        r0 = r34;
        r0 = r0.mAuthority;
        r30 = r0;
        r0 = r30;
        r30 = r0.equals(r5);
        if (r30 == 0) goto L_0x00e7;
    L_0x00d7:
        r0 = r34;
        r30 = r0.isChildDocument(r11, r6);
    L_0x00dd:
        r0 = r20;
        r1 = r31;
        r2 = r30;
        r0.putBoolean(r1, r2);
    L_0x00e6:
        return r20;
    L_0x00e7:
        r30 = 0;
        goto L_0x00dd;
    L_0x00ea:
        r30 = "android:createDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x0135;
    L_0x00f7:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r30 = "mime_type";
        r0 = r37;
        r1 = r30;
        r15 = r0.getString(r1);
        r30 = "_display_name";
        r0 = r37;
        r1 = r30;
        r9 = r0.getString(r1);
        r0 = r34;
        r17 = r0.createDocument(r11, r15, r9);
        r0 = r17;
        r18 = android.provider.DocumentsContract.buildDocumentUriMaybeUsingTree(r12, r0);
        r30 = "uri";
        r0 = r20;
        r1 = r30;
        r2 = r18;
        r0.putParcelable(r1, r2);
        goto L_0x00e6;
    L_0x0135:
        r30 = "android:createWebLinkIntent";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x0170;
    L_0x0142:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r30 = "options";
        r0 = r37;
        r1 = r30;
        r19 = r0.getBundle(r1);
        r0 = r34;
        r1 = r19;
        r13 = r0.createWebLinkIntent(r11, r1);
        r30 = "result";
        r0 = r20;
        r1 = r30;
        r0.putParcelable(r1, r13);
        goto L_0x00e6;
    L_0x0170:
        r30 = "android:renameDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x01cf;
    L_0x017d:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r30 = "_display_name";
        r0 = r37;
        r1 = r30;
        r9 = r0.getString(r1);
        r0 = r34;
        r17 = r0.renameDocument(r11, r9);
        if (r17 == 0) goto L_0x00e6;
    L_0x019f:
        r0 = r17;
        r18 = android.provider.DocumentsContract.buildDocumentUriMaybeUsingTree(r12, r0);
        r30 = android.provider.DocumentsContract.isTreeUri(r18);
        if (r30 != 0) goto L_0x01bc;
    L_0x01ab:
        r16 = getCallingOrSelfUriPermissionModeFlags(r8, r12);
        r30 = r34.getCallingPackage();
        r0 = r30;
        r1 = r18;
        r2 = r16;
        r8.grantUriPermission(r0, r1, r2);
    L_0x01bc:
        r30 = "uri";
        r0 = r20;
        r1 = r30;
        r2 = r18;
        r0.putParcelable(r1, r2);
        r0 = r34;
        r0.revokeDocumentPermission(r11);
        goto L_0x00e6;
    L_0x01cf:
        r30 = "android:deleteDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x01f7;
    L_0x01dc:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r0 = r34;
        r0.deleteDocument(r11);
        r0 = r34;
        r0.revokeDocumentPermission(r11);
        goto L_0x00e6;
    L_0x01f7:
        r30 = "android:copyDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x026a;
    L_0x0204:
        r30 = "android.content.extra.TARGET_URI";
        r0 = r37;
        r1 = r30;
        r29 = r0.getParcelable(r1);
        r29 = (android.net.Uri) r29;
        r28 = android.provider.DocumentsContract.getDocumentId(r29);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceReadPermissionInner(r12, r1, r2);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r29;
        r2 = r30;
        r3 = r31;
        r0.enforceWritePermissionInner(r1, r2, r3);
        r0 = r34;
        r1 = r28;
        r17 = r0.copyDocument(r11, r1);
        if (r17 == 0) goto L_0x00e6;
    L_0x023f:
        r0 = r17;
        r18 = android.provider.DocumentsContract.buildDocumentUriMaybeUsingTree(r12, r0);
        r30 = android.provider.DocumentsContract.isTreeUri(r18);
        if (r30 != 0) goto L_0x025c;
    L_0x024b:
        r16 = getCallingOrSelfUriPermissionModeFlags(r8, r12);
        r30 = r34.getCallingPackage();
        r0 = r30;
        r1 = r18;
        r2 = r16;
        r8.grantUriPermission(r0, r1, r2);
    L_0x025c:
        r30 = "uri";
        r0 = r20;
        r1 = r30;
        r2 = r18;
        r0.putParcelable(r1, r2);
        goto L_0x00e6;
    L_0x026a:
        r30 = "android:moveDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x0301;
    L_0x0277:
        r30 = "parentUri";
        r0 = r37;
        r1 = r30;
        r23 = r0.getParcelable(r1);
        r23 = (android.net.Uri) r23;
        r22 = android.provider.DocumentsContract.getDocumentId(r23);
        r30 = "android.content.extra.TARGET_URI";
        r0 = r37;
        r1 = r30;
        r29 = r0.getParcelable(r1);
        r29 = (android.net.Uri) r29;
        r28 = android.provider.DocumentsContract.getDocumentId(r29);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r23;
        r2 = r30;
        r3 = r31;
        r0.enforceReadPermissionInner(r1, r2, r3);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r29;
        r2 = r30;
        r3 = r31;
        r0.enforceWritePermissionInner(r1, r2, r3);
        r0 = r34;
        r1 = r22;
        r2 = r28;
        r17 = r0.moveDocument(r11, r1, r2);
        if (r17 == 0) goto L_0x00e6;
    L_0x02d6:
        r0 = r17;
        r18 = android.provider.DocumentsContract.buildDocumentUriMaybeUsingTree(r12, r0);
        r30 = android.provider.DocumentsContract.isTreeUri(r18);
        if (r30 != 0) goto L_0x02f3;
    L_0x02e2:
        r16 = getCallingOrSelfUriPermissionModeFlags(r8, r12);
        r30 = r34.getCallingPackage();
        r0 = r30;
        r1 = r18;
        r2 = r16;
        r8.grantUriPermission(r0, r1, r2);
    L_0x02f3:
        r30 = "uri";
        r0 = r20;
        r1 = r30;
        r2 = r18;
        r0.putParcelable(r1, r2);
        goto L_0x00e6;
    L_0x0301:
        r30 = "android:removeDocument";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x0348;
    L_0x030e:
        r30 = "parentUri";
        r0 = r37;
        r1 = r30;
        r23 = r0.getParcelable(r1);
        r23 = (android.net.Uri) r23;
        r22 = android.provider.DocumentsContract.getDocumentId(r23);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r23;
        r2 = r30;
        r3 = r31;
        r0.enforceReadPermissionInner(r1, r2, r3);
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceWritePermissionInner(r12, r1, r2);
        r0 = r34;
        r1 = r22;
        r0.removeDocument(r11, r1);
        goto L_0x00e6;
    L_0x0348:
        r30 = "android:findDocumentPath";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x046b;
    L_0x0355:
        r14 = android.provider.DocumentsContract.isTreeUri(r12);
        if (r14 == 0) goto L_0x0402;
    L_0x035b:
        r30 = r34.getCallingPackage();
        r31 = 0;
        r0 = r34;
        r1 = r30;
        r2 = r31;
        r0.enforceReadPermissionInner(r12, r1, r2);
    L_0x036a:
        if (r14 == 0) goto L_0x0410;
    L_0x036c:
        r21 = android.provider.DocumentsContract.getTreeDocumentId(r12);
    L_0x0370:
        r0 = r34;
        r1 = r21;
        r24 = r0.findDocumentPath(r1, r11);
        if (r14 == 0) goto L_0x045d;
    L_0x037a:
        r30 = r24.getPath();
        r31 = 0;
        r30 = r30.get(r31);
        r0 = r30;
        r1 = r21;
        r30 = java.util.Objects.equals(r0, r1);
        if (r30 != 0) goto L_0x041f;
    L_0x038e:
        r31 = "DocumentsProvider";
        r30 = new java.lang.StringBuilder;
        r30.<init>();
        r32 = "Provider doesn't return path from the tree root. Expected: ";
        r0 = r30;
        r1 = r32;
        r30 = r0.append(r1);
        r0 = r30;
        r1 = r21;
        r30 = r0.append(r1);
        r32 = " found: ";
        r0 = r30;
        r1 = r32;
        r32 = r0.append(r1);
        r30 = r24.getPath();
        r33 = 0;
        r0 = r30;
        r1 = r33;
        r30 = r0.get(r1);
        r30 = (java.lang.String) r30;
        r0 = r32;
        r1 = r30;
        r30 = r0.append(r1);
        r30 = r30.toString();
        r0 = r31;
        r1 = r30;
        android.util.Log.wtf(r0, r1);
        r10 = new java.util.LinkedList;
        r30 = r24.getPath();
        r0 = r30;
        r10.<init>(r0);
    L_0x03e2:
        r30 = r10.size();
        r31 = 1;
        r0 = r30;
        r1 = r31;
        if (r0 <= r1) goto L_0x0414;
    L_0x03ee:
        r30 = r10.getFirst();
        r0 = r30;
        r1 = r21;
        r30 = java.util.Objects.equals(r0, r1);
        r30 = r30 ^ 1;
        if (r30 == 0) goto L_0x0414;
    L_0x03fe:
        r10.removeFirst();
        goto L_0x03e2;
    L_0x0402:
        r30 = r34.getContext();
        r31 = "android.permission.MANAGE_DOCUMENTS";
        r32 = 0;
        r30.enforceCallingPermission(r31, r32);
        goto L_0x036a;
    L_0x0410:
        r21 = 0;
        goto L_0x0370;
    L_0x0414:
        r24 = new android.provider.DocumentsContract$Path;
        r30 = 0;
        r0 = r24;
        r1 = r30;
        r0.<init>(r1, r10);
    L_0x041f:
        r30 = r24.getRootId();
        if (r30 == 0) goto L_0x045d;
    L_0x0425:
        r30 = "DocumentsProvider";
        r31 = new java.lang.StringBuilder;
        r31.<init>();
        r32 = "Provider returns root id :";
        r31 = r31.append(r32);
        r32 = r24.getRootId();
        r31 = r31.append(r32);
        r32 = " unexpectedly. Erase root id.";
        r31 = r31.append(r32);
        r31 = r31.toString();
        android.util.Log.wtf(r30, r31);
        r25 = new android.provider.DocumentsContract$Path;
        r30 = r24.getPath();
        r31 = 0;
        r0 = r25;
        r1 = r31;
        r2 = r30;
        r0.<init>(r1, r2);
        r24 = r25;
    L_0x045d:
        r30 = "result";
        r0 = r20;
        r1 = r30;
        r2 = r24;
        r0.putParcelable(r1, r2);
        goto L_0x00e6;
    L_0x046b:
        r30 = "android:getDocumentMetadata";
        r0 = r30;
        r1 = r35;
        r30 = r0.equals(r1);
        if (r30 == 0) goto L_0x048c;
    L_0x0478:
        r30 = "android:documentMetadataTags";
        r0 = r37;
        r1 = r30;
        r30 = r0.getStringArray(r1);
        r0 = r34;
        r1 = r30;
        r30 = r0.getDocumentMetadata(r11, r1);
        return r30;
    L_0x048c:
        r30 = new java.lang.UnsupportedOperationException;
        r31 = new java.lang.StringBuilder;
        r31.<init>();
        r32 = "Method not supported ";
        r31 = r31.append(r32);
        r0 = r31;
        r1 = r35;
        r31 = r0.append(r1);
        r31 = r31.toString();
        r30.<init>(r31);
        throw r30;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.provider.DocumentsProvider.callUnchecked(java.lang.String, java.lang.String, android.os.Bundle):android.os.Bundle");
    }

    public final void revokeDocumentPermission(String documentId) {
        Context context = getContext();
        context.revokeUriPermission(DocumentsContract.buildDocumentUri(this.mAuthority, documentId), -1);
        context.revokeUriPermission(DocumentsContract.buildTreeDocumentUri(this.mAuthority, documentId), -1);
    }

    public final ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(DocumentsContract.getDocumentId(uri), mode, null);
    }

    public final ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(DocumentsContract.getDocumentId(uri), mode, signal);
    }

    public final AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        enforceTree(uri);
        ParcelFileDescriptor fd = openDocument(DocumentsContract.getDocumentId(uri), mode, null);
        if (fd != null) {
            return new AssetFileDescriptor(fd, 0, -1);
        }
        return null;
    }

    public final AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        enforceTree(uri);
        ParcelFileDescriptor fd = openDocument(DocumentsContract.getDocumentId(uri), mode, signal);
        if (fd != null) {
            return new AssetFileDescriptor(fd, 0, -1);
        }
        return null;
    }

    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, mimeTypeFilter, opts, null);
    }

    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, mimeTypeFilter, opts, signal);
    }

    public String[] getDocumentStreamTypes(String documentId, String mimeTypeFilter) {
        AutoCloseable autoCloseable = null;
        String[] strArr = null;
        try {
            autoCloseable = queryDocument(documentId, null);
            if (autoCloseable.moveToFirst()) {
                String mimeType = autoCloseable.getString(autoCloseable.getColumnIndexOrThrow("mime_type"));
                if ((512 & autoCloseable.getLong(autoCloseable.getColumnIndexOrThrow("flags"))) == 0 && mimeType != null && mimeTypeMatches(mimeTypeFilter, mimeType)) {
                    strArr = new String[]{mimeType};
                    return strArr;
                }
            }
            IoUtils.closeQuietly(autoCloseable);
            return null;
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            IoUtils.closeQuietly(autoCloseable);
        }
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        enforceTree(uri);
        return getDocumentStreamTypes(DocumentsContract.getDocumentId(uri), mimeTypeFilter);
    }

    private final AssetFileDescriptor openTypedAssetFileImpl(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        enforceTree(uri);
        String documentId = DocumentsContract.getDocumentId(uri);
        if (opts != null && opts.containsKey("android.content.extra.SIZE")) {
            return openDocumentThumbnail(documentId, (Point) opts.getParcelable("android.content.extra.SIZE"), signal);
        }
        if ("*/*".equals(mimeTypeFilter)) {
            return openAssetFile(uri, "r");
        }
        String baseType = getType(uri);
        if (baseType == null || !ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
            return openTypedDocument(documentId, mimeTypeFilter, opts, signal);
        }
        return openAssetFile(uri, "r");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean mimeTypeMatches(String filter, String test) {
        if (test == null) {
            return false;
        }
        if (filter == null || "*/*".equals(filter) || filter.equals(test)) {
            return true;
        }
        if (filter.endsWith("/*")) {
            return filter.regionMatches(0, test, 0, filter.indexOf(47));
        }
        return false;
    }
}
