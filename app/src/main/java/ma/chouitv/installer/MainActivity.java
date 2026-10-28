package ma.chouitv.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ma.chouitv.installer.cleanup.CleanupData;
import ma.chouitv.installer.cleanup.Executor;
import ma.chouitv.installer.cleanup.Facts;
import ma.chouitv.installer.cleanup.Planner;
import ma.chouitv.installer.cleanup.Reactivation;
import ma.chouitv.installer.profiles.Preview;
import ma.chouitv.installer.profiles.Profile;
import ma.chouitv.installer.profiles.ProfileStore;
import ma.chouitv.installer.cleanup.Restore;
import ma.chouitv.installer.cleanup.Shell;

import static ma.chouitv.installer.Strings.t;

/**
 * ChouiTV Installer: six screens (language, prepare, connect, install, personalise, done) in ChouiTV's own look: dark green, gold,
 * glass cards, Thmanyah, and a Moroccan touch (zellige lattice, horseshoe arch, eight-point stars). Arabic, French or English.
 * It talks to the TV over classic ADB (Network debugging, port 5555).
 */
public class MainActivity extends Activity {
    private static final int GOLD = 0xFFD6AF6B, SAND = 0xFFF6EFE0, MUTED = 0xFFC6BBA4, DARK = 0xFF040E09;
    private static final int PICK_SETTINGS = 7, AUTH_SECONDS = 120;

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Typeface display, medium, regular, script, ruqaa;
    private FrameLayout root;
    private int step;
    private Adb tv;
    private Flow flow;
    private String model = "";
    private volatile boolean busy;
    private Flow.Home homeResult;
    private String tvBrand = "", tvModel = "";
    private String chosen = "";
    private Runnable ticker;

    // personalisation
    private String city = "الدار البيضاء", tz = "Africa/Casablanca", method = "21";
    private boolean adhanOn = true;
    private List<String[]> cityList = new ArrayList<>();
    private String importedB64;
    private TextView cityV, tzV, methodV, adhanV, importV;

