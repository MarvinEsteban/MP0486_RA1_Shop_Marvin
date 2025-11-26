package model;

public class Product {
	private int id;
    private String name;
    private Amount publicPrice;
    private Amount wholesalerPrice;
    private boolean available;
    private int stock;
    
    // Eliminada la variable estática totalProducts.
    
    public final static double EXPIRATION_RATE=0.60;
    
	public Product(String name, Amount wholesalerPrice, boolean available, int stock) {
		super();
		// El ID se inicializa a 0, será asignado por la base de datos (AUTO_INCREMENT) 
		// y actualizado por el DAO al momento de la inserción.
		this.id = 0; 
		this.name = name;
		this.wholesalerPrice = wholesalerPrice;
		// El precio público sigue calculándose basado en el precio mayorista.
		this.publicPrice = new Amount(wholesalerPrice.getValue() * 2);
		this.available = available;
		this.stock = stock;
		// Eliminada la línea totalProducts++;
	}

    // Constructor opcional para usar al leer de la base de datos (si no lo tienes, añádelo)
    /*
    public Product(int id, String name, Amount wholesalerPrice, boolean available, int stock) {
        this(name, wholesalerPrice, available, stock);
        this.id = id;
    }
    */
    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		// Este setter es crucial para que el DAO pueda asignar el ID generado por MySQL
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

	// ELIMINADOS: getTotalProducts() y setTotalProducts()
	
	public void expire() {
		this.publicPrice.setValue(this.getPublicPrice().getValue()*EXPIRATION_RATE); ;
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", name=" + name + ", publicPrice=" + publicPrice + ", wholesalerPrice=" + wholesalerPrice
				+ ", available=" + available + ", stock=" + stock + "]";
	}
    
}