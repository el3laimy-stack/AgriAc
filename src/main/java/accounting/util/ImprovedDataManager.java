package accounting.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import accounting.formatter.FormatUtils;

/**
 * مدير قاعدة البيانات المحسن مع تجميع الاتصالات وإدارة المعاملات
 */
public class ImprovedDataManager {

    private static final Logger LOGGER = Logger.getLogger(ImprovedDataManager.class.getName());

    private static HikariDataSource dataSource;
    private static ImprovedDataManager instance;

    private ImprovedDataManager() {
        // Private constructor to prevent instantiation
        initialize();
    }

    /**
     * الحصول على مثيل وحيد من مدير قاعدة البيانات
     */
    public static synchronized ImprovedDataManager getInstance() {
        if (instance == null) {
            instance = new ImprovedDataManager();
        }
        return instance;
    }

    /**
     * Initializes the database pool and creates the schema.
     */
    private void initialize() {
        // If dataSource is already configured (by a test), don't re-configure it.
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Using pre-configured DataSource.");
            // Ensure schema is created on the pre-configured datasource
            try (Connection conn = getConnection()) {
                createTables(conn);
                createIndexes(conn);
                createDefaultAccounts(conn);
            } catch (SQLException e) {
                ErrorHandler.showError("Database Error", "Failed to create schema on pre-configured database", e.getMessage(), e);
                throw new RuntimeException("Failed to create schema on pre-configured database", e);
            }
            return;
        }

