package Settings;

public class Settings {
    private int id;
    private Language language;
    private boolean activeNotifications;
    private UnitatsDist distanceUnits;

    public Settings(Language language, boolean activeNotifications, UnitatsDist distanceUnits) {
        this.language = language;
        this.activeNotifications = activeNotifications;
        this.distanceUnits = distanceUnits;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean isActiveNotifications() {
        return activeNotifications;
    }

    public void setActiveNotifications(boolean activeNotifications) {
        this.activeNotifications = activeNotifications;
    }

    public UnitatsDist getDistanceUnits() {
        return distanceUnits;
    }

    public void setDistanceUnits(UnitatsDist distanceUnits) {
        this.distanceUnits = distanceUnits;
    }

}
