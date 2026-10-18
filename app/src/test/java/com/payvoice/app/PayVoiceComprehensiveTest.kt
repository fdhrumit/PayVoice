package com.payvoice.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payvoice.app.data.preferences.AppSettings
import com.payvoice.app.domain.parser.PaymentParser
import com.payvoice.app.utils.Deduplicator
import com.payvoice.app.utils.NumberToWordsConverter
import com.payvoice.app.utils.PaymentSourceType
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
        val parsed4000 = PaymentParser.parseBankSms("AD-SBIINB-S", msg4000)
        assertNotNull(parsed4000)
        assertEquals(4000.0, parsed4000!!.amount, 0.001)
        assertEquals("4921", parsed4000.accountLast4)

        val msg400 = "Dear Customer, A/c *8812 is credited by INR 400 on 18-Oct-26 by transfer from Ramesh."
        val parsed400 = PaymentParser.parseBankSms("VM-HDFCBK", msg400)
        assertNotNull(parsed400)
        assertEquals(400.0, parsed400!!.amount, 0.001)
        assertEquals("8812", parsed400.accountLast4)

        // ₹1,25,000
        val msg125k = "A/c 1234 credited with ₹1,25,000 via UPI."
        val parsed125k = PaymentParser.parseBankSms("JD-ICICIB-T", msg125k)
        assertNotNull(parsed125k)
        assertEquals(125000.0, parsed125k!!.amount, 0.001)

        // Paise: 250.50
        val msgPaise = "Your account is credited by Rs 250.50. Avl bal is Rs 10,000."
        val parsedPaise = PaymentParser.parseBankSms("VK-KOTAKB", msgPaise)
        assertNotNull(parsedPaise)
        assertEquals(250.50, parsedPaise!!.amount, 0.001)
    }

    // 2. Debits are never announced
    @Test
    fun `debit messages, OTPs, and balance alerts are ignored`() {
        val debit1 = "Your A/c *4921 debited by Rs. 500.00 on 18-Oct-26. Avl Bal Rs. 4,500."
        assertNull(PaymentParser.parseBankSms("AD-SBIINB-S", debit1))

        val debit2 = "Sent Rs. 1,200 to Vijay Medicals from your HDFC Bank account."
        assertNull(PaymentParser.parseBankSms("VM-HDFCBK", debit2))

        val debit3 = "Rs 300 withdrawn from ATM using card ending 1234."
        assertNull(PaymentParser.parseBankSms("JD-ICICIB-T", debit3))

        val otp = "Your secret OTP for transaction of Rs 500 is 492011. Do not share with anyone."
        assertNull(PaymentParser.parseBankSms("AD-SBIINB-S", otp))

        val promo = "Pre-approved loan offer of Rs 5,00,000! Apply now at bank link."
        assertNull(PaymentParser.parseBankSms("AD-SBIINB-S", promo))
    }

    // 3. De-duplication: ONLY cross-source (SMS vs UPI) is duplicate; two SMS or two UPI are separate payments
    @Test
    fun `cross-channel deduplication only deduplicates between SMS and UPI notification`() {
        val amount = 500.0
        val account = "4921"

        // 1st SMS comes in
        assertFalse(
            "First SMS should not be duplicate",
            Deduplicator.isDuplicate(amount, account, PaymentSourceType.SMS)
        )
        Deduplicator.recordPayment(amount, account, PaymentSourceType.SMS)

        // 2nd SMS comes in with same amount (another customer paid 500) -> MUST BE ANNOUNCED!
        assertFalse(
            "Second SMS with same amount is a separate payment and must NOT be duplicate",
            Deduplicator.isDuplicate(amount, account, PaymentSourceType.SMS)
        )
        Deduplicator.recordPayment(amount, account, PaymentSourceType.SMS)

        // Now UPI notification arrives for the same payment (500, account 4921) within 5 mins -> MUST BE DEDUPLICATED!
        assertTrue(
            "UPI notification following SMS for same payment within 5 mins must be detected as duplicate",
            Deduplicator.isDuplicate(amount, account, PaymentSourceType.UPI)
        )
    }

    // 4. Sender IDs: Match operator prefixes and suffixes like VM-HDFCBK, AD-SBIINB-S, JD-ICICIB-T
    @Test
    fun `sender IDs with operator prefixes and suffixes are correctly matched`() {
        assertTrue(PaymentParser.isKnownBankSender("VM-HDFCBK"))
        assertTrue(PaymentParser.isKnownBankSender("AD-SBIINB-S"))
        assertTrue(PaymentParser.isKnownBankSender("JD-ICICIB-T"))
        assertTrue(PaymentParser.isKnownBankSender("VK-KOTAKB"))
        assertTrue(PaymentParser.isKnownBankSender("AX-BOBTXN"))
        assertTrue(PaymentParser.isKnownBankSender("JX-PNBSMS"))

        // Unknown senders must be rejected
        assertFalse(PaymentParser.isKnownBankSender("LM-FAKEWIN"))
        assertFalse(PaymentParser.isKnownBankSender("+919876543210"))
        assertFalse(PaymentParser.isKnownBankSender("PROMOALERT"))
    }

    // 5. Merchant UPI notification packages
    @Test
    fun `merchant UPI apps are recognized and parsed`() {
        // PhonePe Business
        assertTrue(PaymentParser.isKnownUpiPackage("com.phonepe.app.business"))
        val parsedPhonePe = PaymentParser.parseUpiNotification(
            "com.phonepe.app.business",
            "PhonePe Business",
            "Received ₹750 from Suresh Patel"
        )
        assertNotNull(parsedPhonePe)
        assertEquals(750.0, parsedPhonePe!!.amount, 0.001)

        // Paytm for Business
        assertTrue(PaymentParser.isKnownUpiPackage("com.paytm.business"))
        val parsedPaytm = PaymentParser.parseUpiNotification(
            "com.paytm.business",
            "Paytm for Business",
            "Payment of ₹1,200 received successfully from Rajesh"
        )
        assertNotNull(parsedPaytm)
        assertEquals(1200.0, parsedPaytm!!.amount, 0.001)

        // BharatPe
        assertTrue(PaymentParser.isKnownUpiPackage("com.bharatpe.app"))
        val parsedBharatPe = PaymentParser.parseUpiNotification(
            "com.bharatpe.app",
            "BharatPe",
            "₹500 received via BharatPe QR from Customer"
        )
        assertNotNull(parsedBharatPe)
        assertEquals(500.0, parsedBharatPe!!.amount, 0.001)

        // Google Pay for Business
        assertTrue(PaymentParser.isKnownUpiPackage("com.google.android.apps.nbu.paisa.merchant"))
        val parsedGPayBiz = PaymentParser.parseUpiNotification(
            "com.google.android.apps.nbu.paisa.merchant",
            "Google Pay for Business",
            "You received ₹350.00 from Amit Shah"
        )
        assertNotNull(parsedGPayBiz)
        assertEquals(350.0, parsedGPayBiz!!.amount, 0.001)
    }

    // 6. Quiet hours block speech
    @Test
    fun `quiet hours block speech correctly`() {
        val settings = AppSettings(
            quietHoursEnabled = true,
            quietHoursStartMinutes = 22 * 60,
            quietHoursEndMinutes = 7 * 60
        )

        assertTrue(settings.isQuietHourNow(23 * 60 + 30))
        assertTrue(settings.isQuietHourNow(3 * 60))
        assertFalse(settings.isQuietHourNow(14 * 60))

        val disabledSettings = settings.copy(quietHoursEnabled = false)
        assertFalse(disabledSettings.isQuietHourNow(23 * 60 + 30))
    }

    // 7. Minimum amount filter works
    @Test
    fun `minimum amount filter logic works`() {
        val settings = AppSettings(minAmountToAnnounce = 100.0)
        assertTrue(50.0 < settings.minAmountToAnnounce)
        assertFalse(100.0 < settings.minAmountToAnnounce)
        assertFalse(250.0 < settings.minAmountToAnnounce)
    }

    // 8. Number to words conversion
    @Test
    fun `number to words converter formats correctly across languages`() {
        val en500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "en", "Ramesh", true)
        assertEquals("Five Hundred rupees received from Ramesh", en500)

        val enPaise = NumberToWordsConverter.convertAmountToSpeech(250.50, "en", announceSender = false)
        assertEquals("Two Hundred Fifty rupees Fifty paise received", enPaise)

        val hi500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "hi", announceSender = false)
        assertTrue(hi500.contains("पाँच सौ") || hi500.contains("रुपये"))
        assertTrue(hi500.contains("प्राप्त हुए"))

        val gu500 = NumberToWordsConverter.convertAmountToSpeech(500.0, "gu", announceSender = false)
        assertTrue(gu500.contains("પાંચ") || gu500.contains("રૂપિયા"))
        assertTrue(gu500.contains("મળ્યા"))
    }
}
