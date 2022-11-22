package ksl.utilities.dbutil

import java.sql.Types
import javax.sql.rowset.CachedRowSet

fun isEven(value: Int) = value % 2 == 0
fun isOdd(value: Int) = value % 2 == 1

//TODO need a default format
class DbResultsAsText(private val rowSet: CachedRowSet, var dFormat: String? = null) : Iterable<List<String>> {

    private val myColumns = mutableListOf<DbColumn>()

    val columns: List<DbColumn>
        get() = myColumns

    private val tableNames = mutableListOf<String>()

    /**
     *  the number of rows in the row set
     */
    val numRows = rowSet.size()

    /**
     * @return the list of column names
     */
    val columnNames: List<String>

    var paddingSize = DEFAULT_PADDING

    val rowSeparator: String

    val columnHeader: String

    val header: String

    val columnMetaData = DatabaseIfc.columnMetaData(rowSet)

    init {
        val columnCount = columnMetaData.size
        val rs = StringBuilder()
        val ch = StringBuilder()
        val h = StringBuilder()
        val list = mutableListOf<String>()
        for (i in 1..columnCount) {
            columnMetaData[i-1].label
            val column = DbColumn(i, columnMetaData[i-1].label, columnMetaData[i-1].type, columnMetaData[i-1].typeName)
            list.add(column.name)
            myColumns.add(column)
            val tn = columnMetaData[i-1].tableName
            if (!tableNames.contains(tn)) {
                tableNames.add(tn)
            }
            rs.append("+")
            rs.append("-".repeat(column.width))
            ch.append("|")
            ch.append(" ".repeat(paddingSize))
            ch.append(column.name)
            ch.append(" ".repeat(paddingSize))
        }
        rs.append("+")
//        rs.appendLine()
        ch.append("|")
//        ch.appendLine()
        columnNames = list
        rowSeparator = rs.toString()
        columnHeader = ch.toString()
        h.append(rs)
        h.appendLine()
        h.append(ch)
        h.appendLine()
        h.append(rs)
        header = h.toString()
    }

    /** Converts the column values to string values. Any instance that cannot
     *  be converted is replaced with a string representing the SQL type in parentheses.
     *  For example, SQL type BLOB will be (BLOB).
     *
     * @param rowNum the row of the row set to convert
     * @return the values across the columns for the row as strings
     */
    fun rowAsStrings(rowNum: Int): List<String> {
        if (rowNum !in 1..numRows) {
            return emptyList()
        }
        val list = mutableListOf<String>()
        rowSet.absolute(rowNum)
        for (i in 1..myColumns.size) {
            list.add(columnObjectAsString(i))
        }
        return list
    }

    fun rowAsInsertString(rowNum: Int): List<String>{
        if (rowNum !in 1..numRows) {
            return emptyList()
        }
        val list = mutableListOf<String>()
        rowSet.absolute(rowNum)
        for (i in 1..myColumns.size) {
            val str = columnObjectAsInsertString(i)
            list.add(str)
        }
        return list
    }

    private fun columnObjectAsInsertString(col: Int): String {
        val any = rowSet.getObject(col) ?: return "NULL"
        val index = col - 1 // zero based indexing of list
        val dbColumn = myColumns[index]
        return when (dbColumn.textType) {
            TextType.DATETIME,
            TextType.STRING -> {
                "'%s'".format(any)
            }
            else -> any.toString()
        }
    }

    fun formattedRow(rowNum: Int): String {
        val list = rowAsStrings(rowNum)
        val sb = StringBuilder()
        for (i in myColumns.indices) {
            val c = myColumns[i]
            val v = list[i]
            sb.append("|")
            if (v.length >= c.width) {
                sb.append("*".repeat(c.width))
            } else {
                val gap = c.width - v.length
                if (gap == 1){
                    sb.append(" ")
                    sb.append(v)
                } else {
                    val fps = if (isEven(gap)){
                        (gap/2)
                    } else {
                        (gap/2) - 1
                    }
                    sb.append(" ".repeat(fps))
                    sb.append(v)
                    sb.append(" ".repeat(gap - fps))
                }
            }
        }
        sb.append("|")
        return sb.toString()
    }

