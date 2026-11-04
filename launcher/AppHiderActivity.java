package com.spyvault.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SPY VAULT Home: a self-contained Android HOME activity and private app drawer.
 * Hidden apps are filtered only inside this launcher; installed packages are never disabled.
 */
public class AppHiderActivity extends Activity {
    private static final String PREFS = "spy_vault_launcher";
    private static final String KEY_HIDDEN = "hidden_packages";

    private static final int BG_TOP = Color.rgb(10, 14, 28);
    private static final int BG_BOTTOM = Color.rgb(21, 26, 46);
    private static final int SURFACE = Color.rgb(27, 34, 56);
    private static final int SURFACE_2 = Color.rgb(35, 43, 68);
    private static final int ACCENT = Color.rgb(137, 104, 255);
    private static final int ACCENT_2 = Color.rgb(78, 190, 210);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(164, 174, 198);
    private static final int GREEN = Color.rgb(64, 190, 143);
    private static final int RED = Color.rgb(232, 105, 125);

    private final List<ResolveInfo> apps = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private PackageManager packageManager;
    private SharedPreferences preferences;
    private LinearLayout root;
    private EditText homeSearch;
    private EditText drawerSearch;
    private boolean drawerOpen = false;
    private boolean showingHidden = false;
    private boolean ready = false;
    private String searchText = "";

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        packageManager = getPackageManager();
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> saved = preferences.getStringSet(KEY_HIDDEN, Collections.<String>emptySet());
        if (saved != null) hidden.addAll(saved);
        loadApps();
        ready = true;
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ready) {
            loadApps();
            if (drawerOpen) showDrawer(); else showHome();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        drawerOpen = false;
        searchText = "";
        loadApps();
        showHome();
    }

    private void loadApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY);
        LinkedHashMap<String, ResolveInfo> unique = new LinkedHashMap<>();
        for (ResolveInfo info : found) {
            if (info == null || info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName())) continue;
            if (!unique.containsKey(pkg)) unique.put(pkg, info);
        }
        apps.addAll(unique.values());
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                String an = String.valueOf(a.loadLabel(packageManager));
                String bn = String.valueOf(b.loadLabel(packageManager));
                return an.compareToIgnoreCase(bn);
            }
        });
    }

    private void saveHidden() {
        preferences.edit().putStringSet(KEY_HIDDEN, new HashSet<>(hidden)).apply();
    }

    private GradientDrawable shape(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable gradient(int first, int second, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{first, second});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView centered(String value, float size, int color, boolean bold) {
        TextView t = text(value, size, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private void baseScreen() {
        root = vertical();
        root.setClipToPadding(false);
        root.setPadding(dp(18), dp(16), dp(18), dp(12));
        root.setBackground(gradient(BG_TOP, BG_BOTTOM, 0));
        setContentView(root);
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.argb(35, 210, 220, 255));
        return v;
    }

    private void addGap(LinearLayout target, int height) {
        View gap = new View(this);
        target.addView(gap, lp(1, height));
    }

    private void showHome() {
        drawerOpen = false;
        baseScreen();

        LinearLayout top = horizontal();
        LinearLayout brand = vertical();
        brand.addView(text("SPY VAULT", 19, TEXT, true));
        TextView caption = text("YOUR PRIVATE HOME", 9, MUTED, true);
        caption.setLetterSpacing(0.14f);
        brand.addView(caption);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = centered("SV", 14, TEXT, true);
        badge.setBackground(gradient(ACCENT, ACCENT_2, 16));
        top.addView(badge, lp(42, 42));
        root.addView(top);

        addGap(root, 16);
        LinearLayout hero = vertical();
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        hero.setBackground(gradient(Color.rgb(48, 54, 91), Color.rgb(30, 58, 83), 22));
        String time = new SimpleDateFormat("h:mm", Locale.getDefault()).format(new java.util.Date());
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new java.util.Date());
        hero.addView(text(time, 39, TEXT, true));
        TextView dateText = text(date, 12, Color.rgb(213, 222, 246), false);
        dateText.setPadding(0, dp(1), 0, dp(8));
        hero.addView(dateText);
        LinearLayout meta = horizontal();
        meta.addView(text(visibleApps().size() + " apps available", 11, Color.rgb(218, 226, 247), false),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView hiddenCount = centered(hidden.size() + " hidden", 10, TEXT, true);
        hiddenCount.setPadding(dp(10), dp(6), dp(10), dp(6));
        hiddenCount.setBackground(shape(Color.argb(55, 255, 255, 255), 20));
        meta.addView(hiddenCount);
        hero.addView(meta);
        LinearLayout.LayoutParams heroP = lp(-1, -2);
        root.addView(hero, heroP);

        addGap(root, 14);
        homeSearch = searchField("Search apps on your phone");
        root.addView(homeSearch, lp(-1, 50));
        homeSearch.setText(searchText);
        homeSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                updateHomeGrid();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        addGap(root, 15);
        LinearLayout heading = horizontal();
        heading.addView(text("YOUR APPS", 12, MUTED, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = text("SEE ALL  ›", 10, Color.rgb(189, 174, 255), true);
        all.setOnClickListener(v -> openDrawer(false));
        heading.addView(all);
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);
        scroll.addView(grid);
        LinearLayout.LayoutParams scrollP = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollP.topMargin = dp(10);
        root.addView(scroll, scrollP);
        populateHomeGrid(grid, searchText);

        addGap(root, 10);
        LinearLayout nav = horizontal();
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        nav.setBackground(shape(SURFACE, 20));
        nav.addView(navItem("⌂", "HOME", true, null), new LinearLayout.LayoutParams(0, dp(54), 1));
        nav.addView(navItem("▦", "ALL APPS", false, () -> openDrawer(false)),
                new LinearLayout.LayoutParams(0, dp(54), 1));
        nav.addView(navItem("◈", "HIDDEN", false, () -> openDrawer(true)),
                new LinearLayout.LayoutParams(0, dp(54), 1));
        nav.addView(navItem("⚙", "DEFAULT", false, this::openHomeSettings),
                new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(nav);
    }

    private TextView navItem(String icon, String label, boolean active, Runnable click) {
        TextView item = centered(icon + "\n" + label, 10, active ? Color.rgb(197, 180, 255) : MUTED, true);
        item.setLineSpacing(dp(2), 1f);
        item.setOnClickListener(v -> { if (click != null) click.run(); });
        return item;
    }

    private EditText searchField(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("⌕   " + hint);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(shape(SURFACE_2, 16));
        input.setCompoundDrawablePadding(dp(8));
        return input;
    }

    private List<ResolveInfo> visibleApps() {
        List<ResolveInfo> list = new ArrayList<>();
        for (ResolveInfo info : apps) {
            if (!hidden.contains(info.activityInfo.packageName)) list.add(info);
        }
        return list;
    }

    private void populateHomeGrid(GridLayout grid, String filter) {
        grid.removeAllViews();
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (hidden.contains(pkg)) continue;
            String name = String.valueOf(info.loadLabel(packageManager));
            if (!name.toLowerCase(Locale.ROOT).contains(q) && !pkg.toLowerCase(Locale.ROOT).contains(q)) continue;
            GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
            tileParams.width = 0;
            tileParams.height = dp(105);
            tileParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            tileParams.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(appTile(info, name), tileParams);
            count++;
        }
        if (count == 0) {
            TextView empty = centered(q.isEmpty() ? "No launchable apps found" : "No apps match your search",
                    13, MUTED, false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = -1;
            p.height = dp(105);
            p.columnSpec = GridLayout.spec(0, 4);
            grid.addView(empty, p);
        }
    }

    private void updateHomeGrid() {
        if (root == null) return;
        // Rebuild only the app grid, leaving the search field and keyboard untouched.
        ScrollView scroll = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ScrollView) { scroll = (ScrollView) child; break; }
        }
        if (scroll != null && scroll.getChildAt(0) instanceof GridLayout) {
            populateHomeGrid((GridLayout) scroll.getChildAt(0), searchText);
        }
    }

    private View appTile(ResolveInfo info, String name) {
        LinearLayout tile = vertical();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(9), dp(3), dp(8));
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.loadIcon(packageManager)); }
        catch (Exception ignored) { icon.setImageDrawable(null); }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout iconFrame = new FrameLayout(this);
        iconFrame.setBackground(gradient(tileColor(info.activityInfo.packageName),
                Color.rgb(50, 61, 93), 18));
        iconFrame.setPadding(dp(11), dp(11), dp(11), dp(11));
        iconFrame.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        tile.addView(iconFrame, lp(58, 58));
        TextView label = centered(name, 10, TEXT, false);
        label.setMaxLines(2);
        label.setMinHeight(dp(30));
        tile.addView(label, lp(-1, -2));
        tile.setOnClickListener(v -> launchApp(info));
        tile.setOnLongClickListener(v -> {
            showAppActions(info, name);
            return true;
        });
        return tile;
    }

    private int tileColor(String pkg) {
        int[] colors = {
            Color.rgb(71, 82, 151), Color.rgb(43, 123, 133),
            Color.rgb(126, 75, 141), Color.rgb(125, 90, 55),
            Color.rgb(57, 105, 142), Color.rgb(105, 78, 127)
        };
        return colors[Math.abs(pkg.hashCode() % colors.length)];
    }

    private void openDrawer(boolean hiddenOnly) {
        drawerOpen = true;
        showingHidden = hiddenOnly;
        searchText = "";
        showDrawer();
    }

    private void showDrawer() {
        drawerOpen = true;
        baseScreen();
        LinearLayout header = horizontal();
        TextView back = centered("‹", 28, TEXT, true);
        back.setBackground(shape(SURFACE, 15));
        header.addView(back, lp(44, 44));
        back.setOnClickListener(v -> { drawerOpen = false; searchText = ""; showHome(); });
        LinearLayout title = vertical();
        title.setPadding(dp(11), 0, 0, 0);
        title.addView(text("APP LIBRARY", 17, TEXT, true));
        title.addView(text(showingHidden ? "Manage hidden apps" : "All installed launchable apps", 10, MUTED, false));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = centered(String.valueOf(showingHidden ? hidden.size() : visibleApps().size()), 12, TEXT, true);
        count.setBackground(shape(SURFACE_2, 14));
        header.addView(count, lp(42, 38));
        root.addView(header);

        addGap(root, 14);
        LinearLayout tabs = horizontal();
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(shape(SURFACE, 16));
        TextView allTab = centered("ALL APPS", 11, showingHidden ? MUTED : TEXT, true);
        TextView hiddenTab = centered("HIDDEN (" + hidden.size() + ")", 11, showingHidden ? TEXT : MUTED, true);
        allTab.setBackground(showingHidden ? null : shape(Color.rgb(66, 59, 105), 12));
        hiddenTab.setBackground(showingHidden ? shape(Color.rgb(66, 59, 105), 12) : null);
        tabs.addView(allTab, new LinearLayout.LayoutParams(0, dp(42), 1));
        tabs.addView(hiddenTab, new LinearLayout.LayoutParams(0, dp(42), 1));
        allTab.setOnClickListener(v -> { showingHidden = false; showDrawer(); });
        hiddenTab.setOnClickListener(v -> { showingHidden = true; showDrawer(); });
        root.addView(tabs);

        addGap(root, 10);
        drawerSearch = searchField("Search apps");
        root.addView(drawerSearch, lp(-1, 48));
        drawerSearch.setText(searchText);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = vertical();
        scroll.addView(list);
        LinearLayout.LayoutParams scrollP = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollP.topMargin = dp(9);
        root.addView(scroll, scrollP);
        populateDrawer(list, searchText);

        drawerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                populateDrawer(list, searchText);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void populateDrawer(LinearLayout list, String filter) {
        list.removeAllViews();
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            boolean isHidden = hidden.contains(pkg);
            if (isHidden != showingHidden) continue;
            String name = String.valueOf(info.loadLabel(packageManager));
            if (!name.toLowerCase(Locale.ROOT).contains(q) && !pkg.toLowerCase(Locale.ROOT).contains(q)) continue;
            list.addView(drawerRow(info, name, isHidden));
            count++;
        }
        if (count == 0) {
            TextView empty = centered(showingHidden ? "No hidden apps" : "No apps found", 13, MUTED, false);
            empty.setPadding(0, dp(36), 0, dp(36));
            list.addView(empty, lp(-1, -2));
        }
    }

    private View drawerRow(ResolveInfo info, String name, boolean isHidden) {
        LinearLayout row = horizontal();
        row.setPadding(dp(11), dp(9), dp(10), dp(9));
        row.setBackground(shape(SURFACE, 16));
        LinearLayout.LayoutParams rowP = lp(-1, 68);
        rowP.bottomMargin = dp(7);
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.loadIcon(packageManager)); } catch (Exception ignored) {}
        row.addView(icon, lp(42, 42));
        LinearLayout detail = vertical();
        detail.setPadding(dp(11), 0, dp(5), 0);
        detail.addView(text(name, 13, TEXT, true));
        TextView pkg = text(info.activityInfo.packageName, 9, MUTED, false);
        pkg.setMaxLines(1);
        pkg.setSingleLine(true);
        detail.addView(pkg);
        row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
        TextView action = centered(isHidden ? "RESTORE" : "HIDE", 9, TEXT, true);
        action.setPadding(dp(9), 0, dp(9), 0);
        action.setBackground(shape(isHidden ? GREEN : ACCENT, 12));
        row.addView(action, lp(-2, 36));
        action.setOnClickListener(v -> toggleHidden(info, isHidden));
        row.setOnClickListener(v -> { if (!isHidden) launchApp(info); });
        row.setOnLongClickListener(v -> {
            showAppActions(info, name);
            return true;
        });
        return row;
    }

    private void toggleHidden(ResolveInfo info, boolean currentlyHidden) {
        String pkg = info.activityInfo.packageName;
        if (currentlyHidden) hidden.remove(pkg); else hidden.add(pkg);
        saveHidden();
        Toast.makeText(this, currentlyHidden ? "App restored in SPY VAULT" :
                "App hidden from SPY VAULT Home", Toast.LENGTH_SHORT).show();
        if (drawerOpen) showDrawer(); else showHome();
    }

    private void showAppActions(ResolveInfo info, String name) {
        String pkg = info.activityInfo.packageName;
        boolean isHidden = hidden.contains(pkg);
        String[] options = isHidden
                ? new String[]{"Restore in SPY VAULT", "Open app info"}
                : new String[]{"Open app", "Hide in SPY VAULT", "Open app info"};
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(options, (dialog, which) -> {
                    if (isHidden) {
                        if (which == 0) toggleHidden(info, true); else openAppInfo(pkg);
                    } else {
                        if (which == 0) launchApp(info);
                        else if (which == 1) toggleHidden(info, false);
                        else openAppInfo(pkg);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void launchApp(ResolveInfo info) {
        try {
            Intent launch = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName);
            if (launch == null) {
                launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "This app could not be opened", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo(String pkg) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "App information is unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) {
                Toast.makeText(this, "Open Settings to change your Home app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            drawerOpen = false;
            searchText = "";
            showHome();
            return;
        }
        // A Home app should remain a usable Home surface on Back, not exit to a blank screen.
    }
}
