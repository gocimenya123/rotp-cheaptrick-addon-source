package com.cheaptrick.addon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cheaptrick.addon.logic.BackGeometry;

public class BackGeometryTest {
    // Носитель в (0,0,0), yaw=0 => смотрит в +Z, спина обращена в -Z.
    private static boolean look(double ex, double ey, double ez, double lx, double ly, double lz, double yaw, boolean crouch) {
        return BackGeometry.standingBackHit(0, 0, 0, yaw, crouch, ex, ey, ez, lx, ly, lz,
                24, 60, 0.6, 0.8, 0.1, new double[3]);
    }

    @Test void observerBehindLookingAtBackHits() {
        // Наблюдатель в 5 блоках позади (z=-5), глаза на высоте 1.62, смотрит на торс (y≈1.1) вдоль +Z
        double dy = 1.1 - 1.62, dz = 5.0, len = Math.sqrt(dy * dy + dz * dz);
        assertTrue(look(0, 1.62, -5, 0, dy / len, dz / len, 0, false));
    }

    @Test void observerInFrontSeesFaceNoHit() {
        double dy = 1.1 - 1.62, dz = -5.0, len = Math.sqrt(dy * dy + dz * dz);
        assertFalse(look(0, 1.62, 5, 0, dy / len, dz / len, 0, false));
    }

    @Test void observerBehindLookingAwayNoHit() {
        assertFalse(look(0, 1.62, -5, 0, 0, -1, 0, false));
    }

    @Test void observerBehindLookingAboveNoHit() {
        assertFalse(look(0, 1.62, -5, 0, 0.6, 0.8, 0, false));
    }

    @Test void observerBesideNoHit() {
        // Сбоку (угол 90°) — вне конуса 60°
        assertFalse(look(5, 1.62, 0, -1, -0.1, 0, 0, false));
    }

    @Test void rotatedCarrierBackFollowsBodyYaw() {
        // yaw=90: вперёд = (-1,0,0), спина обращена в +X. Наблюдатель в +X смотрит в -X — попадание.
        assertTrue(look(5, 1.1, 0, -1, 0, 0, 90, false));
        // Тот же наблюдатель при yaw=-90 видит лицо.
        assertFalse(look(5, 1.1, 0, -1, 0, 0, -90, false));
    }

    @Test void tooFarNoHit() {
        assertFalse(look(0, 1.1, -30, 0, 0, 1, 0, false));
    }

    @Test void crouchingLowersBackArea() {
        // Взгляд на высоте 1.45 попадает в стоящего, но промахивается по приседающему (торс ниже на 0.3)
        assertTrue(look(0, 1.45, -4, 0, 0, 1, 0, false));
        assertFalse(look(0, 1.45, -4, 0, 0, 1, 0, true));
    }

    @Test void swimmingBackIsUp() {
        double[] hit = new double[3];
        // Хитбокс 0.6x0.6 на уровне y 0..0.6, наблюдатель выше, смотрит вниз на спину
        assertTrue(BackGeometry.horizontalBackHit(-0.3, 0, -0.3, 0.3, 0.6, 0.3, 0, 2.5, 0, 0, -1, 0, 24, 0.1, hit));
        // Наблюдатель ниже спины — нет
        assertFalse(BackGeometry.horizontalBackHit(-0.3, 0, -0.3, 0.3, 0.6, 0.3, 0, -1.0, 0, 0, 1, 0, 24, 0.1, hit));
    }
}
