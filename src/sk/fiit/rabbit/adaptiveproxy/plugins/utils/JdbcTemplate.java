package sk.fiit.rabbit.adaptiveproxy.plugins.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.mysql.jdbc.Statement;

public class JdbcTemplate {

	private final Connection connection;

	public JdbcTemplate(Connection connection) {
		this.connection = connection;
	}

	public <T> T queryFor(String query, Object[] parameters, Class<T> ofClass) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			setStatementParameters(statement, parameters);

			ResultSet rs = statement.executeQuery();

			if (rs.first()) {
				return (T) rs.getObject(1);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new DatabaseAccessException(e);
		} finally {
			SqlUtils.close(statement);
		}
	}
	
	public interface ResultProcessor<T> {
		public T processRow(ResultSet rs) throws SQLException;
	}
	
	public <T> List<T> findAll(String query, Object[] parameters, ResultProcessor<T> processor) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			setStatementParameters(statement, parameters);

			ResultSet rs = statement.executeQuery();
			
			List<T> results = new LinkedList<T>();

			while(rs.next()) {
				results.add(processor.processRow(rs));
			}
			
			return results;
		} catch (SQLException e) {
			throw new DatabaseAccessException(e);
		} finally {
			SqlUtils.close(statement);
		}		
	}
	
	public <T> T find(String query, Object[] parameters, ResultProcessor<T> processor) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			setStatementParameters(statement, parameters);

			ResultSet rs = statement.executeQuery();
			
			if(rs.first()) {
				return processor.processRow(rs);
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
