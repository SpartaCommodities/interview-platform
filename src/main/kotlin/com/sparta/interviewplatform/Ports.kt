package com.sparta.interviewplatform

import javax.sql.DataSource

data class Port(val code: String, val name: String, val country: String)

class DuplicatePortException(val code: String) : Exception("port '$code' already exists")

interface PortRepository {
    fun all(): List<Port>
    fun byCode(code: String): Port?
    fun add(port: Port): Port
}

class JdbcPortRepository(private val dataSource: DataSource) : PortRepository {
    override fun all(): List<Port> =
        dataSource.connection.use { connection ->
            connection.prepareStatement("select code, name, country from ports order by code")
                .use { statement ->
                    statement.executeQuery().use { rows ->
                        buildList {
                            while (rows.next()) add(rows.toPort())
                        }
                    }
                }
        }

    override fun byCode(code: String): Port? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("select code, name, country from ports where code = ?")
                .use { statement ->
                    statement.setString(1, code)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) rows.toPort() else null
                    }
                }
        }

    override fun add(port: Port): Port {
        if (byCode(port.code) != null) throw DuplicatePortException(port.code)
        dataSource.connection.use { connection ->
            connection.prepareStatement("insert into ports (code, name, country) values (?, ?, ?)")
                .use { statement ->
                    statement.setString(1, port.code)
                    statement.setString(2, port.name)
                    statement.setString(3, port.country)
                    statement.executeUpdate()
                }
        }
        return port
    }

    private fun java.sql.ResultSet.toPort() =
        Port(getString("code"), getString("name"), getString("country"))
}
