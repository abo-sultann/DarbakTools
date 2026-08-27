package com.abosultan.darbaktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.List;

public class TransferActivity extends Activity {
    private static final int REQ_CAPTURE = 81;
    private final Handler handler = new Handler();
    private TextView address;
    private TextView serverState;
    private TextView captureState;
    private TextView touchState;
    private TextView networkDetails;
    private ImageView qr;
    private Button captureButton;
    private String currentUrl;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1400);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DarbakServerService.ensureStarted(this);
        setContentView(buildUi());
        handler.post(updater);
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackground(Ui.verticalGradient(this));
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14));
        Ui.rtl(root);

        LinearLayout info = Ui.card(this);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -1, 1);
        ip.setMargins(Ui.dp(this, 7), Ui.dp(this, 5), Ui.dp(this, 7), Ui.dp(this, 5));
        root.addView(info, ip);

        TextView title = Ui.title(this, "الآيفون والتحكم", 27);
        title.setTextColor(Ui.GOLD);
        info.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));

        TextView hint = Ui.title(this, "افتح العنوان في Safari. الصفحة تجمع التحكم بالشاشة والملفات والتطبيقات والروابط.", 14);
        hint.setTextColor(Ui.MUTED);
        info.addView(hint, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));

        address = Ui.title(this, "جاري اكتشاف الشبكة…", 21);
        address.setTextColor(Color.WHITE);
        address.setGravity(Gravity.CENTER);
        address.setBackground(Ui.rounded(Ui.PANEL_2, Ui.LINE, 13, this));
        info.addView(address, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));
        address.setOnClickListener(v -> copyAddress());

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(statusRow);
        info.addView(statusRow, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        serverState = Ui.pill(this, "الخادم…", Ui.MUTED);
        captureState = Ui.pill(this, "العرض…", Ui.MUTED);
        touchState = Ui.pill(this, "اللمس…", Ui.MUTED);
        statusRow.addView(serverState, Ui.weighted(1, 3, this));
        statusRow.addView(captureState, Ui.weighted(1, 3, this));
        statusRow.addView(touchState, Ui.weighted(1, 3, this));

        LinearLayout actions1 = new LinearLayout(this);
        actions1.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(actions1);
        info.addView(actions1, new LinearLayout.LayoutParams(-1, Ui.dp(this, 66)));
        captureButton = Ui.primaryButton(this, "تفعيل عرض الشاشة");
        Button accessibility = Ui.button(this, "تفعيل التحكم باللمس");
        actions1.addView(captureButton, Ui.weighted(1, 4, this));
        actions1.addView(accessibility, Ui.weighted(1, 4, this));

        LinearLayout actions2 = new LinearLayout(this);
        actions2.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(actions2);
        info.addView(actions2, new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));
        Button inbox = Ui.button(this, "فتح الوارد");
        Button copy = Ui.button(this, "نسخ العنوان");
        actions2.addView(inbox, Ui.weighted(1, 4, this));
        actions2.addView(copy, Ui.weighted(1, 4, this));

        networkDetails = Ui.title(this, "", 12);
        networkDetails.setTextColor(Ui.MUTED);
        networkDetails.setGravity(Gravity.RIGHT | Gravity.TOP);
        info.addView(networkDetails, new LinearLayout.LayoutParams(-1, 0, 1));

        captureButton.setOnClickListener(v -> requestScreenCapture());
        accessibility.setOnClickListener(v -> openAccessibilitySettings());
        inbox.setOnClickListener(v -> {
            Intent i = new Intent(this, FileBrowserActivity.class);
            i.putExtra("path", AppPaths.inbox().getAbsolutePath());
            startActivity(i);
        });
        copy.setOnClickListener(v -> copyAddress());

        LinearLayout qrPanel = new LinearLayout(this);
        qrPanel.setOrientation(LinearLayout.VERTICAL);
        qrPanel.setGravity(Gravity.CENTER);
        qrPanel.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        qrPanel.setBackground(Ui.rounded(Color.rgb(245, 247, 246), Ui.GOLD, 20, this));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(Ui.dp(this, 330), -1);
        qp.setMargins(Ui.dp(this, 7), Ui.dp(this, 5), Ui.dp(this, 7), Ui.dp(this, 5));
        root.addView(qrPanel, qp);

        TextView qTitle = Ui.title(this, "افتح من الآيفون", 19);
        qTitle.setTextColor(Ui.BG);
        qTitle.setGravity(Gravity.CENTER);
        qrPanel.addView(qTitle, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        qr = new ImageView(this);
        qr.setAdjustViewBounds(true);
        qrPanel.addView(qr, new LinearLayout.LayoutParams(Ui.dp(this, 260), Ui.dp(this, 260)));

        TextView qrText = Ui.title(this, "امسح الرمز بالكاميرا\nأو اكتب العنوان في Safari", 14);
        qrText.setTextColor(Color.rgb(44, 61, 55));
        qrText.setGravity(Gravity.CENTER);
        qrPanel.addView(qrText, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private void refresh() {
        String ip = NetworkUtils.localIpv4(this);
        boolean server = DarbakServerService.isRunning();
        int port = DarbakServerService.getPort();
        String url = server && ip != null ? "http://" + ip + ":" + port : null;

        serverState.setText(server ? "● الخادم يعمل" : "● الخادم متوقف");
        serverState.setTextColor(server ? Ui.GREEN : Ui.RED);
        boolean capture = ScreenCaptureService.isRunning() && ScreenCaptureService.getLatestFrame() != null;
        captureState.setText(capture ? "● العرض يعمل" : "● العرض متوقف");
        captureState.setTextColor(capture ? Ui.GREEN : Ui.GOLD);
        boolean touch = RemoteAccessibilityService.isReady();
        touchState.setText(touch ? "● اللمس يعمل" : "● اللمس متوقف");
        touchState.setTextColor(touch ? Ui.GREEN : Ui.GOLD);
        captureButton.setText(capture ? "عرض الشاشة مفعّل" : "تفعيل عرض الشاشة");

        if (url != null) {
            address.setText(url);
            if (!url.equals(currentUrl)) {
                currentUrl = url;
                qr.setImageBitmap(makeQr(url, 300));
            }
        } else {
            currentUrl = null;
            qr.setImageDrawable(null);
            address.setText(server ? "الخادم جاهز • بانتظار اتصال الشبكة" : "تعذر تشغيل الخادم");
        }

        List<String> ips = NetworkUtils.allLocalIpv4();
        StringBuilder details = new StringBuilder();
        if (ips.size() > 1) {
            details.append("عناوين الشبكة البديلة: ");
            for (int i = 0; i < ips.size(); i++) {
                if (i > 0) details.append("  •  ");
                details.append("http://").append(ips.get(i)).append(':').append(port);
            }
            details.append("\n");
        }
        details.append("إذا فتح Safari الصفحة فالاتصال صحيح. لعرض صورة الشاشة فعّل العرض، وللمس فعّل خدمة إمكانية الوصول مرة واحدة.");
        String error = DarbakServerService.getLastError();
        if (!server && error.length() > 0) details.append("\nخطأ الخادم: ").append(error);
        networkDetails.setText(details.toString());
    }

    private void requestScreenCapture() {
        if (ScreenCaptureService.isRunning()) {
            Toast.makeText(this, "عرض الشاشة مفعّل بالفعل", Toast.LENGTH_SHORT).show();
            return;
        }
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "مشاركة الشاشة غير مدعومة على هذا النظام", Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(manager.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    private void openAccessibilitySettings() {
        if (RemoteAccessibilityService.isReady()) {
            Toast.makeText(this, "التحكم باللمس مفعّل بالفعل", Toast.LENGTH_SHORT).show();
            return;
        }

        int result = AccessibilitySettingsHelper.open(this);
        if (result == AccessibilitySettingsHelper.OPENED_ACCESSIBILITY) {
            Toast.makeText(this, "فعّل خدمة «دربك للتحكم» ثم ارجع للتطبيق", Toast.LENGTH_LONG).show();
            return;
        }

        if (result == AccessibilitySettingsHelper.OPENED_GENERAL_SETTINGS) {
            new AlertDialog.Builder(this)
                    .setTitle("إعدادات شاشة السيارة")
                    .setMessage("روم الشاشة لا يفتح صفحة إمكانية الوصول مباشرة. ابحث داخل الإعدادات عن «إمكانية الوصول» أو Accessibility ثم فعّل «دربك للتحكم». إذا لم تجد القائمة أصلًا، يمكننا تفعيلها مرة واحدة عبر ADB أو Root.")
                    .setPositiveButton("حسنًا", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("تعذر فتح إعدادات النظام")
                .setMessage("هذا الروم يبدو أنه أخفى إعدادات إمكانية الوصول. التحكم باللمس يحتاج تفعيل خدمة «دربك للتحكم» مرة واحدة. البديل سيكون عبر ADB أو Root على هذه الشاشة.")
                .setPositiveButton("حسنًا", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenCaptureService.startCapture(this, resultCode, data);
                Toast.makeText(this, "تم تفعيل عرض الشاشة", Toast.LENGTH_SHORT).show();
                handler.postDelayed(this::refresh, 900);
            } else {
                Toast.makeText(this, "لم يتم السماح بعرض الشاشة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap makeQr(String text, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void copyAddress() {
        if (currentUrl == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("DarbakTools", currentUrl));
        Toast.makeText(this, "تم نسخ العنوان", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
