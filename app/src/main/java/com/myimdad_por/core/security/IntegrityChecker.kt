package com.myimdad_por.core.security

import android.content.Context
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import java.security.SecureRandom

/**
 * طبقة تجميع لفحوصات سلامة التطبيق وربطها بـ Google Play Integrity API.
 *
 * ملاحظة مهمة:
 * - هذا الملف يطلب الـ token فقط.
 * - التحقق النهائي من صحة الرد يجب أن يتم على السيرفر.
 */
object IntegrityChecker {

    private const val DEFAULT_NONCE_BYTES = 32
    private val secureRandom = SecureRandom()

    /**
     * يولّد nonce عشوائيًا وفريدًا لكل عملية.
     * يمكن ربطه بمعرّف العملية أو المستخدم على مستوى السيرفر.
     */
    fun generateNonce(): String {
        return generateNonce(DEFAULT_NONCE_BYTES)
    }

    /**
     * يولّد nonce عشوائيًا بطول مخصص.
     */
    fun generateNonce(lengthBytes: Int): String {
        require(lengthBytes > 0) { "lengthBytes must be greater than zero." }

        val bytes = ByteArray(lengthBytes)
        secureRandom.nextBytes(bytes)

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
        )
    }

    /**
     * ينشئ كائن IntegrityManager من سياق التطبيق.
     */
    fun createIntegrityManager(context: Context): IntegrityManager {
        return IntegrityManagerFactory.create(context.applicationContext)
    }

    /**
     * يبني طلب Play Integrity باستخدام nonce ورقم مشروع Google Cloud.
     */
    fun buildIntegrityTokenRequest(
        nonce: String,
        cloudProjectNumber: Long
    ): IntegrityTokenRequest {
        require(nonce.isNotBlank()) { "nonce must not be blank." }
        require(cloudProjectNumber > 0) { "cloudProjectNumber must be greater than zero." }

        return IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(cloudProjectNumber)
            .build()
    }

    /**
     * يطلب Integrity Token من Google Play Integrity API.
     */
    fun requestIntegrityToken(
        context: Context,
        nonce: String,
        cloudProjectNumber: Long
    ): Task<IntegrityTokenResponse> {
        val integrityManager = createIntegrityManager(context)
        val request = buildIntegrityTokenRequest(
            nonce = nonce,
            cloudProjectNumber = cloudProjectNumber
        )
        return integrityManager.requestIntegrityToken(request)
    }

    /**
     * يبني nonce جاهزًا لعملية حساسة، مع إضافة وسم اختياري للتتبّع على مستوى السيرفر.
     * لا تعتمد على هذا الوسم في القرارات الأمنية داخل الجهاز.
     */
    fun generateOperationNonce(
        operationId: String,
        userId: String? = null
    ): String {
        require(operationId.isNotBlank()) { "operationId must not be blank." }

        val randomPart = generateNonce()
        val userPart = userId?.takeIf { it.isNotBlank() } ?: "anonymous"

        return listOf(operationId, userPart, randomPart).joinToString(separator = ".")
    }

    /**
     * يجهّز كل شيء لطلب token واحد في عملية حساسة.
     * يعيد الـ nonce نفسه حتى يُرسل للسيرفر مع الـ token.
     */
    fun prepareIntegrityChallenge(
        operationId: String,
        userId: String? = null
    ): String {
        return generateOperationNonce(operationId = operationId, userId = userId)
    }
}