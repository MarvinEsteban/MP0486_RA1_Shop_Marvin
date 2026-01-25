package model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "historical_inventory")
public class ProductHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "name")
	private String name;

	@Column(name = "price")
	private double price;

	@Column(name = "stock")
	private int stock;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	// Constructor vacío
	public ProductHistory() {
	}

	// Constructor útil para convertir un Product en un historial
	public ProductHistory(Product p) {
		this.name = p.getName();
		// Sacamos el double del objeto Amount
		this.price = p.getPublicPrice().getValue();
		this.stock = p.getStock();
		this.createdAt = new Date(); // Fecha y hora actual
	}

	// Getters (para que Hibernate pueda leer los datos)
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	public Date getCreatedAt() {
		return createdAt;
	}
}