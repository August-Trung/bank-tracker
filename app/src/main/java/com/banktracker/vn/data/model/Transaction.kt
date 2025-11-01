package com.banktracker.vn.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val bankName: String,
    val bankCode: String,
    val amount: Double,
    val transactionType: TransactionType,
    val content: String,
    val balance: Double?,
    val timestamp: Long = System.currentTimeMillis(),
    val notificationText: String,
    val isRead: Boolean = false
) : Parcelable

enum class BankCode(
    val code: String,
    val fullName: String,
    val packageNames: List<String>
) {
    // Big 4 Banks
    MB(
        "MB",
        "MB Bank",
        listOf("com.mbmobile", "com.mbbank")
    ),
    VCB(
        "VCB",
        "Vietcombank",
        listOf("com.VCB", "com.vietcombank")
    ),
    BIDV(
        "BIDV",
        "BIDV",
        listOf("com.vnpay.bidv", "com.bidv.bank")
    ),
    VIETIN(
        "VIETIN",
        "VietinBank",
        listOf("com.vietinbank.ipay", "com.vietinbank")
    ),

    // Top Banks
    TCB(
        "TCB",
        "Techcombank",
        listOf("com.techcombank.bb.app", "com.techcombank")
    ),
    ACB(
        "ACB",
        "ACB",
        listOf("mobile.acb.com.vn", "com.acb.android")
    ),
    AGRI(
        "AGRI",
        "Agribank",
        listOf("com.vnpay.Agribank3g", "com.agribank")
    ),
    VP(
        "VP",
        "VPBank",
        listOf("com.vnpay.vpbankonline", "com.vpbank")
    ),

    // Private Banks
    SCB(
        "SCB",
        "Sacombank",
        listOf("sacombank.mbanking", "com.sacombank")
    ),
    TPB(
        "TPB",
        "TPBank",
        listOf("com.tpb.mb.gprsandroid", "com.tpbank")
    ),
    MSB(
        "MSB",
        "MSB - Maritime Bank",
        listOf("com.msb.mbanking", "com.msb")
    ),
    MB_SHINHAN(
        "SHINHAN",
        "Shinhan Bank",
        listOf("com.shb.mbanking", "com.shinhan")
    ),
    EIB(
        "EIB",
        "Eximbank",
        listOf("com.eximbank.mbanking", "com.eib")
    ),
    OCB(
        "OCB",
        "OCB - Orient Commercial Bank",
        listOf("com.ocb.mbanking", "com.ocb")
    ),
    HDBank(
        "HDBANK",
        "HDBank",
        listOf("com.hdbank.mbanking", "com.hdbank")
    ),
    SHB(
        "SHB",
        "SHB - Saigon-Hanoi Bank",
        listOf("com.shb.mbanking", "com.shbbank")
    ),
    NAM_A(
        "NAMA",
        "Nam A Bank",
        listOf("com.namabank.mbanking", "com.namabank")
    ),
    VIB(
        "VIB",
        "VIB - Vietnam International Bank",
        listOf("com.vib.mbanking", "com.vib")
    ),

    // State Banks
    VBSP(
        "VBSP",
        "Vietnam Bank for Social Policies",
        listOf("com.vbsp.mbanking")
    ),

    // Foreign Banks
    STANDARD_CHARTERED(
        "SCB_INT",
        "Standard Chartered Vietnam",
        listOf("com.scb.breezebanking.vn")
    ),
    HSBC(
        "HSBC",
        "HSBC Vietnam",
        listOf("com.htsu.hsbcvn")
    ),
    CITI(
        "CITI",
        "Citibank Vietnam",
        listOf("com.citi.citimobile")
    ),

    // Digital Banks / Fintech
    TIMO(
        "TIMO",
        "Timo Digital Bank",
        listOf("com.timo.vn")
    ),
    CAKE(
        "CAKE",
        "CAKE by VPBank",
        listOf("vn.cake.app")
    ),
    UBANK(
        "UBANK",
        "Ubank by VIB",
        listOf("com.vib.ubank")
    ),

    // Co-op Banks
    COOP_BANK(
        "COOPBANK",
        "Co-opBank",
        listOf("com.coopbank.mbanking")
    ),

    // Regional Banks
    BAOVIET(
        "BAOVIET",
        "BaoViet Bank",
        listOf("com.baovietbank.mbanking")
    ),
    DAB(
        "DAB",
        "DongA Bank",
        listOf("com.dongabank.mbanking")
    ),
    SEABANK(
        "SEABANK",
        "SeABank",
        listOf("com.seabank.mbanking")
    ),
    PG_BANK(
        "PGBANK",
        "PG Bank - Petrolimex Group",
        listOf("com.pgbank.mbanking")
    ),
    VIET_A(
        "VIETA",
        "VietABank",
        listOf("com.vietabank.mbanking")
    ),
    VIET_BANK(
        "VIETBANK",
        "VietBank",
        listOf("com.vietbank.mbanking")
    ),
    ABBANK(
        "ABBANK",
        "ABBank - An Binh Bank",
        listOf("com.abbank.mbanking")
    ),
    BAC_A(
        "BACA",
        "BacABank",
        listOf("com.bacabank.mbanking")
    ),
    KIENLONGBANK(
        "KIENLONGBANK",
        "KienLongBank",
        listOf("com.kienlongbank.mbanking")
    ),
    NCB(
        "NCB",
        "NCB - National Citizen Bank",
        listOf("com.ncb.mbanking")
    ),
    LIENVIET(
        "LIENVIET",
        "LienVietPostBank",
        listOf("com.lienvietpostbank.mbanking")
    ),
    PUBLIC_BANK(
        "PUBLICBANK",
        "PublicBank Vietnam",
        listOf("com.publicbank.mbanking")
    ),
    GPBANK(
        "GPBANK",
        "GPBank",
        listOf("com.gpbank.mbanking")
    ),
    VIET_CAPITAL(
        "VIETCAPITAL",
        "VietCapital Bank",
        listOf("com.vietcapitalbank.mbanking")
    ),
    OCEANBANK(
        "OCEANBANK",
        "OceanBank",
        listOf("com.oceanbank.mbanking")
    ),

    // UNKNOWN - Cho các ngân hàng chưa hỗ trợ
    UNKNOWN(
        "UNKNOWN",
        "Ngân hàng khác",
        listOf()
    );

    companion object {
        fun fromPackageName(packageName: String): BankCode? {
            // Tìm bank theo package name
            val bank = values().find { bank ->
                bank.packageNames.any { it.equals(packageName, ignoreCase = true) }
            }

            // Nếu không tìm thấy, kiểm tra có chứa từ khóa ngân hàng không
            if (bank == null && containsBankKeyword(packageName)) {
                return UNKNOWN
            }

            return bank
        }

        fun fromCode(code: String): BankCode? {
            return values().find { it.code.equals(code, ignoreCase = true) }
        }

        // Kiểm tra package name có phải app ngân hàng không
        private fun containsBankKeyword(packageName: String): Boolean {
            val keywords = listOf(
                "bank", "mbanking", "ebanking", "mobile.bank",
                "vnpay", "vietqr", "napas"
            )
            return keywords.any { packageName.contains(it, ignoreCase = true) }
        }
    }
}

data class TransactionSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val transactionCount: Int,
    val largestTransaction: Double
)

data class BankSummary(
    val bankCode: String,
    val bankName: String,
    val totalIncome: Double,
    val transactionCount: Int
)