    private fun columnObjectAsString(col: Int): String {
        val any = rowSet.getObject(col) ?: return "NULL"
        val index = col - 1 // zero based indexing of list
        val dbColumn = myColumns[index]
        return when (dbColumn.textType) {
            TextType.BOOLEAN,
            TextType.DATETIME,
            TextType.INTEGER -> any.toString()
            TextType.STRING -> {
                if (any.toString().length < (dbColumn.width - paddingSize)) {
                    any.toString()
                } else {
                    //TODO index out of bounds error, need to rethink this
                    // something to do about max column width
                    // I think I should get the full string and then apply formatting afterwards using %s
                    any.toString().substring(0..(dbColumn.width)).plus(" ...")
                }
            }
            TextType.DOUBLE -> {
                val x: Double = try {
                    any.toString().toDouble()
                } catch (e: NumberFormatException) {
                    Double.NaN
                }
                dFormat?.format(x) ?: x.toString()
            }
            else -> "($dbColumn.typeName)"
        }
    }

    companion object {
        /**
         * Default maximum number of rows to query and print.
         */
        const val DEFAULT_MAX_ROWS = 10

        /**
         * Default maximum width for text columns
         * (like a `VARCHAR`) column.
         */
        var DEFAULT_MAX_COL_WIDTH = 30

        var DEFAULT_PADDING = 4

        /** Mapping sql type to text printing type
         * @param type the SQL type via java.sql.Types
         * @return the corresponding type for text printing conversion
         */
        fun textType(type: Int): TextType {
            return when (type) {
                Types.BIGINT, Types.TINYINT, Types.SMALLINT, Types.INTEGER -> TextType.INTEGER
                Types.REAL, Types.DOUBLE, Types.DECIMAL -> TextType.DOUBLE
                Types.DATE, Types.TIME, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP,
                Types.TIMESTAMP_WITH_TIMEZONE -> TextType.DATETIME
                Types.BOOLEAN -> TextType.BOOLEAN
                Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR,
                Types.CHAR, Types.NCHAR -> TextType.STRING
                else -> TextType.OTHER
            }
        }
    }

    enum class TextType {
        /**
         * Column type category for `CHAR`, `VARCHAR`
         * and similar text columns.
         */
        STRING,

        /**
         * Column type category for `TINYINT`, `SMALLINT`,
         * `INT` and `BIGINT` columns.
         */
        INTEGER,

        /**
         * Column type category for `REAL`, `DOUBLE`,
         * and `DECIMAL` columns.
         */
        DOUBLE,

        /**
         * Column type category for date and time related columns like
         * `DATE`, `TIME`, `TIMESTAMP` etc.
         */
        DATETIME,

        /**
         * Column type category for `BOOLEAN` columns.
         */
        BOOLEAN,

        /**
         * Column type category for types for which the type name
         * will be printed instead of the content, like `BLOB`,
         * `BINARY`, `ARRAY` etc.
         */
        OTHER
    }

    inner class DbColumn(val index: Int, val name: String, val type: Int, val typeName: String) {
        val textType = textType(type)
        val width: Int
            get() {
                return minOf(name.length + 2 * paddingSize, DEFAULT_MAX_COL_WIDTH)
            }
//        var justify = ""
//        fun justifyLeft() {
//            justify = "-"
//        }
    }

    override fun iterator(): Iterator<List<String>> {
        return TextRowsIterator()
    }

    fun formattedRowIterator(): Iterator<String> {
        return FormattedRowIterator()
    }

    fun insertTextRowIterator(): Iterator<List<String>>{
        return InsertTextRowsIterator()
    }

    internal inner class TextRowsIterator : Iterator<List<String>> {
        private var current: Int = 0
        private val end = rowSet.size()

        override fun hasNext(): Boolean {
            return current < end
        }

        override fun next(): List<String> {
            current++
            return rowAsStrings(current)
        }

    }

    internal inner class FormattedRowIterator : Iterator<String> {
        private var current: Int = 0
        private val end = rowSet.size()

        override fun hasNext(): Boolean {
            return current < end
        }

        override fun next(): String {
            current++
            return formattedRow(current)
        }

    }

    internal inner class InsertTextRowsIterator : Iterator<List<String>> {
        private var current: Int = 0
        private val end = rowSet.size()

        override fun hasNext(): Boolean {
            return current < end
        }

        override fun next(): List<String> {
            current++
            return rowAsInsertString(current)
        }

    }
}