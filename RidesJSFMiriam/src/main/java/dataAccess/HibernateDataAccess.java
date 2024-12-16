package dataAccess;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import configuration.UtilDate;
import domain.*;
import eredua.HibernateUtil;
import exceptions.RideAlreadyExistException;
import exceptions.RideMustBeLaterThanTodayException;
import java.util.ResourceBundle;

public class HibernateDataAccess {

    private static Session session;

    /**
     * Initializes the database with sample data.
     */
    public void initializeDB() {
        open();
        try {
            Calendar today = Calendar.getInstance();
            int month = today.get(Calendar.MONTH);
            int year = today.get(Calendar.YEAR);
            if (month == 12) {
                month = 1;
                year += 1;
            }
            Driver driver1 = (Driver) session.get(Driver.class, "driver1@gmail.com");
            Driver driver2 = (Driver) session.get(Driver.class, "driver2@gmail.com");
            Driver driver3 = (Driver) session.get(Driver.class, "driver3@gmail.com");
            if (driver1 == null) {
                driver1 = new Driver("driver1@gmail.com", "Aitor Fernandez");
                driver1.addRide("Donostia", "Bilbo", UtilDate.newDate(year, month, 15), 4, 7);
                driver1.addRide("Donostia", "Gazteiz", UtilDate.newDate(year, month, 6), 4, 8);
                driver1.addRide("Bilbo", "Donostia", UtilDate.newDate(year, month, 25), 4, 4);
                driver1.addRide("Donostia", "Iruña", UtilDate.newDate(year, month, 7), 4, 8);
                session.persist(driver1);
            }

            if (driver2 == null) {
                driver2 = new Driver("driver2@gmail.com", "Ane Gaztañaga");
                driver2.addRide("Donostia", "Bilbo", UtilDate.newDate(year, month, 15), 3, 3);
                driver2.addRide("Bilbo", "Donostia", UtilDate.newDate(year, month, 25), 2, 5);
                driver2.addRide("Eibar", "Gasteiz", UtilDate.newDate(year, month, 6), 2, 5);
                session.persist(driver2);
            }

            if (driver3 == null) {
                driver3 = new Driver("driver3@gmail.com", "Test driver");
                driver3.addRide("Bilbo", "Donostia", UtilDate.newDate(year, month, 14), 1, 3);
                session.persist(driver3);
            }

            session.getTransaction().commit();
            System.out.println("Db initialized");
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        }
    }

    public List<String> getDepartCities() {
    	open();
        try {
            List<String> result = session.createQuery("SELECT DISTINCT r.froml FROM Ride r ORDER BY r.froml").list();
            session.getTransaction().commit();
            return result;
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
            return new ArrayList<>();
        }finally {
        	close();
        }
    }

    public List<String> getArrivalCities(String from) {
    	open();
        List<String> cities = new ArrayList<>();
        try {
            cities = session.createQuery("SELECT DISTINCT r.tol FROM Ride r WHERE r.froml = :departCity ORDER BY r.tol")
                    .setParameter("departCity", from).list();

            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        }finally {
        	close();
        }
        return cities;
    }

    public Ride createRide(String from, String to, Date date, int nPlaces, float price, String driverEmail)
            throws RideAlreadyExistException, RideMustBeLaterThanTodayException {
        session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        System.out.println(">> DataAccess: createRide=> from= " + from + " to= " + to + " driver=" + driverEmail
                + " date " + date);
        try {
            if (new Date().compareTo(date) > 0) {
                throw new RideMustBeLaterThanTodayException(
                        "Ridea ez da gaur baino atzerago izan");
            }
            Driver driver = (Driver) session.get(Driver.class, driverEmail);
            Hibernate.initialize(driver.getEmail());
            if (doesRideExistInDatabase(from, to, date)) {
                session.getTransaction().rollback(); 
                throw new RideAlreadyExistException(
                        "datubasean jada badago");
            }
            Ride ride = driver.addRide(from, to, date, nPlaces, price);
            session.persist(driver);
            session.getTransaction().commit();
            return ride;
        } catch (NullPointerException e) {
            session.getTransaction().rollback();
            e.printStackTrace();
            return null;
        }
    }

    private boolean doesRideExistInDatabase(String from, String to, Date date) {
        try {
            Query query = session.createQuery("SELECT r FROM Ride r WHERE r.froml = :from AND r.tol = :to AND r.date = :date");
            query.setParameter("from", from);
            query.setParameter("to", to);
            query.setParameter("date", date);

            List<Ride> rides = query.list();
            return !rides.isEmpty();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
            return false;
        }
    }


    public List<Ride> getRides(String from, String to, Date date) {
    	open();
        System.out.println(">> DataAccess: getRides=> from= " + from + " to= " + to + " date " + date);

        List<Ride> res = new ArrayList<>();
        try {
            Query query = session.createQuery("SELECT r FROM Ride r WHERE r.froml=:from AND r.tol=:to AND r.date=:date");
            query.setParameter("from", from);
            query.setParameter("to", to);
            query.setParameter("date", date);

            List<Ride> rides = query.list();
            for (Ride ride : rides) {
                Hibernate.initialize(ride.getDriver().getEmail()); // Initialize lazy property
                res.add(ride);
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
        }finally {
        	close();
        }
        return res;
    }
    
	public List<Date> getThisMonthDatesWithRides(String from, String to, Date date) {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		System.out.println(">> DataAccess: getEventsMonth");
		List<Date> res = new ArrayList<>();

		Date firstDayMonthDate = UtilDate.firstDayMonth(date);
		Date lastDayMonthDate = UtilDate.lastDayMonth(date);

		Query query = session.createQuery(
		"SELECT DISTINCT r.date FROM Ride r WHERE r.froml=?1 AND r.tol=?2 AND r.date BETWEEN ?3 and ?4");

		query.setParameter("1", from);
		query.setParameter("2", to);
		query.setParameter("3", firstDayMonthDate);
		query.setParameter("4", lastDayMonthDate);
		List<Date> dates = query.list();
		for (Date d : dates) {
			res.add(d); 
		}
		return res;
	}

    public void open() {
        if (session == null || !session.isOpen()) {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            System.out.println("DataAccess opened successfully");
        } else {
            System.out.println("DataAccess already open");
        }
    }

    public void close() {
        if (session != null && session.isOpen()) {
            session.close();
            System.out.println("DataAccess closed");
        }
    }
}