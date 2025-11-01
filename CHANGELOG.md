# سجل التغييرات

## [الإصدار الحالي] - 2025-07-27

### مُحسَّن

- **أداء واجهة المستخدم:** تم نقل عمليات تحميل البيانات إلى خيط خلفي لمنع تجميد واجهة المستخدم في الشاشات التالية:
  - شاشة المخزون (`InventoryController`)
  - شاشة دفتر اليومية (`JournalViewController`)
  - شاشة إدارة جهات التعامل (`ContactManagementController`)
  - شاشة إدارة المحاصيل (`CropManagementController`)
  - شاشة إدارة الحسابات المالية (`FinancialAccountManagementController`)
  - شاشة الميزانية العمومية (`BalanceSheetController`)
  - شاشة قائمة الدخل (`IncomeStatementController`)
  - شاشة ميزان المراجعة (`TrialBalanceController`)
  - شاشة بيان حقوق الملكية (`EquityStatementController`)
  - شاشة دفتر الأستاذ المالي (`FinancialLedgerController`)
- **تجربة المستخدم:** تمت إضافة مؤشرات تقدم مرئية (Spinners) تظهر أثناء تحميل البيانات في الشاشات المحسّنة.
- **معالجة الأخطاء:** يتم الآن عرض رسالة خطأ واضحة للمستخدم في حالة فشل تحميل البيانات من قاعدة البيانات.