        try {
            Properties props = new Properties();
            try (InputStream input = ImprovedDataManager.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    ErrorHandler.showError("Error", "Configuration file not found", "Could not find config.properties in the classpath", null);
                    return;
                }
                props.load(input);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url", "jdbc:sqlite:agricultural_accounting.db"));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setLeakDetectionThreshold(15000);
            config.setConnectionTimeout(30000);

            dataSource = new HikariDataSource(config);

            try (Connection conn = getConnection()) {
                createTables(conn);
                createIndexes(conn);
                createDefaultAccounts(conn);
            }
            LOGGER.info("تم تهيئة قاعدة البيانات بنجاح");
        } catch (Exception e) {
            ErrorHandler.showError("Database Error", "Failed to initialize database", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * الحصول على اتصال من التجميع
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * إغلاق جميع الاتصالات
     */
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
        LOGGER.info("تم إغلاق مدير قاعدة البيانات");
    }
    
    /**
     * إنشاء جداول قاعدة البيانات المحسنة
     */
    public void createTables(Connection conn) throws SQLException {
        String[] createTableQueries = {
            // جدول المحاصيل المحسن
            """
            CREATE TABLE IF NOT EXISTS crops (
                crop_id INTEGER PRIMARY KEY AUTOINCREMENT,
                crop_name TEXT NOT NULL UNIQUE,
                allowed_pricing_units TEXT NOT NULL,
                conversion_factors TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1
            )
            """,
            
            // جدول جهات التعامل المحسن
            """
            CREATE TABLE IF NOT EXISTS contacts (
                contact_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                address TEXT,
                email TEXT,
                tax_number TEXT,
                is_supplier BOOLEAN DEFAULT 0,
                is_customer BOOLEAN DEFAULT 0,
                credit_limit REAL DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1
            )
            """,
            
            // جدول الحسابات المالية المحسن
            """
            CREATE TABLE IF NOT EXISTS financial_accounts (
                account_id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_name TEXT NOT NULL UNIQUE,
                account_type TEXT NOT NULL CHECK (account_type IN ('CASH', 'BANK', 'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE', 'HEADER', 'CURRENT_ASSET', 'ACCOUNTS_RECEIVABLE', 'ACCOUNTS_PAYABLE')),
                account_number TEXT,
                bank_name TEXT,
                opening_balance REAL DEFAULT 0,
                opening_balance_date DATE,
                current_balance REAL DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1
            )
            """,
            
            // جدول المشتريات المحسن
            """
            CREATE TABLE IF NOT EXISTS purchases (
            		purchase_id INTEGER PRIMARY KEY AUTOINCREMENT,
            		crop_id INTEGER NOT NULL,
            		supplier_id INTEGER NOT NULL,
            		purchase_date DATE NOT NULL,
            		quantity_kg REAL NOT NULL CHECK (quantity_kg > 0),
            		pricing_unit TEXT NOT NULL,
            		specific_factor REAL NOT NULL CHECK (specific_factor > 0),
            		unit_price REAL NOT NULL CHECK (unit_price > 0),
            		total_cost REAL NOT NULL CHECK (total_cost > 0),
            		amount_paid REAL DEFAULT 0, -- << تم إضافة هذا الحقل
            		payment_status TEXT DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PARTIAL', 'PAID')), -- << تم تحديث هذا الحقل
            		invoice_number TEXT,
            		notes TEXT,
            		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            		FOREIGN KEY (crop_id) REFERENCES crops (crop_id),
            		FOREIGN KEY (supplier_id) REFERENCES contacts (contact_id)
            )
            """,
            
            // جدول المبيعات المحسن
            """
            CREATE TABLE IF NOT EXISTS sales (
            		sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
            		customer_id INTEGER NOT NULL,
            		crop_id INTEGER NOT NULL,
            		sale_date DATE NOT NULL,
            		quantity_sold_kg REAL NOT NULL CHECK (quantity_sold_kg > 0),
            		selling_pricing_unit TEXT NOT NULL,
            		specific_selling_factor REAL NOT NULL CHECK (specific_selling_factor > 0),
            		selling_unit_price REAL NOT NULL CHECK (selling_unit_price > 0),
            		total_sale_amount REAL NOT NULL CHECK (total_sale_amount > 0),
            		amount_paid REAL DEFAULT 0, -- << تم إضافة هذا الحقل
            		payment_status TEXT DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PARTIAL', 'PAID')), -- << تم تحديث هذا الحقل
            		sale_invoice_number TEXT,
            		notes TEXT,
            		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            		FOREIGN KEY (customer_id) REFERENCES contacts (contact_id),
            		FOREIGN KEY (crop_id) REFERENCES crops (crop_id)
            )
            """,
            
            // جدول الحركات المالية المحسن
            """
            CREATE TABLE IF NOT EXISTS financial_transactions (
                transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL,
                transaction_date DATE NOT NULL,
                transaction_type TEXT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                related_contact_id INTEGER,
                related_purchase_id INTEGER,
                related_sale_id INTEGER,
                reference_number TEXT,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (account_id) REFERENCES financial_accounts (account_id),
                FOREIGN KEY (related_contact_id) REFERENCES contacts (contact_id),
                FOREIGN KEY (related_purchase_id) REFERENCES purchases (purchase_id),
                FOREIGN KEY (related_sale_id) REFERENCES sales (sale_id)
            )
            """,
            
            // جدول المخزون المحسن
            """
            CREATE TABLE IF NOT EXISTS inventory (
                inventory_id INTEGER PRIMARY KEY AUTOINCREMENT,
                crop_id INTEGER NOT NULL,
                current_stock_kg REAL DEFAULT 0 CHECK (current_stock_kg >= 0),
                reserved_stock_kg REAL DEFAULT 0 CHECK (reserved_stock_kg >= 0),
                average_cost_per_kg REAL DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (crop_id) REFERENCES crops (crop_id),
                UNIQUE(crop_id)
            )
            """,
            """
			CREATE TABLE IF NOT EXISTS inventory_adjustments (
			    adjustment_id INTEGER PRIMARY KEY AUTOINCREMENT,
			    crop_id INTEGER NOT NULL,
			    adjustment_date DATE NOT NULL,
			    adjustment_type TEXT NOT NULL CHECK (adjustment_type IN ('DAMAGE', 'SHORTAGE', 'SURPLUS')),
			    quantity_kg REAL NOT NULL,
			    reason TEXT,
			    cost REAL NOT NULL,
			    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			    FOREIGN KEY (crop_id) REFERENCES crops (crop_id)
			)
			""",
            
            // جدول حركات المخزون
            """
            CREATE TABLE IF NOT EXISTS inventory_movements (
                movement_id INTEGER PRIMARY KEY AUTOINCREMENT,
                crop_id INTEGER NOT NULL,
                movement_type TEXT NOT NULL CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
                quantity_kg REAL NOT NULL,
                unit_cost REAL,
                reference_type TEXT,
                reference_id INTEGER,
                movement_date DATE NOT NULL,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (crop_id) REFERENCES crops (crop_id)
            )
            """,
            """
	            CREATE TABLE IF NOT EXISTS purchase_returns (
	            return_id INTEGER PRIMARY KEY AUTOINCREMENT,
	            original_purchase_id INTEGER NOT NULL,
	            return_date DATE NOT NULL,
	            crop_id INTEGER NOT NULL,
	           quantity_kg REAL NOT NULL,
	    return_reason TEXT,
	    returned_cost REAL NOT NULL,
	    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	    FOREIGN KEY (original_purchase_id) REFERENCES purchases (purchase_id),
	    FOREIGN KEY (crop_id) REFERENCES crops (crop_id)
            )
            """,
            """
			CREATE TABLE IF NOT EXISTS sale_returns (
			    return_id INTEGER PRIMARY KEY AUTOINCREMENT,
			    original_sale_id INTEGER NOT NULL,
			    return_date DATE NOT NULL,
			    crop_id INTEGER NOT NULL,
			    quantity_kg REAL NOT NULL,
			    return_reason TEXT,
			    refund_amount REAL NOT NULL,
			    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			    FOREIGN KEY (original_sale_id) REFERENCES sales (sale_id),
			    FOREIGN KEY (crop_id) REFERENCES crops (crop_id)
			)
			""",
            
            // جدول سجل التدقيق
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                table_name TEXT NOT NULL,
                record_id INTEGER NOT NULL,
                operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
                old_values TEXT,
                new_values TEXT,
                user_name TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS general_ledger (
                entry_id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_ref TEXT NOT NULL,
                entry_date DATE NOT NULL,
                account_id INTEGER NOT NULL,
                debit REAL DEFAULT 0,
                credit REAL DEFAULT 0,
                description TEXT,
                source_type TEXT,
                source_id INTEGER,
            is_deleted INTEGER DEFAULT 0,
                transaction_type TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (account_id) REFERENCES financial_accounts (account_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS seasons (
                season_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED')),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // جدول الأسعار اليومية - مخصص لشركات تجارة المحاصيل
            """
            CREATE TABLE IF NOT EXISTS daily_prices (
                price_id INTEGER PRIMARY KEY AUTOINCREMENT,
                crop_id INTEGER NOT NULL,
                price_date DATE NOT NULL,
                opening_price REAL NOT NULL CHECK (opening_price > 0),
                high_price REAL NOT NULL CHECK (high_price > 0),
                low_price REAL NOT NULL CHECK (low_price > 0),
                closing_price REAL NOT NULL CHECK (closing_price > 0),
                average_price REAL NOT NULL CHECK (average_price > 0),
                trading_volume REAL DEFAULT 0 CHECK (trading_volume >= 0),
                market_condition TEXT,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (crop_id) REFERENCES crops (crop_id),
                UNIQUE(crop_id, price_date)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS payments (
                payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                payment_date DATE NOT NULL,
                contact_id INTEGER NOT NULL,
                payment_account_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                payment_type TEXT NOT NULL CHECK (payment_type IN ('PAY', 'RECEIVE')),
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (contact_id) REFERENCES contacts (contact_id),
                FOREIGN KEY (payment_account_id) REFERENCES financial_accounts (account_id)
            )
            """
            
        };
        
        for (String query : createTableQueries) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(query);
            }
        }
        
        // Add columns if they don't exist for backward compatibility
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE general_ledger ADD COLUMN source_type TEXT;");
            stmt.execute("ALTER TABLE general_ledger ADD COLUMN source_id INTEGER;");
            stmt.execute("ALTER TABLE general_ledger ADD COLUMN transaction_type TEXT;");
            stmt.execute("ALTER TABLE financial_transactions ADD COLUMN season_id INTEGER REFERENCES seasons(season_id);");
            stmt.execute("ALTER TABLE purchases ADD COLUMN season_id INTEGER REFERENCES seasons(season_id);");
            stmt.execute("ALTER TABLE sales ADD COLUMN season_id INTEGER REFERENCES seasons(season_id);");
            stmt.execute("ALTER TABLE inventory_adjustments ADD COLUMN season_id INTEGER REFERENCES seasons(season_id);");
        } catch (SQLException e) {
            // Ignore "duplicate column name" error, which is expected if the columns already exist.
            if (!e.getMessage().contains("duplicate column name")) {
                throw e;
            }
        }
    }
    
    /**
     * إنشاء المؤشرات لتحسين الأداء
     */
    private void createIndexes(Connection conn) throws SQLException {
        String[] indexQueries = {
            "CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases (purchase_date)",
            "CREATE INDEX IF NOT EXISTS idx_purchases_crop ON purchases (crop_id)",
            "CREATE INDEX IF NOT EXISTS idx_purchases_supplier ON purchases (supplier_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_date ON sales (sale_date)",
            "CREATE INDEX IF NOT EXISTS idx_sales_crop ON sales (crop_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_customer ON sales (customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_date ON financial_transactions (transaction_date)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_account ON financial_transactions (account_id)",
            "CREATE INDEX IF NOT EXISTS idx_inventory_movements_date ON inventory_movements (movement_date)",
            "CREATE INDEX IF NOT EXISTS idx_inventory_movements_crop ON inventory_movements (crop_id)",
            "CREATE INDEX IF NOT EXISTS idx_audit_log_table_record ON audit_log (table_name, record_id)",
            "CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log (timestamp)",
            "CREATE INDEX IF NOT EXISTS idx_gl_source ON general_ledger (source_type, source_id)"
        };
        
        for (String query : indexQueries) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(query);
            }
        }
    }
    
    /**
     * تنفيذ معاملة قاعدة بيانات مع إدارة تلقائية للمعاملات
     */
    public <T> T executeTransaction(DatabaseTransaction<T> transaction) throws SQLException {
        try (Connection conn = getConnection()){
            conn.setAutoCommit(false);
            try {
                T result = transaction.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Transaction failed due to unexpected exception", e);
            }
        } 
    }
    
    /**
     * واجهة للمعاملات
     * 
     * تُستخدم هذه الواجهة لتحديد العمليات التي يجب تنفيذها ضمن معاملة قاعدة بيانات.
     * يجب أن تُنفذ جميع عمليات قاعدة البيانات المرتبطة ببعضها البعض ضمن كتلة معاملة واحدة
     * لضمان استمرارية البيانات.
     * 
     * @param <T> نوع القيمة المرتجعة من المعاملة
     */
    @FunctionalInterface
    public interface DatabaseTransaction<T> {
        /**
         * تنفيذ العمليات ضمن معاملة قاعدة بيانات
         * 
         * @param conn اتصال قاعدة البيانات الذي سيتم استخدامه في المعاملة
         * @return نتيجة المعاملة
         * @throws SQLException في حالة حدوث خطأ في قاعدة البيانات
         */
        T execute(Connection conn) throws SQLException;
    }
    
    
    /**
     * تسجيل عملية في سجل التدقيق
     * تم تعديلها لتقبل اتصال موجود لمنع قفل قاعدة البيانات
     */
    public void logAuditEntry(String tableName, int recordId, String operation, 
                             String oldValues, String newValues, String userName, Connection conn) throws SQLException {
        String query = """
            INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values, user_name)
            VALUES (?, ?, ?, ?, ?, ?)
            """
        ;
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            stmt.setInt(2, recordId);
            stmt.setString(3, operation);
            stmt.setString(4, oldValues);
            stmt.setString(5, newValues);
            stmt.setString(6, userName);
            stmt.executeUpdate();
        }
    }
    
    /**
     * تحديث رصيد المخزون - تستخدم اتصالاً موجوداً
     */
    public void updateInventory(int cropId, double quantityChange, double unitCost, 
                               String movementType, String referenceType, int referenceId, Connection conn) throws SQLException {
        // ... (الكود الداخلي لهذه الدالة يبقى كما هو)
        // This is the private worker method, we just need to make it public and accept a connection
        String updateInventoryQuery = "INSERT OR REPLACE INTO inventory (crop_id, current_stock_kg, average_cost_per_kg, last_updated) VALUES (?, COALESCE((SELECT current_stock_kg FROM inventory WHERE crop_id = ?), 0) + ?, CASE WHEN ? > 0 THEN (COALESCE((SELECT current_stock_kg * average_cost_per_kg FROM inventory WHERE crop_id = ?), 0) + (? * ?)) / (COALESCE((SELECT current_stock_kg FROM inventory WHERE crop_id = ?), 0) + ?) ELSE COALESCE((SELECT average_cost_per_kg FROM inventory WHERE crop_id = ?), 0) END, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement stmt = conn.prepareStatement(updateInventoryQuery)) {
            stmt.setInt(1, cropId);
            stmt.setInt(2, cropId);
            stmt.setDouble(3, quantityChange);
            stmt.setDouble(4, quantityChange);
            stmt.setInt(5, cropId);
            stmt.setDouble(6, quantityChange);
            stmt.setDouble(7, unitCost);
            stmt.setInt(8, cropId);
            stmt.setDouble(9, quantityChange);
            stmt.setInt(10, cropId);
            stmt.executeUpdate();
        }
        
        String insertMovementQuery = "INSERT INTO inventory_movements (crop_id, movement_type, quantity_kg, unit_cost, reference_type, reference_id, movement_date) VALUES (?, ?, ?, ?, ?, ?, DATE('now'))";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertMovementQuery)) {
            stmt.setInt(1, cropId);
            stmt.setString(2, movementType);
            stmt.setDouble(3, quantityChange);
            stmt.setDouble(4, unitCost);
            stmt.setString(5, referenceType);
            stmt.setInt(6, referenceId);
            stmt.executeUpdate();
        }
    }
    
     /**
     * تحديث رصيد الحساب المالي - تستخدم اتصالاً موجوداً
     */
    public void updateAccountBalance(int accountId, double amount, Connection conn) throws SQLException {
        String query = "UPDATE financial_accounts SET current_balance = current_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("لم يتم العثور على الحساب المالي رقم: " + accountId);
            }
            LOGGER.info("تم تحديث رصيد الحساب " + accountId + " بمبلغ: " + amount);
        }
    }
    public void addFinancialTransaction(Connection conn, int accountId, LocalDate date, 
            String type, String description, double amount, 
            int contactId, Integer purchaseId, Integer saleId, 
            String referenceNumber) throws SQLException {
    	String query = """
    			INSERT INTO financial_transactions (account_id, transaction_date, transaction_type, 
                   description, amount, related_contact_id, 
                   related_purchase_id, related_sale_id, reference_number)
    			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    			""";

    	try (PreparedStatement stmt = conn.prepareStatement(query)) {
    		stmt.setInt(1, accountId);
    		stmt.setString(2, FormatUtils.formatDateForDatabase(date));
    		stmt.setString(3, type);
    		stmt.setString(4, description);
    		stmt.setDouble(5, amount);
    		stmt.setInt(6, contactId);

    			if (purchaseId != null) {
    				stmt.setInt(7, purchaseId);
    			} else {
    				stmt.setNull(7, Types.INTEGER);
    			}

    			if (saleId != null) {
    				stmt.setInt(8, saleId);
    			} else {
    				stmt.setNull(8, Types.INTEGER);
    			}

    			stmt.setString(9, referenceNumber);

    			stmt.executeUpdate();
    	}
    }
    
    public void addLedgerEntry(Connection conn, String transactionRef, LocalDate entryDate,
            int accountId, double debit, double credit, String description, String sourceType, Integer sourceId, String transactionType) throws SQLException {
    	String sql = "INSERT INTO general_ledger (transaction_ref, entry_date, account_id, debit, credit, description, source_type, source_id, transaction_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    	try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    		stmt.setString(1, transactionRef);
    		stmt.setString(2, FormatUtils.formatDateForDatabase(entryDate));
    		stmt.setInt(3, accountId);
    		stmt.setDouble(4, debit);
    		stmt.setDouble(5, credit);
    		stmt.setString(6, description);
            stmt.setString(7, sourceType);
            if (sourceId != null) {
                stmt.setInt(8, sourceId);
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setString(9, transactionType);
    		stmt.executeUpdate();
            LOGGER.info("تم إضافة قيد دفتر الأستاذ: " + description + " (مدين: " + debit + ", دائن: " + credit + ")");
    	}
    }
    
    /**
     * إنشاء الحسابات الافتراضية في شجرة الحسابات إذا لم تكن موجودة.
     * تستخدم INSERT OR IGNORE لتجنب الأخطاء عند إعادة التشغيل.
     * @param conn اتصال قاعدة البيانات.
     * @throws SQLException
     */
    private void createDefaultAccounts(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO financial_accounts (account_id, account_name, account_type, is_active, opening_balance, opening_balance_date) VALUES (?, ?, ?, 1, 0.0, ?)";
        
        // تاريخ اليوم كرصيد افتتاحي
        String today = FormatUtils.formatDateForDatabase(LocalDate.now());

        // تعريف شجرة الحسابات الأساسية
        // ملاحظة: قمنا بتوسيع enum AccountType ليشمل أنواعاً أكثر دقة
        Object[][] accounts = {
            // الأصول
            {1, "الأصول", "HEADER", today},
            {101, "الأصول المتداولة", "HEADER", today},
            {10101, "الخزنة الرئيسية", "CASH", today},
            {10102, "حساب بنك مصر", "BANK", today},
            {10103, "المخزون", "CURRENT_ASSET", today},
            {10104, "الذمم المدينة (العملاء)", "ACCOUNTS_RECEIVABLE", today},
            // الخصوم
            {2, "الخصوم", "HEADER", today},
            {201, "الخصوم المتداولة", "HEADER", today},
            {20101, "الذمم الدائنة (الموردين)", "ACCOUNTS_PAYABLE", today},
            // حقوق الملكية
            {3, "حقوق الملكية", "HEADER", today},
            {30101, "رأس المال", "EQUITY", today},
            {30103, "الأرباح المحتجزة", "EQUITY", today},
            {30102, "المسحوبات الشخصية", "EQUITY", today},
            // الإيرادات
            {4, "الإيرادات", "HEADER", today},
            {40101, "إيرادات المبيعات", "REVENUE", today},
            {40102, "مرتجعات ومسموحات المبيعات", "REVENUE", today},
            {40105, "أرباح فروقات المخزون", "REVENUE", today},
            // المصروفات
            {5, "المصروفات", "HEADER", today},
            {50101, "تكلفة البضاعة المباعة", "EXPENSE", today},
            {50102, "مصروفات عمومية وإدارية", "EXPENSE", today},
            {50108, "خسائر المخزون", "EXPENSE", today}
        };

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Object[] account : accounts) {
                stmt.setInt(1, (Integer) account[0]);
                stmt.setString(2, (String) account[1]);
                stmt.setString(3, (String) account[2]);
                stmt.setString(4, (String) account[3]);
                stmt.addBatch();
            }
            stmt.executeBatch();
            LOGGER.info("تم التحقق من/إنشاء الحسابات الافتراضية بنجاح.");
        }
    }
    
    /**
     * Re-initializes the database manager for testing purposes with a specific database URL.
     * This method should ONLY be used in a test context.
     * @param testJdbcUrl The JDBC URL for the test database (e.g., an in-memory database).
     */
    public static synchronized void reinitializeForTest(String testJdbcUrl) {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
        if (dataSource != null) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(testJdbcUrl);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setPoolName("TestPool"); // Use a different pool name for tests

        dataSource = new HikariDataSource(config);
        LOGGER.info("DataSource configured for testing with URL: " + testJdbcUrl);
    }

    // This is the old method, we'll keep it for now to avoid breaking other parts of the code
    // that might not pass the source type and id. We can phase it out later.
    public void addLedgerEntry(Connection conn, String transactionRef, LocalDate entryDate,
            int accountId, double debit, double credit, String description) throws SQLException {
        addLedgerEntry(conn, transactionRef, entryDate, accountId, debit, credit, description, "MANUAL", null, null);
    }

    public double getTransactionSumForAccount(int accountId, Connection conn) throws SQLException {
        String sql = "SELECT SUM(debit) - SUM(credit) as total FROM general_ledger WHERE account_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    public boolean hasTransactions(int accountId, Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM general_ledger WHERE account_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<accounting.model.LedgerEntry> getLedgerEntriesByRef(String transactionRef, Connection conn) throws SQLException {
        List<accounting.model.LedgerEntry> entries = new java.util.ArrayList<>();
        String sql = "SELECT * FROM general_ledger WHERE transaction_ref = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionRef);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new accounting.model.LedgerEntry(
                        rs.getInt("entry_id"),
                        rs.getString("transaction_ref"),
                        LocalDate.parse(rs.getString("entry_date"), accounting.formatter.FormatUtils.DATE_FORMATTER),
                        rs.getInt("account_id"),
                        rs.getDouble("debit"),
                        rs.getDouble("credit"),
                        rs.getString("description"),
                        rs.getString("source_type"),
                        rs.getInt("source_id")
                    ));
                }
            }
        }
        return entries;
    }

    public void deleteLedgerEntriesByRef(String transactionRef, Connection conn) throws SQLException {
        String sql = "DELETE FROM general_ledger WHERE transaction_ref = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionRef);
            stmt.executeUpdate();
        }
    }
    
}
