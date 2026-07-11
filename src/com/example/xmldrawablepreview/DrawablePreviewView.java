package com.example.xmldrawablepreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DrawablePreviewView extends View {
    private final Paint checkerPaint = new Paint();
    private final Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable drawable;
    private VectorModel vectorModel;
    private int previewSize = 160;
    private final Set<Integer> states = new HashSet<Integer>();
    private String error;
    private final Context context;

    public DrawablePreviewView(Context context) { super(context); this.context = context; init(); }
    public DrawablePreviewView(Context context, AttributeSet attrs) { super(context, attrs); this.context = context; init(); }

    private void init() {
        checkerPaint.setColor(0xffeeeeee);
        debugPaint.setColor(Color.MAGENTA);
        debugPaint.setStrokeWidth(2);
        debugPaint.setStyle(Paint.Style.STROKE);
        setBackgroundColor(Color.WHITE);
    }

    public void setPreviewSize(int size) {
        previewSize = size;
        invalidate();
    }

    public void setState(int state, boolean enabled) {
        if (enabled) states.add(state); else states.remove(state);
        refreshDrawableState();
        invalidate();
    }

    public void setXml(byte[] xml) {
        error = null;
        vectorModel = null;
        try {
            try {
                XmlPullParser drawableParser = XmlPullParserFactory.newInstance().newPullParser();
                drawableParser.setInput(new ByteArrayInputStream(xml), null);
                drawable = Drawable.createFromXml(context.getResources(), drawableParser);
            } catch (Exception ignored) {
                drawable = null;
            }
            vectorModel = VectorModel.parse(xml);
            if (drawable == null && vectorModel == null) error = "Unsupported XML drawable";
        } catch (Exception e) {
            drawable = null;
            error = e.getMessage();
        }
        invalidate();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] base = super.onCreateDrawableState(extraSpace + states.size());
        int[] extra = new int[states.size()];
        int i = 0;
        for (Integer state : states) extra[i++] = state.intValue();
        mergeDrawableStates(base, extra);
        return base;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int left = (getWidth() - previewSize) / 2;
        int top = (getHeight() - previewSize) / 2;
        canvas.drawRect(left, top, left + previewSize, top + previewSize, checkerPaint);
        if (drawable != null) {
            drawable.setState(getDrawableState());
            drawable.setBounds(left, top, left + previewSize, top + previewSize);
            drawable.draw(canvas);
        } else if (vectorModel != null) {
            vectorModel.draw(canvas, new RectF(left, top, left + previewSize, top + previewSize));
        }
        if (vectorModel != null) vectorModel.drawDebug(canvas, new RectF(left, top, left + previewSize, top + previewSize), debugPaint);
        if (error != null) {
            debugPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(error, 10, 30, debugPaint);
            debugPaint.setStyle(Paint.Style.STROKE);
        }
    }

    private static class VectorModel {
        float width = 24, height = 24, viewportWidth = 24, viewportHeight = 24;
        ArrayList<PathEntry> paths = new ArrayList<PathEntry>();

        static VectorModel parse(byte[] xml) throws Exception {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new ByteArrayInputStream(xml), null);
            VectorModel model = null;
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                if (type != XmlPullParser.START_TAG) continue;
                String name = parser.getName();
                if ("vector".equals(name)) {
                    model = new VectorModel();
                    model.width = dim(attr(parser, "width"), 24);
                    model.height = dim(attr(parser, "height"), 24);
                    model.viewportWidth = number(attr(parser, "viewportWidth"), model.width);
                    model.viewportHeight = number(attr(parser, "viewportHeight"), model.height);
                } else if (model != null && "path".equals(name)) {
                    String data = attr(parser, "pathData");
                    if (data == null) data = attr(parser, "d");
                    if (data != null) {
                        PathEntry entry = new PathEntry();
                        entry.path = PathParser.createPathFromPathData(data);
                        entry.controls = PathParser.getLastControlPoints();
                        entry.debug = hasRawDebugAttribute(parser);
                        model.paths.add(entry);
                    }
                }
            }
            return model;
        }

        void draw(Canvas canvas, RectF dst) {
            canvas.save();
            canvas.translate(dst.left, dst.top);
            canvas.scale(dst.width() / viewportWidth, dst.height() / viewportHeight);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            for (PathEntry entry : paths) canvas.drawPath(entry.path, paint);
            canvas.restore();
        }

        void drawDebug(Canvas canvas, RectF dst, Paint paint) {
            canvas.save();
            canvas.translate(dst.left, dst.top);
            canvas.scale(dst.width() / viewportWidth, dst.height() / viewportHeight);
            for (PathEntry entry : paths) {
                if (!entry.debug) continue;
                canvas.drawPath(entry.path, paint);
                for (PointF point : entry.controls) {
                    canvas.drawCircle(point.x, point.y, 2.5f, paint);
                    canvas.drawLine(point.x - 3, point.y, point.x + 3, point.y, paint);
                    canvas.drawLine(point.x, point.y - 3, point.x, point.y + 3, paint);
                }
            }
            canvas.restore();
        }

        private static boolean hasRawDebugAttribute(XmlPullParser parser) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if (parser.getAttributeNamespace(i) == null || parser.getAttributeNamespace(i).length() == 0) {
                    String n = parser.getAttributeName(i);
                    if ("debug".equals(n) || "debugControlPoints".equals(n) || "showControlPoints".equals(n)) return true;
                }
            }
            return false;
        }

        private static String attr(XmlPullParser parser, String local) {
            for (int i = 0; i < parser.getAttributeCount(); i++) if (local.equals(parser.getAttributeName(i))) return parser.getAttributeValue(i);
            return null;
        }

        private static float dim(String value, float fallback) { return number(value == null ? null : value.replace("dp", "").replace("px", ""), fallback); }
        private static float number(String value, float fallback) {
            if (value == null) return fallback;
            try { return Float.parseFloat(value); } catch (NumberFormatException e) { return fallback; }
        }
    }

    private static class PathEntry { Path path; ArrayList<PointF> controls = new ArrayList<PointF>(); boolean debug; }
}
