package Users;

import java.util.logging.Logger;

public  class Admin extends Usuari{
    private AuthLevel authLevel;
    Logger logger = Logger.getLogger(getClass().getName());

    public Admin(int id, String name, String email, String password, AuthLevel authLevel ) {
        super(id, name, email, password);
        this.authLevel = authLevel;
    }

    public void modificarPuntdeRuta(PuntRuta punt){
        logger.info("modificant punt de ruta...");
    }

    public AuthLevel getPermisos(){
        return authLevel;
    }

    public void setPermisos(AuthLevel authLevel){
        this.authLevel = authLevel;
    }
}