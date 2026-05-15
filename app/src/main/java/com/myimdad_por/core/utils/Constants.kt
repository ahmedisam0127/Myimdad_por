package com.myimdad_por.core.utils

/**
 * ثوابت التطبيق العامة
 */
object Constants {

    object App {
        const val APP_NAME = "MyImdad Por"
        const val PACKAGE_NAME = "com.myimdad_por"
        const val DEFAULT_ANIMATION_DURATION = 300L
    }

    object Network {
        // الرابط الجديد الذي طلبته
        const val BASE_URL = "https://imdad-api.onrender.com"

        
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 30L
        const val WRITE_TIMEOUT_SECONDS = 30L
        const val CALL_TIMEOUT_SECONDS = 60L
    }

    object Database {
        const val DATABASE_NAME = "myimdad_por_db"
    }

    object Prefs {
        const val PREFS_NAME = "myimdad_por_prefs"
        const val KEY_AUTH_TOKEN = "key_auth_token"
        const val KEY_REFRESH_TOKEN = "key_refresh_token"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    }

    object Validation {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 15
        const val MAX_EMAIL_LENGTH = 254
    }

    object Pagination {
        const val FIRST_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
    }

    object Bundle {
        const val KEY_ID = "key_id"
        const val KEY_NAME = "key_name"
        const val KEY_TITLE = "key_title"
    }

    object Ui {
        const val EMPTY_TEXT = ""
        const val DEFAULT_ERROR_MESSAGE = "حدث خطأ غير متوقع"
        const val DEFAULT_SUCCESS_MESSAGE = "تمت العملية بنجاح"
    }
}
