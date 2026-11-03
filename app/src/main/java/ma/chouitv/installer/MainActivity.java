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
import android.os.SystemClock;
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
import ma.chouitv.installer.cleanup.Recheck;
import ma.chouitv.installer.guide.Guide;
import ma.chouitv.installer.guide.Outcome;
import ma.chouitv.installer.profiles.Preview;
import ma.chouitv.installer.profiles.Profile;
import ma.chouitv.installer.profiles.ProfileStore;
import ma.chouitv.installer.cleanup.Restore;
import ma.chouitv.installer.cleanup.Shell;
import ma.chouitv.installer.remote.Remote;
import ma.chouitv.installer.remote.AppSlots;
import ma.chouitv.installer.remote.Apps;
import ma.chouitv.installer.remote.LastTv;
import ma.chouitv.installer.remote.Keycap;
import ma.chouitv.installer.remote.RcIcon;
import ma.chouitv.installer.remote.RemoteBody;
import ma.chouitv.installer.remote.Socket;
import ma.chouitv.installer.remote.RcRing;
import ma.chouitv.installer.remote.RcSlider;
import ma.chouitv.installer.remote.Volume;
import ma.chouitv.installer.remote.Reconnect;

import static ma.chouitv.installer.Strings.t;

/**
 * ChouiTV Installer: six screens (language, prepare, connect, install, personalise, done) in ChouiTV's own look: dark green, gold,
 * glass cards, Thmanyah, and a Moroccan touch (zellige lattice, horseshoe arch, eight-point stars). Arabic, French or English.
 * It talks to the TV over classic ADB (Network debugging, port 5555).
 */
