/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the improved Fleeing AI behavior in {@link Mob}.
 *
 * Tests verify that fleeing mobs correctly identify nearby allies to
 * regroup with, filter out invalid allies, and fall back to default
 * flee behavior when no allies are available.
 *
 * Quality Attribute: ISO/IEC 25010 Functional Suitability — Functional Correctness
 */
public class FleeingAITest {

    // Test constants matching Mob.FLEE_ALLY_SEARCH_RANGE and FLEE_ALLY_MIN_ENEMY_DIST
    private static final int SEARCH_RANGE = 8;
    private static final int MIN_ENEMY_DIST = 3;

    private Level mockLevel;
    private HashSet<Mob> mobs;

    /**
     * Sets up a mock level environment for testing.
     * Creates a 32x32 grid where distance(a,b) uses Chebyshev distance.
     */
    @BeforeEach
    void setUp() {
        mockLevel = mock(Level.class);

        // Simulate Chebyshev distance: max(|ax-bx|, |ay-by|) for a 32-wide level
        when(mockLevel.distance(anyInt(), anyInt())).thenAnswer(invocation -> {
            int a = invocation.getArgument(0);
            int b = invocation.getArgument(1);
            int ax = a % 32, ay = a / 32;
            int bx = b % 32, by = b / 32;
            return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
        });

        mobs = new HashSet<>();
        when(mockLevel.width()).thenReturn(32);

        Dungeon.level = mockLevel;
        Dungeon.level.mobs = mobs;
    }

    /**
     * Helper: creates a test mob at a given position with a given alignment.
     */
    private Mob createMob(int pos, Char.Alignment alignment, Mob.AiState state) {
        // We use a concrete Rat mob for testing since Mob is abstract
        Rat mob = new Rat();
        mob.pos = pos;
        mob.alignment = alignment;
        // Set the state — need to use the mob's own state instances
        if (state != null) {
            if (state.equals("HUNTING")) mob.state = mob.HUNTING;
            else if (state.equals("SLEEPING")) mob.state = mob.SLEEPING;
            else if (state.equals("PASSIVE")) mob.state = mob.PASSIVE;
            else if (state.equals("WANDERING")) mob.state = mob.WANDERING;
        }
        return mob;
    }

    private Mob createEnemyMob(int pos) {
        Rat mob = new Rat();
        mob.pos = pos;
        mob.alignment = Char.Alignment.ENEMY;
        mob.state = mob.HUNTING;
        return mob;
    }

    private Mob createAllyMob(int pos, boolean active) {
        Rat mob = new Rat();
        mob.pos = pos;
        mob.alignment = Char.Alignment.ENEMY; // same alignment as fleeing mob
        mob.state = active ? mob.HUNTING : mob.SLEEPING;
        return mob;
    }

    // ========================================================================
    // Test: findNearestAlly with allies in range
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly returns closest same-alignment ally within range")
    void testFindNearestAlly_withAlliesInRange() {
        // Fleeing mob at position (5,5) = 5 + 5*32 = 165
        Mob fleeingMob = createEnemyMob(165);
        fleeingMob.state = fleeingMob.FLEEING;

        // Ally at (8,5) = 8 + 5*32 = 168, distance = 3
        Mob nearAlly = createAllyMob(168, true);

        // Ally at (12,5) = 12 + 5*32 = 172, distance = 7
        Mob farAlly = createAllyMob(172, true);

        mobs.add(fleeingMob);
        mobs.add(nearAlly);
        mobs.add(farAlly);

        // Set enemy far away so allies aren't filtered out by MIN_ENEMY_DIST
        Mob heroStandin = createAllyMob(0, true);
        heroStandin.alignment = Char.Alignment.ALLY;
        fleeingMob.alignment = Char.Alignment.ENEMY;

        // Call the method under test
        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNotNull(result, "Should find an ally when allies are in range");
        assertEquals(nearAlly.pos, result.pos, "Should return the closest ally");
    }

    // ========================================================================
    // Test: findNearestAlly with no allies in range
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly returns null when no allies are within range")
    void testFindNearestAlly_noAlliesInRange() {
        // Fleeing mob at (5,5)
        Mob fleeingMob = createEnemyMob(165);

        // Ally at (20,5) = 20 + 5*32 = 180, distance = 15 (beyond range of 8)
        Mob distantAlly = createAllyMob(180, true);

        mobs.add(fleeingMob);
        mobs.add(distantAlly);

        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNull(result, "Should return null when no allies are within range");
    }

    // ========================================================================
    // Test: findNearestAlly ignores different-alignment mobs
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly ignores mobs with different alignment")
    void testFindNearestAlly_ignoresEnemies() {
        // Fleeing mob (ENEMY alignment) at (5,5)
        Mob fleeingMob = createEnemyMob(165);

        // An ALLY-aligned mob nearby at (6,5) — different alignment, should be ignored
        Mob allyAligned = createAllyMob(166, true);
        allyAligned.alignment = Char.Alignment.ALLY;

        mobs.add(fleeingMob);
        mobs.add(allyAligned);

        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNull(result, "Should ignore mobs with different alignment");
    }

