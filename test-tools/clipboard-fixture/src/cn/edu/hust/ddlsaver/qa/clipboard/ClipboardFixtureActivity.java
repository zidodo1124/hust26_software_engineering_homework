package cn.edu.hust.ddlsaver.qa.clipboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A deliberately separate QA application. It never reads the current clipboard. */
public final class ClipboardFixtureActivity extends Activity {
    private TextView status;
    private ClipboardManager clipboard;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        final int spacing = (int) (20 * getResources().getDisplayMetrics().density);
        ScrollView scroll = new ScrollView(this);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(spacing, spacing, spacing, spacing);
        scroll.addView(content);
        TextView heading = new TextView(this);
        heading.setText("QA TEST TOOL\nSynthetic image clipboard fixtures\n\n"
            + "Buttons replace the clipboard with test content. No personal clipboard reads, "
            + "network access, or shared-storage access. Return to DDL Saver and tap Paste Image.");
        heading.setTextSize(18);
        content.addView(heading);
        addButton(content, "Copy blue", view -> copy("QA1027-blue-landscape.png"));
        addButton(content, "Copy orange", view -> copy("QA1027-orange-portrait.png"));
        addButton(content, "Copy two images", view -> copy(
            "QA1027-blue-landscape.png", "QA1027-orange-portrait.png"));
        addButton(content, "Copy broken image", view -> copy("QA1027-broken.png"));
        addButton(content, "Clear clipboard", view -> {
            if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip();
            else clipboard.setPrimaryClip(ClipData.newPlainText("QA empty", ""));
            status.setText("Clipboard cleared. No previous clipboard was read.");
        });
        addButton(content, "Copy text", view -> {
            clipboard.setPrimaryClip(ClipData.newPlainText("QA text", "https://example.invalid/qa-image.png"));
            status.setText("Copied plain-text URL. It is not an image URI.");
        });
        status = new TextView(this);
        status.setText("Ready. Nothing has been copied yet.");
        status.setTextSize(16);
        status.setPadding(0, spacing, 0, spacing);
        content.addView(status);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(click);
        parent.addView(button, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void copy(String... names) {
        try {
            Uri first = FixtureProvider.uriFor(names[0]);
            ClipData clip = ClipData.newUri(getContentResolver(), "QA synthetic images", first);
            for (int i = 1; i < names.length; i++) {
                clip.addItem(getContentResolver(), new ClipData.Item(FixtureProvider.uriFor(names[i])));
            }
            // Standard Android clipboard URI grant handling; no direct grant to DDL Saver.
            clipboard.setPrimaryClip(clip);
            status.setText("Copied " + names.length + " synthetic image URI(s). Return to DDL Saver.");
        } catch (Exception failure) {
            status.setText("Fixture copy failed: " + failure.getClass().getSimpleName());
        }
    }
}
