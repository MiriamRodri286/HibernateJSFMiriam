package eredua.bean;

import businessLogic.BLFacade;
import businessLogic.BLFacadeImplementation;
import dataAccess.HibernateDataAccess;
public class FacadeBean {
	private static FacadeBean singleton = new FacadeBean();
	private static BLFacade facadeInterface;
	private FacadeBean() {
		try {
			facadeInterface = new BLFacadeImplementation();
			System.out.println("FacadeBean initialized successfully.");
		} catch (Exception e) {
			System.out.println("FacadeBean: negozioaren logika sortzean errorea: " + e.getMessage());
		}
	}
	public static BLFacade getBusinessLogic() {
		if (facadeInterface == null) {
	        throw new IllegalStateException("Business logic interface is not initialized.");
	    }
	    return facadeInterface;
	}
}