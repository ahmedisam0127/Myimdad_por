import os
import re

def get_clean_content(file_path):
    """يقرأ الملف، يحذف التعليقات (// و /* */) والأسطر الفارغة، ويعيد النص مضغوطاً"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 1. إزالة التعليقات متعددة الأسطر /* ... */
        content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
        
        # 2. إزالة التعليقات السطرية //
        content = re.sub(r'//.*', '', content)
        
        # 3. تنظيف الأسطر: حذف الفراغات الزائدة والأسطر الفارغة تماماً
        clean_lines = []
        for line in content.splitlines():
            stripped = line.strip()
            if stripped:  # إذا كان السطر يحتوي على كود فعلي بعد الحذف
                clean_lines.append(line.rstrip())
                
        return "\n".join(clean_lines)
    except Exception as e:
        return f"--- تعذر قراءة الملف: {e} ---"

def main():
    # القائمة المطلوبة من الملفات
    files_to_find = "  DashboardUiState.kt, DashboardUiEvent.kt, AppTopBar.kt, LoadingIndicator.kt, ErrorState.kt, SubscriptionWarningBanner.kt, ScreenRoutes.kt, NavigationActions.kt, Color.kt, Dimens.kt, Theme.kt "
    target_filenames = {name.strip() for name in files_to_find.replace('"', '').split(',')}
    
    output_filename = "Ahme_ed"
    # المسار المحدد الذي طلبته
    search_path = "/storage/emulated/0/AndroidIDEProjects/Myimdad_por/app/src/main/java/com/myimdad_por"
    
    found_count = 0

    # التأكد من وجود المسار قبل البدء
    if not os.path.exists(search_path):
        print(f"❌ المسار غير موجود: {search_path}")
        return

    print(f"🔍 جاري البحث في: {search_path}")
    print(f"📝 سيتم حفظ النتيجة في: {output_filename}")

    with open(output_filename, 'w', encoding='utf-8') as outfile:
        outfile.write(f"=== PROJECT FILES SUMMARY (MINIMIZED) ===\n")
        outfile.write(f"Search Path: {search_path}\n\n")

        for root, dirs, files in os.walk(search_path):
            # استبعاد مجلدات البناء والمجلدات المخفية
            dirs[:] = [d for d in dirs if not (d == 'build' or d.startswith('.'))]
            
            for file in files:
                if file in target_filenames:
                    found_count += 1
                    full_path = os.path.join(root, file)
                    content = get_clean_content(full_path)

                    outfile.write("-" * 30 + "\n")
                    outfile.write(f"FILE: {file}\n")
                    outfile.write("-" * 30 + "\n")
                    outfile.write(content + "\n\n")
                    print(f"✅ تم معالجة: {file}")

    if found_count > 0:
        print(f"\n✨ تمت العملية بنجاح!")
        print(f"📊 تم العثور على {found_count} ملفات وتجميعها في: {os.path.abspath(output_filename)}")
    else:
        print("\n⚠️ لم يتم العثور على أي ملفات من القائمة في المسار المحدد.")

if __name__ == "__main__":
    main()
