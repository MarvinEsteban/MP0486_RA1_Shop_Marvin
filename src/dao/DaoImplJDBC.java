package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import model.Amount;
import model.Employee;
import model.Product;

public class DaoImplJDBC implements Dao {
	Connection connection;
	private static final String PRODUCT_TABLE = "inventory"; 
    private static final String HISTORICAL_TABLE = "historical_inventory"; 

	@Override
	public void connect() {
		// Define connection parameters
		String url = "jdbc:mysql://localhost:3306/shop";
		String user = "root";
		String pass = "";
		try {
			this.connection = DriverManager.getConnection(url, user, pass);
//            System.out.println("Conexión a la base de datos establecida.");
		} catch (SQLException e) {
			System.err.println("Error al conectar con la base de datos.");
			e.printStackTrace();
		}

	}

	@Override
	public void disconnect() {
		if (connection != null) {
			try {
				connection.close();
//               System.out.println("Conexión a la base de datos cerrada.");
			} catch (SQLException e) {
				System.err.println("Error al cerrar la conexión.");
				e.printStackTrace();
			}
		}

	}

	@Override
	public Employee getEmployee(int employeeId, String password) {
		Employee employee = null;
		String query = "select * from employee where employeeId= ? and password = ? ";

		try {
            if (connection == null) {
                System.err.println("Error: La conexión no está inicializada. Llame a connect() primero.");
                return null;
            }
            try (PreparedStatement ps = connection.prepareStatement(query)) {
        		ps.setInt(1, employeeId);
        	  	ps.setString(2, password);
                
                try (ResultSet rs = ps.executeQuery()) {
                	if (rs.next()) {
                		employee = new Employee(rs.getInt(1), rs.getString(2), rs.getString(3));
                	}
                }
            }
        } catch (SQLException e) {
			System.err.println("Error en la consulta SQL para autenticar al empleado.");
			e.printStackTrace();
		}
    	return employee;
	}

	/**
	 * Carga todos los productos del inventario desde la base de datos. 
     * Ajustado para usar las columnas 'id', 'name' y 'available'.
	 */
	@Override
	public ArrayList<Product> getInventory() {
		ArrayList<Product> inventory = new ArrayList<>();
		String query = "SELECT id, name, wholesalerPrice, available, stock FROM " + PRODUCT_TABLE;

		if (connection == null) {
			System.err.println(
					"Error: La conexión a la base de datos no está activa. Llame a connect() antes de getInventory().");
			return inventory; 
		}

		System.out.println("DEBUG QUERY: " + query);
		if(inventory != null) {
			System.out.println("Inventario cargado correctamente.");
		}

		try (PreparedStatement ps = connection.prepareStatement(query); 
             ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				// 1. Extraer datos de la base de datos, usando los nombres de columna correctos
				int id = rs.getInt("id");
				String name = rs.getString("name");
				double wholesalerPrice = rs.getDouble("wholesalerPrice");
				boolean isAvailable = rs.getBoolean("available"); 
				int stock = rs.getInt("stock");

				// 2. Mapear datos a objeto Product
				Product product = new Product(name, new Amount(wholesalerPrice), isAvailable, stock);
				product.setId(id); // Asignar el ID de la BD al objeto

				// 3. Añadir a la lista
				inventory.add(product);
			}

		} catch (SQLException e) {
			System.err.println(
					"Error al ejecutar la consulta SELECT para el inventario. Verifique el nombre de la tabla ('"
							+ PRODUCT_TABLE + "') y las columnas.");
			e.printStackTrace();
		}

