package cn.edu.hust.ddlsaver.qa.clipboard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only four immutable bundled fixtures may be read, through individually granted URIs. */
public final class FixtureProvider extends ContentProvider {
    public static final String AUTHORITY = "cn.edu.hust.ddlsaver.qa.clipboard.files";
    private static final Set<String> FILES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "QA1027-blue-landscape.png", "QA1027-orange-portrait.png",
        "QA1027-exif-rotate90.jpg", "QA1027-broken.png")));
    private File root;

    public static Uri uriFor(String name) {
        if (!FILES.contains(name)) throw new IllegalArgumentException("Unknown QA fixture");
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(name).build();
    }

    @Override public boolean onCreate() {
        root = new File(getContext().getCacheDir(), "qa-clipboard-fixtures");
        if (!root.isDirectory() && !root.mkdirs()) return false;
        try {
            for (String name : FILES) {
                File target = new File(root, name);
                if (!target.exists()) {
                    try (InputStream source = getContext().getAssets().open(name);
                         FileOutputStream destination = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = source.read(buffer)) != -1) destination.write(buffer, 0, count);
                    }
                }
            }
            return true;
        } catch (IOException failure) {
            return false;
        }
    }

    private String name(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (!"content".equals(uri.getScheme()) || !AUTHORITY.equals(uri.getAuthority())
                || uri.getQuery() != null || uri.getFragment() != null
                || segments.size() != 1 || !FILES.contains(segments.get(0))) {
            throw new IllegalArgumentException("Only a named bundled QA fixture is available");
        }
        return segments.get(0);
    }

    @Override public String getType(Uri uri) {
        return name(uri).endsWith(".jpg") ? "image/jpeg" : "image/png";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String name = name(uri);
        String[] columns = projection == null
            ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = name;
            else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = new File(root, name).length();
        }
        cursor.addRow(values);
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("QA fixtures are read-only");
        return ParcelFileDescriptor.open(new File(root, name(uri)), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only QA fixtures");
    }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only QA fixtures");
    }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only QA fixtures");
    }
}
