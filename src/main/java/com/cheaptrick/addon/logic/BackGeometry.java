package com.cheaptrick.addon.logic;

/**
 * Чистая геометрия проверки «взгляд на спину» (без классов Minecraft — поэтому покрыта юнит-тестами).
 *
 * Система координат тела носителя: ось Z — «вперёд» (куда смотрит тело), X — вбок, Y — вверх.
 * Поворот тела bodyYaw задан в градусах в соглашении Minecraft: yaw 0 смотрит в +Z мира,
 * вектор «вперёд» = (-sin(yaw), 0, cos(yaw)).
 */
public final class BackGeometry {
    private BackGeometry() {}

    /** Полуглубина тела игрока (4 px / 2 / 16). Спина — плоскость z = -BODY_HALF_DEPTH. */
    public static final double BODY_HALF_DEPTH = 0.125D;
    /** Высота нижнего края торса над ногами у стоящего игрока (12 px). */
    public static final double BODY_BOTTOM = 0.75D;
    /** Высота верхнего края торса у стоящего игрока (24 px). */
    public static final double BODY_TOP = 1.5D;
    /** Опускание торса при приседании. */
    public static final double CROUCH_DROP = 0.3D;

    /**
     * Проверка для вертикальной позы носителя (стоит, идёт, бежит, приседает).
     *
     * @param hitOut массив из 3 элементов: сюда записывается мировая точка попадания луча в область спины
     * @return true, если наблюдатель находится позади носителя, смотрит в область спины и в пределах дистанции
     *         (препятствия-блоки здесь НЕ проверяются — это делает вызывающий код)
     */
    public static boolean standingBackHit(
            double cx, double cy, double cz, double bodyYawDeg, boolean crouching,
            double ex, double ey, double ez,
            double lookX, double lookY, double lookZ,
            double maxDist, double halfAngleDeg, double areaWidth, double areaHeight, double tolerance,
            double[] hitOut) {
        double yaw = Math.toRadians(bodyYawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw); // вперёд
        double rx = Math.cos(yaw), rz = Math.sin(yaw);  // вбок

        // 1) Наблюдатель в локальных координатах носителя
        double relEx = ex - cx, relEz = ez - cz;
        double elx = relEx * rx + relEz * rz;
        double elz = relEx * fx + relEz * fz;
        double ely = ey - cy;

        // 2) Должен находиться позади: угол между «назад» и направлением на наблюдателя <= halfAngle
        double horiz = Math.sqrt(elx * elx + elz * elz);
        if (horiz < 1.0E-4D) {
            return false;
        }
        double cosAngle = (-elz) / horiz;
        if (cosAngle < Math.cos(Math.toRadians(halfAngleDeg))) {
            return false;
        }

        // 3) Луч взгляда в локальных координатах
        double llx = lookX * rx + lookZ * rz;
        double llz = lookX * fx + lookZ * fz;
        double lly = lookY;
        double len = Math.sqrt(llx * llx + lly * lly + llz * llz);
        if (len < 1.0E-6D) {
            return false;
        }
        llx /= len; lly /= len; llz /= len;
        // Взгляд должен быть направлен «вперёд» (в сторону тела), иначе наблюдатель отвернулся
        if (llz <= 0.0D) {
            return false;
        }

        // 4) Область спины в локальных координатах
        double drop = crouching ? CROUCH_DROP : 0.0D;
        double minX = -areaWidth / 2.0D - tolerance;
        double maxX = areaWidth / 2.0D + tolerance;
        double minY = BODY_BOTTOM - drop - tolerance;
        double maxY = Math.min(BODY_BOTTOM + areaHeight, BODY_TOP) - drop + tolerance;
        double minZ = -BODY_HALF_DEPTH - 0.10D - tolerance;
        double maxZ = -BODY_HALF_DEPTH + 0.05D + tolerance;

        double[] t = new double[2];
        if (!slab(elx, llx, minX, maxX, t, true)) return false;
        if (!slab(ely, lly, minY, maxY, t, false)) return false;
        if (!slab(elz, llz, minZ, maxZ, t, false)) return false;
        double tHit = Math.max(t[0], 0.0D);
        if (t[0] > t[1] || t[1] < 0.0D || tHit > maxDist) {
            return false;
        }
        double hlx = elx + llx * tHit, hly = ely + lly * tHit, hlz = elz + llz * tHit;
        if (hitOut != null && hitOut.length >= 3) {
            hitOut[0] = cx + rx * hlx + fx * hlz;
            hitOut[1] = cy + hly;
            hitOut[2] = cz + rz * hlx + fz * hlz;
        }
        return true;
    }

    /**
     * Проверка для горизонтальной позы (плавание, полёт на элитрах): спина обращена вверх.
     * Область спины — верхний слой хитбокса; наблюдатель должен смотреть на неё сверху.
     */
    public static boolean horizontalBackHit(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double ex, double ey, double ez,
            double lookX, double lookY, double lookZ,
            double maxDist, double tolerance, double[] hitOut) {
        double slabMinY = maxY - 0.30D - tolerance;
        double slabMaxY = maxY + tolerance;
        if (ey < slabMaxY) {
            return false; // наблюдатель должен быть выше спины
        }
        double len = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        if (len < 1.0E-6D) {
            return false;
        }
        lookX /= len; lookY /= len; lookZ /= len;
        double[] t = new double[2];
        if (!slab(ex, lookX, minX - tolerance, maxX + tolerance, t, true)) return false;
        if (!slab(ey, lookY, slabMinY, slabMaxY, t, false)) return false;
        if (!slab(ez, lookZ, minZ - tolerance, maxZ + tolerance, t, false)) return false;
        double tHit = Math.max(t[0], 0.0D);
        if (t[0] > t[1] || t[1] < 0.0D || tHit > maxDist) {
            return false;
        }
        if (hitOut != null && hitOut.length >= 3) {
            hitOut[0] = ex + lookX * tHit;
            hitOut[1] = ey + lookY * tHit;
            hitOut[2] = ez + lookZ * tHit;
        }
        return true;
    }

    /**
     * Один шаг slab-метода пересечения луча с AABB. Состояние накапливается в t[0] (вход) и t[1] (выход).
     * @param first true для первой оси — инициализирует t
     */
    private static boolean slab(double origin, double dir, double min, double max, double[] t, boolean first) {
        double t0, t1;
        if (Math.abs(dir) < 1.0E-9D) {
            if (origin < min || origin > max) {
                return false;
            }
            t0 = Double.NEGATIVE_INFINITY;
            t1 = Double.POSITIVE_INFINITY;
        } else {
            double a = (min - origin) / dir;
            double b = (max - origin) / dir;
            t0 = Math.min(a, b);
            t1 = Math.max(a, b);
        }
        if (first) {
            t[0] = t0;
            t[1] = t1;
        } else {
            t[0] = Math.max(t[0], t0);
            t[1] = Math.min(t[1], t1);
        }
        return t[0] <= t[1];
    }
}
