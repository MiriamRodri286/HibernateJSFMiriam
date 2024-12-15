package dataAccess;

import domain.Driver;
import domain.Ride;
import eredua.HibernateUtil;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RideManager {

    private static HibernateDataAccess dataAccess;

    public RideManager() {
        dataAccess = new HibernateDataAccess();
    }

    /**
     * Crea y almacena un viaje en la base de datos.
     *
     * @param from     Origen del viaje
     * @param to       Destino del viaje
     * @param date     Fecha del viaje
     * @param nPlaces  Número de plazas disponibles
     * @param price    Precio del viaje
     * @param active   Estado del viaje (activo/inactivo)
     */
    private void createAndStoreRide(String from, String to, Date date, int nPlaces, float price, Driver driver) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;
        
        try {
        	tx = session.beginTransaction();
            
			if (driver.getEmail() != null) {
	            session.saveOrUpdate(driver);
	        }
			Ride ride = new Ride(from, to, date, nPlaces, price, driver);
			session.saveOrUpdate(ride);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Recupera todos los viajes almacenados en la base de datos.
     *
     * @return Lista de objetos Ride
     */
    private List<Ride> getAllRides() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;
        List<Ride> rides = null;
        try {
            tx = session.beginTransaction();
            rides = session.createQuery("from Ride").list();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
        return rides;
    }

    public static void main(String[] args) {
        RideManager manager = new RideManager();
        Driver d= new Driver("driver8@gmail.com", "Kepa");
        try {
			dataAccess.initializeDB();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // Crear un viaje de ejemplo
        manager.createAndStoreRide("Bilbao", "San Sebastian", new Date(), 3, 15.5f, d);
        manager.createAndStoreRide("a", "b", new Date(), 2, 40f, d);
        manager.createAndStoreRide("c", "d", new Date(), 4, 35f, d);
        manager.createAndStoreRide("e", "f", new Date(), 3, 30f, d);
        // Obtener y mostrar todos los viajes
        List<Ride> rides = manager.getAllRides();
        if (rides != null) {
            for (Ride ride : rides) {
                System.out.println(ride);
            }
        }
    }
}