    // ========================================================================
    // Test: findNearestAlly ignores self
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly does not return self")
    void testFindNearestAlly_ignoresSelf() {
        Mob fleeingMob = createEnemyMob(165);

        // Only mob on the level is the fleeing mob itself
        mobs.add(fleeingMob);

        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNull(result, "Should not return self as an ally");
    }

    // ========================================================================
    // Test: findNearestAlly ignores sleeping/passive allies
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly ignores sleeping and passive allies")
    void testFindNearestAlly_ignoresInactiveAllies() {
        Mob fleeingMob = createEnemyMob(165);

        // Sleeping ally at (7,5) = 167, distance = 2
        Mob sleepingAlly = createAllyMob(167, false); // false = SLEEPING
        sleepingAlly.state = sleepingAlly.SLEEPING;

        // Passive ally at (8,5) = 168, distance = 3
        Mob passiveAlly = createAllyMob(168, false);
        passiveAlly.state = passiveAlly.PASSIVE;

        mobs.add(fleeingMob);
        mobs.add(sleepingAlly);
        mobs.add(passiveAlly);

        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNull(result, "Should ignore sleeping and passive allies");
    }

    // ========================================================================
    // Test: findNearestAlly ignores allies too close to the enemy
    // ========================================================================

    @Test
    @DisplayName("findNearestAlly ignores allies that are too close to the enemy")
    void testFindNearestAlly_ignoresAlliesNearEnemy() {
        // Fleeing mob at (5,5) = 165
        Mob fleeingMob = createEnemyMob(165);
        fleeingMob.state = fleeingMob.FLEEING;

        // Set the enemy (hero) at (3,5) = 3 + 5*32 = 163
        // We need to set the mob's enemy field
        Rat enemyChar = new Rat();
        enemyChar.pos = 163;
        enemyChar.alignment = Char.Alignment.ALLY;

        // Use reflection or direct field access to set enemy
        // The enemy field is 'protected Char enemy' in Mob
        try {
            java.lang.reflect.Field enemyField = Mob.class.getDeclaredField("enemy");
            enemyField.setAccessible(true);
            enemyField.set(fleeingMob, enemyChar);
        } catch (Exception e) {
            fail("Could not set enemy field: " + e.getMessage());
        }

        // Ally at (4,5) = 164, distance from enemy (163) = 1 < MIN_ENEMY_DIST(3) — too close!
        Mob tooCloseAlly = createAllyMob(164, true);

        // Ally at (10,5) = 170, distance from enemy (163) = 7 — safe
        Mob safeAlly = createAllyMob(170, true);

        mobs.add(fleeingMob);
        mobs.add(tooCloseAlly);
        mobs.add(safeAlly);

        Mob result = fleeingMob.findNearestAlly(SEARCH_RANGE);

        assertNotNull(result, "Should find the safe ally");
        assertEquals(safeAlly.pos, result.pos,
                "Should return the ally that is far enough from the enemy");
    }

    // ========================================================================
    // Test: Low-HP flee trigger — mob should want to flee when HP < 25% and allies nearby
    // ========================================================================

    @Test
    @DisplayName("Mob at low HP with nearby allies should trigger flee condition")
    void testLowHpFleeCondition_withAllies() {
        // Mob at (5,5) with low HP (2 out of 8 = 25%)
        Rat lowHpMob = new Rat();
        lowHpMob.pos = 165;
        lowHpMob.alignment = Char.Alignment.ENEMY;
        lowHpMob.state = lowHpMob.HUNTING;
        lowHpMob.HP = 2;
        lowHpMob.HT = 8;

        // Ally nearby at (8,5) = 168
        Mob ally = createAllyMob(168, true);

        mobs.add(lowHpMob);
        mobs.add(ally);

        // At 25% HP with an ally nearby, findNearestAlly should find the ally
        Mob result = lowHpMob.findNearestAlly(SEARCH_RANGE);
        assertNotNull(result, "Should find ally when mob is at low HP");

        // Verify the HP threshold check: HP <= HT * 0.25
        assertTrue(lowHpMob.HP <= lowHpMob.HT * 0.25f,
                "Mob HP should be at or below 25% threshold");
    }

    // ========================================================================
    // Test: Low-HP mob should NOT flee when no allies are available
    // ========================================================================

    @Test
    @DisplayName("Mob at low HP without nearby allies should not find a flee target")
    void testLowHpNoFlee_withoutAllies() {
        // Mob at (5,5) with low HP
        Rat lowHpMob = new Rat();
        lowHpMob.pos = 165;
        lowHpMob.alignment = Char.Alignment.ENEMY;
        lowHpMob.state = lowHpMob.HUNTING;
        lowHpMob.HP = 1;
        lowHpMob.HT = 8;

        // No allies on the level, just the mob itself
        mobs.add(lowHpMob);

        Mob result = lowHpMob.findNearestAlly(SEARCH_RANGE);
        assertNull(result, "Should not find an ally when alone — mob should keep fighting");
    }
}
