package Users;

public class Client extends Usuari{
    private int gamificationPoints;
    private ReputationLevel reputation;

    public Client(int id, String name, String email, String password, int gamificationPoints, ReputationLevel reputation) {
        super(id, name, email, password);
        this.gamificationPoints = gamificationPoints;
        this.reputation = reputation;
    }

    public int getGamificationPoints() {
        return gamificationPoints;
    }

    public void setGamificationPoints(int gamificationPoints) {
        this.gamificationPoints = gamificationPoints;
    }

    public ReputationLevel getReputation() {
        return reputation;
    }

    public void setReputation(ReputationLevel reputation) {
        this.reputation = reputation;
    }
}
