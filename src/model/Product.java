package model;

import javax.persistence.Transient;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "inventory") 
public class Product {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) //Usamos el generatedValue para el  autoIncremente
	@Column
	private int id;
	
	@Column
    private String name;
	
	@Column(name = "price")
	private double priceValue;
	
	@Transient
    private Amount publicPrice;
	
	@Transient
    private Amount wholesalerPrice;
    
    @Column
    private boolean available;
    
    @Column 
    private int stock;
    
    public final static double EXPIRATION_RATE=0.60;
    
	public Product(String name, Amount wholesalerPrice, boolean available, int stock) {
		super();
		// El ID se inicializa a 0, será asignado por la base de datos (AUTO_INCREMENT) 
		// y actualizado por el DAO al momento de la inserción.
		this.id = 0; 
		this.name = name;
		this.wholesalerPrice = wholesalerPrice;
		this.publicPrice = new Amount(wholesalerPrice.getValue() * 2);
		this.available = available;
		this.stock = stock;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Amount getPublicPrice() {
		return publicPrice;
	}

	public void setPublicPrice(Amount publicPrice) {
		this.publicPrice = publicPrice;
	}

	public Amount getWholesalerPrice() {
		return wholesalerPrice;
	}

	public void setWholesalerPrice(Amount wholesalerPrice) {
		this.wholesalerPrice = wholesalerPrice;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}
	
	public void expire() {
		this.publicPrice.setValue(this.getPublicPrice().getValue()*EXPIRATION_RATE); ;
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", name=" + name + ", publicPrice=" + publicPrice + ", wholesalerPrice=" + wholesalerPrice
				+ ", available=" + available + ", stock=" + stock + "]";
	}
    
}