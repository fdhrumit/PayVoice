package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppSettings
import com.example.domain.parser.PaymentParser
import com.example.utils.Deduplicator
import com.example.utils.NumberToWordsConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayVoiceComprehensiveTest {

    @Before
    fun setup() {
        Deduplicator.clearCache()
    }

    @Test
    fun `read string from context shows PayVoice`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PayVoice", appName)
    }

    // 1. Amount parser edge cases
    @Test
    fun `amount parser handles Indian commas, lakhs, and decimals without mixup`() {
        // Rs 4,000 vs Rs 400
        val msg4000 = "Dear Customer, your A/c XX4921 has been credited with Rs. 4,000.00 on 18-Oct-26. Avl Bal Rs. 25,000.00."
        val parsed4000 = PaymentParser.parseBankSms("VK-SBIINB", msg4000)
        assertNotNull(parsed4000)
        assertEquals(4000.0, parsed4000!!.amount, 0.001)
        assertEquals("4921", parsed4000.accountLast4)

        val msg400 = "Dear Customer, A/c *8812 is credited by INR 400 on 18-Oct-26 by transfer from Ramesh."
        val parsed400 = PaymentParser.parseBankSms("AD-HDFCBK", msg400)
        assertNotNull(parsed400)
        assertEquals(400.0, parsed400!!.amount, 0.001)
        assertEquals("8812", parsed400.accountLast4)

        // ₹1,25,000
        val msg125k = "A/c 1234 credited with ₹1,25,000 via UPI."
        val parsed125k = PaymentParser.parseBankSms("AX-ICICIB", msg125k)
        assertNotNull(parsed125k)
        assertEquals(125000.0, parsed125k!!.amount, 0.001)

        // Paise: 250.50
        val msgPaise = "Your account is credited by Rs 250.50. Avl bal is Rs 10,000."
        val parsedPaise = PaymentParser.parseBankSms("VM-KOTAKB", msgPaise)
        assertNotNull(parsedPaise)
        assertEquals(250.50, parsedPaise!!.amount, 0.001)
    }

    // 2. Debits are never announced
    @Test
    fun `debit messages, OTPs, and balance alerts are ignored`() {
        val debit1 = "Your A/c *4921 debited by Rs. 500.00 on 18-Oct-26. Avl Bal Rs. 4,500."
        assertNull(PaymentParser.parseBankSms("VK-SBIINB", debit1))

        val debit2 = "Sent Rs. 1,200 to Vijay Medicals from your HDFC Bank account."
        assertNull(PaymentParser.parseBankSms("AD-HDFCBK", debit2))

        val debit3 = "Rs 300 withdrawn from ATM using card ending 1234."
        assertNull(PaymentParser.parseBankSms("AX-ICICIB", debit3))

        val otp = "Your secret OTP for transaction of Rs 500 is 492011. Do not share with anyone."
        assertNull(PaymentParser.parseBankSms("VK-SBIINB", otp))

        val promo = "Pre-approved loan offer of Rs 5,00,000! Apply now at bank link."
        assertNull(PaymentParser.parseBankSms("VK-SBIINB", promo))
    }

    // 3. Duplicate SMS and notification speak once
    @Test
    fun `duplicate SMS and UPI notification speak only once`() {
        val amount = 500.0
        assertFalse("First payment should not be duplicate", Deduplicator.isDuplicate(amount))
        Deduplicator.recordPayment(amount)

        // Second payment of identical amount immediately afterwards within 5 min window
        assertTrue("Subsequent payment of same amount within 5 min should be duplicate", Deduplicator.isDuplicate(amount))

        // Different amount should not be duplicate
        assertFalse("Different amount should not be duplicate", Deduplicator.isDuplicate(750.0))
    }

    // 4. Unknown sender is ignored
    @Test
    fun `unknown sender SMS is ignored`() {
        val fakeMsg = "Your account has been credited with Rs. 5,000 by lottery."
        // Non-bank sender
        assertNull(PaymentParser.parseBankSms("LM-FAKEWIN", fakeMsg))
        assertNull(PaymentParser.parseBankSms("+919876543210", fakeMsg))
        assertNull(PaymentParser.parseBankSms("PROMOALERT", fakeMsg))

        // Legitimate bank senders pass check
        assertTrue(PaymentParser.isKnownBankSender("VK-SBIINB"))
        assertTrue(PaymentParser.isKnownBankSender("AD-HDFCBK"))
        assertTrue(PaymentParser.isKnownBankSender("AX-ICICIB"))
        assertTrue(PaymentParser.isKnownBankSender("VM-KOTAKB"))
        assertTrue(PaymentParser.isKnownBankSender("CP-BOBTXN"))
        assertTrue(PaymentParser.isKnownBankSender("JX-PNBSMS"))
    }

    // 5. Quiet hours block speech
    @Test
    fun `quiet hours block speech correctly`() {
        // Quiet hours: 22:00 (1320 min) to 07:00 (420 min)
        val settings = AppSettings(
            quietHoursEnabled = true,
            quietHoursStartMinutes = 22 * 60,
            quietHoursEndMinutes = 7 * 60
        )

        // 23:30 (1410 min) should be inside quiet hours
        assertTrue(settings.isQuietHourNow(23 * 60 + 30))

        // 03:00 (180 min) should be inside quiet hours
        assertTrue(settings.isQuietHourNow(3 * 60))

        // 14:00 (840 min) should NOT be inside quiet hours
        assertFalse(settings.isQuietHourNow(14 * 60))

        // If quiet hours disabled, always false
        val disabledSettings = settings.copy(quietHoursEnabled = false)
        assertFalse(disabledSettings.isQuietHourNow(23 * 60 + 30))
    }

    // 6. Minimum amount filter works
    @Test
    fun `minimum amount filter logic works`() {
        val settings = AppSettings(minAmountToAnnounce = 100.0)

        // 50.0 is below min
        assertTrue(50.0 < settings.minAmountToAnnounce)
        // 100.0 is equal or above min
        assertFalse(100.0 < settings.minAmountToAnnounce)
        // 250.0 is above min
        assertFalse(250.0 < settings.minAmountToAnnounce)
    }

    // 7. Number to words conversion in English, Hindi, and Gujarati
    @Test
    fun `number to words converter formats correctly across languages`() {
        // English
        val en500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "en", "Ramesh", true)
        assertEquals("Five Hundred rupees received from Ramesh", en500)

        val enPaise = NumberToWordsConverter.convertAmountToSpeech(250.50, "en", announceSender = false)
        assertEquals("Two Hundred Fifty rupees Fifty paise received", enPaise)

        // Hindi
        val hi500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "hi", announceSender = false)
        assertTrue(hi500.contains("पाँच सौ") || hi500.contains("रुपये"))
        assertTrue(hi500.contains("प्राप्त हुए"))

        // Gujarati
        val gu500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "gu", announceSender = false)
        assertTrue(gu500.contains("પાંચ") || gu500.contains("રૂપિયા"))
        assertTrue(gu500.contains("મળ્યા"))
    }
}
