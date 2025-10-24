package dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

import model.Amount;
import model.Employee;
import model.Product;
import model.Sale;
import main.Shop;
public class DaoImplFile implements Dao{

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
		
		return null;
	}

	@Override
	public ArrayList<Product> getInventory() {
		ArrayList <Product> products = new ArrayList <>();
		/**
		 * read inventory from file
		 */
			// locate file, path and name
			File f = new File(System.getProperty("user.dir") + File.separator + "files/inputInventory.txt");
			
			try {			
				// wrap in proper classes
				FileReader fr;
				fr = new FileReader(f);				
				BufferedReader br = new BufferedReader(fr);
				
				// read first line
				String line = br.readLine();
				
				// process and read next line until end of file
				while (line != null) {
					// split in sections
					String[] sections = line.split(";");
					
					String name = "";
					double wholesalerPrice=0.0;
					int stock = 0;
					
					// read each sections
					for (int i = 0; i < sections.length; i++) {
						// split data in key(0) and value(1) 
						String[] data = sections[i].split(":");
						
						switch (i) {
						case 0:
							// format product name
							name = data[1];
							break;
							
						case 1:
							// format price
							wholesalerPrice = Double.parseDouble(data[1]);
							break;
							
						case 2:
							// format stock
							stock = Integer.parseInt(data[1]);
							break;
							
						default:
							break;
						}
					}
					// add product to inventory
					products.add(new Product(name, new Amount(wholesalerPrice), true, stock));
					
					// read next line
					line = br.readLine();
				}
				fr.close();
				br.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return products;
	}

	@Override
	public boolean writeInventory(ArrayList<Product> products) {		
		// define file name based on date
		LocalDate myObj = LocalDate.now();
		String fileName = "inventory_" + myObj.toString() + ".txt";
		
		// locate file, path and name
		File f = new File(System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName);
				
		try {
			// wrap in proper classes
			FileWriter fw;
			fw = new FileWriter(f, true);
			PrintWriter pw = new PrintWriter(fw);
			
			// write line by line
			int counter=1;
			for (Product product : products) {				
				// format first line TO BE -> 1;Client=PERE;Date=29-02-2024 12:49:50;
				StringBuilder firstLine = new StringBuilder(counter+";Product:"+ product.getName() + ";Price:" + product.getPublicPrice() + ";Stock:" + product.getStock() );
				pw.write(firstLine.toString());
				fw.write("\n");				
				
				// increment counter product
				counter++;
				
			}
			// close files
			pw.close();
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
