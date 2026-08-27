package com.abosultan.darbaktools;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class TransferActivity extends Activity implements TransferServer.Listener {
    private TransferServer server;
    private TextView status;
    private TextView address;
    private ImageView qr;
    private Button latestLinkButton;
    private String latestLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(buildUi());
        startServer();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 22), Ui.dp(this, 16), Ui.dp(this, 22), Ui.dp(this, 16));
        Ui.rtl(root);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        root.addView(info, new LinearLayout.LayoutParams(0, -1, 1.25f));

        TextView title = Ui.title(this, "اتصال الآيفون", 28);
        title.setTextColor(Ui.GOLD);
        info.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));

        TextView hint = Ui.title(this, "اجعل الآيفون والشاشة على نفس الشبكة، ثم امسح الرمز أو افتح العنوان في Safari.", 17);
        hint.setTextColor(Ui.MUTED);
        info.addView(hint, new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        address = Ui.title(this, "جاري اكتشاف الشبكة...", 22);
        address.setTextColor(Color.WHITE);
        address.setGravity(Gravity.CENTER);
        address.setBackground(Ui.rounded(Ui.PANEL, Ui.GOLD, 12, this));
        info.addView(address, new LinearLayout.LayoutParams(-1, Ui.dp(this, 64)));

        status = Ui.title(this, "", 16);
        status.setTextColor(Ui.GOLD);
        info.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        info.addView(buttons, new LinearLayout.LayoutParams(-1, 0, 1));

        Button inbox = Ui.button(this, "فتح الوارد");
        Button copy = Ui.button(this, "نسخ العنوان");
        latestLinkButton = Ui.button(this, "فتح آخر رابط");
        latestLinkButton.setEnabled(false);
        buttons.addView(inbox, Ui.weighted(1, 5, this));
        buttons.addView(copy, Ui.weighted(1, 5, this));
        buttons.addView(latestLinkButton, Ui.weighted(1, 5, this));

        inbox.setOnClickListener(v -> {
            Intent i = new Intent(this, FileBrowserActivity.class);
            i.putExtra("path", AppPaths.inbox().getAbsolutePath());
            startActivity(i);
        });
        copy.setOnClickListener(v -> copyAddress());
        latestLinkButton.setOnClickListener(v -> openLatestLink());

        LinearLayout qrPanel = new LinearLayout(this);
        qrPanel.setOrientation(LinearLayout.VERTICAL);
        qrPanel.setGravity(Gravity.CENTER);
        qrPanel.setBackground(Ui.rounded(Color.WHITE, Ui.GOLD, 18, this));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(Ui.dp(this, 360), -1);
        qp.setMargins(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        root.addView(qrPanel, qp);

        qr = new ImageView(this);
        qr.setAdjustViewBounds(true);
        qrPanel.addView(qr, new LinearLayout.LayoutParams(Ui.dp(this, 320), Ui.dp(this, 320)));

        TextView qrText = Ui.title(this, "امسح بالكاميرا", 18);
        qrText.setTextColor(Ui.BG);
        qrText.setGravity(Gravity.CENTER);
        qrPanel.addView(qrText, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        return root;
    }

    private void startServer() {
        String ip = NetworkUtils.localIpv4();
        if (ip == null) {
            address.setText("لا يوجد اتصال شبكة");
            status.setText("اربط الشاشة والآيفون بنفس Wi‑Fi ثم أعد فتح الصفحة.");
            return;
        }
        String url = "http://" + ip + ":8080";
        try {
            server = new TransferServer(8080, AppPaths.inbox(), AppPaths.linksFile(), this);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            address.setText(url);
            status.setText("الخادم يعمل • جاهز لاستقبال الملفات");
            qr.setImageBitmap(makeQr(url, 320));
        } catch (IOException e) {
            address.setText(url);
            status.setText("تعذر تشغيل الخادم على المنفذ 8080: " + e.getMessage());
        }
    }

    private Bitmap makeQr(String text, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void copyAddress() {
        String value = address.getText().toString();
        if (!value.startsWith("http")) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("DarbakTools", value));
        Toast.makeText(this, "تم نسخ العنوان", Toast.LENGTH_SHORT).show();
    }

    private void openLatestLink() {
        if (latestLink == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(latestLink)));
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileReceived(String fileName) {
        runOnUiThread(() -> status.setText("تم استلام: " + fileName));
    }

    @Override
    public void onLinkReceived(String url) {
        runOnUiThread(() -> {
            latestLink = url;
            latestLinkButton.setEnabled(true);
            status.setText("وصل رابط من الآيفون • اضغط فتح آخر رابط");
        });
    }

    @Override
    protected void onDestroy() {
        if (server != null) server.stop();
        super.onDestroy();
    }
}
