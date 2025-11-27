package main;

import model.Product;
import model.Sale;
import model.Amount;
import model.Client;
import model.Employee;
import dao.Dao; // Importamos la interfaz
import dao.DaoImplJDBC; // Importamos la implementación JDBC
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

public class Shop {
	private Amount cash = new Amount(100.00);
//	private Product[] inventory;
	private ArrayList<Product> inventory;
	private int numberProducts;
//	private Sale[] sales;
	private ArrayList<Sale> sales;
	private int numberSales;
	// Atributo sustituido de File a interface dao
	private Dao dao; 
	
	final static double TAX_RATE = 1.04;

	public Shop() {
		inventory = new ArrayList<Product>();
		sales = new ArrayList<Sale>();
		
		// Inicializar con la implementación JDBC
		dao = new DaoImplJDBC(); 
		dao.connect(); 
	}

	public Amount getCash() {
		return cash;
	}

	public void setCash(Amount cash) {
		this.cash = cash;
	}

	public ArrayList<Product> getInventory() {
		return inventory;
	}

	public void setInventory(ArrayList<Product> inventory) {
		this.inventory = inventory;
	}

	public int getNumberProducts() {
		return numberProducts;
	}

	public void setNumberProducts(int numberProducts) {
		this.numberProducts = numberProducts;
	}

	public ArrayList<Sale> getSales() {
		return sales;
	}

	public void setSales(ArrayList<Sale> sales) {
		this.sales = sales;
	}

	public int getNumberSales() {
		return numberSales;
	}

	public void setNumberSales(int numberSales) {
		this.numberSales = numberSales;
	}

	public static void main(String[] args) {
		Shop shop = new Shop();

		// load inventory from external data
		
		shop.loadInventory();
		System.out.println("\n✅ Datos de inventario cargados correctamente.");
		
		// init session as employee
		shop.initSession();

		Scanner scanner = new Scanner(System.in);
		int opcion = 0;
		boolean exit = false;

		do {
			System.out.println("\n");
			System.out.println("===========================");
			System.out.println("Menu principal miTienda.com");
			System.out.println("===========================");
			System.out.println("0) Exportar Inventario Histórico"); // OPCIÓN AGREGADA
			System.out.println("1) Contar caja");
			System.out.println("2) Añadir producto");
			System.out.println("3) Añadir stock");
			System.out.println("4) Marcar producto proxima caducidad");
			System.out.println("5) Ver inventario");
			System.out.println("6) Venta");
			System.out.println("7) Ver ventas");
			System.out.println("8) Ver venta total");
			System.out.println("9) Eliminar producto");
			System.out.println("10) Salir programa");
			System.out.print("Seleccione una opción: ");
			
			// MANEJO DE ENTRADA ROBUSTO (CORRECCIÓN)
			if (scanner.hasNextInt()) {
			    opcion = scanner.nextInt();
			} else {
			    System.out.println("Opción inválida. Intente de nuevo.");
			    scanner.next(); 
			    continue; 
			}
			

			switch (opcion) {
			case 0: // CASE PARA EXPORTAR HISTORIAL
				boolean success = shop.writeInventory();
				if (success) {
					System.out.println("✅ Inventario exportado correctamente a 'historical_inventory'.");
				} else {
					System.out.println("❌ ERROR al exportar el inventario a la base de datos.");
				}
				break;
			case 1:
				shop.showCash();
				break;

			case 2:
				shop.addProduct();
				break;

			case 3:
				shop.addStock();
				break;

			case 4:
				shop.setExpired();
				break;

			case 5:
				shop.showInventory(); 
				break;

			case 6:
				shop.sale();
				break;

			case 7:
				shop.showSales();
				break;

			case 8:
				shop.showSalesAmount();
				break;

			case 9:
				shop.removeProduct();
				break;

			case 10:
				System.out.println("Cerrando programa ...");
				exit = true;
				break;
			}

		} while (!exit);
		
		// Desconexión de la base de datos al salir
		shop.dao.disconnect(); 
		scanner.close(); // Cerrar el scanner
	}

