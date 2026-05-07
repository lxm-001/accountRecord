package com.mian.accountrecord.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoginRateLimiterTest {

    private lateinit var limiter: LoginRateLimiter

    @Before
    fun setup() {
        limiter = LoginRateLimiter()
    }

    @Test
    fun `canLogin returns true initially`() {
        assertTrue(limiter.canLogin())
    }

    @Test
    fun `canLogin returns true after fewer than 6 attempts`() {
        repeat(5) { limiter.recordLoginAttempt() }
        assertTrue(limiter.canLogin())
    }

    @Test
    fun `canLogin returns false after 6 attempts within 1 minute`() {
        repeat(6) { limiter.recordLoginAttempt() }
        assertFalse(limiter.canLogin())
    }

    @Test
    fun `getCooldownRemainingSeconds returns 0 when no cooldown`() {
        assertEquals(0, limiter.getCooldownRemainingSeconds())
    }

    @Test
    fun `getCooldownRemainingSeconds returns positive value during cooldown`() {
        repeat(6) { limiter.recordLoginAttempt() }
        val remaining = limiter.getCooldownRemainingSeconds()
        assertTrue("Expected remaining > 0, got $remaining", remaining > 0)
        assertTrue("Expected remaining <= 180, got $remaining", remaining <= 180)
    }

    @Test
    fun `calling canLogin multiple times does not change state`() {
        repeat(5) { limiter.recordLoginAttempt() }
        assertTrue(limiter.canLogin())
        assertTrue(limiter.canLogin())
        assertTrue(limiter.canLogin())
    }
}
