package sk.fiit.rabbit.adaptiveproxy.plugins.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.Statement;

public class JdbcTemplate {

	private final Connection connection;

	public JdbcTemplate(Connection connection) {
		this.connection = connection;
	}

	public Object queryFor(String query, Object[] parameters, Class<?> ofClass) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			setStatementParameters(statement, parameters);

			ResultSet rs = statement.executeQuery();

			if (rs.first()) {
				return rs.getObject(1);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new DatabaseAccessException(e);
		} finally {
			SqlUtils.close(statement);
		}
	}
	
	public Integer insert(String query, Object[] parameters) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			setStatementParameters(statement, parameters);

			statement.execute();
			
			ResultSet keys = statement.getGeneratedKeys();
			
			if(keys.first()) {
				return keys.getInt(1);
			} else {
				throw new DatabaseAccessException(String.format("Insert failed (%s)", query));
			}
		} catch (SQLException e) {
			throw new DatabaseAccessException(e);
		} finally {
			SqlUtils.close(statement);
		}
	}
	
	public void update(String query, Object[] parameters) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			setStatementParameters(statement, parameters);

			statement.execute();
		} catch (SQLException e) {
			throw new DatabaseAccessException(e);
		} finally {
			SqlUtils.close(statement);
		}
	}
	
	private void setStatementParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
		for (int i = 0; i < parameters.length; i++) {
			statement.setObject(i + 1, parameters[i]);
		}
	}
}
