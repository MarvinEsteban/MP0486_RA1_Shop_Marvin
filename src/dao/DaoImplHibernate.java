package dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Transaction;

import org.hibernate.Session;
import org.hibernate.query.Query;

import utils.HibernateUtil;
import model.Employee;
import model.Product;
import model.ProductHistory;

public class DaoImplHibernate implements Dao{

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Employee getEmployee(int employeeId, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Product> getInventory() {
		ArrayList <Product> inventory = new ArrayList<>();
		
		try (Session session = HibernateUtil.getSessionFactory().openSession()){
			//Creación de consulta HQL para obtener todos los datos.
            Query<Product> query = session.createQuery("from Product", Product.class);
            
            // Obtenemos la lista de resultados
            List<Product> list = query.getResultList();
            
            // Convertimos el List a ArrayList ya que es lo que devuelve el método de interface Dao
            if (list != null) {
                inventory = new ArrayList<>(list);
            }
            
        } catch (Exception e) {
            System.err.println("Error al obtener el inventario de la base de datos:");
            e.printStackTrace();
        }
        
        return inventory;
		
	}

	@Override
	public boolean writeInventory(ArrayList<Product> products) {
	    Transaction transaction = null;
	    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
	        transaction = session.beginTransaction();

	        // Recorremos la lista de productos que nos llega desde Shop
	        for (Product p : products) {
	            // Convertimos cada Product en un ProductHistory
	            ProductHistory history = new ProductHistory(p);
	            
	            // Le decimos a Hibernate que lo guarde en la tabla historical_inventory
	            session.save(history);
	        }

	        // Si todo va bien, confirmamos la transacción
	        transaction.commit();
	        return true; 
	        
	    } catch (Exception e) {
	        // Si hay un error, deshacemos los cambios
	        if (transaction != null) transaction.rollback();
	        e.printStackTrace();
	        return false;
	    }
	}

	@Override
    public void addProduct(Product product) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(product); // Guarda el producto
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

	@Override
    public void updateProduct(Product product) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(product); // Actualiza stock/precio
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

	@Override
    public void deleteProduct(int id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // Primero buscamos el producto por ID
            Product product = session.get(Product.class, id);
            if (product != null) {
                session.delete(product); // Lo borramos si existe
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

}
