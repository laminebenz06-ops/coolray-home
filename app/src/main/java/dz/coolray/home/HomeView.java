package dz.coolray.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/** Écran d'accueil dessiné : ciel violet, SUV stylisé, compteur GPS, heure. */
public class HomeView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float[] stars = new float[60 * 3];
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.FRANCE);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE d MMMM", Locale.FRANCE);
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    private float speedKmh = 0f;
    private float heading = -1f;
    private double altitude = Double.NaN;
    private boolean fix = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(this, 1000);
        }
    };

    public HomeView(Context c) {
        super(c);
        Random r = new Random(7);
        for (int i = 0; i < 60; i++) {
            stars[i * 3] = r.nextFloat();
            stars[i * 3 + 1] = r.nextFloat() * 0.55f;
            stars[i * 3 + 2] = 0.6f + r.nextFloat() * 1.6f;
        }
    }

    public void setGps(float speedKmh, float heading, double altitude, boolean fix) {
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.altitude = altitude;
        this.fix = fix;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();

        drawBackground(canvas, w, h);
        drawCar(canvas, w, h);
        drawPanel(canvas, w, h);
        drawClock(canvas, w, h);
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF140A3C, 0xFF4A22B8, 0xFF7A44E8, 0xFF2A1470},
                new float[]{0f, 0.45f, 0.70f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);
        p.setShader(null);

        for (int i = 0; i < 60; i++) {
            p.setColor(0xFFFFFFFF);
            p.setAlpha(110);
            canvas.drawCircle(stars[i * 3] * w, stars[i * 3 + 1] * h, stars[i * 3 + 2], p);
        }

        // Collines au loin
        path.reset();
        path.moveTo(0, h * 0.62f);
        path.quadTo(w * 0.25f, h * 0.52f, w * 0.5f, h * 0.60f);
        path.quadTo(w * 0.75f, h * 0.68f, w, h * 0.56f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        p.setColor(0x992A1470);
        canvas.drawPath(path, p);
    }

    private void drawCar(Canvas canvas, float w, float h) {
        float carW = w * 0.46f;
        float s = carW / 100f;
        float left = w * 0.5f - carW / 2f;
        float top = h * 0.40f;

        // Lueur au sol
        float gx = w * 0.5f;
        float gy = top + 35f * s;
        p.setShader(new RadialGradient(gx, gy, carW * 0.65f,
                new int[]{0xAA9B7BFF, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.scale(1f, 0.18f, gx, gy);
        canvas.drawCircle(gx, gy, carW * 0.65f, p);
        canvas.restore();
        p.setShader(null);

        canvas.save();
        canvas.translate(left, top);
        canvas.scale(s, s);

        // Carrosserie
        path.reset();
        path.moveTo(3, 28);
        path.lineTo(3, 20);
        path.quadTo(3, 17, 8, 16);
        path.lineTo(26, 13);
        path.lineTo(36, 5);
        path.quadTo(38, 3, 42, 3);
        path.lineTo(70, 3);
        path.quadTo(76, 3, 80, 8);
        path.lineTo(92, 15);
        path.quadTo(97, 17, 97, 21);
        path.lineTo(97, 28);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 3, 0, 30,
                new int[]{0xFFB9A6FF, 0xFF4B3FB0, 0xFF1B1450}, new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(path, p);
        p.setShader(null);

        // Vitres
        p.setColor(0xFF120C3A);
        path.reset();
        path.moveTo(38, 13);
        path.lineTo(43, 5.5f);
        path.lineTo(58, 5.5f);
        path.lineTo(58, 13);
        path.close();
        canvas.drawPath(path, p);
        path.reset();
        path.moveTo(61, 13);
        path.lineTo(61, 5.5f);
        path.lineTo(70, 5.5f);
        path.quadTo(74, 6, 78, 13);
        path.close();
        canvas.drawPath(path, p);

        // Phare avant (gauche) et feu arrière (droite)
        p.setColor(0xFFFFFFFF);
        rect.set(3.5f, 18.5f, 10f, 20.5f);
        canvas.drawRoundRect(rect, 1f, 1f, p);
        p.setColor(0xFFFF2D55);
        rect.set(93f, 18f, 97f, 21f);
        canvas.drawRoundRect(rect, 1f, 1f, p);

        // Ligne de caisse
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.4f);
        p.setColor(0x66FFFFFF);
        canvas.drawLine(10f, 21f, 94f, 21f, p);

        // Roues
        p.setStyle(Paint.Style.FILL);
        float[] wx = {24f, 76f};
        for (float x : wx) {
            p.setColor(0xFF08051F);
            canvas.drawCircle(x, 28f, 7.4f, p);
            p.setColor(0xFF6E62C9);
            canvas.drawCircle(x, 28f, 4.4f, p);
            p.setColor(0xFF08051F);
            canvas.drawCircle(x, 28f, 1.6f, p);
        }
        canvas.restore();
    }

    private void drawPanel(Canvas canvas, float w, float h) {
        float x0 = w * 0.035f, y0 = h * 0.14f, x1 = w * 0.36f, y1 = h * 0.46f;
        rect.set(x0, y0, x1, y1);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x33FFFFFF);
        canvas.drawRoundRect(rect, h * 0.03f, h * 0.03f, p);

        float ph = y1 - y0;
        float cx = x0 + ph * 0.55f;
        float cy = (y0 + y1) / 2f;
        float r = ph * 0.36f;

        // Anneau de vitesse (0 - 200 km/h sur 270°)
        float sw = ph * 0.07f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(sw);
        rect.set(cx - r, cy - r, cx + r, cy + r);
        p.setColor(0x33FFFFFF);
        canvas.drawArc(rect, 135f, 270f, false, p);
        float sweep = Math.min(speedKmh, 200f) / 200f * 270f;
        p.setColor(0xFF8CF2FF);
        if (sweep > 0.5f) {
            canvas.drawArc(rect, 135f, sweep, false, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(0xFFFFFFFF);
        p.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        p.setTextSize(r * 0.95f);
        canvas.drawText(String.valueOf(Math.round(speedKmh)), cx, cy + r * 0.22f, p);
        p.setTextSize(r * 0.28f);
        p.setColor(0xCCFFFFFF);
        canvas.drawText("km/h", cx, cy + r * 0.62f, p);

        // Infos à droite de l'anneau
        p.setTextAlign(Paint.Align.LEFT);
        float tx = cx + r + ph * 0.14f;
        float ts = h * 0.033f;
        p.setTextSize(ts);
        String cap = heading >= 0 ? CARDINALS[((int) ((heading + 22.5f) / 45f)) % 8] + " " + Math.round(heading) + "°" : "--";
        String alt = Double.isNaN(altitude) ? "--" : Math.round(altitude) + " m";
        drawRow(canvas, "CAP", cap, tx, cy - ts * 1.6f, ts);
        drawRow(canvas, "ALT", alt, tx, cy, ts);
        drawRow(canvas, "GPS", fix ? "Signal OK" : "Recherche...", tx, cy + ts * 1.6f, ts);
    }

    private void drawRow(Canvas canvas, String label, String value, float x, float y, float ts) {
        p.setTextSize(ts * 0.8f);
        p.setColor(0x99FFFFFF);
        canvas.drawText(label, x, y, p);
        p.setTextSize(ts);
        p.setColor(0xFFFFFFFF);
        canvas.drawText(value, x + ts * 2.8f, y, p);
    }

    private void drawClock(Canvas canvas, float w, float h) {
        Date now = new Date();
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        p.setColor(0xFFFFFFFF);
        p.setTextSize(h * 0.12f);
        canvas.drawText(timeFmt.format(now), w - w * 0.04f, h * 0.24f, p);
        String d = dateFmt.format(now);
        if (d.length() > 0) {
            d = Character.toUpperCase(d.charAt(0)) + d.substring(1);
        }
        p.setTextSize(h * 0.038f);
        p.setColor(0xCCFFFFFF);
        canvas.drawText(d, w - w * 0.04f, h * 0.31f, p);
    }
}
