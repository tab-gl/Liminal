package app.tab.store.core

import app.tab.store.core.utils.Money
import app.tab.store.core.utils.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `email validation accepts normal addresses and rejects malformed ones`() {
        assertTrue(Validators.isValidEmail("yash@example.com"))
        assertTrue(Validators.isValidEmail("yash.gupta+shop@mail.co.in"))
        assertFalse(Validators.isValidEmail("yash@"))
        assertFalse(Validators.isValidEmail("yash.example.com"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun `password needs eight characters`() {
        assertTrue(Validators.isValidPassword("sproutly1"))
        assertFalse(Validators.isValidPassword("short"))
    }

    @Test
    fun `password strength counts length digits caps and symbols`() {
        assertEquals(1, Validators.passwordStrength("abcdefgh"))
        assertEquals(4, Validators.passwordStrength("Abcdefg1!"))
    }

    @Test
    fun `slugify produces url safe slugs`() {
        // Ampersands and other punctuation are dropped, runs of space collapse.
        assertEquals("home-garden", Validators.slugify("Home & Garden"))
        assertEquals("the-green-shelf", Validators.slugify("  The Green Shelf  "))
    }

    @Test
    fun `rupee strings convert to paise without float drift`() {
        assertEquals(49_900L, Money.toPaise("499"))
        assertEquals(4_999L, Money.toPaise("49.99"))
        assertNull(Money.toPaise("abc"))
        assertEquals("499.00", Money.toRupeeString(49_900L))
    }

    @Test
    fun `pincode and phone validation`() {
        assertTrue(Validators.isValidPincode("560038"))
        assertFalse(Validators.isValidPincode("56003"))
        assertTrue(Validators.isValidPhone("+91 98765 43210"))
        assertFalse(Validators.isValidPhone("12345"))
    }
}
