package com.myimdad_por.data.remote.dto.auth

import com.google.gson.annotations.SerializedName

/**
 * تم تعديل قيم SerializedName لتطابق أسماء الحقول في قاعدة بيانات PostgreSQL 
 * وفي كود Node.js (snake_case).
 */
data class RegisterRequestDto(
    @SerializedName("full_name") 
    val fullName: String,
    
    @SerializedName("email") 
    val email: String,
    
    @SerializedName("username") 
    val username: String? = null,
    
    @SerializedName("password") 
    val password: String,
    
    @SerializedName("confirm_password") 
    val confirmPassword: String,
    
    @SerializedName("role") 
    val role: String,
    
    // حقول المدير
    @SerializedName("store_name") 
    val storeName: String? = null,
    
    @SerializedName("store_location") 
    val storeLocation: String? = null,
    
    // حقول الموظف
    @SerializedName("manager_email") 
    val managerEmail: String? = null,
    
    // حقول إضافية
    @SerializedName("phone_number") 
    val phoneNumber: String? = null
)
