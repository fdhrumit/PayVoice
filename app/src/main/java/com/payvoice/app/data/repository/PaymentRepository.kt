package com.payvoice.app.data.repository

import com.payvoice.app.data.local.dao.BankAccountDao
import com.payvoice.app.data.local.dao.PaymentDao
import com.payvoice.app.data.local.entity.BankAccountEntity
import com.payvoice.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val bankAccountDao: BankAccountDao
) {
    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPaymentsFlow()

    fun getTodayPayments(): Flow<List<PaymentEntity>> {
        val startOfDay = getStartOfDayMillis()
        return paymentDao.getTodayPaymentsFlow(startOfDay)
    }

    fun getPaymentsBetween(startTime: Long, endTime: Long): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsBetweenFlow(startTime, endTime)
    }

    fun getLatestPayment(): Flow<PaymentEntity?> = paymentDao.getLatestPaymentFlow()

    suspend fun insertPayment(payment: PaymentEntity): Long = paymentDao.insertPayment(payment)

    suspend fun updatePayment(payment: PaymentEntity) = paymentDao.updatePayment(payment)

    suspend fun deletePayment(id: Long) = paymentDao.deletePaymentById(id)

    suspend fun clearAll() = paymentDao.clearAllPayments()

    // Bank accounts
    fun getAllAccounts(): Flow<List<BankAccountEntity>> = bankAccountDao.getAllAccountsFlow()

    fun getEnabledAccounts(): Flow<List<BankAccountEntity>> = bankAccountDao.getEnabledAccountsFlow()

    suspend fun insertAccount(bankName: String, last4: String): Long {
        return bankAccountDao.insertAccount(
            BankAccountEntity(
                bankName = bankName.trim(),
                last4Digits = last4.trim(),
                isEnabled = true
            )
        )
    }

    suspend fun updateAccount(account: BankAccountEntity) = bankAccountDao.updateAccount(account)

    suspend fun deleteAccount(id: Long) = bankAccountDao.deleteAccountById(id)

    suspend fun getAccountCount(): Int = bankAccountDao.getAccountCount()

    companion object {
        fun getStartOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
