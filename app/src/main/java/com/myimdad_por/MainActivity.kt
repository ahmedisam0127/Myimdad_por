package com.myimdad_por

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.myimdad_por.ui.ImdadPorRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * النشاط الرئيسي للتطبيق (Entry Point).
 * تم وضع علامة @AndroidEntryPoint لتمكين Hilt من حقن التبعيات داخل الأنشطة والـ Composables.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. تفعيل وضع Edge-to-Edge لتجربة بصرية غامرة (خلفية شفافة لأشرطة النظام)
        enableEdgeToEdge()

        // 2. ضمان أن المحتوى يظهر بشكل صحيح تحت أشرطة النظام (Status & Navigation bars)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // 3. استدعاء الحاوية الجذرية للتطبيق التي تحتوي على الثيم والملاحة
            ImdadPorRoot()
        }
    }
}
