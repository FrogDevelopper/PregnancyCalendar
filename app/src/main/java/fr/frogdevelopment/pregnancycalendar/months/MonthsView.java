package fr.frogdevelopment.pregnancycalendar.months;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.preference.PreferenceManager;
import android.view.View;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.ChronoUnit;

import fr.frogdevelopment.pregnancycalendar.R;

public class MonthsView extends View {

    // defines paint and canvas
    private final Paint drawPaint;
    private Shader shader;
    private long mDuration;

    public MonthsView(Context context) {
        super(context);
        this.drawPaint = new Paint();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        int mDaysToFecundation = sharedPref.getInt("pref_key_days_to_fecundation", getResources().getInteger(R.integer.default_days_to_fecundation));
        int mGestationMax = sharedPref.getInt("pref_key_gestation_max", getResources().getInteger(R.integer.default_gestation_max));

        mDuration = mGestationMax - mDaysToFecundation;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPaint.setColor(Color.BLACK);
//        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
//        drawPaint.setStyle(Paint.Style.STROKE);
//        drawPaint.setStrokeJoin(Paint.Join.ROUND);
//        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setTextSize(50);

        // Set a pixels value to offset the line from canvas edge
        int offsetY = 75;
        int offsetX = canvas.getWidth() / 4;

        int start = offsetY;
        int end = canvas.getHeight() - offsetY;

        /*or REPEAT*/
        if (shader == null) {
            shader = new LinearGradient(
                    offsetX,
                    start,
                    offsetX,
                    end,
                    getResources().getColor(android.R.color.holo_orange_light),
                    getResources().getColor(android.R.color.holo_green_dark),
                    Shader.TileMode.MIRROR /*or REPEAT*/);
        }

        drawPaint.setShader(shader);

        // draw vertical line
        canvas.drawLine(offsetX, start, offsetX, end - 3 /* fixme find why this is need !!*/, drawPaint);

        int length = end - start;
        int lengthMonth = length / 9;
        int positionMonth = offsetY;
        int positionText = positionMonth + lengthMonth / 2;

        LocalDate currentMonth = /*PregnancyUtils.conceptionDate*/null; // fixme
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
        for (int i = 0; i <= 9; i++) {
            // line month
            canvas.drawLine(
                    offsetX - 100, // startX
                    positionMonth, // startY
                    offsetX + 100, // stopX
                    positionMonth, // stopY
                    drawPaint // Paint
            );

            // label month
            canvas.drawText(
                    currentMonth.format(dateTimeFormatter),
                    offsetX + 120,
                    positionMonth,
                    drawPaint
            );

            currentMonth = currentMonth.plusMonths(1);

            if (i < 9) {
                // number month
                canvas.drawText(
                        getContext().getString(R.string.month_month, i + 1),
                        offsetX - 200,
                        positionText,
                        drawPaint
                );
            }

            positionMonth = positionMonth + lengthMonth;
            positionText = positionMonth + lengthMonth / 2 + 20;
        }

        drawPaint.setShader(null);

        addDate(canvas, offsetX, offsetY, length, getResources().getColor(R.color.colorPrimary), LocalDate.now());

//        addDate(canvas, offsetX, offsetY, length, Color.RED, "test", LocalDate.of(2016, 11, 5));
    }

    private void addDate(Canvas canvas, int offsetX, int offsetY, int length, int color, LocalDate date) {
        long now = ChronoUnit.DAYS.between(/*PregnancyUtils.conceptionDate*/null/*fixme*/, date);

        int y = (int) (now * length / mDuration) + offsetY;

        drawTriangle(canvas, offsetX, y, color);
    }

    private void drawTriangle(Canvas canvas, int x, int y, int color) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 50, y - 25);
        path.lineTo(x + 50, y + 25);
        path.close();

        drawPaint.setColor(color);
        drawPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, drawPaint);
    }
}
