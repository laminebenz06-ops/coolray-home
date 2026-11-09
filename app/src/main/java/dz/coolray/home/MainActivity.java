package dz.coolray.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity implements LocationListener {

    /** Applis épinglées par défaut si elles sont installées. */
    private static final String[] DEFAULT_PINS = {
            "com.google.android.apps.maps",
            "app.organicmaps",
            "net.osmand",
            "net.osmand.plus",
            "com.spotify.music",
            "com.google.android.youtube",
            "com.android.chrome"
    };
    private static final int MAX_PINS = 6;
    private static final int COLUMNS = 7;

    private PackageManager pm;
    private SharedPreferences prefs;
    private LocationManager lm;

    private FrameLayout root;
    private HomeView home;
    private LinearLayout dock;
    private ScrollView drawer;
    private LinearLayout drawerContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        prefs = getSharedPreferences("coolray_home", MODE_PRIVATE);
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        home = new HomeView(this);
        root.addView(home, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x55FFFFFF);
        bg.setCornerRadius(dp(28));
        dock.setBackground(bg);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(104), Gravity.BOTTOM);
        dlp.setMargins(dp(24), 0, dp(24), dp(14));
        root.addView(dock, dlp);

        drawer = new ScrollView(this);
        drawer.setBackgroundColor(0xF0140A3C);
        drawer.setVisibility(View.GONE);
        drawerContent = new LinearLayout(this);
        drawerContent.setOrientation(LinearLayout.VERTICAL);
        drawerContent.setPadding(dp(24), dp(16), dp(24), dp(24));
        drawer.addView(drawerContent, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(drawer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        if (Build.VERSION.SDK_INT >= 23
                && checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDock();
        startGps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            lm.removeUpdates(this);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        hideDrawer();
    }

    @Override
    public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) {
            hideDrawer();
        }
        // Lanceur : on ne quitte pas l'écran d'accueil.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startGps();
    }

    // ---------------------------------------------------------------- GPS

    private void startGps() {
        if (checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            if (lm.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onLocationChanged(Location loc) {
        float kmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
        if (kmh < 2f) {
            kmh = 0f;
        }
        home.setGps(kmh,
                loc.hasBearing() ? loc.getBearing() : -1f,
                loc.hasAltitude() ? loc.getAltitude() : Double.NaN,
                true);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        home.setGps(0f, -1f, Double.NaN, false);
    }

    // --------------------------------------------------------------- Dock

    private List<String> getPinned() {
        String s = prefs.getString("pins", null);
        List<String> pins = new ArrayList<String>();
        if (s == null) {
            for (String pkg : DEFAULT_PINS) {
                if (pm.getLaunchIntentForPackage(pkg) != null && pins.size() < MAX_PINS) {
                    pins.add(pkg);
                }
            }
            savePinned(pins);
        } else if (s.length() > 0) {
            Collections.addAll(pins, s.split(","));
        }
        return pins;
    }

    private void savePinned(List<String> pins) {
        prefs.edit().putString("pins", TextUtils.join(",", pins)).apply();
    }

    private void refreshDock() {
        dock.removeAllViews();
        for (final String pkg : getPinned()) {
            final Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) {
                continue;
            }
            Drawable icon;
            CharSequence label;
            try {
                icon = pm.getApplicationIcon(pkg);
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            View t = tile(icon, label, 56, 0xFFFFFFFF);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(launch);
                }
            });
            dock.addView(t, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        View all = tile(new GridIcon(), "Toutes les apps", 56, 0xFFFFFFFF);
        all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDrawer();
            }
        });
        dock.addView(all, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ------------------------------------------------------------ Tiroir

    private void showDrawer() {
        drawerContent.removeAllViews();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Applications  (appui long = épingler / retirer du dock)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(this);
        close.setText("Fermer");
        close.setTextColor(Color.WHITE);
        close.setTextSize(18);
        close.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable cb = new GradientDrawable();
        cb.setColor(0x44FFFFFF);
        cb.setCornerRadius(dp(18));
        close.setBackground(cb);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDrawer();
            }
        });
        header.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawerContent.addView(header);

        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        final PackageManager pmf = pm;
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pmf).toString().compareToIgnoreCase(b.loadLabel(pmf).toString());
            }
        });

        LinearLayout row = null;
        int n = 0;
        for (final ResolveInfo ri : apps) {
            final String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) {
                continue;
            }
            if (row == null || n % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.topMargin = dp(18);
                drawerContent.addView(row, rlp);
            }
            View t = tile(ri.loadIcon(pm), ri.loadLabel(pm), 64, 0xFFFFFFFF);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launch = pm.getLaunchIntentForPackage(pkg);
                    if (launch != null) {
                        hideDrawer();
                        startActivity(launch);
                    }
                }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    togglePin(pkg);
                    return true;
                }
            });
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            n++;
        }
        // Complète la dernière ligne pour garder des colonnes alignées.
        if (row != null) {
            while (n % COLUMNS != 0) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
                n++;
            }
        }

        drawer.scrollTo(0, 0);
        drawer.setVisibility(View.VISIBLE);
    }

    private void hideDrawer() {
        drawer.setVisibility(View.GONE);
    }

    private void togglePin(String pkg) {
        List<String> pins = getPinned();
        if (pins.contains(pkg)) {
            pins.remove(pkg);
            Toast.makeText(this, "Retiré du dock", Toast.LENGTH_SHORT).show();
        } else if (pins.size() >= MAX_PINS) {
            Toast.makeText(this, "Dock plein (" + MAX_PINS + " max)", Toast.LENGTH_SHORT).show();
            return;
        } else {
            pins.add(pkg);
            Toast.makeText(this, "Épinglé au dock", Toast.LENGTH_SHORT).show();
        }
        savePinned(pins);
        refreshDock();
    }

    // ------------------------------------------------------------ Outils

    private View tile(Drawable icon, CharSequence label, int sizeDp, int textColor) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setClickable(true);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        t.addView(iv, new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(textColor);
        tv.setTextSize(13);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(4);
        t.addView(tv, tlp);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Icône « toutes les apps » : grille de 3x3 points. */
    private static class GridIcon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GridIcon() {
            paint.setColor(0xFFFFFFFF);
        }

        @Override
        public void draw(Canvas c) {
            float w = getBounds().width();
            float h = getBounds().height();
            float r = Math.min(w, h) * 0.07f;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    c.drawCircle(getBounds().left + w * (0.25f + 0.25f * i),
                            getBounds().top + h * (0.25f + 0.25f * j), r, paint);
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
