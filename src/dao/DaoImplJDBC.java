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
	@Override
	public void connect() {
		// Define connection parameters
		String url = "jdbc:mysql://localhost:3306/shop";
		String user = "root";
		String pass = "";
		try {
			this.connection = DriverManager.getConnection(url, user, pass);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public Employee getEmployee(int employeeId, String password) {
		Employee employee = null;
		String query = "select * from employee where employeeId= ? and password = ? ";
		
		try (PreparedStatement ps = connection.prepareStatement(query)) { 
    		ps.setInt(1,employeeId);
    	  	ps.setString(2,password);
    	  	//System.out.println(ps.toString());
            try (ResultSet rs = ps.executeQuery()) {
            	if (rs.next()) {
            		employee = new Employee(rs.getInt(1), rs.getString(2), rs.getString(3));
            	}
            }
        } catch (SQLException e) {
			// in case error in SQL
			e.printStackTrace();
		}
    	return employee;
	}

	@Override
	public ArrayList<Product> getInventory() {
		ArrayList<Product> inventory = new ArrayList<>();
        // CONSULTA ACTUALIZADA: Usando las columnas productName, wholeSalerPrice y stock
        String query = "SELECT productName, wholeSalerPrice, stock FROM " + PRODUCT_TABLE;
        
        // Usamos try-with-resources para asegurar el cierre automático del PreparedStatement y ResultSet
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // 1. Extraer datos de la base de datos, usando los nombres de columna de tu DDL
                String name = rs.getString("productName");
                double wholesalerPrice = rs.getDouble("wholeSalerPrice");
                int stock = rs.getInt("stock");
                
                // 2. LOGICA DE NEGOCIO: Derivar 'isAvailable' si stock > 0
                boolean isAvailable = stock > 0;
                
                // 3. Mapear datos a objeto Product
                // Se asume que Amount es la clase para manejar el precio
                Product product = new Product(
                    name, 
                    new Amount(wholesalerPrice), 
                    isAvailable, 
                    stock
                );
                
                // 4. Añadir a la lista
                inventory.add(product);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al ejecutar la consulta SELECT para el inventario.");
			e.printStackTrace();
		}
        
		return inventory;
	}

	@Override
	public boolean writeInventory(ArrayList<Product> products) {
        String deleteQuery = "DELETE FROM " + PRODUCT_TABLE;
        // Solo necesitamos insertar los campos no AUTO_INCREMENT
        String insertQuery = "INSERT INTO " + PRODUCT_TABLE + " (productName, wholeSalerPrice, stock) VALUES (?, ?, ?)";
        
        if (connection == null) {
            System.err.println("Error: Conexión no activa para escribir inventario.");
            return false;
        }

        try {
            // 1. Deshabilitar el autocommit para iniciar la transacción
            connection.setAutoCommit(false); 

            // 2. Eliminar todos los productos existentes
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(deleteQuery);
            }
            System.out.println("Inventario anterior eliminado para reemplazo.");

            // 3. Insertar todos los productos nuevos en un lote (batch)
            try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
                for (Product product : products) {
                    ps.setString(1, product.getName());
                    ps.setDouble(2, product.getWholesalerPrice().getValue());
                    ps.setInt(3, product.getStock());
                    ps.addBatch(); // Añadir a la cola de operaciones
                }

                // Ejecutar el lote de inserciones
                ps.executeBatch();
            }

            // 4. Confirmar (commit) la transacción
            connection.commit(); 
            System.out.println("Inventario escrito con éxito en la base de datos. Total de productos: " + products.size());
            return true;

        } catch (SQLException e) {
            System.err.println("Error en la transacción al escribir el inventario. Se realizará Rollback.");
            e.printStackTrace();
            
            // Si hay un error, intentar revertir
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
            // Restaurar el autocommit a true después de la transacción
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
	 * Inserta un nuevo producto en la tabla 'inventory'.
	 * Retorna el objeto Product (sin el ID generado).
	 */
	@Override
	public Product addProduct(Product product) {
		// La cláusula productId es AUTO_INCREMENT, por lo que no la incluimos en el INSERT.
		String query = "INSERT INTO " + PRODUCT_TABLE + " (productName, wholeSalerPrice, stock) VALUES (?, ?, ?)";
		
		if (connection == null) {
            System.err.println("Error: Conexión no activa para añadir producto.");
            return null;
        }

		// Usamos Statement.RETURN_GENERATED_KEYS para obtener el ID recién creado
		try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			// Establecer parámetros
			ps.setString(1, product.getName());
			ps.setDouble(2, product.getWholesalerPrice().getValue());
			ps.setInt(3, product.getStock());

			// Ejecutar la inserción
			int affectedRows = ps.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("La inserción del producto falló, no se afectó ninguna fila.");
			}

			// Obtener la clave generada
			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int generatedId = generatedKeys.getInt(1);
					// Si Product tiene setProductId(), lo usarías aquí: product.setProductId(generatedId);
					System.out.println("Producto '" + product.getName() + "' añadido con éxito. ID generado: " + generatedId);
					return product; 
				} else {
					// Si no se pudo obtener el ID, pero la inserción fue exitosa
					System.out.println("Producto '" + product.getName() + "' añadido con éxito, pero no se pudo obtener el ID.");
					return product; 
				}
			}

		} catch (SQLException e) {
			System.err.println("Error al añadir producto en la base de datos.");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Product updateProduct(Product product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int deleteProduct(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

}
