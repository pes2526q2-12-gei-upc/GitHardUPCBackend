package Users;

import java.util.logging.Logger;

public  class Usuari {
    private int id;
    private String name;
    private String email;
    private String password;
    private boolean isLogged;
    Logger logger = Logger.getLogger(getClass().getName());

    public Usuari(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void iniciarSessio(){
        if (this.isLogged) {
            logger.info("No et pots loggejar perque ja has iniciat sessio.");
        }
        else{
            logger.info("Iniciant Sessio....");
        }
    }

    public void tancarSessio(){
        if (this.isLogged) {
            logger.info("Sessio tancada correctament.");
        }
        else{
            logger.info("No pots tancar una sessio si no esta iniciada.");
        }
    }
}