		return inventory;
	}

    /**
     * Prevención a la hora de crear la tabla historical.
     */
    private void createHistoricalTableIfNotExists() {
         String createTableSQL = "CREATE TABLE IF NOT EXISTS " + HISTORICAL_TABLE + " (" +
            "`id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT," +
            "`id_product` INT NOT NULL," +
            "`name` VARCHAR(25) NOT NULL," +
            "`wholesalerPrice` DOUBLE(5,2) NOT NULL," +
            "`available` TINYINT(1) NOT NULL," +
            "`stock` INT NOT NULL," +
            "`created_at` TIMESTAMP NOT NULL" +
        ");";
        
        if (connection == null) {
             System.err.println("Error: Conexión no activa para verificar/crear tabla histórica.");
             return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla de historial '" + HISTORICAL_TABLE + "' asegurada.");
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla de historial: " + e.getMessage());
        }
    }


	/**
	 * **CORREGIDO**
	 * Exporta el inventario completo a la tabla de historial (historical_inventory).
     * @param products La lista de productos a guardar.
     * @return true si la transacción fue exitosa.
     * 
     */
	@Override
	public boolean writeInventory(ArrayList<Product> products) {
        createHistoricalTableIfNotExists();
        
        String insertQuery = "INSERT INTO " + HISTORICAL_TABLE + 
                             " (id_product, productName, wholeSalerPrice, available, stock, created_at) " +
                             " VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP())";
        
        if (connection == null) {
            System.err.println("Error: Conexión no activa para escribir inventario histórico.");
            return false;
        }

        try {
            // 1. Deshabilitar el autocommit para iniciar la transacción de lote
            connection.setAutoCommit(false); 

            try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
                for (Product product : products) {
                    ps.setInt(1, product.getId()); // id_product 
                    ps.setString(2, product.getName()); 
                    ps.setDouble(3, product.getWholesalerPrice().getValue()); 
                    ps.setBoolean(4, product.isAvailable()); 
                    ps.setInt(5, product.getStock()); 
                    ps.addBatch(); 
                }

                ps.executeBatch();
            }

            connection.commit(); 
            System.out.println("Inventario exportado con éxito a la tabla histórica. Total de registros: " + products.size());
            return true;

        } catch (SQLException e) {
            System.err.println("Error en la transacción al escribir el inventario histórico. Se realizará Rollback.");
            e.printStackTrace();
            
            try {
                if (connection != null) {
                    connection.rollback();
                    System.out.println("Rollback de la transacción completado.");
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error al intentar rollback: " + rollbackEx.getMessage());
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException autoCommitEx) {
                System.err.println("Error al restaurar AutoCommit: " + autoCommitEx.getMessage());
            }
        }
	}


	/**
	 * Inserta un nuevo producto en la tabla 'inventory'. Retorna el objeto Product
	 * actualizado con el ID generado por la BD.
	 */
	@Override
	public void addProduct(Product product) {
		String query = "INSERT INTO " + PRODUCT_TABLE + " (name, wholesalerPrice, available, stock) VALUES (?, ?, ?, ?)";

		if (connection == null) {
			System.err.println("Error: Conexión no activa para añadir producto.");
		}

		// Usamos Statement.RETURN_GENERATED_KEYS para obtener el ID recién creado
		try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, product.getName());
			ps.setDouble(2, product.getWholesalerPrice().getValue());
			ps.setBoolean(3, product.isAvailable()); 
			ps.setInt(4, product.getStock());

			int affectedRows = ps.executeUpdate();
            
			if (affectedRows == 0) {
				throw new SQLException("La inserción del producto falló, no se afectó ninguna fila.");
			}

			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int generatedId = generatedKeys.getInt(1);

					product.setId(generatedId);

					System.out.println(
							"Producto '" + product.getName() + "' añadido con éxito. ID generado: " + generatedId);
				} else {
					System.out.println(
							"Producto '" + product.getName() + "' añadido con éxito, pero no se pudo obtener el ID.");
				}
			}

		} catch (SQLException e) {
			System.err.println("Error al añadir producto en la base de datos.");
			e.printStackTrace();
		}
	}

	/**
	 * Actualiza los campos de un producto existente en la base de datos.
	 */
	@Override
	public void updateProduct(Product product) {
		String query = "UPDATE " + PRODUCT_TABLE + " SET name=?, wholesalerPrice=?, available=?, stock=? WHERE id=?";

		if (connection == null) {
			System.err.println("Error: Conexión no activa para actualizar producto.");
			
		}

		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, product.getName());
			ps.setDouble(2, product.getWholesalerPrice().getValue());
			ps.setBoolean(3, product.isAvailable()); 
			ps.setInt(4, product.getStock());
			ps.setInt(5, product.getId()); 

			// Ejecutar la actualización
			int affectedRows = ps.executeUpdate();

			if (affectedRows > 0) {
				System.out.println("Producto con ID " + product.getId() + " actualizado correctamente.");
				
			} else {
				System.out.println(
						"Advertencia: No se encontró el producto con ID " + product.getId() + " para actualizar.");
				
			}

		} catch (SQLException e) {
			System.err.println("Error al actualizar el producto en la base de datos.");
			e.printStackTrace();
			
		}
	}

	/**
	 * Elimina un producto de la base de datos usando su ID.
	 */
	@Override
	public void deleteProduct(int id) {
		String query = "DELETE FROM " + PRODUCT_TABLE + " WHERE id = ?";

		if (connection == null) {
			System.err.println("Error: Conexión no activa para eliminar producto.");
			
		}

		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, id);

			int affectedRows = ps.executeUpdate();

			if (affectedRows > 0) {
				System.out.println(
						"Producto con ID " + id + " eliminado correctamente. Filas afectadas: " + affectedRows);
			} else {
				System.out.println("Advertencia: No se encontró el producto con ID " + id + " para eliminar.");
			}

		
		} catch (SQLException e) {
			System.err.println("Error al eliminar el producto con ID " + id + " de la base de datos.");
			e.printStackTrace();
		
		}
	}

}