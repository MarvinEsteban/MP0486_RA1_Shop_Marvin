package dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Date;

import model.Amount;
import model.Employee;
import model.Product;

public class DaoImplMongoDB implements Dao {

	private MongoClient mongoClient;
	private MongoDatabase database;
	private MongoCollection<Document> usersCollection;
	private MongoCollection<Document> inventoryCollection;
	private MongoCollection<Document> historicalCollection;

	@Override
	public void connect() {
		// Inicio del cliente
		mongoClient = MongoClients.create("mongodb://localhost:27017");

		// selección de base de datos
		database = mongoClient.getDatabase("shop");

		// Inicialización de referencias a mongoDB
		usersCollection = database.getCollection("users");
		inventoryCollection = database.getCollection("inventory");
		historicalCollection = database.getCollection("historical_inventory");

	}

	@Override
	public void disconnect() {
		mongoClient.close();

	}

	@Override
	public Employee getEmployee(int employeeId, String password) {
		var filter = Filters.and(Filters.eq("employeeId", employeeId), Filters.eq("password", password));

		Document userDoc = usersCollection.find(filter).first();

		if (userDoc != null) {
			String name = userDoc.getString("name");
			return new Employee(employeeId, name, password);
		}
		return null;
	}

	@Override
	public ArrayList<Product> getInventory() {
		ArrayList<Product> inventory = new ArrayList<>();

		// 1. Iteramos sobre todos los documentos de la colección
		for (Document doc : inventoryCollection.find()) {

			// 2. Extraemos los datos básicos
			int id = doc.getInteger("id");
			String name = doc.getString("name");
			int stock = doc.getInteger("stock");
			boolean available = doc.getBoolean("available");

			// 3. Extraemos el objeto anidado 'wholesalerPrice'
			Document priceDoc = (Document) doc.get("wholesalerPrice");
			double value = priceDoc.getDouble("value");

			// 4. Creamos el producto
			Product p = new Product(name, new Amount(value), available, stock);

			// Actualizamos ID
			p.setId(id);

			inventory.add(p);
		}

		return inventory;
	}

	@Override
	public boolean writeInventory(ArrayList<Product> inventory) {
		MongoCollection<Document> historicalCollection = database.getCollection("historical_inventory");
		
	    // Obtenemos la fecha actual
	    Date now = new Date();

	    for (Product product : inventory) {
	        
	        Document priceDoc = new Document("value", product.getWholesalerPrice().getValue())
	                                   .append("currency", "€");

	        Document doc = new Document("name", product.getName())
	                .append("wholesalerPrice", priceDoc)
	                .append("available", product.isAvailable())
	                .append("stock", product.getStock())
	                .append("id", product.getId())
	                .append("created_at", now); 

	        historicalCollection.insertOne(doc);
	    }
	    
	    System.out.println("Inventario exportado al histórico con fecha " + now);
		return true;
	}

	@Override
	public void addProduct(Product product) {
		Document priceDoc = new Document("value", product.getWholesalerPrice().getValue())
				.append("currency", "€"); 
		Document doc = new Document("name", product.getName())
				.append("wholesalerPrice", priceDoc)
				.append("available", product.isAvailable())
				.append("stock", product.getStock())
				.append("id", product.getId());


		inventoryCollection.insertOne(doc);

		System.out.println("Producto '" + product.getName() + "' insertado correctamente en MongoDB.");
	}

	@Override
	public void updateProduct(Product product) {
		// buscamos el documento donde el campo "id" sea igual al ID del objeto Java
	    var filter = Filters.eq("id", product.getId());

	    // Preparamos los cambios. 
	    var update = Updates.combine(
	            Updates.set("stock", product.getStock()),
	            Updates.set("available", product.isAvailable()),
	            Updates.set("name", product.getName())
	    );

	    // Ejecutamos la actualización
	    inventoryCollection.updateOne(filter, update);
	    
	    System.out.println("Producto " + product.getName() + " actualizado en MongoDB.");

	}

	@Override
	public void deleteProduct(int id) {
		// Filtramos por el id y borramos el primer documento que coincida
	    var resultado = inventoryCollection.deleteOne(Filters.eq("id", id));
	    
	    if (resultado.getDeletedCount() > 0) {
	        System.out.println("Producto con ID " + id + " eliminado correctamente.");
	    } else {
	        System.out.println("No se encontró ningún producto con ID " + id);
	    }

	}

}