	private void initSession() {
		// TODO Auto-generated method stub

		Employee employee = new Employee("test");
		boolean logged = false;

		do {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Introduzca numero de empleado: ");
			int employeeId = 0;
			// Manejo de entrada robusto para el ID de empleado
			if (scanner.hasNextInt()) {
			    employeeId = scanner.nextInt();
			} else {
			    scanner.next(); // Limpiar el buffer
			    continue;
			}

			System.out.println("Introduzca contraseña: ");
			String password = scanner.next();
			
			// NOTA: Aquí se debería usar el DAO para autenticar:
			// Employee loggedEmployee = dao.getEmployee(employeeId, password);
			// logged = (loggedEmployee != null);

			logged = employee.login(employeeId, password);
			if (logged) {
				System.out.println("Login correcto ");
			} else {
				System.out.println("Usuario o password incorrectos ");
			}
		} while (!logged);

	}

	/**
	 * load initial inventory to shop
	 */
	public void loadInventory() {
		// Los productos se cargan de la base de datos
		this.readInventory(); 
	}

	/**
	 * read inventory from DB using DAO
	 */
	private void readInventory() {
		this.inventory = (ArrayList<Product>) dao.getInventory();
	}
	/**
	 * write inventory (now writes to DB via DAO - Historical Export)
	 */
	public boolean writeInventory() {
		return dao.writeInventory(inventory);
	}

	/**
	 * show current total cash
	 */
	private void showCash() {
		System.out.println("Dinero actual: " + cash);
	}
	
	/**
	 * Invoca al DAO para actualizar un producto en la base de datos.
	 * @param product El producto con los datos actualizados.
	 * @return El producto actualizado si la operación fue exitosa, o null si falló.
	 */
	public Product updateProduct(Product product) {
		return dao.updateProduct(product);
	}

	/**
	 * Invoca al DAO para eliminar un producto de la base de datos por su ID.
	 * @param id El ID del producto a eliminar.
	 * @return El número de filas afectadas (1 si se eliminó, 0 si no se encontró o falló).
	 */
	public int deleteProduct(int id) {
		return dao.deleteProduct(id);
	}
	
	/**
	 * Encapsula la lógica de añadir un producto. Llama al DAO para insertar en BD.
     * El método ahora devuelve el objeto Product con el ID de la BD.
	 * * @param product El producto a insertar.
	 * @return El producto con el ID asignado por la BD, o null si falló.
	 */
	public Product addProduct(Product product) {
		if (isInventoryFull()) {
			System.out.println("No se pueden añadir más productos");
			return null;
		}
		
		// 1. Insertar en la base de datos y obtener el objeto actualizado con el ID de la BD
		Product insertedProduct = dao.addProduct(product); 
		
		if (insertedProduct != null) {
			// 2. Añadir a la lista en memoria SOLO si fue exitoso en la BD
			inventory.add(insertedProduct); 
			numberProducts++;
			return insertedProduct;
		} else {
			return null;
		}
	}
	
	/**
	 * add a new product to inventory getting data from console (método sin argumentos)
	 */
	public void addProduct() {
		if (isInventoryFull()) {
			System.out.println("No se pueden añadir más productos");
			return;
		}
		Scanner scanner = new Scanner(System.in);
		System.out.print("Nombre: ");
		String name = scanner.nextLine();
		System.out.print("Precio mayorista: ");
		double wholesalerPrice = scanner.nextDouble();
		System.out.print("Stock: ");
		int stock = scanner.nextInt();
		
		// Determinamos la disponibilidad al crear el producto
		boolean available = stock > 0;

		Product newProduct = new Product(name, new Amount(wholesalerPrice), available, stock);
		
		// Usamos el método encapsulado y actualizado addProduct(Product)
		Product insertedProduct = this.addProduct(newProduct); 
		
		if (insertedProduct != null) {
			System.out.println("Producto añadido con éxito y guardado en la BD. ID: " + insertedProduct.getId());
		} else {
			System.out.println("Error al guardar el producto en la base de datos.");
		}
	}