    private static final String[] ZONES = {"Africa/Casablanca", "city", "Africa/Algiers", "Africa/Tunis", "Africa/Cairo", "Asia/Riyadh", "Asia/Dubai", "Europe/Paris"};
    private static final String[] METHODS = {"21", "auto", "19", "18", "5", "4", "3"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        display = Typeface.createFromAsset(getAssets(), "fonts/ThmanyahDisplay-Bold.otf");
        medium = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf");
        regular = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Regular.otf");
        script = Typeface.createFromAsset(getAssets(), "fonts/GreatVibes-Regular.ttf");
        ruqaa = Typeface.createFromAsset(getAssets(), "fonts/ArefRuqaa-Bold.ttf");
        SharedPreferences sp = getSharedPreferences("installer", MODE_PRIVATE);
        String saved = sp.getString("lang", null);
        if (saved == null) {
            String l = Locale.getDefault().getLanguage();
            saved = l.equals("ar") || l.equals("fr") ? l : "en";
        }
        Strings.lang = saved;
        root = new FrameLayout(this);
        root.setBackground(background());
        setContentView(root);
        show(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTicker();
        if (tv != null) tv.close();
        bg.shutdownNow();
    }

    @Override
    public void onBackPressed() {
        if (busy) return;
        if (step == 12) show(0);
        else if (step == 8 || step == 9 || step == 10) { if (cleanFromDone) show(5); else show(12); }
        else if (step == 11 || step == 13 || step == 14) show(12);
        else if (step == 15 || step == 16) show(14);
        else if (step == 1) show(12);
        else if (step == 2) show(1);
        else super.onBackPressed();
    }

    // ---------- the six screens ----------

    private void show(int n) {
        step = n;
        stopTicker();
        root.setLayoutDirection(Strings.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.removeAllViews();
        root.addView(new Orn.Lattice(this), new FrameLayout.LayoutParams(-1, -1));
        root.addView(new Orn.Strip(this), new FrameLayout.LayoutParams(-1, dp(24), Gravity.TOP));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(22), dp(48), dp(22), dp(18));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        if (n >= 1 && n <= 4) {
            col.addView(dots(n));
            String[][] k = {{"prep_title", "prep_sub"}, {"conn_title", "conn_sub"}, {"install_title", "install_sub"}, {"custom_title", "custom_sub"}};
            col.addView(text(t(k[n - 1][0]), display, 30, SAND), lp(0, 22, 0, 0));
            col.addView(text(t(k[n - 1][1]), regular, 15, MUTED), lp(0, 4, 0, 6));
            col.addView(new Orn.Rule(this), new LinearLayout.LayoutParams(-1, dp(22)));
            col.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(8)));
        }
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        sv.addView(body, new ViewGroup.LayoutParams(-1, -2));
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView button = button("");
        col.addView(button, lp(0, 12, 0, 0));
        switch (n) {
            case 0: language(body, button); break;
            case 1: prepare(body, button); break;
            case 2: connect(body, button); break;
            case 3: install(body, button); break;
            case 4: personalise(body, button); break;
            case 12: hub(body, button); break;
            case 8: cleanupPlan(body, button); break;
            case 9: cleanupRun(body, button); break;
            case 10: cleanupResult(body, button); break;
            case 11: reactivate(body, button); break;
            case 13: reactivated(body, button); break;
            case 14: settingsHome(body, button); break;
            case 15: sendScreen(body, button); break;
            case 16: sentScreen(body, button); break;
            default: done(body, button); break;
        }
    }

    private void language(final LinearLayout body, TextView button) {
        Orn.Arch arch = new Orn.Arch(this);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setGravity(Gravity.CENTER);
        in.setPadding(dp(18), dp(40), dp(18), dp(20));
        TextView logo = text("ChouiTV", script, 54, SAND);
        logo.setGravity(Gravity.CENTER);
        gold(logo);
        in.addView(logo, new LinearLayout.LayoutParams(-1, -2));
        TextView tag = text(t("tagline"), Strings.rtl() ? ruqaa : medium, Strings.rtl() ? 22 : 16, SAND);
        tag.setGravity(Gravity.CENTER);
        in.addView(tag, lp(0, 12, 0, 0));
        arch.addView(in, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(300), dp(250));
        ap.gravity = Gravity.CENTER_HORIZONTAL;
        ap.topMargin = dp(8);
        body.addView(arch, ap);
        TextView title = text(t("lang_title"), medium, 20, GOLD);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 22, 0, 0));
        TextView hint = text("Choisissez votre langue  ·  Choose your language", regular, 12, MUTED);
        hint.setGravity(Gravity.CENTER);
        hint.setTextDirection(View.TEXT_DIRECTION_LTR);
        body.addView(hint, lp(0, 2, 0, 12));
        String[][] langs = {{"ar", "AR", "العربية"}, {"fr", "FR", "Français"}, {"en", "EN", "English"}};
        for (final String[] l : langs) {
            LinearLayout c = card(Strings.lang.equals(l[0]));
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            TextView code = text(l[1], medium, 14, Strings.lang.equals(l[0]) ? DARK : SAND);
            code.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(8));
            g.setColor(Strings.lang.equals(l[0]) ? GOLD : 0xFF3C4840);
            code.setBackground(g);
            c.addView(code, new LinearLayout.LayoutParams(dp(40), dp(32)));
            TextView name = text(l[2], medium, 20, SAND);
            name.setTextDirection(l[0].equals("ar") ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
            name.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
            np.setMarginStart(dp(14));
            c.addView(name, np);
            c.setOnClickListener(v -> {
                Strings.lang = l[0];
                getSharedPreferences("installer", MODE_PRIVATE).edit().putString("lang", l[0]).apply();
                show(0);
            });
            body.addView(c, lp(0, 0, 0, 10));
        }
        button.setText(t("continue"));
        button.setOnClickListener(v -> show(12));
    }

    private void prepare(LinearLayout body, TextView button) {
        String[][] s = {{"prep1a", "prep1b"}, {"prep2a", "prep2b"}, {"prep3a", "prep3b"}};
        for (int i = 0; i < 3; i++) {
            LinearLayout c = card(false);
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.addView(new Orn.StarNum(this, String.valueOf(i + 1), medium), new LinearLayout.LayoutParams(dp(40), dp(40)));
            LinearLayout tt = new LinearLayout(this);
            tt.setOrientation(LinearLayout.VERTICAL);
            tt.addView(text(t(s[i][0]), medium, 17, SAND));
            tt.addView(text(t(s[i][1]), regular, 14, MUTED));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
            tp.setMarginStart(dp(14));
            c.addView(tt, tp);
            body.addView(c, lp(0, 0, 0, 12));
        }
        button.setText(t("next"));
        button.setOnClickListener(v -> show(2));
    }

    private void connect(final LinearLayout body, final TextView button) {
        body.addView(text(t("found"), medium, 15, GOLD), lp(0, 0, 0, 8));
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list);
        list.addView(text(t("searching"), regular, 15, MUTED));
        body.addView(text(t("or_type"), medium, 15, GOLD), lp(0, 16, 0, 8));
        final EditText input = new EditText(this);
        input.setHint("192.168.1.24");
        input.setHintTextColor(0x66C6BBA4);
        input.setTextColor(SAND);
        input.setTypeface(regular);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        input.setBackground(cardBg(false));
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        body.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout tip = card(false);
        tip.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        tip.addView(text(t("on_tv"), medium, 15, GOLD));
        tip.addView(text(t("allow1"), regular, 14, SAND));
        tip.addView(text(t("allow2"), regular, 13, MUTED));
        body.addView(tip, lp(0, 16, 0, 0));
        final TextView status = text("", regular, 14, GOLD);
        body.addView(status, lp(0, 12, 0, 12));
        button.setText(t("connect"));
        button.setOnClickListener(v -> {
            if (busy) return;
            String ip = input.getText().toString().trim();
            if (ip.isEmpty()) ip = chosen;
            if (ip.isEmpty()) { Toast.makeText(this, t("pick_tv"), Toast.LENGTH_SHORT).show(); return; }
            connectTo(ip, button, status);
        });
        chosen = "";
        bg.execute(() -> {
            final List<String> tvs = Net.findTvs(5555);
            runOnUiThread(() -> {
                if (step != 2) return;
                list.removeAllViews();
                if (tvs.isEmpty()) list.addView(text(t("none_found"), regular, 14, MUTED));
                for (final String ip : tvs) {
                    final LinearLayout c = card(false);
                    c.addView(text(t("tv"), medium, 18, SAND));
                    TextView a = text(ip, regular, 14, MUTED);
                    a.setTextDirection(View.TEXT_DIRECTION_LTR);
                    c.addView(a);
                    c.setOnClickListener(v -> {
                        chosen = ip;
                        for (int i = 0; i < list.getChildCount(); i++) list.getChildAt(i).setBackground(cardBg(false));
                        c.setBackground(cardBg(true));
                        input.setText("");
                    });
                    list.addView(c, lp(0, 0, 0, 10));
                }
            });
        });
    }

    private void connectTo(final String ip, final TextView button, final TextView status) {
        busy = true;
        button.setAlpha(0.6f);
        status.setTextColor(GOLD);
        final int[] left = {AUTH_SECONDS};
        stopTicker();
        ticker = new Runnable() {
            @Override public void run() {
                if (!busy) return;
                status.setText(t("connecting", Math.max(left[0], 0)));
                left[0]--;
                ui.postDelayed(this, 1000);
            }
        };
        ui.post(ticker);
        bg.execute(() -> {
            try {
                Adb a = Adb.connect(ip, 5555, new File(getFilesDir(), "keys"));
                tv = a;
                flow = new Flow(a, getCacheDir());
                model = flow.model();
                tvModel = model;
                try { tvBrand = flow.prop("ro.product.manufacturer"); } catch (IOException ignored) { }
                runOnUiThread(() -> afterConnect(button, status));
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    busy = false;
                    stopTicker();
                    button.setAlpha(1f);
                    status.setTextColor(0xFFE8896B);
                    status.setText(friendly(e, ip, true));
                });
            }
        });
    }

    /** The weight of each of the eight steps in the global percentage (they add up to 100). */
    private static final int[] W = {3, 25, 15, 12, 15, 6, 12, 12};

    private void install(final LinearLayout body, final TextView button) {
        final int n = W.length;
        final TextView pctV = text("0%", display, 58, SAND);
        pctV.setGravity(Gravity.CENTER);
        pctV.setTextDirection(View.TEXT_DIRECTION_LTR);
        body.addView(pctV, lp(0, 0, 0, 4));
        final Orn.Bar bar = new Orn.Bar(this);
        body.addView(bar, new LinearLayout.LayoutParams(-1, dp(32)));
        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        final TextView stepV = text(t("step_of", 1, n), medium, 14, SAND);
        final TextView etaV = text("", regular, 14, GOLD);
        etaV.setGravity(Gravity.END);
        meta.addView(stepV, new LinearLayout.LayoutParams(0, -2, 1f));
        meta.addView(etaV, new LinearLayout.LayoutParams(0, -2, 1f));
        body.addView(meta, lp(6, 6, 6, 14));
        final String[] titles = {t("s_check"), t("s_download", ""), t("s_saver"), t("s_channels"), t("s_home"), t("s_perms"), t("s_defaults"), t("s_verify")};
        final String[] subs0 = {"", "", "", t("ch_sub"), "", t("sub_perms"), t("sub_defaults"), t("sub_verify")};
        final Orn.Node[] nodes = new Orn.Node[n];
        final TextView[] titleV = new TextView[n], subV = new TextView[n];
        for (int i = 0; i < n; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            nodes[i] = new Orn.Node(this, i == 0, i == n - 1);
            row.addView(nodes[i], new LinearLayout.LayoutParams(dp(30), -1));
            LinearLayout tt = new LinearLayout(this);
            tt.setOrientation(LinearLayout.VERTICAL);
            tt.setPadding(0, dp(8), 0, dp(10));
            titleV[i] = text(titles[i], medium, 16, SAND);
            subV[i] = text(subs0[i], regular, 13, MUTED);
            subV[i].setTextDirection(Strings.rtl() ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
            tt.addView(titleV[i]);
            tt.addView(subV[i]);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
            tp.setMarginStart(dp(10));
            row.addView(tt, tp);
            body.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        final TextView err = text("", regular, 14, 0xFFE8896B);
        body.addView(err, lp(0, 10, 0, 0));
        button.setText(t("installing"));
        button.setAlpha(0.6f);
        busy = true;
        final long t0 = System.currentTimeMillis();
        bg.execute(() -> {
            int cur = 0;
            try {
                cur = 0;
                node(nodes, 0, Orn.Node.RUN);
                progress(0, 0f, bar, pctV, stepV, etaV, t0);
                String m = flow.model(), av = flow.androidVersion();
                tvModel = m;
                try { tvBrand = flow.prop("ro.product.manufacturer"); } catch (IOException ignored) { }
                sub(subV, 0, m + " · Android " + av);
                node(nodes, 0, Orn.Node.DONE);
                progress(0, 1f, bar, pctV, stepV, etaV, t0);

                cur = 1;
                node(nodes, 1, Orn.Node.RUN);
                final Net.Release rel = Net.latest();
                final String ver = rel.version;
                runOnUiThread(() -> titleV[1].setText(t("s_download", ver)));
                final String[] pk = {Flow.SAVER, Flow.CHANNELS, Flow.HOME};
                final java.io.File[] files = new java.io.File[3];
                for (int j = 0; j < 3; j++) {
                    String url = null, sha = "";
                    for (Net.Apk a : rel.apks) if (a.pkg.equals(pk[j])) { url = a.url; sha = a.sha; }
                    if (j == 1) { url = Net.CHANNELS; sha = ""; }
                    if (url == null) throw new IOException("no link for " + pk[j]);
                    final int jj = j;
                    files[j] = flow.download(pk[j], url, sha, (d, tt) -> {
                        progress(1, (jj + (tt > 0 ? (float) d / tt : 0f)) / 3f, bar, pctV, stepV, etaV, t0);
                        sub(subV, 1, (jj + 1) + "/3 · " + mb(d) + (tt > 0 ? " / " + mb(tt) : ""));
                    });
                }
                node(nodes, 1, Orn.Node.DONE);
                progress(1, 1f, bar, pctV, stepV, etaV, t0);

                for (int k = 0; k < 3; k++) {
                    cur = 2 + k;
                    final int s = cur;
                    node(nodes, s, Orn.Node.RUN);
                    flow.installFile(pk[k], files[k], (d, tt) -> {
                        float f = tt > 0 ? (float) d / tt : 0f;
                        progress(s, f * 0.85f, bar, pctV, stepV, etaV, t0);
                        sub(subV, s, f >= 1f ? t("sub_install") : t("sub_send", (int) (f * 100)));
                    });
                    if (s != 3) sub(subV, s, ver);
                    else sub(subV, s, t("ch_sub"));
                    node(nodes, s, Orn.Node.DONE);
                    progress(s, 1f, bar, pctV, stepV, etaV, t0);
                }

                cur = 5;
                node(nodes, 5, Orn.Node.RUN);
                flow.grantPermissions();
                node(nodes, 5, Orn.Node.DONE);
                progress(5, 1f, bar, pctV, stepV, etaV, t0);

                cur = 6;
                node(nodes, 6, Orn.Node.RUN);
                sub(subV, 6, t("sub_switch"));
                flow.setDefaults();
                try { homeResult = flow.switchHome(); } catch (IOException lost) { homeResult = Flow.Home.UNSURE; }
                sub(subV, 6, t("sub_defaults"));
                node(nodes, 6, Orn.Node.DONE);
                progress(6, 1f, bar, pctV, stepV, etaV, t0);

                cur = 7;
                node(nodes, 7, Orn.Node.RUN);
                try { flow.verify(); } catch (IOException lost) { if (homeResult != Flow.Home.UNSURE) throw lost; }
                sub(subV, 7, homeResult == Flow.Home.OURS ? t("home_ok") : t("home_warn"));
                node(nodes, 7, Orn.Node.DONE);
                progress(7, 1f, bar, pctV, stepV, etaV, t0);

                try { cityList = flow.cities(); } catch (Exception ignored) { }
                Thread.sleep(700);
                runOnUiThread(() -> { busy = false; show(4); });
            } catch (final Exception e) {
                final int failed = cur;
                runOnUiThread(() -> {
                    busy = false;
                    nodes[failed].set(Orn.Node.FAIL);
                    err.setText(t("err_install", friendly(e, "", false)));
                    button.setText(t("retry"));
                    button.setAlpha(1f);
                    button.setOnClickListener(v -> show(3));
                });
            }
        });
    }

    private void node(final Orn.Node[] nodes, final int i, final int state) {
        runOnUiThread(() -> nodes[i].set(state));
    }

    private void sub(final TextView[] subs, final int i, final String text) { runOnUiThread(() -> subs[i].setText(text)); }

    private String mb(long bytes) { return String.format(Locale.ENGLISH, "%.1f %s", bytes / 1048576.0, t("mb")); }

    /** The global percentage: the weights of the finished steps plus the share of the running one. */
    private void progress(final int step, float frac, final Orn.Bar bar, final TextView pctV, final TextView stepV, final TextView etaV, final long t0) {
        float done = 0;
        for (int i = 0; i < step; i++) done += W[i];
        final float pct = Math.min(100f, done + W[step] * Math.max(0f, Math.min(1f, frac)));
        final long el = (System.currentTimeMillis() - t0) / 1000;
        final String eta = pct < 6f || pct >= 100f ? "" : eta(Math.round(el * (100 - pct) / pct));
        runOnUiThread(() -> {
            pctV.setText(Math.round(pct) + "%");
            bar.set(pct / 100f);
            stepV.setText(t("step_of", step + 1, W.length));
            etaV.setText(eta);
        });
    }

    private String eta(long sec) { return sec < 45 ? t("eta_lt1") : t("eta_min", Math.max(1, Math.round(sec / 60f))); }

    private void personalise(LinearLayout body, final TextView button) {
        cityV = addChoice(body, t("city"), city, v -> pickCity());
        tzV = addChoice(body, t("tz"), label("z_", tz), v -> pick(t("tz"), ZONES, "z_", x -> { tz = x; tzV.setText(label("z_", tz)); }));
        methodV = addChoice(body, t("method"), label("m_", method), v -> pick(t("method"), METHODS, "m_", x -> { method = x; methodV.setText(label("m_", method)); }));
        adhanV = addChoice(body, t("adhan"), adhanOn ? t("on") : t("off"), v -> { adhanOn = !adhanOn; adhanV.setText(adhanOn ? t("on") : t("off")); });
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        c.addView(text(t("import"), medium, 17, GOLD));
        importV = text(importedB64 != null ? t("import_ready") : t("import_sub"), regular, 13, MUTED);
        c.addView(importV);
        c.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PICK_SETTINGS);
        });
        body.addView(c, lp(0, 6, 0, 0));
        button.setText(t("apply"));
        button.setOnClickListener(v -> {
            if (busy) return;
            busy = true;
            button.setAlpha(0.6f);
            button.setText(t("applying"));
            bg.execute(() -> {
                try {
                    if (importedB64 != null) {
                        flow.importSettings(importedB64, true);
                    } else {
                        flow.setCity(city);
                        flow.setTz(tz);
                        flow.setMethod(method);
                        flow.adhan(adhanOn);
                    }
                    flow.rescheduleAdhan();
                    runOnUiThread(() -> { busy = false; show(5); });
                } catch (final Exception e) {
                    runOnUiThread(() -> {
                        busy = false;
                        button.setAlpha(1f);
                        button.setText(t("apply"));
                        Toast.makeText(this, t("err_apply", friendly(e, "", false)), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void done(LinearLayout body, TextView button) {
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Orn.Arch arch = new Orn.Arch(this);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setGravity(Gravity.CENTER);
        in.setPadding(dp(18), dp(36), dp(18), dp(16));
        TextView check = text("✓", medium, 40, GOLD);
        check.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0xFF142C1F);
        ring.setStroke(dp(3), GOLD);
        check.setBackground(ring);
        in.addView(check, new LinearLayout.LayoutParams(dp(84), dp(84)));
        TextView logo = text("ChouiTV", script, 38, SAND);
        logo.setGravity(Gravity.CENTER);
        gold(logo);
        in.addView(logo, lp(0, 10, 0, 0));
        arch.addView(in, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(230), dp(230));
        ap.gravity = Gravity.CENTER_HORIZONTAL;
        ap.topMargin = dp(10);
        body.addView(arch, ap);
        TextView title = text(t("thanks_title"), display, 24, SAND);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 16, 0, 4));
        TextView s = text(t("thanks_sub"), regular, 15, MUTED);
        s.setGravity(Gravity.CENTER);
        body.addView(s);
        // the ask: a star on GitHub
        LinearLayout ask = card(false);
        ask.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        ask.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView st = text("★", medium, 26, GOLD);
        st.setGravity(Gravity.CENTER);
        ask.addView(st, new LinearLayout.LayoutParams(-1, -2));
        TextView q = text(t("star_title"), medium, 18, GOLD);
        q.setGravity(Gravity.CENTER);
        ask.addView(q);
        TextView qt = text(t("star_text"), regular, 14, SAND);
        qt.setGravity(Gravity.CENTER);
        ask.addView(qt, lp(0, 4, 0, 0));
        body.addView(ask, lp(0, 18, 0, 10));
        TextView star = text("★  " + t("star_btn"), medium, 17, GOLD);
        star.setGravity(Gravity.CENTER);
        GradientDrawable sb = new GradientDrawable();
        sb.setCornerRadius(dp(28));
        sb.setColor(0xFF142C1F);
        sb.setStroke(dp(2), GOLD);
        star.setBackground(sb);
        star.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EMCHOUIT/ChouiTV"))); } catch (Exception ignored) { }
        });
        body.addView(star, new LinearLayout.LayoutParams(-1, dp(52)));
        cleanNowCard(body);
        if (homeResult != null && homeResult != Flow.Home.OURS) {
            LinearLayout hw = card(false);
            hw.setBackground(cardBg(false, 0x33E8896B, 0xFFE8896B));
            hw.addView(text(homeResult == Flow.Home.UNSURE || homeResult == Flow.Home.FAILED ? t("home_unsure") : t("home_kept"), regular, 14, SAND));
            body.addView(hw, lp(0, 14, 0, 0));
        }
        LinearLayout tip = card(false);
        tip.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        tip.addView(text(t("tip"), medium, 15, GOLD));
        tip.addView(text(t("tip_text"), regular, 14, SAND));
        body.addView(tip, lp(0, 14, 0, 0));
        button.setText(t("open_tv"));
        button.setOnClickListener(v -> bg.execute(() -> { try { flow.openOnTv(); } catch (Exception ignored) { } }));
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    // ---------- the cleanup: offer, plan, run, result, and the restore screen ----------

    private CleanupData cleanupData;
    private Planner.Plan cleanPlan;
    /** The packages ticked in the plan, and whether the person confirmed having looked at the lists. */
    private final java.util.Set<String> cleanSel = new java.util.LinkedHashSet<>();
    private boolean cleanConfirmed;
    private Executor.Result cleanResult;

    private static final String CLEANUP_URL = "https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/optimize/lists/cleanup.json";

    /** The lists: the published file if it can be read, else the copy that came with the app. */
    private CleanupData loadCleanupData() throws Exception {
        CleanupData bundled;
        try (InputStream in = getAssets().open("cleanup.json")) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            bundled = CleanupData.parse(new String(o.toByteArray(), "UTF-8"));
        }
        try {
            CleanupData online = CleanupData.parse(new String(Net.fetch(CLEANUP_URL), "UTF-8"));
            // the published lists are the truth; the names this app was built with fill any name they do not have yet
            for (java.util.Map.Entry<String, String> e : bundled.display.entrySet()) online.display.putIfAbsent(e.getKey(), e.getValue());
            for (java.util.Map.Entry<String, String> e : bundled.namesAr.entrySet()) online.namesAr.putIfAbsent(e.getKey(), e.getValue());
            for (java.util.Map.Entry<String, String> e : bundled.namesFr.entrySet()) online.namesFr.putIfAbsent(e.getKey(), e.getValue());
            return online;
        } catch (Exception offline) {
            return bundled;
        }
    }


    private String niceName(String pkg) {
        String ar = cleanupData == null ? null : cleanupData.namesAr.get(pkg);
        return Strings.lang.equals("ar") && ar != null ? ar : pkg;
    }

    private String groupName(CleanupData.Group g) {
        switch (g) {
            case TRACKING: return t("cl_g_tracking");
            case ADS: return t("cl_g_ads");
            case OPTIONAL: return t("cl_g_opt");
            case USER: return t("cl_g_usr");
            default: return t("cl_g_pre");
        }
    }

    private void titleBlock(LinearLayout body, String title, String sub) {
        body.addView(text(title, display, 30, SAND), lp(0, 8, 0, 0));
        body.addView(text(sub, regular, 15, MUTED), lp(0, 4, 0, 6));
        body.addView(new Orn.Rule(this), new LinearLayout.LayoutParams(-1, dp(22)));
    }

    private TextView ghostButton(String label, int color) {
        TextView g = text(label, medium, 17, color);
        g.setGravity(Gravity.CENTER);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(28)); d.setColor(0xFF142C1F); d.setStroke(dp(2), color);
        g.setBackground(d);
        return g;
    }

    private void cleanupPlan(LinearLayout body, final TextView button) {
        titleBlock(body, t("cl_plan_title"), t("cl_plan_sub"));
        final Planner.Plan p = cleanPlan;
        String src = p.source.equals("ours") ? t("cl_src_ours") : p.source.equals("community") ? t("cl_src_model")
            : p.why.endsWith("ambiguous") ? t("cl_src_amb") : t("cl_src_cons");
        LinearLayout info = card(false);
        info.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        info.addView(text(tvModelLine(), medium, 16, SAND));
        info.addView(text(src + (p.alreadyOff > 0 ? " · " + t("cl_already", p.alreadyOff) : ""), regular, 13, MUTED));
        body.addView(info, lp(0, 10, 0, 6));
        if (p.total() == 0) {
            LinearLayout none = card(false);
            none.addView(text(t("cl_none"), medium, 17, GOLD));
            body.addView(none, lp(0, 8, 0, 0));
            afterCleanup(button);
            return;
        }
        final java.util.List<Planner.Item> noise = new java.util.ArrayList<>(p.groups.get(CleanupData.Group.TRACKING));
        noise.addAll(p.groups.get(CleanupData.Group.ADS));
        if (!noise.isEmpty()) addNoiseCard(body, noise, button);
        addChecklist(body, button, CleanupData.Group.PREINSTALLED, "cl_pre_h", "cl_pre_d");
        addChecklist(body, button, CleanupData.Group.OPTIONAL, "cl_str_h", "cl_str_d");
        addChecklist(body, button, CleanupData.Group.USER, "cl_usr_h", "cl_usr_d");
        LinearLayout prot = card(false);
        prot.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        prot.addView(text(t("cl_protected", cleanupData.keep.size()), medium, 16, GOLD));
        prot.addView(text(t("cl_protected_d"), regular, 13, SAND), lp(0, 4, 0, 0));
        body.addView(prot, lp(0, 4, 0, 10));
        // the confirmation: the person is asked to look, and the button waits for it
        boolean streaming = !p.groups.get(CleanupData.Group.OPTIONAL).isEmpty(), mine = !p.groups.get(CleanupData.Group.USER).isEmpty();
        LinearLayout ask = card(false);
        ask.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        ask.addView(text(t("cl_conf_h"), medium, 18, GOLD));
        if (streaming) ask.addView(text(t("cl_conf_stream"), regular, 14, SAND), lp(0, 6, 0, 0));
        if (mine) ask.addView(text(t("cl_conf_mine"), regular, 14, SAND), lp(0, 6, 0, 0));
        ask.addView(text(t("cl_conf_keep"), regular, 14, SAND), lp(0, 6, 0, 0));
        final TextView sure = text(t("cl_conf_check"), medium, 15, SAND);
        final TextView box = checkBox(cleanConfirmed);
        LinearLayout sureRow = new LinearLayout(this);
        sureRow.setOrientation(LinearLayout.HORIZONTAL);
        sureRow.setGravity(Gravity.CENTER_VERTICAL);
        sureRow.addView(box, new LinearLayout.LayoutParams(dp(30), dp(30)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -2, 1f);
        sp.setMarginStart(dp(12));
        sureRow.addView(sure, sp);
        sureRow.setOnClickListener(v -> { cleanConfirmed = !cleanConfirmed; styleCheck(box, cleanConfirmed); updateRunButton(button); });
        ask.addView(sureRow, lp(0, 12, 0, 0));
        body.addView(ask, lp(0, 0, 0, 0));
        updateRunButton(button);
    }

    /** One switch for the tracking and the ads: it is not a choice most people want to make app by app; touching the card shows the names. */
    private void addNoiseCard(LinearLayout body, final java.util.List<Planner.Item> noise, final TextView button) {
        final LinearLayout c = card(false);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.addView(text(t("cl_g_noise"), medium, 18, SAND));
        tt.addView(text(t("cl_count", noise.size()) + " · " + t("cl_tap"), regular, 13, MUTED));
        head.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        final android.widget.Switch sw = new android.widget.Switch(this);
        boolean on = true;
        for (Planner.Item i : noise) if (!cleanSel.contains(i.pkg)) on = false;
        sw.setChecked(on);
        sw.setThumbTintList(new android.content.res.ColorStateList(new int[][] {{android.R.attr.state_checked}, {}}, new int[] {GOLD, 0xFF96A098}));
        sw.setTrackTintList(new android.content.res.ColorStateList(new int[][] {{android.R.attr.state_checked}, {}}, new int[] {0x66D6AF6B, 0x66505A52}));
        head.addView(sw);
        c.addView(head);
        final TextView list = text("", regular, 12, MUTED);
        list.setTextDirection(View.TEXT_DIRECTION_LTR);
        StringBuilder b = new StringBuilder();
        for (Planner.Item it : noise) b.append("• ").append(cleanupData.label(it.pkg, Strings.lang)).append("\n   ").append(it.pkg).append("\n");
        list.setText(b.toString().trim());
        list.setVisibility(View.GONE);
        c.addView(list, lp(0, 8, 0, 0));
        c.setOnClickListener(v -> list.setVisibility(list.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        sw.setOnCheckedChangeListener((x, isOn) -> {
            for (Planner.Item i : noise) { if (isOn) cleanSel.add(i.pkg); else cleanSel.remove(i.pkg); }
            updateRunButton(button);
        });
        body.addView(c, lp(0, 0, 0, 8));
    }

    /** A section with a tick box per app, the count and a «tick all / none» link. */
    private void addChecklist(LinearLayout body, final TextView button, final CleanupData.Group g, String titleKey, String descKey) {
        final java.util.List<Planner.Item> items = cleanPlan.groups.get(g);
        if (items.isEmpty()) return;
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(t(titleKey), medium, 19, GOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView count = text("", medium, 13, SAND);
        head.addView(count);
        body.addView(head, lp(4, 10, 4, 0));
        body.addView(text(t(descKey), regular, 13, MUTED), lp(4, 2, 4, 6));
        final java.util.List<TextView> boxes = new java.util.ArrayList<>();
        final Runnable recount = () -> {
            int n = 0;
            for (Planner.Item i : items) if (cleanSel.contains(i.pkg)) n++;
            count.setText(t("cl_ticked", n, items.size()));
        };
        for (final Planner.Item it : items) {
            final TextView box = checkBox(cleanSel.contains(it.pkg));
            boxes.add(box);
            LinearLayout row = card(false);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.addView(box, new LinearLayout.LayoutParams(dp(30), dp(30)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            String name = cleanupData.label(it.pkg, Strings.lang);
            TextView nm = text(name, medium, 16, SAND);
            nm.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            col.addView(nm);
            if (!name.equals(it.pkg)) {
                TextView pk = text(it.pkg, regular, 11, MUTED);
                pk.setTextDirection(View.TEXT_DIRECTION_LTR);
                col.addView(pk);
            }
            Planner.UserApp ua = cleanPlan.userApps.get(it.pkg);
            if (ua != null) col.addView(chipsFor(ua), lp(0, 6, 0, 0));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMarginStart(dp(12));
            row.addView(col, cp);
            row.setOnClickListener(v -> {
                boolean on = !cleanSel.contains(it.pkg);
                if (on) cleanSel.add(it.pkg); else cleanSel.remove(it.pkg);
                styleCheck(box, on);
                recount.run();
                updateRunButton(button);
            });
            body.addView(row, lp(0, 0, 0, 6));
        }
        recount.run();
        final TextView all = text(t("cl_untick_all"), medium, 14, GOLD);
        all.setGravity(Gravity.CENTER);
        all.setPadding(0, dp(6), 0, dp(10));
        all.setOnClickListener(v -> {
            boolean any = false;
            for (Planner.Item i : items) if (cleanSel.contains(i.pkg)) any = true;
            for (int k = 0; k < items.size(); k++) {
                if (any) cleanSel.remove(items.get(k).pkg); else cleanSel.add(items.get(k).pkg);
                styleCheck(boxes.get(k), !any);
            }
            all.setText(t(any ? "cl_tick_all" : "cl_untick_all"));
            recount.run();
            updateRunButton(button);
        });
        all.setText(t(allTicked(items) ? "cl_untick_all" : "cl_tick_all"));
        body.addView(all, lp(0, 0, 0, 4));
    }

    private boolean allTicked(java.util.List<Planner.Item> items) {
        for (Planner.Item i : items) if (!cleanSel.contains(i.pkg)) return false;
        return true;
    }

    /** «Recommended», «starts with the TV» and the memory it holds: the reasons, on the row, so the person can decide. */
    private View chipsFor(Planner.UserApp u) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        if (u.recommended) r.addView(chip(t("cl_chip_rec"), GOLD, DARK, 0));
        if (u.autostart) r.addView(chip(t("cl_chip_boot"), 0x44D6826E, SAND, 0xFFD6826E));
        if (u.ramMb > 0) r.addView(chip(t("cl_chip_ram", u.ramMb), 0, SAND, 0xFF96A098));
        else if (u.daysSinceUse >= 0 && u.daysSinceUse < Facts.LONG_UNUSED_DAYS) r.addView(chip(t("cl_chip_recent"), 0, SAND, 0xFF96A098));
        return r;
    }

    private TextView chip(String label, int fill, int color, int stroke) {
        TextView c = text(label, medium, 11, color);
        c.setPadding(dp(9), dp(2), dp(9), dp(2));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(fill);
        if (stroke != 0 || fill == 0) g.setStroke(dp(1), stroke == 0 ? 0xFF96A098 : stroke);
        c.setBackground(g);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMarginEnd(dp(6));
        c.setLayoutParams(p);
        return c;
    }

    private TextView checkBox(boolean on) {
        TextView b = text("", medium, 16, DARK);
        b.setGravity(Gravity.CENTER);
        styleCheck(b, on);
        return b;
    }

    private void styleCheck(TextView b, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(8));
        if (on) { g.setColor(GOLD); b.setText("✓"); b.setTextColor(DARK); }
        else { g.setColor(0); g.setStroke(dp(2), 0xFF96A098); b.setText(""); }
        b.setBackground(g);
    }

    private int selectedCount() {
        int n = 0;
        for (CleanupData.Group g : CleanupData.Group.values()) for (Planner.Item i : cleanPlan.groups.get(g)) if (cleanSel.contains(i.pkg)) n++;
        return n;
    }

    /** The button shows how many apps will be turned off and waits for the confirmation; with nothing ticked it simply continues. */
    private void updateRunButton(TextView button) {
        int n = selectedCount();
        if (n == 0) {
            afterCleanup(button);
            button.setAlpha(1f);
            return;
        }
        button.setText(t("cl_run", n));
        button.setAlpha(cleanConfirmed ? 1f : 0.45f);
        button.setOnClickListener(v -> {
            if (busy) return;
            if (!cleanConfirmed) { Toast.makeText(this, t("cl_conf_needed"), Toast.LENGTH_SHORT).show(); return; }
            show(9);
        });
    }

    private String tvModelLine() { return (tvBrand.isEmpty() ? "" : cap(tvBrand) + " · ") + tvModel; }

    private String newToken() {
        byte[] b = new byte[8]; new java.security.SecureRandom().nextBytes(b);
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }

    private java.io.File restoreFile() { return new java.io.File(getFilesDir(), "cleanup-restore.json"); }

    private void saveRestore(Restore r) {
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(restoreFile())) { o.write(r.toJson().getBytes("UTF-8")); } catch (Exception ignored) { }
    }

    private Restore loadRestore() {
        try (InputStream in = new java.io.FileInputStream(restoreFile())) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            return Restore.fromJson(new String(o.toByteArray(), "UTF-8"));
        } catch (Exception e) { return null; }
    }

    /** A plain copy of the restore point on the TV itself, for a rescue without the phone. */
    private void pushRestoreToTv(Restore r) {
        try {
            Shell sh = flow.shell();
            sh.run("rm -f /data/local/tmp/chouitv-restore.txt");
            for (String line : r.plainText().split("\n")) {
                if (line.indexOf('\'') >= 0) continue;
                sh.run("echo '" + line + "' >> /data/local/tmp/chouitv-restore.txt");
            }
        } catch (Exception ignored) { }
    }

    private void cleanupRun(final LinearLayout body, final TextView button) {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        titleBlock(body, t("cl_run_title"), t("cl_run_sub"));
        LinearLayout rp = card(false);
        rp.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        rp.addView(text(t("cl_rp"), medium, 16, SAND));
        rp.addView(text(t("cl_rp_d"), regular, 13, MUTED));
        body.addView(rp, lp(0, 10, 0, 10));
        final TextView pctV = text("0%", display, 58, SAND);
        pctV.setGravity(Gravity.CENTER); pctV.setTextDirection(View.TEXT_DIRECTION_LTR);
        body.addView(pctV, lp(0, 0, 0, 4));
        final Orn.Bar bar = new Orn.Bar(this);
        body.addView(bar, new LinearLayout.LayoutParams(-1, dp(32)));
        final TextView countV = text("", medium, 14, SAND);
        body.addView(countV, lp(6, 6, 6, 8));
        final TextView last = text(t("cl_checked"), regular, 13, MUTED);
        last.setTextDirection(View.TEXT_DIRECTION_LTR);
        body.addView(last, lp(6, 0, 6, 0));
        button.setText(t("cl_running"));
        button.setAlpha(0.6f);
        busy = true;
        final java.util.List<String> planned = new java.util.ArrayList<>();
        for (CleanupData.Group g : CleanupData.Group.values()) for (Planner.Item it : cleanPlan.groups.get(g)) if (cleanSel.contains(it.pkg)) planned.add(it.pkg);
        final String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new java.util.Date());
        bg.execute(() -> {
            try {
                Restore point = new Restore(tvModel, date, planned);   // saved BEFORE the first change
                saveRestore(point);
                pushRestoreToTv(point);
                Executor ex = new Executor(flow.shell(), Thread::sleep, newToken());
                cleanResult = ex.runSelected(cleanPlan, new java.util.LinkedHashSet<>(cleanSel), new Executor.Listener() {
                    public void started(int total) { }
                    public void package_(CleanupData.Group g, final String pkg, boolean ok, final int done, final int total) {
                        runOnUiThread(() -> { int pct = total == 0 ? 0 : done * 100 / total; pctV.setText(pct + "%"); bar.set(pct / 100f); countV.setText(done + " / " + total); last.setText(pkg); });
                    }
                    public void groupDone(CleanupData.Group g, boolean healthy) { }
                });
                Restore real = new Restore(tvModel, date, cleanResult.disabled);   // what was really turned off
                saveRestore(real);
                pushRestoreToTv(real);
                runOnUiThread(() -> { busy = false; getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); show(10); });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    busy = false;
                    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    TextView err = text(t("cl_net_lost"), regular, 14, 0xFFE8896B);
                    body.addView(err, lp(0, 12, 0, 0));
                    afterCleanup(button); button.setAlpha(1f);
                });
            }
        });
    }

    private void cleanupResult(LinearLayout body, TextView button) {
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Orn.Arch arch = new Orn.Arch(this);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL); in.setGravity(Gravity.CENTER); in.setPadding(dp(18), dp(36), dp(18), dp(16));
        TextView check = text("✓", medium, 40, GOLD);
        check.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL); ring.setColor(0xFF142C1F); ring.setStroke(dp(3), GOLD);
        check.setBackground(ring);
        in.addView(check, new LinearLayout.LayoutParams(dp(84), dp(84)));
        arch.addView(in, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(180), dp(180));
        ap.gravity = Gravity.CENTER_HORIZONTAL; ap.topMargin = dp(10);
        body.addView(arch, ap);
        TextView title = text(t("cl_res_title"), display, 26, SAND);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 14, 0, 10));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        int nOff = cleanResult == null ? 0 : cleanResult.disabled.size();
        String[][] st = {{String.valueOf(nOff), t("cl_res_off")}, {String.valueOf(cleanupData.keep.size()), t("cl_res_kept")}, {"0", t("cl_res_del")}};
        for (String[] s : st) {
            LinearLayout c = card(false);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setBackground(cardBg(false, 0x18D6AF6B, 0xAAD6AF6B));
            TextView v = text(s[0], display, 30, GOLD); v.setGravity(Gravity.CENTER);
            TextView l = text(s[1], regular, 12, SAND); l.setGravity(Gravity.CENTER);
            c.addView(v); c.addView(l);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMargins(dp(4), 0, dp(4), 0);
            stats.addView(c, cp);
        }
        body.addView(stats, lp(0, 0, 0, 0));
        if (cleanResult != null && cleanResult.rolledBack != null) {
            LinearLayout w = card(false);
            w.setBackground(cardBg(false, 0x33E8896B, 0xFFE8896B));
            w.addView(text(t("cl_rolled", groupName(cleanResult.rolledBack)), regular, 14, SAND));
            body.addView(w, lp(0, 12, 0, 0));
        }
        if (cleanResult != null && !cleanResult.refused.isEmpty()) {
            body.addView(text(t("cl_refused", cleanResult.refused.size()), regular, 13, MUTED), lp(4, 10, 4, 0));
        }
        LinearLayout undo = card(false);
        undo.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        undo.addView(text(t("cl_res_undo_t"), medium, 16, GOLD));
        undo.addView(text(t("cl_res_undo_d"), regular, 13, SAND), lp(0, 4, 0, 0));
        body.addView(undo, lp(0, 12, 0, 8));
        final TextView undoAll = ghostButton(t("cl_undo_all"), MUTED);
        undoAll.setOnClickListener(v -> {
            if (busy) return;
            busy = true; undoAll.setAlpha(0.6f);
            bg.execute(() -> {
                try {
                    Restore r = loadRestore();
                    if (r != null) Restore.apply(flow.shell(), r.packages);
                    runOnUiThread(() -> { busy = false; undoAll.setText(t("cl_restored")); undoAll.setAlpha(1f); });
                } catch (Exception e) { runOnUiThread(() -> { busy = false; undoAll.setAlpha(1f); Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); }); }
            });
        });
        body.addView(undoAll, new LinearLayout.LayoutParams(-1, dp(52)));
        if (!cleanFromDone) {
            TextView home = ghostButton(t("cl_home"), MUTED);
            home.setOnClickListener(v -> show(12));
            body.addView(home, lp(0, 8, 0, 0));
        }
        afterCleanup(button);
    }

    // ---------- the three spaces: clean, install, turn back on ----------

    private static final int SPACE_CLEAN = 0, SPACE_INSTALL = 1, SPACE_REACT = 2;
    private int space = SPACE_INSTALL;
    private boolean cleanedHere, cleanFromDone;
    private Reactivation.Plan reactPlan;
    private final java.util.Set<String> reactSel = new java.util.LinkedHashSet<>();
    private java.util.List<String> reactBack = new ArrayList<>();
    private int reactAsked;

    private void hub(LinearLayout body, TextView button) {
        cleanedHere = false;
        cleanFromDone = false;
        titleBlock(body, t("hub_title"), t("hub_sub"));
        String[][] cards = {{"1", "hub_clean_t", "hub_clean_d", "hub_clean_tag"}, {"2", "hub_inst_t", "hub_inst_d", "hub_inst_tag"}, {"3", "hub_set_t", "hub_set_d", "hub_set_tag"}, {"4", "hub_react_t", "hub_react_d", "hub_react_tag"}};
        for (int i = 0; i < cards.length; i++) {
            final int sp = i == 0 ? SPACE_CLEAN : i == 1 ? SPACE_INSTALL : i == 2 ? SPACE_SET : SPACE_REACT;
            LinearLayout c = card(i == 0);
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.addView(new Orn.StarNum(this, cards[i][0], medium), new LinearLayout.LayoutParams(dp(42), dp(42)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(chip(t(cards[i][3]), i == 0 ? GOLD : 0, i == 0 ? DARK : SAND, i == 0 ? 0 : 0xFF96A098));
            col.addView(text(t(cards[i][1]), display, 21, SAND), lp(0, 4, 0, 0));
            col.addView(text(t(cards[i][2]), regular, 13, MUTED), lp(0, 2, 0, 0));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMarginStart(dp(14));
            c.addView(col, cp);
            c.setOnClickListener(v -> { if (!busy) { space = sp; show(1); } });
            body.addView(c, lp(0, 12, 0, 0));
        }
        button.setText(t("hub_lang"));
        button.setAlpha(0.9f);
        button.setOnClickListener(v -> show(0));
    }

    /** After the TV answers, each space reads what it needs from it: the install goes straight on, the others first read the apps. */
    private void afterConnect(final TextView button, final TextView status) {
        if (space == SPACE_INSTALL) { busy = false; show(3); return; }
        if (space == SPACE_SET) { busy = false; show(14); return; }
        stopTicker();
        status.setTextColor(GOLD);
        status.setText(t(space == SPACE_CLEAN ? "cl_loading" : "re_loading"));
        bg.execute(() -> {
            try {
                if (space == SPACE_CLEAN) loadCleanupPlan(); else loadReactivation();
                runOnUiThread(() -> { busy = false; show(space == SPACE_CLEAN ? 8 : 11); });
            } catch (final Exception e) {
                runOnUiThread(() -> { busy = false; button.setAlpha(1f); status.setTextColor(0xFFE8896B); status.setText(t("cl_err", friendly(e, "", false))); });
            }
        });
    }

    private void loadCleanupPlan() throws Exception {
        cleanupData = loadCleanupData();
        cleanPlan = Planner.plan(cleanupData, flow.tvInfo());
        cleanSel.clear();
        cleanSel.addAll(cleanPlan.defaultSelection());
        cleanConfirmed = false;
    }

    /** What is off on the TV right now, and which of it a ChouiTV cleanup turned off (read from this phone's restore point and from the copy kept on the TV). */
    private void loadReactivation() throws Exception {
        cleanupData = loadCleanupData();
        java.util.Set<String> ours = new java.util.HashSet<>();
        Restore r = loadRestore();
        if (r != null) ours.addAll(r.packages);
        try { ours.addAll(Reactivation.parseRestoreText(flow.shell().run("cat /data/local/tmp/chouitv-restore.txt 2> /dev/null"))); } catch (IOException ignored) { }
        reactPlan = Reactivation.plan(Reactivation.disabledNow(flow.shell()), ours, cleanupData);
        reactSel.clear();
    }

    /** What follows a cleanup: from the home it leads to the install (step 2); from the end of an install it goes back to the end. */
    private void afterCleanup(TextView button) {
        cleanedHere = true;
        if (cleanFromDone) {
            button.setText(t("cl_continue"));
            button.setOnClickListener(v -> show(5));
        } else {
            button.setText(t("cl_next_install"));
            button.setOnClickListener(v -> { space = SPACE_INSTALL; show(3); });
        }
    }

    private void reactivate(LinearLayout body, final TextView button) {
        titleBlock(body, t("re_title"), t("re_sub"));
        final Reactivation.Plan p = reactPlan;
        if (p.total() == 0) {
            LinearLayout none = card(false);
            none.addView(text(t("re_none"), medium, 17, GOLD));
            body.addView(none, lp(0, 10, 0, 0));
            button.setText(t("cl_home"));
            button.setOnClickListener(v -> show(12));
            return;
        }
        reactSection(body, button, p.ours, "re_ours_h", "re_ours_d");
        reactSection(body, button, p.others, "re_oth_h", "re_oth_d");
        updateReactButton(button);
    }

    private void reactSection(LinearLayout body, final TextView button, final java.util.List<String> pkgs, String titleKey, String descKey) {
        if (pkgs.isEmpty()) return;
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(t(titleKey), medium, 19, GOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView count = text("", medium, 13, SAND);
        head.addView(count);
        body.addView(head, lp(4, 10, 4, 0));
        body.addView(text(t(descKey), regular, 13, MUTED), lp(4, 2, 4, 6));
        final Runnable recount = () -> {
            int n = 0;
            for (String s : pkgs) if (reactSel.contains(s)) n++;
            count.setText(t("cl_ticked", n, pkgs.size()));
        };
        for (final String pkg : pkgs) {
            final TextView box = checkBox(reactSel.contains(pkg));
            LinearLayout row = card(false);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.addView(box, new LinearLayout.LayoutParams(dp(30), dp(30)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            String name = cleanupData.label(pkg, Strings.lang);
            TextView nm = text(name, medium, 16, SAND);
            nm.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            col.addView(nm);
            if (!name.equals(pkg)) {
                TextView pk = text(pkg, regular, 11, MUTED);
                pk.setTextDirection(View.TEXT_DIRECTION_LTR);
                col.addView(pk);
            }
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMarginStart(dp(12));
            row.addView(col, cp);
            row.setOnClickListener(v -> {
                boolean on = !reactSel.contains(pkg);
                if (on) reactSel.add(pkg); else reactSel.remove(pkg);
                styleCheck(box, on);
                recount.run();
                updateReactButton(button);
            });
            body.addView(row, lp(0, 0, 0, 6));
        }
        recount.run();
    }

    private void updateReactButton(final TextView button) {
        final int n = reactSel.size();
        button.setText(n == 0 ? t("re_pick") : t("re_btn", n));
        button.setAlpha(n == 0 ? 0.45f : 1f);
        button.setOnClickListener(v -> {
            if (busy || reactSel.isEmpty()) return;
            busy = true;
            button.setAlpha(0.6f);
            button.setText(t("re_busy"));
            final java.util.List<String> asked = new ArrayList<>(reactSel);
            bg.execute(() -> {
                try {
                    java.util.List<String> back = Reactivation.apply(flow.shell(), asked);
                    Restore r = loadRestore();
                    if (r != null) {   // the restore point forgets what is on again
                        java.util.List<String> left = new ArrayList<>(r.packages);
                        left.removeAll(back);
                        Restore now = new Restore(r.tvModel, r.date, left);
                        saveRestore(now);
                        pushRestoreToTv(now);
                    }
                    reactBack = back;
                    reactAsked = asked.size();
                    runOnUiThread(() -> { busy = false; show(13); });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; button.setAlpha(1f); updateReactButton(button); Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
    }

    private void reactivated(LinearLayout body, TextView button) {
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Orn.Arch arch = new Orn.Arch(this);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL); in.setGravity(Gravity.CENTER); in.setPadding(dp(18), dp(36), dp(18), dp(16));
        TextView check = text("✓", medium, 40, GOLD);
        check.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL); ring.setColor(0xFF142C1F); ring.setStroke(dp(3), GOLD);
        check.setBackground(ring);
        in.addView(check, new LinearLayout.LayoutParams(dp(84), dp(84)));
        arch.addView(in, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(180), dp(180));
        ap.gravity = Gravity.CENTER_HORIZONTAL; ap.topMargin = dp(10);
        body.addView(arch, ap);
        TextView title = text(t("re_done_t"), display, 26, SAND);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 14, 0, 10));
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        StringBuilder names = new StringBuilder();
        for (String p : reactBack) names.append("• ").append(cleanupData.label(p, Strings.lang)).append("\n");
        c.addView(text(t("re_done_d", reactBack.size()), medium, 16, GOLD));
        TextView list = text(names.toString().trim(), regular, 14, SAND);
        list.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        c.addView(list, lp(0, 6, 0, 0));
        if (reactBack.size() < reactAsked) c.addView(text(t("re_fail", reactAsked - reactBack.size()), regular, 13, 0xFFE8896B), lp(0, 8, 0, 0));
        body.addView(c, lp(0, 0, 0, 10));
        final TextView more = ghostButton(t("re_more"), GOLD);
        more.setOnClickListener(v -> {
            if (busy) return;
            busy = true; more.setAlpha(0.6f);
            bg.execute(() -> {
                try { loadReactivation(); runOnUiThread(() -> { busy = false; show(11); }); }
                catch (final Exception e) { runOnUiThread(() -> { busy = false; more.setAlpha(1f); Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); }); }
            });
        });
        body.addView(more, new LinearLayout.LayoutParams(-1, dp(52)));
        button.setText(t("cl_home"));
        button.setOnClickListener(v -> show(12));
    }

    /** At the end of an install, a person who did not clean first is offered to do it now. */
    private void cleanNowCard(LinearLayout body) {
        if (cleanedHere) return;
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        c.addView(text(t("cl_notyet_t"), medium, 17, GOLD));
        c.addView(text(t("cl_notyet_d"), regular, 13, SAND), lp(0, 4, 0, 10));
        final TextView go = ghostButton(t("cl_notyet_btn"), GOLD);
        go.setOnClickListener(v -> {
            if (busy) return;
            busy = true; go.setAlpha(0.6f);
            bg.execute(() -> {
                try { loadCleanupPlan(); runOnUiThread(() -> { busy = false; cleanFromDone = true; space = SPACE_CLEAN; show(8); }); }
                catch (final Exception e) { runOnUiThread(() -> { busy = false; go.setAlpha(1f); Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); }); }
            });
        });
        c.addView(go, new LinearLayout.LayoutParams(-1, dp(48)));
        body.addView(c, lp(0, 14, 0, 0));
    }

    // ---------- my settings: named profiles, saved from a TV and sent to another ----------

    private static final int SPACE_SET = 3;
    private Profile sendProfile;
    private Preview sendPreview, sentResult;

    private ProfileStore store() { return new ProfileStore(new File(getFilesDir(), "profiles")); }

    private String now() { return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new java.util.Date()); }

    private String defaultProfileName() {
        String day = new java.text.SimpleDateFormat("d MMM yyyy", new Locale(Strings.lang)).format(new java.util.Date());
        return (tvModel.isEmpty() ? "ChouiTV" : tvModel) + " · " + day;
    }

    private TextView pill(String label, boolean filled) {
        TextView p = text(label, medium, 14, filled ? DARK : GOLD);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(10), dp(9), dp(10), dp(9));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(22));
        g.setColor(filled ? GOLD : 0xFF142C1F);
        g.setStroke(dp(2), GOLD);
        p.setBackground(g);
        return p;
    }

    private void settingsHome(final LinearLayout body, TextView button) {
        titleBlock(body, t("st_title"), t("st_sub"));
        LinearLayout save = card(false);
        save.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        save.addView(text(t("st_save_h"), medium, 18, GOLD));
        save.addView(text(t("st_save_d"), regular, 13, MUTED), lp(0, 2, 0, 8));
        final EditText name = new EditText(this);
        name.setText(defaultProfileName());
        name.setTextColor(SAND);
        name.setTypeface(regular);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        name.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        name.setBackground(cardBg(false));
        name.setPadding(dp(16), dp(10), dp(16), dp(10));
        save.addView(name, new LinearLayout.LayoutParams(-1, dp(52)));
        final TextView saveBtn = button(t("st_save_btn"));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(52));
        sp.topMargin = dp(10);
        saveBtn.setLayoutParams(sp);
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        saveBtn.setOnClickListener(v -> {
            if (busy) return;
            busy = true;
            saveBtn.setAlpha(0.6f);
            final String typed = name.getText().toString();
            bg.execute(() -> {
                try {
                    String json = flow.exportSettings();
                    store().save(typed, now(), tvModel, json);
                    runOnUiThread(() -> { busy = false; Toast.makeText(this, t("st_saved"), Toast.LENGTH_SHORT).show(); show(14); });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; saveBtn.setAlpha(1f); Toast.makeText(this, t("st_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
        save.addView(saveBtn);
        body.addView(save, lp(0, 10, 0, 8));
        final java.util.List<Profile> all = store().list();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(text(t("st_list_h"), medium, 19, GOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(text(String.valueOf(all.size()), medium, 14, SAND));
        body.addView(head, lp(4, 8, 4, 6));
        if (all.isEmpty()) body.addView(text(t("st_empty"), regular, 14, MUTED), lp(4, 0, 4, 8));
        for (final Profile p : all) {
            LinearLayout c = card(false);
            TextView nm = text(p.name, medium, 19, SAND);
            nm.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            c.addView(nm);
            TextView meta = text((p.tv.isEmpty() ? "" : p.tv + " · ") + "ChouiTV " + p.version + " · " + p.date, regular, 12, MUTED);
            meta.setTextDirection(View.TEXT_DIRECTION_LTR);
            c.addView(meta, lp(0, 2, 0, 10));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            final TextView send = pill(t("st_send"), true), ren = pill(t("st_rename"), false), del = pill(t("st_delete"), false);
            LinearLayout.LayoutParams w = new LinearLayout.LayoutParams(0, -2, 1f);
            w.setMarginEnd(dp(8));
            row.addView(send, w);
            row.addView(ren, w);
            row.addView(del, new LinearLayout.LayoutParams(0, -2, 1f));
            send.setOnClickListener(v -> startSend(p, send));
            ren.setOnClickListener(v -> askRename(p));
            del.setOnClickListener(v -> askDelete(p));
            c.addView(row);
            body.addView(c, lp(0, 0, 0, 8));
        }
        button.setText(t("cl_home"));
        button.setOnClickListener(v -> show(12));
    }

    private void askRename(final Profile p) {
        final EditText in = new EditText(this);
        in.setText(p.name);
        in.setSingleLine(true);
        in.setSelection(in.getText().length());
        new AlertDialog.Builder(this).setTitle(t("st_rename_h")).setView(in)
            .setPositiveButton(t("st_ok"), (d, w) -> {
                try { store().rename(p.id, in.getText().toString()); } catch (Exception e) { Toast.makeText(this, t("st_err", e.getMessage()), Toast.LENGTH_LONG).show(); }
                show(14);
            }).setNegativeButton(t("cancel"), null).show();
    }

    private void askDelete(final Profile p) {
        new AlertDialog.Builder(this).setTitle(t("st_del_q", p.name))
            .setPositiveButton(t("st_delete"), (d, w) -> { store().delete(p.id); show(14); })
            .setNegativeButton(t("cancel"), null).show();
    }

    /** Asks the TV what an import would change; nothing is applied. */
    private void startSend(final Profile p, final TextView trigger) {
        if (busy) return;
        busy = true;
        trigger.setAlpha(0.6f);
        bg.execute(() -> {
            String out;
            try {
                out = flow.importSettings(Base64.encodeToString(p.settingsJson.getBytes("UTF-8"), Base64.NO_WRAP), false);
            } catch (IOException e) {
                out = e.getMessage() == null ? "error: no answer" : e.getMessage();   // the TV's «error: …», or a lost connection
            } catch (Exception e) {
                out = "error: " + e;
            }
            final Preview pv = Preview.parse(out);
            runOnUiThread(() -> { busy = false; sendProfile = p; sendPreview = pv; show(15); });
        });
    }

    private void sendScreen(LinearLayout body, final TextView button) {
        final Profile p = sendProfile;
        final Preview pv = sendPreview;
        titleBlock(body, t("st_send_title", p.name), t("st_send_sub"));
        LinearLayout info = card(false);
        info.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        info.addView(text(t("st_to", tvModelLine()), medium, 16, SAND));
        info.addView(text(pv.error != null ? "" : pv.nothing ? t("st_nothing") : t("st_count", pv.count), regular, 13, MUTED));
        body.addView(info, lp(0, 10, 0, 8));
        if (pv.error != null) {
            LinearLayout e = card(false);
            e.setBackground(cardBg(false, 0x33E8896B, 0xFFE8896B));
            e.addView(text(t("st_err_import"), medium, 15, SAND));
            e.addView(text(pv.error, regular, 12, MUTED), lp(0, 4, 0, 0));
            body.addView(e);
            button.setText(t("st_back"));
            button.setOnClickListener(v -> show(14));
            return;
        }
        if (pv.nothing) {
            button.setText(t("st_back"));
            button.setOnClickListener(v -> show(14));
            return;
        }
        for (String change : pv.changes) {
            LinearLayout c = card(false);
            c.setPadding(dp(16), dp(10), dp(16), dp(10));
            TextView tv = text(change.replace(" -> ", "  →  "), regular, 14, SAND);
            tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            c.addView(tv);
            body.addView(c, lp(0, 0, 0, 6));
        }
        body.addView(text(t("st_keep"), regular, 13, MUTED), lp(4, 8, 4, 0));
        button.setText(t("st_apply", pv.count));
        button.setOnClickListener(v -> {
            if (busy) return;
            busy = true;
            button.setAlpha(0.6f);
            bg.execute(() -> {
                try {
                    String out = flow.importSettings(Base64.encodeToString(p.settingsJson.getBytes("UTF-8"), Base64.NO_WRAP), true);
                    try { flow.rescheduleAdhan(); } catch (IOException ignored) { }
                    final Preview done = Preview.parse(out);
                    runOnUiThread(() -> { busy = false; sentResult = done; show(16); });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; button.setAlpha(1f); Toast.makeText(this, t("st_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
    }

    private void sentScreen(LinearLayout body, TextView button) {
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Orn.Arch arch = new Orn.Arch(this);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL); in.setGravity(Gravity.CENTER); in.setPadding(dp(18), dp(36), dp(18), dp(16));
        TextView check = text("✓", medium, 40, GOLD);
        check.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL); ring.setColor(0xFF142C1F); ring.setStroke(dp(3), GOLD);
        check.setBackground(ring);
        in.addView(check, new LinearLayout.LayoutParams(dp(84), dp(84)));
        arch.addView(in, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(180), dp(180));
        ap.gravity = Gravity.CENTER_HORIZONTAL; ap.topMargin = dp(10);
        body.addView(arch, ap);
        TextView title = text(t("st_sent_t"), display, 26, SAND);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 14, 0, 10));
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        c.addView(text(t("st_sent_d", sentResult == null ? 0 : sentResult.count, tvModelLine()), regular, 15, SAND));
        body.addView(c, lp(0, 0, 0, 10));
        final TextView undo = ghostButton(t("st_undo"), MUTED);
        undo.setOnClickListener(v -> {
            if (busy) return;
            busy = true; undo.setAlpha(0.6f);
            bg.execute(() -> {
                try {
                    flow.undoImport();
                    try { flow.rescheduleAdhan(); } catch (IOException ignored) { }
                    runOnUiThread(() -> { busy = false; undo.setAlpha(1f); undo.setText(t("st_undone")); undo.setEnabled(false); });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; undo.setAlpha(1f); Toast.makeText(this, t("st_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
        body.addView(undo, new LinearLayout.LayoutParams(-1, dp(52)));
        button.setText(t("st_back"));
        button.setOnClickListener(v -> show(14));
    }

    // ---------- choosers ----------

    private void pickCity() {
        if (cityList.isEmpty()) { Toast.makeText(this, t("cities_unavail"), Toast.LENGTH_SHORT).show(); return; }
        final String[] regions = new String[cityList.size()];
        for (int i = 0; i < regions.length; i++) regions[i] = cityList.get(i)[0];
        new AlertDialog.Builder(this).setTitle(t("region")).setItems(regions, (d, which) -> {
            final String[] cities = cityList.get(which)[1].split("،\\s*");
            new AlertDialog.Builder(this).setTitle(regions[which]).setItems(cities, (d2, w2) -> { city = cities[w2].trim(); cityV.setText(city); }).show();
        }).show();
    }

    private interface Pick { void got(String v); }

    private void pick(String title, final String[] keys, final String prefix, final Pick done) {
        String[] names = new String[keys.length];
        for (int i = 0; i < keys.length; i++) names[i] = label(prefix, keys[i]);
        new AlertDialog.Builder(this).setTitle(title).setItems(names, (d, w) -> done.got(keys[w])).show();
    }

    private static String label(String prefix, String key) { return t(prefix + key); }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_SETTINGS || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        bg.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                if (o.size() > 200000) throw new java.io.IOException(t("file_big"));
                final String b64 = Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
                final String preview = flow.importSettings(b64, false);   // nothing is applied yet: what would change
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle(t("will_change"))
                    .setMessage(preview.isEmpty() ? t("no_change") : preview)
                    .setPositiveButton(t("use_file"), (d, w) -> { importedB64 = b64; if (importV != null) importV.setText(t("import_ready")); })
                    .setNegativeButton(t("cancel"), null).show());
            } catch (final Exception e) {
                runOnUiThread(() -> Toast.makeText(this, t("err_read", e.getMessage()), Toast.LENGTH_LONG).show());
            }
        });
    }

    // ---------- errors in plain words ----------

    private String friendly(Exception e, String ip, boolean connecting) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (e instanceof UnknownHostException || m.startsWith("HTTP ") || m.contains("checksum") || m.contains("too small")) return t("err_net");
        if (e instanceof ConnectException || e instanceof NoRouteToHostException || m.contains("failed to connect") || m.contains("Network is unreachable")) {
            return connecting ? t("err_unreach", ip) : t("err_generic", m);
        }
        if (e instanceof SocketTimeoutException || m.contains("timed out") || m.contains("connection abort") || m.contains("closed the connection") || m.contains("did not accept")) {
            return connecting ? t("err_allow") : t("err_generic", m);
        }
        return t("err_generic", m.isEmpty() ? e.getClass().getSimpleName() : m);
    }

    private void stopTicker() {
        if (ticker != null) ui.removeCallbacks(ticker);
        ticker = null;
    }

    // ---------- views ----------

    private LinearLayout dots(int n) {
        LinearLayout d = new LinearLayout(this);
        d.setGravity(Gravity.START);
        for (int i = 0; i < 6; i++) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(4));
            g.setColor(i <= n ? GOLD : 0xFF3C4840);
            v.setBackground(g);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(i == n ? 22 : 8), dp(5));
            p.setMarginEnd(dp(6));
            d.addView(v, p);
        }
        return d;
    }

    private TextView addChoice(LinearLayout body, String label, String value, View.OnClickListener click) {
        LinearLayout c = card(false);
        c.addView(text(label, regular, 13, MUTED));
        TextView v = text(value, medium, 19, SAND);
        v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);   // an Arabic city name in a French page still starts on the reading side
        c.addView(v);
        c.setOnClickListener(click);
        body.addView(c, lp(0, 0, 0, 10));
        return v;
    }

    private void mark(TextView m, LinearLayout card, String state) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        switch (state) {
            case "done": g.setColor(0xFF3F7A5C); m.setText("✓"); m.setTextColor(SAND); card.setBackground(cardBg(false)); break;
            case "run": g.setColor(0); g.setStroke(dp(2), GOLD); m.setText("…"); m.setTextColor(GOLD); card.setBackground(cardBg(true)); break;
            case "fail": g.setColor(0xFF7A3F3F); m.setText("!"); m.setTextColor(SAND); break;
            default: g.setColor(0); g.setStroke(dp(2), 0xFF50645A); m.setText(""); break;
        }
        m.setBackground(g);
    }

    private void gold(final TextView tv) {
        tv.post(() -> {
            tv.getPaint().setShader(new LinearGradient(0, 0, 0, tv.getHeight(), 0xFFFFF0C4, 0xFFB27E30, Shader.TileMode.CLAMP));
            tv.invalidate();
        });
    }

    private TextView button(String label) {
        TextView b = text(label, medium, 19, DARK);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFFFFE296, 0xFFC48E3A});
        g.setCornerRadius(dp(30));
        b.setBackground(g);
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(56)));
        return b;
    }

    private LinearLayout card(boolean selected) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(14), dp(18), dp(14));
        c.setBackground(cardBg(selected));
        c.setClickable(true);
        return c;
    }

    private GradientDrawable cardBg(boolean selected) {
        return selected ? cardBg(true, 0x28D6AF6B, GOLD) : cardBg(false, 0x14FFFFFF, 0x2BFFFFFF);
    }

    private GradientDrawable cardBg(boolean selected, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(fill);
        g.setStroke(dp(selected ? 2 : 1), stroke);
        return g;
    }

    private GradientDrawable background() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFF0C1D12, DARK, 0xFF0A2016});
    }

    private TextView text(String s, Typeface f, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTypeface(f);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        t.setTextDirection(Strings.rtl() ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_FIRST_STRONG);
        t.setLineSpacing(dp(3), 1f);
        return t;
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
