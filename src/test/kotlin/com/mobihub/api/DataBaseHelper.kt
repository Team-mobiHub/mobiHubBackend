package com.mobihub.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper class to generate sample data for the database.
 *
 * @author Team-MobiHub
 */
object DataBaseHelper {
    /**
     * Generates sample data for the database.
     */
    fun generateSampleData() {
        val url = "jdbc:postgresql://ifv-mobihub.ifv.kit.edu:5431/mobiHub"
        val user = "admin"
        val password = "adminAdmin#"

        Database.connect(url = url, user = user, password = password)

        transaction {
            exec("CALL generate_sample_data();")
        }
    }

    /**
     * Clears the database.
     */
    fun clearDataBase() {
        val url = "jdbc:postgresql://ifv-mobihub.ifv.kit.edu:5431/mobiHub"
        val user = "admin"
        val password = "adminAdmin#"

        Database.connect(url = url, user = user, password = password)

        transaction {
            exec("CALL truncate_data();")
        }
    }
}
