package app.tab.store.domain

import app.tab.store.domain.model.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressTest {

    private val complete = Address(
        fullName = "Yash Gupta",
        phone = "9876543210",
        line1 = "12, Indiranagar",
        line2 = "Near the park",
        city = "Bengaluru",
        state = "Karnataka",
        pincode = "560038",
    )

    @Test
    fun `complete address validates`() {
        assertTrue(complete.isComplete())
    }

    @Test
    fun `short pincode or missing street fails validation`() {
        assertFalse(complete.copy(pincode = "5600").isComplete())
        assertFalse(complete.copy(line1 = "").isComplete())
        assertFalse(complete.copy(city = "").isComplete())
    }

    @Test
    fun `phone is optional because nothing is delivered`() {
        assertTrue(complete.copy(phone = "").isComplete())
        assertTrue(complete.copy(phone = "98765").isComplete())
    }

    @Test
    fun `one line address joins the parts`() {
        assertEquals(
            "12, Indiranagar, Near the park, Bengaluru, Karnataka, 560038",
            complete.oneLine,
        )
    }
}
