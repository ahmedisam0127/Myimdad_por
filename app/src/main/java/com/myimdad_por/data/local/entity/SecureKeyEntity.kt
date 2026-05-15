package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.util.UUID

@Entity(
    tableName = "secure_keys",
    indices = [
        Index(value = ["alias"], unique = true),
        Index(value = ["key_type"]),
        Index(value = ["algorithm"]),
        Index(value = ["sync_state"]),
        Index(value = ["is_active"])
    ]
)
data class SecureKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "alias")
    val alias: String,

    @ColumnInfo(name = "key_type")
    val keyType: String,

    @ColumnInfo(name = "algorithm")
    val algorithm: String,

    @ColumnInfo(name = "key_size_bits")
    val keySizeBits: Int = 256,

    @ColumnInfo(name = "purpose")
    val purpose: String? = null,

    @ColumnInfo(name = "provider")
    val provider: String? = null,

    @ColumnInfo(name = "is_hardware_backed")
    val isHardwareBacked: Boolean = false,

    @ColumnInfo(name = "requires_user_auth")
    val requiresUserAuth: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "last_used_at_millis")
    val lastUsedAtMillis: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(alias.isNotBlank()) { "alias cannot be blank." }
        require(keyType.isNotBlank()) { "keyType cannot be blank." }
        require(algorithm.isNotBlank()) { "algorithm cannot be blank." }
        require(keySizeBits > 0) { "keySizeBits must be greater than zero." }
    }

    fun markUsed(nowMillis: Long = System.currentTimeMillis()): SecureKeyEntity {
        return copy(
            lastUsedAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
    }

    fun deactivate(nowMillis: Long = System.currentTimeMillis()): SecureKeyEntity {
        return copy(
            isActive = false,
            updatedAtMillis = nowMillis
        )
    }

    fun activate(nowMillis: Long = System.currentTimeMillis()): SecureKeyEntity {
        return copy(
            isActive = true,
            updatedAtMillis = nowMillis
        )
    }

    companion object {
        fun create(
            alias: String,
            keyType: String,
            algorithm: String,
            keySizeBits: Int = 256,
            purpose: String? = null,
            provider: String? = null,
            isHardwareBacked: Boolean = false,
            requiresUserAuth: Boolean = false,
            metadata: Map<String, String> = emptyMap()
        ): SecureKeyEntity {
            return SecureKeyEntity(
                alias = alias,
                keyType = keyType,
                algorithm = algorithm,
                keySizeBits = keySizeBits,
                purpose = purpose,
                provider = provider,
                isHardwareBacked = isHardwareBacked,
                requiresUserAuth = requiresUserAuth,
                metadataJson = metadata.toJsonString()
            )
        }
    }
}

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}