	/**
	 * remove a new product to inventory getting data from console
	 * (Refactorizado para usar el método deleteProduct(id) encapsulado)
	 */
	public void removeProduct() {
		if (inventory.size() == 0) {
			System.out.println("No se pueden eliminar productos, inventario vacio");
			return;
		}
		Scanner scanner = new Scanner(System.in);
		System.out.print("Seleccione un nombre de producto: ");
		String name = scanner.next();
		Product product = findProduct(name);

		if (product != null) {
			// 1. Eliminar de la base de datos usando el método encapsulado
			int deletedRows = deleteProduct(product.getId()); 
			
			if (deletedRows > 0) {
				// 2. Si la eliminación en BD fue exitosa, eliminar de la lista en memoria
				if (inventory.remove(product)) {
					numberProducts--; // Mantener el conteo consistente
					System.out.println("El producto " + name + " ha sido eliminado (BD y memoria).");
				} else {
					System.out.println("Advertencia: Eliminado de BD pero fallo al eliminar de la memoria.");
				}
			} else {
				System.out.println("Error: No se pudo eliminar el producto " + name + " de la base de datos.");
			}
		} else {
			System.out.println("No se ha encontrado el producto con nombre " + name);
		}
	}

	/**
	 * add stock for a specific product (updates the database)
	 */
	public void addStock() {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Seleccione un nombre de producto: ");
		String name = scanner.next();
		Product product = findProduct(name);

		if (product != null) {
			// ask for stock
			System.out.print("Seleccione la cantidad a añadir: ");
			int stock = scanner.nextInt();
			
			// 1. Actualizar stock en memoria
			int newStock = product.getStock() + stock;
			product.setStock(newStock);
			
			// 2. Actualizar disponibilidad si el stock pasa de 0 a > 0
			if (newStock > 0 && !product.isAvailable()) {
			    product.setAvailable(true);
			}

			Product updatedProduct = updateProduct(product);
			
			if (updatedProduct != null) {
			    System.out.println("El stock del producto " + name + " ha sido actualizado a " + product.getStock() + " (BD y memoria).");
			} else {
			    System.out.println("Error al actualizar el stock del producto " + name + " en la base de datos.");
			}
		} else {
			System.out.println("No se ha encontrado el producto con nombre " + name);
		}
	}


	/**
	 * set a product as expired
	 */
	private void setExpired() {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Seleccione un nombre de producto: ");
		String name = scanner.next();

		Product product = findProduct(name);

		if (product != null) {
			product.expire();
			System.out.println("El precio del producto " + name + " ha sido actualizado a " + product.getPublicPrice());
			
			//Product updatedProduct = updateProduct(product);
		}
	}

	/**
	 * show all inventory
	 */
	public void showInventory() {
		System.out.println("Contenido actual de la tienda:");
		for (Product product : inventory) {
			if (product != null) {
				System.out.println(product);
			}
		}
	}