public class MainActivity extends Activity {
    private static final int GOLD = 0xFFD6AF6B, SAND = 0xFFF6EFE0, MUTED = 0xFFC6BBA4, DARK = 0xFF040E09;
    // «your turn» (an action the person does) and «to know» (an indication): two families, named here so the theme can change in one place
    private static final int ACTION_FILL = 0xFF1B3D2A, ACTION_BORDER = GOLD, ACTION_PILL = GOLD, ACTION_PILL_TEXT = DARK, ACTION_TEXT = SAND, ACTION_SUB = MUTED;
    private static final int INFO_EDGE = 0xFF3FA58A, INFO_FILL = 0x0FFFFFFF, INFO_ICON = 0xFF7FCDB2, INFO_TEXT = 0xFFC9D3C7;
    private static final int LINK = GOLD;
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
    private int cleanAsked;
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
        guide = Guide.fromJson(sp.getString("guide", ""));
        root = new FrameLayout(this);
        root.setBackground(background());
        setContentView(root);
        if (sp.getString("lang", null) != null && LastTv.load(prefs()) != null) {
            show(12);
            if (sp.getBoolean("auto_remote", false)) ui.post(this::quickRemote);
        } else show(0);
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
        int to = Nav.backTarget(step, busy, guide != null, cleanFromDone);
        if (to == Nav.BLOCKED) return;
        if (to == Nav.EXIT) {
            // the end of the line: a second press within two seconds leaves, the first one only says so
            long now = SystemClock.elapsedRealtime();
            if (Nav.exitNow(lastBack, now)) { super.onBackPressed(); return; }
            lastBack = now;
            Toast.makeText(this, t("exit_again"), Toast.LENGTH_SHORT).show();
            return;
        }
        show(to);
    }

    private long lastBack;

    /** The visible «‹ Retour» at the top of a screen: it does exactly what the system Back key does there. Drawn chevron, so it follows the reading direction. */
    private View backControl() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setClickable(true);
        r.setContentDescription(t("back"));
        r.addView(new Orn.Chevron(this, LINK), new LinearLayout.LayoutParams(dp(12), dp(18)));
        TextView l = text(t("back"), medium, 16, LINK);
        l.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.setMarginStart(dp(8));
        r.addView(l, tp);
        r.setPadding(0, 0, dp(24), 0);
        r.setOnClickListener(v -> onBackPressed());
        return r;
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
        boolean back = Nav.showsBack(n, busy, guide != null, cleanFromDone);
        col.setPadding(dp(22), dp(back ? 28 : 48), dp(22), dp(18));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        if (back) col.addView(backControl(), new LinearLayout.LayoutParams(-2, dp(48)));
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
        View fade = new View(this);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x000A1B13, 0xE60A1B13}));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, dp(28));
        fp.topMargin = -dp(28);
        col.addView(fade, fp);
        TextView button = button("");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(BTN_H));
        bp.topMargin = dp(8);
        col.addView(button, bp);
        if (guide != null && (n == 1 || n == 2 || n == 4 || n == 8 || n == 11 || n == 14 || n == 17 || n == 21)) {
            if (guide.path == Guide.Path.EQUIP && n == 4) { guide.step = 2; saveGuide(); }
            guideBar(body);
        }
        if (guide != null && ((n == 5 && guide.path == Guide.Path.EQUIP) || n == 13)) clearGuide();
        switch (n) {
            case 0: language(body, button); break;
            case 18: modeScreen(body, button); break;
            case 19: pathsScreen(body, button); break;
            case 20: switchScreen(body, button); break;
            case 21: nowOrLater(body, button); break;
            case 22: kept(body, button); break;
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
            case 14: profilesScreen(body, button, true); break;
            case 17: profilesScreen(body, button, false); break;
            case 15: sendScreen(body, button); break;
            case 23: progressScreen(body, button); break;
            case 24: endScreen(body, button); break;
            case 25: remoteScreen(body, button); break;
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
        button.setOnClickListener(v -> show(18));
    }

    private void prepare(LinearLayout body, TextView button) {
        actionCard(body, true, new String[] {t("prep1a"), t("prep1b")}, new String[] {t("prep2a"), t("prep2b")}, new String[] {t("prep3a"), t("prep3b")});
        button.setText(t("next"));
        button.setOnClickListener(v -> show(2));
    }

    private void connect(final LinearLayout body, final TextView button) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(t("found"), medium, 15, GOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView refresh = ghostButton("↻  " + t("refresh"), GOLD);
        refresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        refresh.setMinHeight(dp(44));
        refresh.setSingleLine(true);
        refresh.setPadding(dp(18), 0, dp(18), 0);
        head.addView(refresh, new LinearLayout.LayoutParams(-2, dp(44)));
        body.addView(head, lp(0, 0, 0, 8));
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list);
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
        actionCard(body, true, new String[] {t("allow1")}, new String[] {t("allow2")});
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
        final boolean[] scanning = {false};
        final Runnable scan = new Runnable() {
            @Override public void run() {
                if (scanning[0]) return;
                scanning[0] = true;
                chosen = "";
                refresh.setText("…  " + t("searching_short"));
                refresh.setAlpha(0.6f);
                list.removeAllViews();
                list.addView(text(t("searching"), regular, 15, MUTED));
                bg.execute(() -> {
                    final List<String> tvs = Net.findTvs(5555);
                    final java.util.Map<String, String> names = new java.util.HashMap<>();
                    final SharedPreferences sp = getSharedPreferences("installer", MODE_PRIVATE);
                    for (String ip : tvs) {
                        String who = Adb.probe(ip, 5555, new File(getFilesDir(), "keys"));
                        if (who != null) { sp.edit().putString("tv_" + ip, who).apply(); names.put(ip, who + "|known"); }
                        else if (sp.getString("tv_" + ip, null) != null) names.put(ip, sp.getString("tv_" + ip, "") + "|seen");
                    }
                    runOnUiThread(() -> {
                        scanning[0] = false;
                        if (step != 2) return;
                        refresh.setText("↻  " + t("refresh"));
                        refresh.setAlpha(1f);
                        list.removeAllViews();
                        if (tvs.isEmpty()) list.addView(text(t("none_found"), regular, 14, MUTED));
                        for (final String ip : tvs) {
                            final LinearLayout c = card(false);
                            String[] w = names.containsKey(ip) ? names.get(ip).split("\\|") : null;
                            String title = w == null ? t("tv") : (w[0].isEmpty() ? "" : w[0] + " · ") + w[1];
                            String state = w == null ? t("tv_first") : w[2].equals("known") ? t("tv_known") : t("tv_seen");
                            c.addView(text(title, medium, 18, SAND));
                            TextView a = text(ip + " · " + state, regular, 14, MUTED);
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
        };
        refresh.setOnClickListener(v -> { if (!busy) scan.run(); });
        scan.run();
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
                if (tv != null && tv != a) tv.close();
                tv = a;
                flow = new Flow(a, getCacheDir());
                flow.ask = this::askRemoval;
                model = flow.model();
                tvModel = model;
                try { tvBrand = flow.prop("ro.product.manufacturer"); } catch (IOException ignored) { }
                getSharedPreferences("installer", MODE_PRIVATE).edit().putString("tv_" + ip, tvBrand + "|" + model).apply();
                LastTv.save(prefs(), ip, (tvBrand + " " + model).trim());
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

    /** The weight of each of the eight steps in the global percentage (they add up to 100): check, download, Channels, home, permissions, defaults, verify, old settings. */
    private static final int[] W = {3, 25, 17, 20, 6, 12, 9, 8};

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
        final String[] titles = {t("s_check"), t("s_download", ""), t("s_channels"), t("s_home"), t("s_perms"), t("s_defaults"), t("s_verify"), t("s_migrate")};
        final String[] subs0 = {"", "", t("ch_sub"), "", t("sub_perms"), t("sub_defaults"), t("sub_verify"), ""};
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
        actionCard(body, true, new String[] {t("inst_ok")});
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
                // a TV still on 3.x has the separate screensaver app: its settings are read now, before anything changes
                final Migration.Store kept = Migration.fileStore(new java.io.File(getFilesDir(), "old-screensaver-settings.json"));
                final Migration.State migration = Migration.before(flow.migrationTv(), kept);
                node(nodes, 0, Orn.Node.DONE);
                progress(0, 1f, bar, pctV, stepV, etaV, t0);

                cur = 1;
                node(nodes, 1, Orn.Node.RUN);
                final Net.Release rel = Net.latest();
                final String ver = rel.version;
                runOnUiThread(() -> titleV[1].setText(t("s_download", ver)));
                final String[] pk = {Flow.CHANNELS, Flow.HOME};
                final java.io.File[] files = new java.io.File[2];
                // an app the TV already has at this version or newer is neither downloaded nor installed (Channels has no version in the manifest: it always goes)
                int tvHome = -1;
                try { tvHome = flow.installedCode(Flow.HOME); } catch (IOException unreadable) { }
                final boolean[] skip = {false, InstallPlan.upToDate(tvHome, rel.code)};
                for (int j = 0; j < 2; j++) {
                    if (skip[j]) continue;
                    String url = null, sha = "";
                    for (Net.Apk a : rel.apks) if (a.pkg.equals(pk[j])) { url = a.url; sha = a.sha; }
                    if (j == 0) { url = Net.CHANNELS; sha = Trust.channelsSha(rel.channelsSha); }
                    if (url == null) throw new IOException("no link for " + pk[j]);
                    final int jj = j;
                    final String u = url, h = sha;
                    // one app at a time, and one more try for it before the whole install is given up
                    files[j] = twice(() -> flow.download(pk[jj], u, h, (d, tt) -> {
                        progress(1, (jj + (tt > 0 ? (float) d / tt : 0f)) / 2f, bar, pctV, stepV, etaV, t0);
                        sub(subV, 1, (jj + 1) + "/2 · " + mb(d) + (tt > 0 ? " / " + mb(tt) : ""));
                    }));
                    if (j != 0) ApkCheck.verify(this, files[j], pk[j], rel.code);   // the right app, the right version, signed with our key
                }
                if (skip[1]) sub(subV, 1, t("up_to_date", ver));
                node(nodes, 1, Orn.Node.DONE);
                progress(1, 1f, bar, pctV, stepV, etaV, t0);

                for (int k = 0; k < 2; k++) {
                    cur = 2 + k;
                    final int s = cur;
                    node(nodes, s, Orn.Node.RUN);
                    final int kk = k;
                    if (skip[kk]) {
                        sub(subV, s, t("up_to_date", ver));
                        node(nodes, s, Orn.Node.DONE);
                        progress(s, 1f, bar, pctV, stepV, etaV, t0);
                        continue;
                    }
                    final boolean[] did = {true};
                    twice(() -> {
                        if (!files[kk].exists()) {   // a second try after a failure: the file was deleted, it is fetched and checked again
                            files[kk] = flow.download(pk[kk], kk == 0 ? Net.CHANNELS : urlOf(rel, pk[kk]), kk == 0 ? Trust.channelsSha(rel.channelsSha) : shaOf(rel, pk[kk]), null);
                            if (kk != 0) ApkCheck.verify(this, files[kk], pk[kk], rel.code);
                        }
                        did[0] = flow.installFile(pk[kk], files[kk], (d, tt) -> {
                            float f = tt > 0 ? (float) d / tt : 0f;
                            progress(s, f * 0.85f, bar, pctV, stepV, etaV, t0);
                            sub(subV, s, f >= 1f ? t("sub_install") : t("sub_send", (int) (f * 100)));
                        }, kk == 0 ? 0 : rel.code);
                        return files[kk];
                    });
                    if (!did[0]) sub(subV, s, t("up_to_date", kk == 0 ? t("ch_sub") : ver));
                    else if (k == 0) sub(subV, s, t("ch_sub"));
                    else sub(subV, s, ver);
                    node(nodes, s, Orn.Node.DONE);
                    progress(s, 1f, bar, pctV, stepV, etaV, t0);
                }

                cur = 4;
                node(nodes, 4, Orn.Node.RUN);
                flow.grantPermissions();
                node(nodes, 4, Orn.Node.DONE);
                progress(4, 1f, bar, pctV, stepV, etaV, t0);

                cur = 5;
                node(nodes, 5, Orn.Node.RUN);
                sub(subV, 5, t("sub_switch"));
                flow.setDefaults();
                try { homeResult = flow.switchHome(); } catch (IOException lost) { homeResult = Flow.Home.UNSURE; }
                sub(subV, 5, t("sub_defaults"));
                node(nodes, 5, Orn.Node.DONE);
                progress(5, 1f, bar, pctV, stepV, etaV, t0);

                cur = 6;
                node(nodes, 6, Orn.Node.RUN);
                try { flow.verify(rel.code); } catch (IOException lost) { if (homeResult != Flow.Home.UNSURE) throw lost; }
                sub(subV, 6, homeResult == Flow.Home.OURS ? t("home_ok") : t("home_warn"));
                node(nodes, 6, Orn.Node.DONE);
                progress(6, 1f, bar, pctV, stepV, etaV, t0);

                // last, once 4.0 is confirmed: the old settings go into the merged app; the old app goes only if the person says so
                cur = 7;
                node(nodes, 7, Orn.Node.RUN);
                flow.askOldSaver = this::askRemoveOldSaver;
                Migration.Outcome moved = Migration.after(flow.migrationTv(), kept, migration, flow.askOldSaver);
                sub(subV, 7, t(moved == Migration.Outcome.NOTHING ? "sub_migrate_none" : moved == Migration.Outcome.REMOVED ? "sub_migrate_removed"
                    : moved == Migration.Outcome.KEPT ? "sub_migrate_kept" : moved == Migration.Outcome.REMOVE_FAILED ? "sub_migrate_stuck" : "sub_migrate_failed"));
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
                    err.setText(e instanceof Flow.AlreadyNewer ? friendly(e, "", false) : t("err_install", friendly(e, "", false)));
                    button.setText(t("retry"));
                    button.setAlpha(1f);
                    button.setOnClickListener(v -> show(3));
                });
            }
        });
    }

    private interface Attempt { java.io.File run() throws Exception; }

    /** One more try after a failure of the network or of the TV; a refusal by the person or by the rules is final. */
    private java.io.File twice(Attempt a) throws Exception {
        try {
            return a.run();
        } catch (Flow.Declined | Flow.AlreadyNewer | SecurityException stop) {
            throw stop;
        } catch (IOException first) {
            if (String.valueOf(first.getMessage()).contains("signed with the ChouiTV key") || String.valueOf(first.getMessage()).contains("not an app")) throw first;
            Thread.sleep(2500);
            return a.run();
        }
    }

    private static String urlOf(Net.Release rel, String pkg) throws IOException {
        for (Net.Apk a : rel.apks) if (a.pkg.equals(pkg)) return a.url;
        throw new IOException("no link for " + pkg);
    }

    private static String shaOf(Net.Release rel, String pkg) throws IOException {
        for (Net.Apk a : rel.apks) if (a.pkg.equals(pkg)) return a.sha;
        throw new IOException("no checksum for " + pkg);
    }

    /** Asked on the worker thread when an app of ours is signed with another key: the screen asks the person and this waits for the answer. */
    private boolean askRemoval(final String pkg) {
        final java.util.concurrent.CountDownLatch answered = new java.util.concurrent.CountDownLatch(1);
        final boolean[] yes = {false};
        runOnUiThread(() -> {
            AlertDialog dlg = new AlertDialog.Builder(this).setTitle(t("rm_title")).setMessage(t("rm_msg", pkg))
                .setPositiveButton(t("rm_keep"), (d, w) -> answered.countDown())
                .setNegativeButton(t("rm_do"), (d, w) -> { yes[0] = true; answered.countDown(); })
                .setOnCancelListener(d -> answered.countDown()).create();
            dlg.setOnShowListener(x -> {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFB27E30);
                dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFD9573F);
            });
            dlg.show();
        });
        try { answered.await(); } catch (InterruptedException e) { return false; }
        return yes[0];
    }

    /** Asked on the worker thread once the old settings are in the new app: remove the old 3.x screensaver app? Nothing is removed unless the person says yes. */
    private boolean askRemoveOldSaver(final String pkg) {
        final java.util.concurrent.CountDownLatch answered = new java.util.concurrent.CountDownLatch(1);
        final boolean[] yes = {false};
        runOnUiThread(() -> {
            AlertDialog dlg = new AlertDialog.Builder(this).setTitle(t("old_title")).setMessage(t("old_msg", pkg))
                .setPositiveButton(t("old_keep"), (d, w) -> answered.countDown())
                .setNegativeButton(t("old_do"), (d, w) -> { yes[0] = true; answered.countDown(); })
                .setOnCancelListener(d -> answered.countDown()).create();
            dlg.setOnShowListener(x -> {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFB27E30);
                dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFD9573F);
            });
            dlg.show();
        });
        try { answered.await(); } catch (InterruptedException e) { return false; }
        return yes[0];
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
        sectionLabel(body, t("sec_choose"));
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
        body.addView(star, new LinearLayout.LayoutParams(-1, dp(BTN_H)));
        boolean offer = cleanNowCard(body, button);
        if (homeResult != null && homeResult != Flow.Home.OURS) {
            sectionLabel(body, t("sec_left"));
            infoBar(body, homeResult == Flow.Home.UNSURE || homeResult == Flow.Home.FAILED ? t("home_unsure") : t("home_kept"));
        }
        sectionLabel(body, t("sec_know"));
        infoBar(body, t("tip") + " : " + t("tip_text"));
        TextView open = ghostButton(t("open_tv"), GOLD);
        open.setOnClickListener(v -> bg.execute(() -> { try { flow.openOnTv(); } catch (Exception ignored) { } }));
        body.addView(open, lp(0, 14, 0, 0));
        if (offer) {
            // the cleanup is the next step: it is the bottom button, and the way back to the menu sits quietly under it
            secondaryBelow(button, t("cl_home"), () -> show(18));
        } else {
            button.setText(t("cl_home"));
            ready(button, true);
            button.setOnClickListener(v -> show(18));
        }
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
            byte[] online = Net.fetch(CLEANUP_URL, Trust.MAX_MANIFEST_BYTES);
            String sig = new String(Net.fetch(CLEANUP_URL + ".sig", Trust.MAX_MANIFEST_BYTES), "UTF-8");
            return CleanupSource.choose(bundled, online, sig);   // unsigned or changed: the lists built into the app
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

    /** The same size for every button of the same kind, on every screen: full width 64 dp, small ones in a row 52 dp. */
    private static final int BTN_H = 64, PILL_H = 52;

    /** A ready button is gold; one that is not ready yet stays clearly readable (grey-green, full-strength text), never faded away. */
    private void ready(TextView b, boolean on) {
        b.setAlpha(1f);
        GradientDrawable g;
        if (on) {
            g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFFFFE296, 0xFFC48E3A});
            g.setStroke(dp(1), 0xAAFFF4CD);
            b.setTextColor(DARK);
        } else {
            g = new GradientDrawable();
            g.setColor(0xFF3C4840);
            g.setStroke(dp(2), 0xFF96A098);
            b.setTextColor(SAND);
        }
        g.setCornerRadius(dp(30));
        b.setBackground(g);
        if (b instanceof Btn) ((Btn) b).arrow(on);
    }

    /** A grave action is never a big button: a small red link, away from the main one, that only opens a confirmation. */
    private TextView dangerLink(String label) {
        TextView l = text("\u26A0  " + label, medium, 16, 0xFFE8896B);
        l.setGravity(Gravity.CENTER);
        l.setPaintFlags(l.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        l.setPadding(dp(8), dp(14), dp(8), dp(14));
        return l;
    }

    /** The safe answer is the first one (keep), the grave one is second and red. */
    private void confirmGrave(String title, String message, String keep, String doIt, final Runnable run) {
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton(keep, null)
            .setNegativeButton(doIt, (d, w) -> run.run()).create();
        dlg.setOnShowListener(x -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFB27E30);
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFD9573F);
        });
        dlg.show();
    }

    private TextView ghostButton(String label, int color) {
        TextView g = text(label, medium, 19, color);
        g.setGravity(Gravity.CENTER);
        g.setMinHeight(dp(BTN_H));
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
        sectionLabel(body, t("sec_know"));
        infoBar(body, tvModelLine() + " · " + src + (p.alreadyOff > 0 ? " · " + t("cl_already", p.alreadyOff) : ""));
        if (p.total() == 0) {
            LinearLayout none = card(false);
            none.addView(text(t("cl_none"), medium, 17, GOLD));
            body.addView(none, lp(0, 8, 0, 0));
            afterCleanup(button);
            return;
        }
        sectionLabel(body, t("sec_choose"));
        final java.util.List<Planner.Item> noise = new java.util.ArrayList<>(p.groups.get(CleanupData.Group.TRACKING));
        noise.addAll(p.groups.get(CleanupData.Group.ADS));
        if (!noise.isEmpty()) addNoiseCard(body, noise, button);
        addChecklist(body, button, CleanupData.Group.PREINSTALLED, "cl_pre_h", "cl_pre_d");
        addChecklist(body, button, CleanupData.Group.OPTIONAL, "cl_str_h", "cl_str_d");
        addChecklist(body, button, CleanupData.Group.USER, "cl_usr_h", "cl_usr_d");
        sectionLabel(body, t("sec_prot"));
        infoBar(body, t("cl_protected", cleanupData.keep.size()) + " · " + t("cl_protected_d"));
        // the confirmation: the person is asked to look, and the button waits for it
        sectionLabel(body, t("sec_before"));
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
        ready(button, cleanConfirmed);
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
        cleanAsked = planned.size();
        final String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new java.util.Date());
        bg.execute(() -> {
            try {
                java.util.Set<String> active = new java.util.HashSet<>();   // the services running now: if a TV feature fails after a restart, these are the suspects
                try { active = Facts.activeServicePackages(flow.shell().run("dumpsys activity services")); } catch (IOException ignored) { }
                Restore point = new Restore(tvModel, date, planned).withServices(active);   // saved BEFORE the first change
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
                Restore real = new Restore(tvModel, date, cleanResult.disabled).withServices(active);   // what was really turned off
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
        Executor.Result r = cleanResult;
        int asked = cleanAsked, done = r == null ? 0 : r.disabled.size();
        boolean problem = r != null && (r.rolledBack != null || r.netLost);
        Outcome.Status st = Outcome.of(asked, done, problem);
        java.util.List<String> did = lines(t("cl_end_done_n", done), t("cl_end_kept", cleanupData.keep.size()), t("cl_end_nodel"));
        java.util.List<String> left = new ArrayList<>();
        if (r != null && r.rolledBack != null) left.add(t("cl_rolled", groupName(r.rolledBack)));
        if (r != null && !r.refused.isEmpty()) left.add(t("cl_refused", r.refused.size()));
        if (r != null && r.netLost) left.add(t("cl_net_lost"));
        setEnd(st, t(st == Outcome.Status.SUCCESS ? "cl_end_ok_t" : st == Outcome.Status.PARTIAL ? "cl_end_part_t" : "cl_end_fail_t"), t("cl_end_sub", done, asked), did, left, t("cl_res_undo_d"));
        if (done > 0) { endUndo = this::undoCleanup; endUndoLabel = t("cl_undo_link"); }
        if (cleanFromDone) { endNextLabel = t("cl_continue"); endNext = () -> show(5); }
        else { endNextLabel = t("cl_next_install"); endNext = () -> { space = SPACE_INSTALL; if (guide != null && guide.path == Guide.Path.EQUIP) { guide.step = 1; saveGuide(); } show(3); }; }
        endScreen(body, button);
    }

    /** The grave one: everything the cleanup turned off comes back, after a confirmation that says what that costs. */
    private void undoCleanup() {
        Restore now = loadRestore();
        final int n = now == null ? 0 : now.packages.size();
        confirmGrave(t("cl_undo_q"), t("cl_undo_msg", n), t("cl_undo_keep"), t("cl_undo_do"), () -> {
            busy = true;
            bg.execute(() -> {
                try {
                    Restore r = loadRestore();
                    java.util.List<String> back = r == null ? new ArrayList<String>() : Restore.apply(flow.shell(), r.packages);
                    final int total = r == null ? 0 : r.packages.size(), ok = back.size();
                    final boolean restart = r != null && Recheck.needsRestartAfterEnable(r, back);
                    runOnUiThread(() -> {
                        busy = false;
                        Outcome.Status st = Outcome.of(total, ok, false);
                        showEnd(st, t(st == Outcome.Status.SUCCESS ? "cl_back_ok_t" : "cl_back_part_t"), t("re_end_sub", ok, total), lines(t("re_done_d", ok)), new ArrayList<String>(), restart ? t("cl_restart_tv") + "\n" + t("cl_back_info") : t("cl_back_info"));
                    });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
    }


    // ---------- the three spaces: clean, install, turn back on ----------

    private static final int SPACE_CLEAN = 0, SPACE_INSTALL = 1, SPACE_REACT = 2;
    private int space = SPACE_INSTALL;
    private boolean cleanedHere, cleanFromDone;
    private Reactivation.Plan reactPlan;
    private Recheck reactRecheck;      // what the last cleanup turned off, compared with what is off now (null: no restore point)
    private boolean reactNeedsRestart;  // a system service was put back: the TV must restart
    private final java.util.Set<String> reactSel = new java.util.LinkedHashSet<>();
    private java.util.List<String> reactBack = new ArrayList<>();
    private int reactAsked;

    private void hub(LinearLayout body, TextView button) {
        cleanedHere = false;
        cleanFromDone = false;
        titleBlock(body, t("hub_title"), t("hub_sub"));
        final LastTv lastTv = LastTv.load(prefs());
        if (lastTv != null) {
            LinearLayout lc = card(true);
            lc.setOrientation(LinearLayout.VERTICAL);
            lc.addView(chip(t("last_tv"), GOLD, DARK, 0));
            lc.addView(text(lastTv.name, display, 21, SAND), lp(0, 6, 0, 0));
            lc.addView(text(lastTv.ip, regular, 13, MUTED), lp(0, 2, 0, 0));
            TextView go = text(t("last_open"), medium, 17, DARK);
            go.setGravity(Gravity.CENTER);
            go.setBackground(rcShape(GOLD, 26, false));
            go.setClickable(true);
            go.setOnClickListener(v -> quickRemote());
            lc.addView(go, lp(0, 12, 0, 0));
            ((LinearLayout.LayoutParams) go.getLayoutParams()).height = dp(52);
            final boolean auto = getSharedPreferences("installer", MODE_PRIVATE).getBoolean("auto_remote", false);
            TextView sw = text((auto ? "\u2611  " : "\u2610  ") + t("last_auto"), regular, 13, LINK);
            sw.setMinHeight(dp(44));
            sw.setGravity(Gravity.CENTER_VERTICAL);
            sw.setOnClickListener(v -> { getSharedPreferences("installer", MODE_PRIVATE).edit().putBoolean("auto_remote", !auto).apply(); show(12); });
            lc.addView(sw, lp(0, 6, 0, 0));
            TextView other = text(t("last_other"), regular, 13, LINK);
            other.setMinHeight(dp(44));
            other.setGravity(Gravity.CENTER_VERTICAL);
            other.setOnClickListener(v -> { clearGuide(); space = SPACE_REMOTE; show(1); });
            lc.addView(other);
            body.addView(lc, lp(0, 4, 0, 4));
        }
        String[][] cards = {{"1", "hub_clean_t", "hub_clean_d", "hub_clean_tag"}, {"2", "hub_inst_t", "hub_inst_d", "hub_inst_tag"}, {"3", "hub_get_t", "hub_get_d", "hub_get_tag"}, {"4", "hub_send_t", "hub_send_d", "hub_send_tag"}, {"5", "hub_react_t", "hub_react_d", "hub_react_tag"}, {"6", "hub_rc_t", "hub_rc_d", "hub_rc_tag"}};
        for (int i = 0; i < cards.length; i++) {
            final int sp = i == 0 ? SPACE_CLEAN : i == 1 ? SPACE_INSTALL : i == 2 ? SPACE_GET : i == 3 ? SPACE_SEND : i == 4 ? SPACE_REACT : SPACE_REMOTE;
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
            c.setOnClickListener(v -> { if (!busy) { clearGuide(); space = sp; show(1); } });   // choosing a space yourself leaves any guided path
            body.addView(c, lp(0, 12, 0, 0));
        }
        resumeCard(body);
        button.setText(t("hub_mode"));
        secondaryStyle(button);   // the choices are the cards: the way back must not compete with them
        button.setOnClickListener(v -> show(18));
    }


    // ---------- the remote control ----------

    private static final int RC_OK = 0, RC_RECONNECTING = 1, RC_OFFLINE = 2;
    private static final int KEY_FILL = 0xFF1F4A33, KEY_PRESSED = 0xFF2C6445, TILE_FILL = 0xFF16382A, KEY_LINE = 0x88D6AF6B;
    private TextView rcPillName, rcPillSub;
    private View rcPillDot;
    private AppSlots rcSlots;
    private List<String> rcTvApps;
    private volatile int rcState = RC_OK;

    private interface Op { void run() throws Exception; }

    private LastTv.Store prefs() {
        final SharedPreferences sp = getSharedPreferences("installer", MODE_PRIVATE);
        return new LastTv.Store() {
            @Override public String get(String k) { return sp.getString(k, null); }
            @Override public void put(String k, String v) { sp.edit().putString(k, v).apply(); }
        };
    }

    private GradientDrawable rcShape(int fill, int radiusDp, boolean line) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(radiusDp));
        g.setColor(fill);
        if (line) g.setStroke(dp(1), KEY_LINE);
        return g;
    }

    private android.graphics.drawable.StateListDrawable rcStates(int fill, int radiusDp, boolean line) {
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_pressed}, rcShape(KEY_PRESSED, radiusDp, line));
        s.addState(new int[] {}, rcShape(fill, radiusDp, line));
        return s;
    }

    private android.graphics.drawable.StateListDrawable rcKeyStates(int fill, int radiusDp) {
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_pressed}, rcLayers(KEY_PRESSED, radiusDp, 3, 0));
        s.addState(new int[] {}, rcLayers(fill, radiusDp, 0, 3));
        return s;
    }

    /** The lip (dark) under the face of the key; {@code top} and {@code bottom} are how far the face is moved down or kept up, in dp. */
    private android.graphics.drawable.LayerDrawable rcLayers(int fill, int radiusDp, int top, int bottom) {
        android.graphics.drawable.LayerDrawable l = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {
            rcShape(0xFF08170F, radiusDp, false), rcShape(fill, radiusDp, true)});
        l.setLayerInset(1, 0, dp(top), 0, dp(bottom));
        return l;
    }

    /** A key: an icon (and a label under it when given) on a rounded raised shape. {@code spoken} is what a screen reader says. */
    private LinearLayout rcKeyBtn(int icon, String label, int w, int h, int fill, int radiusDp, int iconColor, String spoken) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rcKeyStates(fill, radiusDp));
        b.setClickable(true);
        b.setFocusable(true);
        b.setContentDescription(spoken == null ? label : spoken);
        if (label != null && h >= 90) {   // the big tiles: the icon sits in a gold-ringed medallion
            FrameLayout med = new FrameLayout(this);
            GradientDrawable m = new GradientDrawable();
            m.setShape(GradientDrawable.OVAL);
            m.setColor(0xFF0E2418);
            m.setStroke(dp(1), 0xAAD6AF6B);
            med.setBackground(m);
            med.addView(new RcIcon(this, icon, iconColor), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            b.addView(med, new LinearLayout.LayoutParams(dp(46), dp(46)));
        } else {
            int isz = label == null ? 30 : 26;
            b.addView(new RcIcon(this, icon, iconColor), new LinearLayout.LayoutParams(dp(isz), dp(isz)));
        }
        if (label != null) {
            TextView tv1 = text(label, medium, 14, SAND);
            tv1.setGravity(Gravity.CENTER);
            tv1.setMaxLines(2);
            b.addView(tv1, lp(4, 3, 4, 0));
        }
        b.setLayoutParams(new LinearLayout.LayoutParams(w <= 0 ? 0 : dp(w), dp(h), w <= 0 ? 1f : 0f));
        return b;
    }

    /** Taps send the key at once; a finger kept down on an arrow or the volume repeats it (after 0.4 s, then every 0.12 s). */
    private void rcHold(final View v, final int code) {
        final Runnable[] again = new Runnable[1];
        again[0] = new Runnable() { @Override public void run() { rcKey(code); ui.postDelayed(this, 120); } };
        v.setOnTouchListener((x, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    x.setPressed(true);
                    x.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    rcKey(code);
                    ui.postDelayed(again[0], 400);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    x.setPressed(false);
                    ui.removeCallbacks(again[0]);
                    return true;
                default:
                    return true;
            }
        });
    }

    private void rcTap(View v, final Runnable act) {
        v.setOnClickListener(x -> { x.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); act.run(); });
    }

    private void rcFail(final Exception e) {
        runOnUiThread(() -> Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_SHORT).show());
    }

    /** What the status pill says: connected, reconnecting (with the try number), or off. */
    private void rcStatus(final int state, final int attempt) {
        rcState = state;
        runOnUiThread(() -> {
            if (rcPillSub == null) return;
            LastTv last = LastTv.load(prefs());
            String ip = last == null ? "" : last.ip;
            String s = state == RC_OK ? t("rc_st_ok") : state == RC_RECONNECTING ? t("rc_st_re", attempt, Reconnect.MAX_TRIES) : t("rc_st_off");
            rcPillSub.setText(ip.isEmpty() ? s : ip + " · " + s);
            rcPillDot.setBackground(rcShape(state == RC_OK ? 0xFF4CD37F : state == RC_RECONNECTING ? 0xFFD6AF6B : 0xFFE8896B, 8, false));
        });
    }

    /** Runs a command on the TV. If the link dropped, gets it back (a few tries, growing waits) and sends the command once more. */
    private void rcRun(final Op op) {
        bg.execute(() -> {
            try {
                op.run();
                if (rcState != RC_OK) rcStatus(RC_OK, 0);
            } catch (Exception e) {
                if (!Reconnect.worthRetrying(e)) { rcFail(e); return; }
                if (rcReconnect()) {
                    try { op.run(); } catch (Exception e2) { rcFail(e2); }
                }
            }
        });
    }

    /** Reconnects to the remembered TV (on the background thread). False when it does not answer after the tries. */
    private boolean rcReconnect() {
        LastTv last = LastTv.load(prefs());
        if (last == null) { rcStatus(RC_OFFLINE, 0); return false; }
        for (int a = 1; !Reconnect.giveUp(a); a++) {
            rcStatus(RC_RECONNECTING, a);
            try {
                if (a > 1) Thread.sleep(Reconnect.delayMs(a - 1));
                Adb n = Adb.connect(last.ip, 5555, new File(getFilesDir(), "keys"));
                if (tv != null && tv != n) tv.close();
                tv = n;
                flow = new Flow(n, getCacheDir());
                flow.ask = this::askRemoval;
                rcStatus(RC_OK, 0);
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception ignored) { }
        }
        rcStatus(RC_OFFLINE, 0);
        return false;
    }

    private void rcKey(final int code) { rcRun(() -> flow.shell().run(Remote.key(code))); }

    private void rcOpen(final Remote.Target target) {
        rcRun(() -> {
            String pkg = target.pkg();
            if (pkg != null && !Remote.installed(flow.shell(), pkg)) { runOnUiThread(() -> rcMissing(target)); return; }
            if (Remote.start(flow.shell(), target) == Remote.Started.NOT_OPENABLE) {
                runOnUiThread(() -> Toast.makeText(this, t("rc_need_update"), Toast.LENGTH_LONG).show());
                flow.shell().run(Remote.open(Remote.Target.HOME));   // at least the home opens
            }
        });
    }

    /** The app is not on the TV: say so, and offer to install it from here. */
    private void rcMissing(Remote.Target target) {
        final boolean channels = target == Remote.Target.CHANNELS;
        confirmGrave(t(channels ? "rc_nochan_q" : "rc_nohome_q"), t(channels ? "rc_nochan_msg" : "rc_nohome_msg"), t("rc_later"), t("rc_install"),
            () -> { clearGuide(); space = SPACE_INSTALL; show(3); });
    }

    /** The helper that puts text on the TV's clipboard is pushed once (and again when its size is not the one of the app's copy). */
    private void rcEnsureHelper() throws IOException {
        java.io.File f = new java.io.File(getCacheDir(), "chouitv-clip.jar");
        try (InputStream in = getAssets().open(Remote.HELPER_ASSET); java.io.FileOutputStream o = new java.io.FileOutputStream(f)) {
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
        }
        String size = flow.shell().run("wc -c < " + Remote.HELPER_REMOTE + " 2> /dev/null").trim();
        if (!size.equals(String.valueOf(f.length()))) flow.pushFile(f, Remote.HELPER_REMOTE);
    }

    /** A pop-up card in the app's look: dark green, gold line, wide, centred. */
    private android.app.Dialog rcDialog(LinearLayout content) {
        final android.app.Dialog d = new android.app.Dialog(this);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        content.setBackground(rcShape(0xFF12331F, 24, true));
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        d.setContentView(content);
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.94f), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return d;
    }

    private LinearLayout rcDialogHead(String title, String sub, final android.app.Dialog d) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(title, display, 21, SAND));
        if (sub != null) col.addView(text(sub, regular, 13, MUTED));
        head.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView x = text("✕", medium, 20, GOLD);
        x.setGravity(Gravity.CENTER);
        x.setContentDescription(t("rc_close"));
        x.setOnClickListener(v -> d.dismiss());
        head.addView(x, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return head;
    }

    private String rcTvKey() {
        LastTv l = LastTv.load(prefs());
        return l == null ? "tv" : l.ip;
    }

    private void rcPowerAsk() {
        LastTv l = LastTv.load(prefs());
        confirmGrave(t("rc_pw_q", l == null ? "TV" : l.name), t("rc_pw_msg"), t("rc_pw_keep"), t("rc_pw_do"), () -> rcKey(Remote.POWER));
    }

    private void rcMoreMenu() {
        String[] items = {t("rc_menu"), t("rc_vol_limit"), t("rc_switch")};
        new AlertDialog.Builder(this).setTitle(t("rc_more")).setItems(items, (d, which) -> {
            if (which == 0) rcKey(Remote.MENU);
            else if (which == 1) rcLimitDialog();
            else { space = SPACE_REMOTE; show(2); }
        }).show();
    }

    private void rcTextDialog() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog d = rcDialog(c);
        c.addView(rcDialogHead(t("rc_write"), null, d));
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(t("rc_text_hint"));
        et.setHintTextColor(0xFF7A7567);
        et.setTextColor(0xFF1B1B1B);
        et.setTextSize(19);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setBackground(rcShape(0xFFF6EFE0, 14, true));
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.addView(et, lp(0, 10, 0, 0));
        c.addView(text(t("rc_text_tip"), regular, 12, MUTED), lp(2, 6, 2, 0));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView clear = text(t("rc_clear"), medium, 17, SAND);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(rcStates(KEY_FILL, 22, true));
        clear.setClickable(true);
        clear.setOnClickListener(v -> et.setText(""));
        TextView send = text(t("rc_send"), medium, 17, DARK);
        send.setGravity(Gravity.CENTER);
        send.setBackground(rcShape(GOLD, 22, false));
        send.setClickable(true);
        send.setOnClickListener(v -> {
            final String s = et.getText().toString();
            if (s.trim().isEmpty()) { Toast.makeText(this, t("rc_empty"), Toast.LENGTH_SHORT).show(); return; }
            rcRun(() -> {
                try {
                    rcEnsureHelper();
                    Remote.sendText(flow.shell(), s);
                    runOnUiThread(() -> { et.setText(""); Toast.makeText(this, t("rc_sent"), Toast.LENGTH_SHORT).show(); });
                } catch (IllegalArgumentException e) {
                    runOnUiThread(() -> Toast.makeText(this, t("rc_too_long", Remote.MAX_TEXT), Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    if (Reconnect.worthRetrying(e) && !String.valueOf(e.getMessage()).startsWith("the TV did not take")) throw e;
                    runOnUiThread(() -> Toast.makeText(this, t("rc_text_fail", friendly(e, "", false)), Toast.LENGTH_LONG).show());
                }
            });
        });
        row.addView(clear, new LinearLayout.LayoutParams(0, dp(52), 1f));
        LinearLayout.LayoutParams sp1 = new LinearLayout.LayoutParams(0, dp(52), 1.4f);
        sp1.setMarginStart(dp(10));
        row.addView(send, sp1);
        c.addView(row, lp(0, 12, 0, 0));
        d.show();
        et.requestFocus();
    }

    private void rcAppsDialog() {
        rcSlots = AppSlots.load(prefs(), rcTvKey());
        showAppsDialog(false);
    }

    private void showAppsDialog(final boolean edit) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog d = rcDialog(c);
        c.addView(rcDialogHead(t("rc_apps"), edit ? t("rc_edit") : t("rc_apps_sub"), d));
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int k = 0; k < 3; k++) {
                final int i = r * 3 + k;
                final String pkg = rcSlots.get(i);
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setBackground(pkg == null ? rcShape(0x00000000, 16, true) : rcStates(TILE_FILL, 16, true));
                cell.setClickable(true);
                cell.addView(rcMark(pkg, 48), new LinearLayout.LayoutParams(dp(48), dp(48)));
                TextView name = text(pkg == null ? t("rc_add") : rcLabel(pkg) + (edit ? " ✎" : ""), medium, 13, pkg == null ? MUTED : SAND);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);
                cell.addView(name, lp(4, 6, 4, 0));
                cell.setContentDescription(pkg == null ? t("rc_add") : rcLabel(pkg));
                cell.setOnClickListener(v -> {
                    if (pkg == null || edit) { d.dismiss(); rcPick(i); return; }
                    d.dismiss();
                    rcRun(() -> flow.shell().run(Remote.launch(pkg)));
                });
                cell.setOnLongClickListener(v -> { d.dismiss(); rcPick(i); return true; });
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(112), 1f);
                cp.setMargins(dp(5), dp(5), dp(5), dp(5));
                row.addView(cell, cp);
            }
            c.addView(row, lp(0, r == 0 ? 10 : 0, 0, 0));
        }
        TextView toggle = text(edit ? t("rc_done") : t("rc_edit"), medium, 17, edit ? DARK : SAND);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(edit ? rcShape(GOLD, 22, false) : rcStates(KEY_FILL, 22, true));
        toggle.setClickable(true);
        toggle.setOnClickListener(v -> { d.dismiss(); showAppsDialog(!edit); });
        c.addView(toggle, lp(5, 8, 5, 0));
        ((LinearLayout.LayoutParams) toggle.getLayoutParams()).height = dp(52);
        c.addView(text(t("rc_tip_edit"), regular, 12, MUTED), lp(4, 8, 4, 0));
        d.show();
    }

    /** What the TV calls an app (kept on the phone once the TV has told it); the end of the package name only as a last resort. */
    private String rcLabel(String pkg) {
        String l = getSharedPreferences("installer", MODE_PRIVATE).getString("applabel_" + pkg, null);
        return l != null ? l : Apps.label(pkg);
    }

    private File rcIconFile(String pkg) { return new File(new File(getFilesDir(), "app_icons"), pkg + ".png"); }

    private android.graphics.Bitmap rcIcon(String pkg) {
        File f = rcIconFile(pkg);
        return f.isFile() ? android.graphics.BitmapFactory.decodeFile(f.getPath()) : null;
    }

    /** The icon of an app in a round medallion, or its first letter when the TV gave none. */
    private View rcMark(String pkg, int sizeDp) {
        android.graphics.Bitmap bm = pkg == null ? null : rcIcon(pkg);
        if (bm != null) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageBitmap(bm);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(dp(2), dp(2), dp(2), dp(2));
            return iv;
        }
        TextView initial = text(pkg == null ? "+" : rcLabel(pkg).substring(0, 1).toUpperCase(), display, 22, GOLD);
        initial.setGravity(Gravity.CENTER);
        initial.setBackground(rcShape(0xFF0E2418, sizeDp / 2, true));
        return initial;
    }

    /** Asks the TV for the names and icons of the apps we know nothing about yet (a few at a time), and keeps the answers on the phone. */
    private void rcLoadInfo(List<String> pkgs) throws Exception {
        final SharedPreferences sp = getSharedPreferences("installer", MODE_PRIVATE);
        List<String> missing = new ArrayList<>();
        for (String p : pkgs) if (sp.getString("applabel_" + p, null) == null) missing.add(p);
        if (missing.isEmpty()) return;
        rcEnsureHelper();
        new File(getFilesDir(), "app_icons").mkdirs();
        for (int i = 0; i < missing.size(); i += 8) {
            List<String> part = missing.subList(i, Math.min(i + 8, missing.size()));
            java.util.Map<String, Apps.Info> got = Apps.parseInfo(flow.shell().run(Remote.appInfo(part)));
            SharedPreferences.Editor ed = sp.edit();
            for (java.util.Map.Entry<String, Apps.Info> e : got.entrySet()) {
                ed.putString("applabel_" + e.getKey(), e.getValue().label);
                if (e.getValue().icon != null) {
                    try (java.io.FileOutputStream o = new java.io.FileOutputStream(rcIconFile(e.getKey()))) { o.write(e.getValue().icon); }
                }
            }
            ed.apply();
        }
    }

    /** The list of the TV's apps, to put one on slot {@code slot}. */
    private void rcPick(final int slot) {
        final Toast wait = Toast.makeText(this, t("rc_apps_wait"), Toast.LENGTH_SHORT);
        if (rcTvApps == null) wait.show();
        bg.execute(() -> {
            try {
                if (rcTvApps == null) rcTvApps = Apps.choices(Apps.parse(flow.shell().run(Apps.LIST_COMMAND)));
                rcLoadInfo(rcTvApps);
            } catch (Exception e) { rcFail(e); }
            runOnUiThread(() -> showPicker(slot));
        });
    }

    private void showPicker(final int slot) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog d = rcDialog(c);
        c.addView(rcDialogHead(t("rc_pick", slot + 1), null, d));
        List<String> apps = rcTvApps == null ? new ArrayList<String>() : rcTvApps;
        if (apps.isEmpty()) c.addView(text(t("rc_apps_none"), regular, 15, MUTED), lp(4, 14, 4, 14));
        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (final String pkg : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(rcStates(pkg.equals(rcSlots.get(slot)) ? KEY_PRESSED : TILE_FILL, 12, true));
            row.setPadding(dp(12), dp(8), dp(14), dp(8));
            row.setClickable(true);
            row.addView(rcMark(pkg, 44), new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(text(rcLabel(pkg), medium, 17, SAND));
            int other = rcSlots.slotOf(pkg);
            if (other >= 0 && other != slot) names.addView(text(t("rc_in_slot", other + 1), regular, 12, MUTED));
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
            np.setMarginStart(dp(12));
            row.addView(names, np);
            row.setOnClickListener(v -> {
                AppSlots next = rcSlots;
                int was = next.slotOf(pkg);
                if (was >= 0 && was != slot) next = next.with(was, null);   // an app sits on one button only
                rcSlots = next.with(slot, pkg);
                AppSlots.save(prefs(), rcTvKey(), rcSlots);
                d.dismiss();
                showAppsDialog(true);
            });
            list.addView(row, lp(0, 0, 0, 6));
            ((LinearLayout.LayoutParams) row.getLayoutParams()).height = dp(64);
        }
        sv.addView(list);
        c.addView(sv, new LinearLayout.LayoutParams(-1, Math.min(dp(360), getResources().getDisplayMetrics().heightPixels / 2)));
        if (rcSlots.get(slot) != null) {
            TextView rm = text(t("rc_remove"), medium, 16, 0xFFE8896B);
            rm.setGravity(Gravity.CENTER);
            rm.setBackground(rcShape(0x00000000, 22, true));
            rm.setClickable(true);
            rm.setOnClickListener(v -> {
                rcSlots = rcSlots.with(slot, null);
                AppSlots.save(prefs(), rcTvKey(), rcSlots);
                d.dismiss();
                showAppsDialog(true);
            });
            c.addView(rm, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        d.show();
    }

    /** One tap from the hub: reaches the remembered TV (the open link when there is one, else a few quiet tries) and opens its remote. */
    private void quickRemote() {
        space = SPACE_REMOTE;
        Toast.makeText(this, t("last_wait"), Toast.LENGTH_SHORT).show();
        bg.execute(() -> {
            boolean ok = false;
            try { if (flow != null) { flow.shell().run("echo"); ok = true; } } catch (Exception ignored) { }
            if (!ok) ok = rcReconnect();
            final boolean fine = ok;
            runOnUiThread(() -> {
                if (fine) show(25); else Toast.makeText(this, t("last_fail"), Toast.LENGTH_LONG).show();
            });
        });
    }

    // ---- volume: a level set in one command, a step at a time when held, a maximum chosen by the family ----

    private RcSlider rcSlider;
    private volatile int volLevel = -1, volPrev = 0;
    private final java.util.concurrent.atomic.AtomicBoolean volBusy = new java.util.concurrent.atomic.AtomicBoolean(false);

    private int volLimit() { return getSharedPreferences("installer", MODE_PRIVATE).getInt("vol_limit", 0); }

    /** Reads the level the TV reports and shows it; "?" when the TV never answers. */
    private void rcVolRefresh() {
        bg.execute(() -> {
            int l = -1;
            try {
                l = Volume.parseLevel(flow.shell().run(Volume.READ));
                if (l < 0) l = Volume.parseLevel(flow.shell().run(Volume.READ_DUMP));
            } catch (Exception ignored) { }
            final int lv = l;
            volLevel = lv;
            runOnUiThread(() -> { if (rcSlider != null) rcSlider.setLevel(lv, true); });
        });
    }

    /** Sets a level in one command (the keys in one batch when the TV does not know the command), then reads it back. */
    private void rcVolSet(final int wanted) {
        final int level = Volume.capped(wanted, volLimit());
        final int from = volLevel;
        rcRun(() -> {
            String out = flow.shell().run(Volume.set(level));
            if (Volume.failed(out)) {
                String keys = Volume.keys(from, level);
                if (keys != null) flow.shell().run(keys);
            }
            // the TV's setting is slow to follow: trust what was set now, and read it back a little later
            volLevel = level;
            runOnUiThread(() -> {
                if (rcSlider != null) rcSlider.setLevel(level, false);
                ui.postDelayed(this::rcVolRefresh, 1500);
            });
        });
    }

    /** One step up or down; a new step is not started while the last one is still running, so nothing piles up and nothing goes on after the finger leaves. */
    private void rcVolStep(final boolean up) {
        final int now = volLevel, lim = volLimit();
        if (up && lim > 0 && now >= lim) return;
        if (!volBusy.compareAndSet(false, true)) return;
        final int target = now < 0 ? -1 : Volume.capped(now + (up ? 1 : -1), lim);
        if (target >= 0) { volLevel = target; if (rcSlider != null) rcSlider.setLevel(target, false); }
        rcRun(() -> {
            try {
                String out = flow.shell().run(Volume.adj(up));
                if (Volume.failed(out)) flow.shell().run(Remote.key(up ? Remote.VOLUME_UP : Remote.VOLUME_DOWN));
            } finally { volBusy.set(false); }
        });
    }

    /** The - and + keys: a tap is one step, a finger kept down repeats (0.4 s, then every 0.15 s) and stops the moment it lifts; the level is read back at the end. */
    private void rcHoldStep(final View v, final boolean up) {
        final Runnable again = new Runnable() { @Override public void run() { rcVolStep(up); ui.postDelayed(this, 150); } };
        v.setOnTouchListener((x, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    x.setPressed(true);
                    x.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    rcVolStep(up);
                    ui.postDelayed(again, 400);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    x.setPressed(false);
                    ui.removeCallbacks(again);
                    ui.postDelayed(this::rcVolRefresh, 600);
                    return true;
                default:
                    return true;
            }
        });
    }

    /** Mute remembers the level: silent when there is sound, the remembered level back when it is silent. */
    private void rcMuteToggle() {
        final int now = volLevel;
        if (now < 0) { rcKey(Remote.MUTE); return; }   // the level is not known: the TV's own mute key
        if (now > 0) volPrev = now;
        final int target = Volume.muteTarget(now, volPrev);
        if (rcSlider != null) rcSlider.setLevel(target, false);
        rcVolSet(target);
    }

    /** The maximum volume the family chooses (a level, or off). */
    private void rcLimitDialog() {
        final int[] levels = {0, 30, 50, 70, 90};
        String[] names = new String[levels.length];
        for (int i = 0; i < levels.length; i++) names[i] = levels[i] == 0 ? t("rc_vol_off") : String.valueOf(levels[i]);
        int cur = volLimit(), checked = 0;
        for (int i = 0; i < levels.length; i++) if (levels[i] == cur) checked = i;
        new AlertDialog.Builder(this).setTitle(t("rc_vol_limit")).setSingleChoiceItems(names, checked, (dlg, which) -> {
            getSharedPreferences("installer", MODE_PRIVATE).edit().putInt("vol_limit", levels[which]).apply();
            if (rcSlider != null) rcSlider.setLimit(levels[which]);
            dlg.dismiss();
        }).show();
    }

    /** A key sunk in a dark well, like the sockets of a real remote. */
    private LinearLayout rcWell(View inner, int radiusDp, int padDp) {
        LinearLayout w = new LinearLayout(this);
        w.setGravity(Gravity.CENTER);
        w.setBackground(new Socket(getResources().getDisplayMetrics().density, radiusDp));
        w.setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp));
        w.addView(inner);
        return w;
    }

    /** A key with the layered look of a real one (lip, gradient, light edge); {@code cap} is its look, 3 dp at the bottom are kept for the lip. */
    private LinearLayout rcCapBtn(int icon, String label, int w, int h, Keycap cap, int iconColor, String spoken) {
        LinearLayout b = rcKeyBtn(icon, label, w, h + 3, KEY_FILL, 12, iconColor, spoken);
        b.setBackground(cap);
        b.setPadding(0, 0, 0, dp(3));
        return b;
    }

    /** The word printed under a key, in small spaced capitals as on a real remote. */
    private TextView rcPrinted(String word) {
        TextView p = text(word.toUpperCase(java.util.Locale.ROOT), medium, 11, 0xFFE9D9A6);
        p.setGravity(Gravity.CENTER);
        p.setLetterSpacing(0.1f);
        p.setMaxLines(1);
        return p;
    }

    /** A round key in its well with its word printed under it. */
    private LinearLayout rcRoundWithWord(int icon, final String word, int sizeDp, final Runnable act) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout key = rcCapBtn(icon, null, sizeDp, sizeDp, Keycap.hard(getResources().getDisplayMetrics().density, 100), 0xFFE9D9A6, word);
        rcTap(key, act);
        col.addView(rcWell(key, sizeDp / 2 + 4, 4), new LinearLayout.LayoutParams(-2, -2));
        TextView w = rcPrinted(word);
        w.setMinHeight(dp(28));
        w.setGravity(Gravity.CENTER);
        w.setClickable(true);
        rcTap(w, act);
        col.addView(w);
        return col;
    }

    /** One half of the volume bar: rounded on the outer side only. */
    private LinearLayout rcHalfKey(int icon, boolean left, String spoken) {
        LinearLayout k = new LinearLayout(this);
        k.setGravity(Gravity.CENTER);
        k.setClickable(true);
        k.setFocusable(true);
        k.setContentDescription(spoken);
        float big = dp(28), small = dp(6);
        float[] radii = left ? new float[] {big, big, small, small, small, small, big, big} : new float[] {small, small, big, big, big, big, small, small};
        GradientDrawable up = new GradientDrawable();
        up.setCornerRadii(radii);
        up.setColor(0x00000000);
        GradientDrawable down = new GradientDrawable();
        down.setCornerRadii(radii);
        down.setColor(0x55000000);
        android.graphics.drawable.StateListDrawable sl = new android.graphics.drawable.StateListDrawable();
        sl.addState(new int[] {android.R.attr.state_pressed}, down);
        sl.addState(new int[] {}, up);
        k.setBackground(sl);
        k.addView(new RcIcon(this, icon, 0xFFE9D9A6), new LinearLayout.LayoutParams(dp(30), dp(30)));
        return k;
    }

    private void remoteScreen(LinearLayout body, final TextView button) {
        final LastTv last = LastTv.load(prefs());
        rcSlots = AppSlots.load(prefs(), rcTvKey());
        rcTvApps = null;

        // the remote's body: dark green with a thin gold bezel
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        final float density = getResources().getDisplayMetrics().density;
        panel.setBackground(new RemoteBody(density));
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));

        // top: the red power key, the connection light and the name of the TV, more
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout power = rcCapBtn(RcIcon.POWER, null, 54, 54, Keycap.power(density), 0xFFFFF1EC, t("rc_pw_do"));
        rcTap(power, this::rcPowerAsk);
        top.addView(rcWell(power, 30, 4));
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setClickable(true);
        rcPillDot = new View(this);
        status.addView(rcPillDot, new LinearLayout.LayoutParams(dp(10), dp(10)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        rcPillName = text((last == null ? "TV" : last.name).toUpperCase(java.util.Locale.ROOT), medium, 14, SAND);
        rcPillName.setMaxLines(1);
        rcPillName.setLetterSpacing(0.06f);
        rcPillSub = text("", regular, 12, MUTED);
        rcPillSub.setMaxLines(1);
        names.addView(rcPillName);
        names.addView(rcPillSub);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.setMarginStart(dp(10));
        status.addView(names, np);
        rcTap(status, () -> { if (rcState == RC_OFFLINE) bg.execute(this::rcReconnect); else rcMoreMenu(); });
        LinearLayout.LayoutParams sp0 = new LinearLayout.LayoutParams(0, dp(56), 1f);
        sp0.setMargins(dp(12), 0, dp(8), 0);
        top.addView(status, sp0);
        LinearLayout more = rcCapBtn(RcIcon.MORE, null, 48, 48, Keycap.hard(density, 100), 0xFFE9D9A6, t("rc_more"));
        rcTap(more, this::rcMoreMenu);
        top.addView(rcWell(more, 28, 4));
        panel.addView(top);
        rcStatus(rcState, 0);

        // six printed keys for the shortcuts, each in its well
        Object[][] tiles = {
            {RcIcon.BOOK, "rc_listen"}, {RcIcon.TV, "rc_channels"}, {RcIcon.APPS, "rc_apps"},
            {RcIcon.MOON, "rc_saver"}, {RcIcon.KEYBOARD, "rc_write"}, {RcIcon.GEAR, "rc_tvset"}};
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int k = 0; k < 3; k++) {
                final int i = r * 3 + k;
                LinearLayout tile = rcCapBtn((Integer) tiles[i][0], t((String) tiles[i][1]), 0, 80, Keycap.tile(density, 12), GOLD, null);
                LinearLayout cell = rcWell(tile, 16, 3);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
                tp.setMargins(dp(4), dp(r == 0 ? 14 : 2), dp(4), dp(2));
                rcTap(tile, () -> {
                    switch (i) {
                        case 0: rcOpen(Remote.Target.LISTEN); break;
                        case 1: rcOpen(Remote.Target.CHANNELS); break;
                        case 2: rcAppsDialog(); break;
                        case 3: rcOpen(Remote.Target.SAVER); break;
                        case 4: rcTextDialog(); break;
                        default: rcRun(() -> flow.shell().run(Remote.settings()));
                    }
                });
                row.addView(cell, tp);
            }
            panel.addView(row);
        }

        // the pad: four wedges around OK
        RcRing ring = new RcRing(this, display);
        ring.setListener(code -> rcKey(code));
        ring.setNames(t("rc_a_up"), t("rc_a_right"), t("rc_a_down"), t("rc_a_left"), t("rc_a_ok"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(12);
        panel.addView(ring, rp);

        // Back, Home, Mute: round keys with their word printed under
        LinearLayout round = new LinearLayout(this);
        round.setOrientation(LinearLayout.HORIZONTAL);
        round.setGravity(Gravity.CENTER);
        LinearLayout back = rcRoundWithWord(RcIcon.BACK, t("rc_back"), 60, () -> rcKey(Remote.BACK));
        LinearLayout home = rcRoundWithWord(RcIcon.HOME, t("rc_home"), 60, () -> rcKey(Remote.HOME));
        LinearLayout mute = rcRoundWithWord(RcIcon.MUTE, t("rc_mute"), 60, this::rcMuteToggle);
        for (LinearLayout col : new LinearLayout[] {back, home, mute}) round.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(round, lp(0, 10, 0, 0));

        // volume: a - key, the slider (it shows the level the TV reports), a + key
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setClipChildren(false);
        LinearLayout vd = rcCapBtn(RcIcon.VOL_DOWN, null, 54, 54, Keycap.hard(density, 100), 0xFFE9D9A6, t("rc_vdown"));
        LinearLayout vu = rcCapBtn(RcIcon.VOL_UP, null, 54, 54, Keycap.hard(density, 100), 0xFFE9D9A6, t("rc_vup"));
        rcSlider = new RcSlider(this, display);
        rcSlider.setWords(t("rc_vol_w"), t("rc_vol_read"), t("rc_vol_release"), t("rc_vol_a11y"));
        rcSlider.setLimit(volLimit());
        rcSlider.setListener(level -> rcVolSet(level));
        rcHoldStep(vd, false);
        rcHoldStep(vu, true);
        bar.addView(vd, new LinearLayout.LayoutParams(dp(54), dp(57)));
        bar.addView(rcSlider, new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(vu, new LinearLayout.LayoutParams(dp(54), dp(57)));
        bar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        LinearLayout volWell = rcWell(bar, 34, 4);
        volWell.setClipChildren(false);
        panel.setClipChildren(false);
        panel.addView(volWell, lp(0, 8, 0, 0));
        rcVolRefresh();

        body.addView(panel, lp(0, 4, 0, 8));

        button.setText(t("cl_home"));
        secondaryStyle(button);
        button.setOnClickListener(v -> show(12));
    }

    /** After the TV answers, each space reads what it needs from it: the install goes straight on, the others first read the apps. */
    private void afterConnect(final TextView button, final TextView status) {
        if (space == SPACE_INSTALL) { busy = false; show(3); return; }
        if (space == SPACE_GET) { busy = false; show(14); return; }
        if (space == SPACE_SEND) { busy = false; show(17); return; }
        if (space == SPACE_REMOTE) { busy = false; show(25); return; }
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
        java.util.Set<String> offNow = Reactivation.disabledNow(flow.shell());
        reactPlan = Reactivation.plan(offNow, ours, cleanupData);
        reactRecheck = r == null || r.packages.isEmpty() ? null : Recheck.compare(r, offNow);
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
            button.setOnClickListener(v -> {
                space = SPACE_INSTALL;
                if (guide != null && guide.path == Guide.Path.EQUIP) { guide.step = 1; saveGuide(); }
                show(3);
            });
        }
    }

    private void reactivate(LinearLayout body, final TextView button) {
        titleBlock(body, t("re_title"), t("re_sub"));
        recheckCard(body);
        final Reactivation.Plan p = reactPlan;
        if (p.total() == 0) {
            LinearLayout none = card(false);
            none.addView(text(t("re_none"), medium, 17, GOLD));
            body.addView(none, lp(0, 10, 0, 0));
            button.setText(t("cl_home"));
            button.setOnClickListener(v -> show(12));
            return;
        }
        sectionLabel(body, t("sec_choose"));
        reactSection(body, button, p.ours, "re_ours_h", "re_ours_d");
        reactSection(body, button, p.others, "re_oth_h", "re_oth_d");
        updateReactButton(button);
    }

    /** «After a restart»: what a cleanup turned off and is still off, and what the TV turned back on by itself (nothing is claimed off that is not). */
    private void recheckCard(LinearLayout body) {
        final Recheck c = reactRecheck;
        if (c == null) return;
        LinearLayout card = card(false);
        card.addView(text(t("re_check_h"), medium, 17, GOLD));
        card.addView(text(t("re_check_off", c.stillOff.size()), regular, 14, SAND), lp(0, 4, 0, 0));
        if (!c.backOn.isEmpty()) {
            card.addView(text(t("re_check_on", c.backOn.size()), medium, 14, GOLD), lp(0, 8, 0, 0));
            for (String p : c.backOn) card.addView(text(cleanupData.label(p, Strings.lang), regular, 13, SAND), lp(8, 2, 0, 0));
            card.addView(text(t("re_check_on_d"), regular, 13, MUTED), lp(0, 6, 0, 0));
        }
        if (!c.suspects.isEmpty()) {
            card.addView(text(t("re_check_sus"), regular, 13, MUTED), lp(0, 8, 0, 0));
            for (String p : c.suspects) card.addView(text(cleanupData.label(p, Strings.lang), medium, 13, SAND), lp(8, 2, 0, 0));
        }
        body.addView(card, lp(0, 10, 0, 6));
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
        ready(button, n != 0);
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
                    reactNeedsRestart = r != null && Recheck.needsRestartAfterEnable(r, back);
                    if (r != null) {   // the restore point forgets what is on again
                        java.util.List<String> left = new ArrayList<>(r.packages);
                        left.removeAll(back);
                        Restore now = new Restore(r.tvModel, r.date, left).withServices(new java.util.HashSet<>(r.servicesBefore));
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
        int asked = reactAsked, done = reactBack.size();
        Outcome.Status st = Outcome.of(asked, done, false);
        java.util.List<String> did = new ArrayList<>();
        for (String p : reactBack) did.add(cleanupData.label(p, Strings.lang));
        java.util.List<String> left = new ArrayList<>();
        if (done < asked) left.add(t("re_fail", asked - done));
        setEnd(st, t(st == Outcome.Status.SUCCESS ? "re_done_t" : st == Outcome.Status.PARTIAL ? "re_part_t" : "re_none_t"), t("re_end_sub", done, asked), did, left, reactNeedsRestart ? t("cl_restart_tv") + "\n" + t("re_end_info") : t("re_end_info"));
        endNextLabel = t("re_more");
        endNext = () -> {
            busy = true;
            bg.execute(() -> {
                try { loadReactivation(); runOnUiThread(() -> { busy = false; show(11); }); }
                catch (final Exception e) { runOnUiThread(() -> { busy = false; Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); }); }
            });
        };
        endScreen(body, button);
    }


    /** At the end of an install, a person who did not clean first is offered to do it now. */
    private boolean cleanNowCard(LinearLayout body, final TextView button) {
        if (cleanedHere) return false;
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x1FD6AF6B, 0xCCD6AF6B));
        c.addView(text(t("cl_notyet_t"), medium, 17, GOLD));
        c.addView(text(t("cl_notyet_d"), regular, 13, SAND), lp(0, 4, 0, 0));
        // the action itself is the bottom button of the screen
        button.setText(t("cl_notyet_btn"));
        ready(button, true);
        button.setOnClickListener(v -> {
            if (busy) return;
            busy = true; button.setAlpha(0.6f);
            bg.execute(() -> {
                try { loadCleanupPlan(); runOnUiThread(() -> { busy = false; cleanFromDone = true; space = SPACE_CLEAN; show(8); }); }
                catch (final Exception e) { runOnUiThread(() -> { busy = false; button.setAlpha(1f); Toast.makeText(this, t("cl_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); }); }
            });
        });
        body.addView(c, lp(0, 14, 0, 0));
        return true;
    }

    // ---------- my settings: named profiles, saved from a TV and sent to another ----------

    private static final int SPACE_GET = 3, SPACE_SEND = 4, SPACE_REMOTE = 5;
    private Profile sendProfile;
    private Preview sendPreview, sentResult;

    private ProfileStore store() { return new ProfileStore(new File(getFilesDir(), "profiles")); }

    private String now() { return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new java.util.Date()); }

    private String defaultProfileName() {
        String day = new java.text.SimpleDateFormat("d MMM yyyy", new Locale(Strings.lang)).format(new java.util.Date());
        return (tvModel.isEmpty() ? "ChouiTV" : tvModel) + " · " + day;
    }

    private TextView pill(String label, boolean filled) {
        TextView p = text(label, medium, 15, filled ? DARK : GOLD);
        p.setGravity(Gravity.CENTER);
        p.setMinHeight(dp(PILL_H));
        p.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(22));
        g.setColor(filled ? GOLD : 0xFF142C1F);
        g.setStroke(dp(2), GOLD);
        p.setBackground(g);
        return p;
    }

    /** The list of saved settings: from «get» it only manages them; from «send» each one can be sent to the connected TV. */
    private void profilesScreen(final LinearLayout body, TextView button, final boolean getMode) {
        titleBlock(body, t(getMode ? "g_title" : "s_title"), t(getMode ? "g_sub" : "s_sub"));
        if (getMode) addSaveCard(body, button); else addTarget(body);
        java.util.List<Profile> every = store().list();
        if (!getMode && guide != null && guide.path == Guide.Path.COPY && !guide.profile.isEmpty()) {
            java.util.List<Profile> mine = new ArrayList<>();
            for (Profile pr : every) if (pr.name.equals(guide.profile)) mine.add(pr);
            if (!mine.isEmpty()) every = mine;
        }
        final java.util.List<Profile> all = every;
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(text(t(getMode ? "st_list_h" : "sec_choose"), medium, 19, GOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(text(String.valueOf(all.size()), medium, 14, SAND));
        body.addView(head, lp(4, 8, 4, 6));
        if (all.isEmpty()) body.addView(text(t(getMode ? "st_empty" : "s_empty"), regular, 14, MUTED), lp(4, 0, 4, 8));
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
            LinearLayout.LayoutParams w = new LinearLayout.LayoutParams(0, dp(PILL_H), 1f);
            w.setMarginEnd(dp(8));
            if (!getMode) row.addView(send, w);
            row.addView(ren, w);
            row.addView(del, new LinearLayout.LayoutParams(0, dp(PILL_H), 1f));
            send.setOnClickListener(v -> startSend(p, send));
            ren.setOnClickListener(v -> askRename(p));
            del.setOnClickListener(v -> askDelete(p));
            c.addView(row);
            body.addView(c, lp(0, 0, 0, 8));
        }
        if (getMode) {
            secondaryBelow(button, t("cl_home"), () -> show(12));
        } else {
            button.setText(t("cl_home"));
            secondaryStyle(button);   // each saved setting has its own «send»: the way back must not compete with them
            button.setOnClickListener(v -> show(12));
        }
    }

    private void addSaveCard(final LinearLayout body, final TextView button) {
        sectionLabel(body, t("sec_save"));
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
        // saving is the point of this screen: it is the bottom button, the name field above it
        button.setText(t("st_save_btn"));
        ready(button, true);
        button.setOnClickListener(v -> {
            if (busy) return;
            final String typed = name.getText().toString();
            final String[] json = new String[1];
            final Profile[] saved = new Profile[1];
            runSteps(t("pg_get_t"), t("pg_sub"), new String[] {t("pg_get_1"), t("pg_get_2")}, new StepWork[] {
                () -> { json[0] = flow.exportSettings(); },
                () -> { saved[0] = store().save(typed, now(), tvModel, json[0]); }
            }, (failed, err) -> {
                if (failed < 0) {
                    if (guide != null && guide.path == Guide.Path.COPY) { guide.step = 1; guide.profile = saved[0].name; saveGuide(); show(21); return; }
                    setEnd(Outcome.Status.SUCCESS, t("g_end_t"), t("ok_sub"), lines(t("g_end_d1", tvModelLine()), t("g_end_d2", saved[0].name)), new ArrayList<String>(), t("st_save_d"));
                    endNextLabel = t("g_end_next"); endNext = () -> show(17);
                    show(24);
                } else {
                    Outcome.Status st = Outcome.of(2, failed, true);
                    showEnd(st, t(st == Outcome.Status.PARTIAL ? "g_part_t" : "g_fail_t"), t("g_part_sub"), failed == 0 ? new ArrayList<String>() : lines(t("pg_get_1")), lines(friendly(err, "", false)), t("g_fail_info"));
                }
            });
        });
        body.addView(save, lp(0, 10, 0, 8));
    }

    /** On «send», the banner says which TV the settings will go to. */
    private void addTarget(LinearLayout body) {
        sectionLabel(body, t("sec_know"));
        infoBar(body, t("st_to", tvModelLine()) + ". " + t("s_choose"));
    }

    private void askRename(final Profile p) {
        final EditText in = new EditText(this);
        in.setText(p.name);
        in.setSingleLine(true);
        in.setSelection(in.getText().length());
        new AlertDialog.Builder(this).setTitle(t("st_rename_h")).setView(in)
            .setPositiveButton(t("st_ok"), (d, w) -> {
                try { store().rename(p.id, in.getText().toString()); } catch (Exception e) { Toast.makeText(this, t("st_err", e.getMessage()), Toast.LENGTH_LONG).show(); }
                show(step);
            }).setNegativeButton(t("cancel"), null).show();
    }

    private void askDelete(final Profile p) {
        new AlertDialog.Builder(this).setTitle(t("st_del_q", p.name))
            .setPositiveButton(t("st_delete"), (d, w) -> { store().delete(p.id); show(step); })
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
        sectionLabel(body, t("sec_know"));
        infoBar(body, t("st_to", tvModelLine()) + (pv.error != null ? "" : ". " + (pv.nothing ? t("st_nothing") : t("st_count", pv.count))));
        if (pv.error != null) {
            LinearLayout e = card(false);
            e.setBackground(cardBg(false, 0x33E8896B, 0xFFE8896B));
            e.addView(text(t("st_err_import"), medium, 15, SAND));
            e.addView(text(pv.error, regular, 12, MUTED), lp(0, 4, 0, 0));
            body.addView(e);
            button.setText(t("st_back"));
            button.setOnClickListener(v -> show(17));
            return;
        }
        if (pv.nothing) {
            button.setText(t("st_back"));
            button.setOnClickListener(v -> show(17));
            return;
        }
        sectionLabel(body, t("sec_change"));
        for (String change : pv.changes) {
            LinearLayout c = card(false);
            c.setPadding(dp(16), dp(10), dp(16), dp(10));
            TextView tv = text(change.replace(" -> ", "  →  "), regular, 14, SAND);
            tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            c.addView(tv);
            body.addView(c, lp(0, 0, 0, 6));
        }
        infoBar(body, t("st_keep"));
        button.setText(t("st_apply", pv.count));
        button.setOnClickListener(v -> {
            if (busy) return;
            final String b64 = Base64.encodeToString(p.settingsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
            final Preview[] applied = new Preview[1], verify = new Preview[1];
            runSteps(t("pg_send_t"), t("pg_sub"), new String[] {t("pg_send_1"), t("pg_send_2"), t("pg_send_3")}, new StepWork[] {
                () -> { applied[0] = Preview.parse(flow.importSettings(b64, true)); if (applied[0].error != null) throw new IOException(applied[0].error); },
                () -> { try { flow.rescheduleAdhan(); } catch (IOException ignored) { } },
                () -> { verify[0] = Preview.parse(flow.importSettings(b64, false)); if (verify[0].error != null) throw new IOException(verify[0].error); }
            }, (failed, err) -> finishSend(p, pv, applied[0], verify[0], failed, err));
        });
    }

    /** The send ends here: what was applied, checked again on the TV, and what is left if the check still finds changes. */
    private void finishSend(Profile p, Preview pv, Preview applied, Preview verify, int failed, Exception err) {
        int asked = pv.count;
        int stillToDo = verify != null && !verify.nothing ? Math.max(verify.count, verify.changes.size()) : 0;
        int done = failed == 0 || applied == null ? 0 : Math.max(0, Math.min(asked, asked - stillToDo));
        boolean problem = failed >= 0 || stillToDo > 0;
        Outcome.Status st = Outcome.of(asked, done, problem);
        java.util.List<String> did = new ArrayList<>(), left = new ArrayList<>();
        if (done > 0 || failed < 0) { did.add(t("st_end_d1", tvModelLine())); did.add(t("st_end_d2", done)); }
        if (failed < 0 && stillToDo == 0) did.add(t("st_end_d3"));
        if (verify != null && !verify.nothing) for (String c : verify.changes) left.add(c.replace(" -> ", "  →  "));
        if (failed >= 0 && err != null) left.add(friendly(err, "", false));
        setEnd(st, t(st == Outcome.Status.SUCCESS ? "st_sent_t" : st == Outcome.Status.PARTIAL ? "st_part_t" : "st_fail_t"), t("st_end_sub", done, asked), did, left, t("st_keep"));
        endNextLabel = t("st_back"); endNext = () -> show(17);
        if (done > 0) { endUndo = () -> undoSend(); endUndoLabel = t("st_undo"); }
        if (st == Outcome.Status.SUCCESS && guide != null && (guide.path == Guide.Path.COPY || guide.path == Guide.Path.SEND)) clearGuide();
        show(24);
    }

    private void undoSend() {
        confirmGrave(t("st_undo_q"), t("st_undo_msg"), t("st_undo_keep"), t("st_undo_do"), () -> {
            busy = true;
            bg.execute(() -> {
                try {
                    flow.undoImport();
                    try { flow.rescheduleAdhan(); } catch (IOException ignored) { }
                    runOnUiThread(() -> { busy = false; showEnd(Outcome.Status.SUCCESS, t("st_undone_t"), t("ok_sub"), lines(t("st_undone")), new ArrayList<String>(), t("st_undone_info")); });
                } catch (final Exception e) {
                    runOnUiThread(() -> { busy = false; Toast.makeText(this, t("st_err", friendly(e, "", false)), Toast.LENGTH_LONG).show(); });
                }
            });
        });
    }


    // ---------- the guide: choose a mode, choose a path, and know where you are ----------

    private Guide guide;

    private void saveGuide() {
        getSharedPreferences("installer", MODE_PRIVATE).edit().putString("guide", guide == null ? "" : guide.toJson()).apply();
    }

    private void clearGuide() { guide = null; saveGuide(); }

    /** The space a guided step lives in. */
    private int spaceFor(Guide g) {
        switch (g.path) {
            case EQUIP: return g.step == 0 ? SPACE_CLEAN : SPACE_INSTALL;
            case COPY: return g.step == 0 ? SPACE_GET : SPACE_SEND;
            case SEND: return SPACE_SEND;
            default: return SPACE_REACT;
        }
    }

    /** Starts or resumes a guided path: each space begins with the connection to the TV, except the step that only asks to change TV. */
    private void goGuided() {
        if (guide.path == Guide.Path.COPY && guide.step == 1) { show(21); return; }
        if (guide.path == Guide.Path.SEND && guide.step == 0) { show(20); return; }
        if (guide.path == Guide.Path.EQUIP && guide.step == 2) { guide.step = 1; saveGuide(); }   // the settings step needs the install's own state: redo from the install
        space = spaceFor(guide);
        show(1);
    }

    private void modeScreen(LinearLayout body, TextView button) {
        titleBlock(body, t("md_title"), t("md_sub"));
        resumeCard(body);
        String[][] cards = {{"guided", "md_g_t", "md_g_d", "md_g_1", "md_g_2", "md_g_tag"}, {"free", "md_f_t", "md_f_d", "md_f_1", "md_f_2", ""}};
        for (final String[] c : cards) {
            boolean g = c[0].equals("guided");
            LinearLayout card = card(g);
            if (!c[5].isEmpty()) card.addView(chip(t(c[5]), GOLD, DARK, 0));
            card.addView(text(t(c[1]), display, 24, SAND), lp(0, 4, 0, 0));
            card.addView(text(t(c[2]), regular, 14, MUTED), lp(0, 2, 0, 8));
            card.addView(text("◆  " + t(c[3]), regular, 14, SAND), lp(0, 2, 0, 0));
            card.addView(text("◆  " + t(c[4]), regular, 14, SAND), lp(0, 2, 0, 0));
            card.setOnClickListener(v -> { if (!busy) show(g ? 19 : 12); });
            body.addView(card, lp(0, 12, 0, 0));
        }
        button.setText(t("hub_lang"));
        secondaryStyle(button);
        button.setOnClickListener(v -> show(0));
    }

    private void pathsScreen(LinearLayout body, TextView button) {
        titleBlock(body, t("pt_title"), t("pt_sub"));
        resumeCard(body);
        String[][] paths = {{"equip", "pt_equip_t", "pt_equip_d", "pt_equip_n"}, {"copy", "pt_copy_t", "pt_copy_d", "pt_copy_n"}, {"send", "pt_send_t", "pt_send_d", "pt_send_n"}, {"react", "pt_react_t", "pt_react_d", "pt_react_n"}};
        for (final String[] p : paths) {
            LinearLayout card = card(false);
            card.addView(text(t(p[1]), display, 22, SAND));
            card.addView(text(t(p[2]), regular, 14, MUTED), lp(0, 2, 0, 6));
            card.addView(text(t(p[3]), medium, 13, GOLD));
            card.setOnClickListener(v -> {
                if (busy) return;
                guide = new Guide(Guide.Path.valueOf(p[0].toUpperCase(java.util.Locale.ROOT)));
                saveGuide();
                goGuided();
            });
            body.addView(card, lp(0, 12, 0, 0));
        }
        button.setText(t("pt_back"));
        secondaryStyle(button);
        button.setOnClickListener(v -> show(18));
    }

    /** A guided path that was left in the middle: one tap to go on, one to give up. */
    private void resumeCard(LinearLayout body) {
        if (guide == null) return;
        LinearLayout c = card(false);
        c.setBackground(cardBg(false, 0x333F7A5C, 0xFF3F7A5C));
        c.addView(text(t("rs_h"), medium, 17, GOLD));
        c.addView(text(t("pt_" + guide.path.key() + "_t"), medium, 16, SAND), lp(0, 2, 0, 0));
        c.addView(text(t("rs_step", guide.step + 1, guide.path.steps), regular, 13, MUTED), lp(0, 2, 0, 8));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView go = pill(t("rs_go"), true), drop = pill(t("rs_drop"), false);
        LinearLayout.LayoutParams w = new LinearLayout.LayoutParams(0, dp(PILL_H), 1f);
        w.setMarginEnd(dp(8));
        row.addView(go, w);
        row.addView(drop, new LinearLayout.LayoutParams(0, dp(PILL_H), 1f));
        go.setOnClickListener(v -> { if (!busy) goGuided(); });
        drop.setOnClickListener(v -> { clearGuide(); show(step); });
        c.addView(row);
        body.addView(c, lp(0, 10, 0, 0));
    }

    /** The bar of steps and the «now» / «next» card, at the top of each screen of a guided path. */
    private void guideBar(LinearLayout body) {
        final Guide g = guide;
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        Guide probe = new Guide(g.path);
        for (int i = 0; i < g.path.steps; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView dot = text(i < g.step ? "✓" : String.valueOf(i + 1), medium, 15, i == g.step ? DARK : SAND);
            dot.setGravity(Gravity.CENTER);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            if (i == g.step) gd.setColor(GOLD);
            else if (i < g.step) gd.setColor(0xFF3F7A5C);
            else { gd.setColor(0); gd.setStroke(dp(2), 0xFF6E7C72); }
            dot.setBackground(gd);
            cell.addView(dot, new LinearLayout.LayoutParams(dp(34), dp(34)));
            probe.step = i;
            TextView lab = text(t(probe.key('l')), medium, 12, i == g.step ? GOLD : i < g.step ? SAND : 0xFF96A098);
            lab.setGravity(Gravity.CENTER);
            cell.addView(lab, lp(0, 4, 0, 0));
            bar.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(bar, lp(0, 4, 0, 8));
        // where you are is something to know, not something to do: the calm bar, not the gold-bordered card
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.addView(chip(t("gl_now"), 0, INFO_ICON, INFO_EDGE));
        c.addView(text(t(g.key('n')), medium, 18, SAND), lp(0, 6, 0, 0));
        c.addView(text(g.profile.isEmpty() ? t(g.key('d')) : t(g.key('d'), g.profile), regular, 14, INFO_TEXT), lp(0, 4, 0, 8));
        c.addView(text(t("gl_next") + "  " + t(g.key('x')), regular, 13, MUTED));
        body.addView(flatBar(c), lp(0, 0, 0, 10));
    }

    /** After the save in the copy path: send now (both TVs are here) or later (the settings stay on the phone). */
    private void nowOrLater(LinearLayout body, TextView button) {
        titleBlock(body, t("cp_h"), t("cp_sub"));
        sectionLabel(body, t("sec_know"));
        infoBar(body, t("cp_saved") + " : " + (guide != null ? guide.profile : ""));
        sectionLabel(body, t("sec_choose"));
        LinearLayout now = card(true);
        now.addView(text(t("cp_now_t"), display, 22, SAND));
        now.addView(text(t("cp_now_d"), regular, 14, MUTED), lp(0, 2, 0, 6));
        now.addView(text("◆  " + t("cp_now_1"), regular, 14, SAND));
        now.addView(text("◆  " + t("cp_now_2"), regular, 14, SAND));
        now.setOnClickListener(v -> show(20));
        body.addView(now, lp(0, 8, 0, 0));
        LinearLayout later = card(false);
        later.addView(text(t("cp_later_t"), display, 22, SAND));
        later.addView(text(t("cp_later_d"), regular, 14, MUTED), lp(0, 2, 0, 6));
        later.addView(text("◆  " + t("cp_later_1"), regular, 14, SAND));
        later.addView(text("◆  " + t("cp_later_2"), regular, 14, SAND));
        later.setOnClickListener(v -> show(22));
        body.addView(later, lp(0, 10, 0, 0));
        button.setText(t("cp_now_t"));
        button.setOnClickListener(v -> show(20));
    }

    /** The copy path ends here when the settings will be sent later: they stay on the phone, and the way back is told. */
    private void kept(LinearLayout body, TextView button) {
        String name = guide != null ? guide.profile : "";
        clearGuide();
        setEnd(Outcome.Status.SUCCESS, t("kp_h"), t("ok_sub"), lines(t("kp_done", name)), new ArrayList<String>(), t("kp_d", name, name));
        endNextLabel = t("st_back_list"); endNext = () -> show(17);
        endScreen(body, button);
    }


    /** The step that only asks to change TV: it says what to do on the other TV, then connects to it. */
    private void switchScreen(LinearLayout body, TextView button) {
        titleBlock(body, t("sw_title"), t("sw_sub"));
        if (guide != null && !guide.profile.isEmpty()) {
            sectionLabel(body, t("sec_know"));
            infoBar(body, t("sw_saved") + " : " + guide.profile);
        }
        actionCard(body, true, new String[] {t("prep2a")}, new String[] {t("prep3a")}, new String[] {t("prep3b")});
        button.setText(t("sw_btn"));
        button.setOnClickListener(v -> {
            if (busy) return;
            if (guide != null && guide.path == Guide.Path.SEND) { guide.step = 1; saveGuide(); }
            if (tv != null) { tv.close(); tv = null; }
            space = SPACE_SEND;
            show(2);
        });
    }

    // ---------- the end of an operation: one screen for all, success or incomplete, and the way home ----------

    private Outcome.Status endStatus = Outcome.Status.SUCCESS;
    private String endTitle = "", endSub = "", endInfo = "", endNextLabel = "", endUndoLabel = "";
    private final java.util.List<String> endDone = new ArrayList<>(), endLeft = new ArrayList<>();
    private Runnable endNext, endUndo;

    private void setEnd(Outcome.Status st, String title, String sub, java.util.List<String> done, java.util.List<String> left, String info) {
        endStatus = st; endTitle = title; endSub = sub; endInfo = info == null ? "" : info;
        endDone.clear(); endDone.addAll(done); endLeft.clear(); endLeft.addAll(left);
        endNext = null; endNextLabel = ""; endUndo = null; endUndoLabel = "";
    }

    private void showEnd(Outcome.Status st, String title, String sub, java.util.List<String> done, java.util.List<String> left, String info) {
        setEnd(st, title, sub, done, left, info);
        show(24);
    }

    private static java.util.List<String> lines(String... s) { return new ArrayList<>(java.util.Arrays.asList(s)); }

    /** A small title over a group of things, so that what informs, what is chosen and what is done never look alike. */
    private void sectionLabel(LinearLayout body, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label.toUpperCase(Strings.rtl() ? java.util.Locale.ROOT : java.util.Locale.getDefault()), medium, 12, 0xFF96A098);
        l.setLetterSpacing(Strings.rtl() ? 0f : 0.08f);
        box.addView(l);
        View rule = new View(this);
        rule.setBackgroundColor(0x28FFFFFF);
        box.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(box, lp(4, 16, 4, 8));
    }

    /** «To know»: a calm flat bar with a 3 dp teal-green edge on the reading side, no border box, never clickable. Content goes inside. */
    private LinearLayout flatBar(View content) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(INFO_FILL);
        fill.setCornerRadius(dp(6));
        bar.setBackground(fill);
        View edge = new View(this);
        edge.setBackgroundColor(INFO_EDGE);
        bar.addView(edge, new LinearLayout.LayoutParams(dp(3), -1));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMargins(dp(12), dp(8), dp(12), dp(8));
        bar.addView(content, cp);
        return bar;
    }

    /** «To know»: the flat bar with a small (i) and a muted text. */
    private void infoBar(LinearLayout body, String message) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView i = text("i", medium, 12, INFO_ICON);
        i.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL); ring.setColor(0); ring.setStroke(dp(1), INFO_ICON);
        i.setBackground(ring);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(20), dp(20));
        ip.topMargin = dp(2);
        row.addView(i, ip);
        TextView m = text(message, regular, 14, INFO_TEXT);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.setMarginStart(dp(10));
        row.addView(m, mp);
        body.addView(flatBar(row), lp(0, 0, 0, 8));
    }

    private int dpf(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** A gold circle with a number: the steps of an action card. */
    private TextView numCircle(int n) {
        TextView c = text(String.valueOf(n), medium, 15, ACTION_PILL_TEXT);
        c.setGravity(Gravity.CENTER);
        c.setTextDirection(View.TEXT_DIRECTION_LTR);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(ACTION_PILL);
        c.setBackground(g);
        return c;
    }

    /**
     * «Your turn»: something the person does. A card with a gold border and a lighter green fill, a gold pill at its top («Your turn» with a hand, and where: on the TV or on the phone),
     * then numbered steps. Each step is {main text} or {main text, detail}.
     */
    private void actionCard(LinearLayout body, boolean onTv, String[]... steps) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(ACTION_FILL);
        g.setStroke(dpf(1.5f), ACTION_BORDER);
        c.setBackground(g);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(10), dp(4), dp(12), dp(4));
        GradientDrawable pg = new GradientDrawable();
        pg.setCornerRadius(dp(14));
        pg.setColor(ACTION_PILL);
        pill.setBackground(pg);
        pill.addView(new Orn.Hand(this, ACTION_PILL_TEXT), new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView pt = text(t("your_turn"), medium, 14, ACTION_PILL_TEXT);
        LinearLayout.LayoutParams ptp = new LinearLayout.LayoutParams(-2, -2);
        ptp.setMarginStart(dp(6));
        pill.addView(pt, ptp);
        head.addView(pill, new LinearLayout.LayoutParams(-2, -2));
        TextView where = text(t(onTv ? "at_tv" : "at_phone"), medium, 14, ACTION_TEXT);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1f);
        wp.setMarginStart(dp(10));
        head.addView(where, wp);
        c.addView(head, new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < steps.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(28), dp(28));
            np.topMargin = dp(2);
            row.addView(numCircle(i + 1), np);
            LinearLayout tt = new LinearLayout(this);
            tt.setOrientation(LinearLayout.VERTICAL);
            tt.addView(text(steps[i][0], medium, 16, ACTION_TEXT));
            if (steps[i].length > 1) tt.addView(text(steps[i][1], regular, 14, ACTION_SUB));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
            tp.setMarginStart(dp(12));
            row.addView(tt, tp);
            c.addView(row, lp(0, 12, 0, 0));
        }
        body.addView(c, lp(0, 4, 0, 12));
    }

    /** The main button turned into a quiet ghost (a way back or out that must not compete with the choices above it). */
    private void secondaryStyle(TextView b) {
        b.setAlpha(1f);
        b.setTextColor(LINK);
        b.setElevation(0);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(30));
        g.setColor(0);
        g.setStroke(dp(2), 0x99D6AF6B);
        b.setBackground(g);
        if (b instanceof Btn) ((Btn) b).arrow(false);
    }

    /** A quiet text link (at least 48 dp tall) placed in the bottom area, right under the main button. */
    private void secondaryBelow(TextView button, String label, final Runnable run) {
        ViewGroup col = (ViewGroup) button.getParent();
        TextView g = text(label, medium, 16, LINK);
        g.setGravity(Gravity.CENTER);
        g.setPaintFlags(g.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        g.setClickable(true);
        g.setOnClickListener(v -> { if (!busy) run.run(); });
        col.addView(g, col.indexOfChild(button) + 1, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void endScreen(LinearLayout body, TextView button) {
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        final int color = endStatus == Outcome.Status.SUCCESS ? GOLD : endStatus == Outcome.Status.PARTIAL ? 0xFFF0BE5A : 0xFFE8896B;
        TextView mark = text(endStatus == Outcome.Status.SUCCESS ? "✓" : endStatus == Outcome.Status.PARTIAL ? "!" : "✕", medium, 44, color);
        mark.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL); ring.setColor(0xFF142C1F); ring.setStroke(dp(4), color);
        mark.setBackground(ring);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(104), dp(104));
        mp.gravity = Gravity.CENTER_HORIZONTAL; mp.topMargin = dp(12);
        body.addView(mark, mp);
        TextView title = text(endTitle, display, 27, SAND);
        title.setGravity(Gravity.CENTER);
        body.addView(title, lp(0, 14, 0, 4));
        TextView sub = text(endSub, medium, 16, color);
        sub.setGravity(Gravity.CENTER);
        body.addView(sub, lp(0, 0, 0, 6));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.START);
        body.addView(list, new LinearLayout.LayoutParams(-1, -2));
        // what was done, and what is left: the two are never mixed
        if (!endDone.isEmpty()) {
            sectionLabel(list, t("sec_done"));
            for (String s : endDone) {
                TextView r = text("◆  " + s, regular, 15, SAND);
                list.addView(r, lp(4, 0, 4, 6));
            }
        }
        if (!endLeft.isEmpty()) {
            sectionLabel(list, t("sec_left"));
            for (String s : endLeft) {
                TextView r = text("▲  " + s, regular, 15, 0xFFF0BE5A);
                list.addView(r, lp(4, 0, 4, 6));
            }
        }
        if (!endInfo.isEmpty()) {
            sectionLabel(list, t("sec_know"));
            infoBar(list, endInfo);
        }
        if (endUndo != null) {
            TextView u = dangerLink(endUndoLabel);
            final Runnable run = endUndo;
            u.setOnClickListener(v -> { if (!busy) run.run(); });
            list.addView(u, new LinearLayout.LayoutParams(-1, -2));
        }
        if (endNext != null) {
            // the next step is THE bottom button; the way back to the menu is a quiet link just under it
            final Runnable run = endNext;
            button.setText(endNextLabel);
            ready(button, true);
            button.setOnClickListener(v -> { if (!busy) run.run(); });
            secondaryBelow(button, t("cl_home"), () -> show(18));
        } else {
            button.setText(t("cl_home"));
            ready(button, true);
            button.setOnClickListener(v -> { if (!busy) show(18); });
        }
    }

    // ---------- a run of steps, shown with a bar, a percentage and the state of each step ----------

    private interface StepWork { void run() throws Exception; }
    private interface StepsDone { void done(int failed, Exception error); }
    private String progTitle = "", progSub = "";
    private String[] progLabels = new String[0];
    private StepWork[] progWork = new StepWork[0];
    private StepsDone progDone;

    private void runSteps(String title, String sub, String[] labels, StepWork[] work, StepsDone done) {
        progTitle = title; progSub = sub; progLabels = labels; progWork = work; progDone = done;
        show(23);
    }

    private void progressScreen(LinearLayout body, final TextView button) {
        titleBlock(body, progTitle, progSub);
        final int n = progLabels.length;
        final TextView pctV = text("0%", display, 58, SAND);
        pctV.setGravity(Gravity.CENTER);
        pctV.setTextDirection(View.TEXT_DIRECTION_LTR);
        body.addView(pctV, lp(0, 4, 0, 4));
        final Orn.Bar bar = new Orn.Bar(this);
        body.addView(bar, new LinearLayout.LayoutParams(-1, dp(32)));
        sectionLabel(body, t("sec_steps"));
        final TextView[] marks = new TextView[n];
        final LinearLayout[] cards = new LinearLayout[n];
        for (int i = 0; i < n; i++) {
            LinearLayout c = card(false);
            c.setOrientation(LinearLayout.HORIZONTAL);
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.setClickable(false);
            TextView m = text("", medium, 15, SAND);
            m.setGravity(Gravity.CENTER);
            c.addView(m, new LinearLayout.LayoutParams(dp(30), dp(30)));
            TextView l = text(progLabels[i], regular, 16, SAND);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1f);
            lp2.setMarginStart(dp(12));
            c.addView(l, lp2);
            marks[i] = m; cards[i] = c;
            mark(m, c, "wait");
            body.addView(c, lp(0, 0, 0, 8));
        }
        sectionLabel(body, t("sec_know"));
        infoBar(body, t("pg_info"));
        button.setText(t("pg_running"));
        ready(button, false);
        button.setOnClickListener(null);
        busy = true;
        final StepWork[] work = progWork;
        final StepsDone done = progDone;
        bg.execute(() -> {
            int failed = -1;
            Exception error = null;
            for (int i = 0; i < n; i++) {
                final int k = i;
                runOnUiThread(() -> mark(marks[k], cards[k], "run"));
                try {
                    work[i].run();
                } catch (Exception e) {
                    failed = i; error = e;
                    runOnUiThread(() -> mark(marks[k], cards[k], "fail"));
                    break;
                }
                final int pct = Outcome.percent(i + 1, n);
                runOnUiThread(() -> { mark(marks[k], cards[k], "done"); pctV.setText(pct + "%"); bar.set(pct / 100f); });
            }
            try { Thread.sleep(600); } catch (InterruptedException ignored) { }
            final int f = failed; final Exception err = error;
            runOnUiThread(() -> { busy = false; done.done(f, err); });
        });
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
        if (e instanceof Flow.AlreadyNewer) return t("err_newer");
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

    /** The main button of a screen: gold, raised, with a chevron on the reading side when it is ready to go on. */
    private final class Btn extends TextView {
        boolean arrow = true;
        CharSequence raw = "";
        Btn(Context c) { super(c); }
        @Override public void setText(CharSequence t, BufferType type) {
            raw = t == null ? "" : t;
            super.setText(arrow && raw.length() > 0 ? raw + "  " + (Strings.rtl() ? "\u2039" : "\u203A") : raw, type);
        }
        void arrow(boolean on) { arrow = on; setText(raw); }
    }

    private TextView button(String label) {
        Btn b = new Btn(this);
        b.setTypeface(medium);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        b.setTextColor(DARK);
        b.setGravity(Gravity.CENTER);
        b.setText(label);
        ready(b, true);
        b.setElevation(dp(8));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(BTN_H)));
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
