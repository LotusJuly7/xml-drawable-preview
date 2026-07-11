package com.example.xmldrawablepreview;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;

public final class PathParser {
    private static ArrayList<PointF> lastControlPoints = new ArrayList<PointF>();

    private PathParser() { }

    public static ArrayList<PointF> getLastControlPoints() {
        return new ArrayList<PointF>(lastControlPoints);
    }

    public static Path createPathFromPathData(String data) {
        lastControlPoints = new ArrayList<PointF>();
        Path path = new Path();
        Scanner s = new Scanner(data);
        char cmd = 'M';
        float x = 0, y = 0, startX = 0, startY = 0, lastCx = 0, lastCy = 0;
        while (s.hasMore()) {
            if (s.peekCommand()) cmd = s.nextCommand();
            boolean rel = Character.isLowerCase(cmd);
            char c = Character.toUpperCase(cmd);
            if (c == 'Z') {
                path.close(); x = startX; y = startY; s.skipSeparators(); continue;
            }
            if (!s.hasNumber()) break;
            if (c == 'M') {
                x = value(s, rel, x); y = value(s, rel, y); path.moveTo(x, y); startX = x; startY = y; cmd = rel ? 'l' : 'L';
            } else if (c == 'L') {
                x = value(s, rel, x); y = value(s, rel, y); path.lineTo(x, y);
            } else if (c == 'H') {
                x = value(s, rel, x); path.lineTo(x, y);
            } else if (c == 'V') {
                y = value(s, rel, y); path.lineTo(x, y);
            } else if (c == 'C') {
                float x1 = value(s, rel, x), y1 = value(s, rel, y), x2 = value(s, rel, x), y2 = value(s, rel, y);
                float nx = value(s, rel, x), ny = value(s, rel, y);
                add(x1, y1); add(x2, y2); path.cubicTo(x1, y1, x2, y2, nx, ny); x = nx; y = ny; lastCx = x2; lastCy = y2;
            } else if (c == 'S') {
                float x1 = x * 2 - lastCx, y1 = y * 2 - lastCy;
                float x2 = value(s, rel, x), y2 = value(s, rel, y), nx = value(s, rel, x), ny = value(s, rel, y);
                add(x1, y1); add(x2, y2); path.cubicTo(x1, y1, x2, y2, nx, ny); x = nx; y = ny; lastCx = x2; lastCy = y2;
            } else if (c == 'Q') {
                float x1 = value(s, rel, x), y1 = value(s, rel, y), nx = value(s, rel, x), ny = value(s, rel, y);
                add(x1, y1); path.quadTo(x1, y1, nx, ny); x = nx; y = ny; lastCx = x1; lastCy = y1;
            } else if (c == 'T') {
                float x1 = x * 2 - lastCx, y1 = y * 2 - lastCy, nx = value(s, rel, x), ny = value(s, rel, y);
                add(x1, y1); path.quadTo(x1, y1, nx, ny); x = nx; y = ny; lastCx = x1; lastCy = y1;
            } else {
                break;
            }
            s.skipSeparators();
        }
        return path;
    }

    private static void add(float x, float y) { lastControlPoints.add(new PointF(x, y)); }
    private static float value(Scanner s, boolean rel, float current) { float v = s.nextFloat(); return rel ? current + v : v; }

    private static class Scanner {
        final String data; int p;
        Scanner(String data) { this.data = data; }
        boolean hasMore() { skipSeparators(); return p < data.length(); }
        void skipSeparators() { while (p < data.length() && (Character.isWhitespace(data.charAt(p)) || data.charAt(p) == ',')) p++; }
        boolean peekCommand() { skipSeparators(); return p < data.length() && Character.isLetter(data.charAt(p)); }
        char nextCommand() { return data.charAt(p++); }
        boolean hasNumber() { skipSeparators(); return p < data.length() && (data.charAt(p) == '-' || data.charAt(p) == '+' || data.charAt(p) == '.' || Character.isDigit(data.charAt(p))); }
        float nextFloat() {
            skipSeparators(); int start = p;
            if (p < data.length() && (data.charAt(p) == '-' || data.charAt(p) == '+')) p++;
            while (p < data.length() && Character.isDigit(data.charAt(p))) p++;
            if (p < data.length() && data.charAt(p) == '.') { p++; while (p < data.length() && Character.isDigit(data.charAt(p))) p++; }
            if (p < data.length() && (data.charAt(p) == 'e' || data.charAt(p) == 'E')) {
                p++; if (p < data.length() && (data.charAt(p) == '-' || data.charAt(p) == '+')) p++;
                while (p < data.length() && Character.isDigit(data.charAt(p))) p++;
            }
            float v = Float.parseFloat(data.substring(start, p)); skipSeparators(); return v;
        }
    }
}