	/**
	 * make a sale of products to a client
	 */
	public void sale() {
		// ask for client name
		Scanner sc = new Scanner(System.in);
		System.out.println("Realizar venta, escribir nombre cliente");
		String nameClient = sc.nextLine();
		Client client = new Client(nameClient);

		// sale product until input name is not 0
		ArrayList<Product> shoppingCart = new ArrayList<Product>();
		int numberShopping = 0;

		Amount totalAmount = new Amount(0.0);
		String name = "";
		while (!name.equals("0")) {
			System.out.println("Introduce el nombre del producto, escribir 0 para terminar:");
			name = sc.nextLine();

			if (name.equals("0")) {
				break;
			}
			Product product = findProduct(name);
			boolean productAvailable = false;

			if (product != null && product.isAvailable()) {
				productAvailable = true;
				totalAmount.setValue(totalAmount.getValue() + product.getPublicPrice().getValue());
				
				// 1. Actualizar stock en memoria
				product.setStock(product.getStock() - 1);
				
				shoppingCart.add(product);
				numberShopping++;
				
				// 2. Si no queda stock, marcar como no disponible
				if (product.getStock() == 0) {
					product.setAvailable(false);
				}
				
				// 3. Persistir el cambio de stock/disponibilidad en la BD
				Product updatedProduct = updateProduct(product);
				
				if (updatedProduct != null) {
					System.out.println("Producto añadido con éxito");
				} else {
					System.out.println("Error: Producto añadido en memoria, pero falló la actualización en la BD.");
				}
			}

			if (!productAvailable) {
				System.out.println("Producto no encontrado o sin stock");
			}
		}

		totalAmount.setValue(totalAmount.getValue() * TAX_RATE);
		// show cost total
		System.out.println("Venta realizada con éxito, total: " + totalAmount);

		// make payment
		if (!client.pay(totalAmount)) {
			System.out.println("Cliente debe: " + client.getBalance());
		}

		// create sale
		Sale sale = new Sale(client, shoppingCart, totalAmount);

		// add to shop
		sales.add(sale);
//		numberSales++;

		// add to cash
		cash.setValue(cash.getValue() + totalAmount.getValue());
	}

	/**
	 * show all sales
	 */
	private void showSales() {
		System.out.println("Lista de ventas:");
		for (Sale sale : sales) {
			if (sale != null) {
				System.out.println(sale);
			}
		}

		// ask for client name
		Scanner sc = new Scanner(System.in);
		System.out.println("Exportar fichero ventas? S / N");
		String option = sc.nextLine();
		if ("S".equalsIgnoreCase(option)) {
			this.writeSales();
		}

	}

	/**
	 * write in file the sales done
	 */
	private void writeSales() {
		// define file name based on date
		LocalDate myObj = LocalDate.now();
		String fileName = "sales_" + myObj.toString() + ".txt";

		// locate file, path and name
		File f = new File(System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName);

		try {
			// wrap in proper classes
			FileWriter fw;
			fw = new FileWriter(f, true);
			PrintWriter pw = new PrintWriter(fw);

			// write line by line
			int counterSale = 1;
			for (Sale sale : sales) {
				// format first line TO BE -> 1;Client=PERE;Date=29-02-2024 12:49:50;
				StringBuilder firstLine = new StringBuilder(
						counterSale + ";Client=" + sale.getClient() + ";Date=" + sale.formatDate() + ";");
				pw.write(firstLine.toString());
				fw.write("\n");

				// format second line TO BE ->
				// 1;Products=Manzana,20.0€;Fresa,10.0€;Hamburguesa,60.0€;
				// build products line
				StringBuilder productLine = new StringBuilder();
				for (Product product : sale.getProducts()) {
					productLine.append(product.getName() + "," + product.getPublicPrice() + ";");
				}
				StringBuilder secondLine = new StringBuilder(counterSale + ";" + "Products=" + productLine + ";");
				pw.write(secondLine.toString());
				fw.write("\n");

				// format third line TO BE -> 1;Amount=93.60€;
				StringBuilder thirdLine = new StringBuilder(counterSale + ";" + "Amount=" + sale.getAmount() + ";");
				pw.write(thirdLine.toString());
				fw.write("\n");

				// increment counter sales
				counterSale++;
			}
			// close files
			pw.close();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * show total amount all sales
	 */
	private void showSalesAmount() {
		Amount totalAmount = new Amount(0.0);
		for (Sale sale : sales) {
			if (sale != null) {
				totalAmount.setValue(totalAmount.getValue() + sale.getAmount().getValue());
			}
		}
		System.out.println("Total cantidad ventas:");
		System.out.println(totalAmount);
	}

	/**
	 * check if inventory is full or not
	 */
	public boolean isInventoryFull() {
		if (numberProducts == 10) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * find product by name
	 * * @param product name
	 */
	public Product findProduct(String name) {
		for (int i = 0; i < inventory.size(); i++) {
			if (inventory.get(i) != null && inventory.get(i).getName().equalsIgnoreCase(name)) {
				return inventory.get(i);
			}
		}
		return null;

	}

}