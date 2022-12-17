public class FullMovement {
    public int craneId;
    public int containerId;
    public int pickupTime;
    public int endTime;
    public double pickupPosX;
    public double pickupPosY;
    public double endPosX;
    public double endPosY;

    public FullMovement() {}
    public FullMovement(int craneId, int containerId, int pickupTime, int endTime, double pickupPosX, double pickupPosY, double endPosX, double endPosY) {
        this.craneId = craneId;
        this.containerId = containerId;
        this.pickupTime = pickupTime;
        this.endTime = endTime;
        this.pickupPosX = pickupPosX;
        this.pickupPosY = pickupPosY;
        this.endPosX = endPosX;
        this.endPosY = endPosY;
    }
    public void setStart(Coordinate coordinate) {
        pickupPosX = (coordinate.getX());
        pickupPosY = (coordinate.getY());
    }


    public int getCraneId() {
        return craneId;
    }

    public void setCraneId(int craneId) {
        this.craneId = craneId;
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public int getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(int pickupTime) {
        this.pickupTime = pickupTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public double getPickupPosX() {
        return pickupPosX;
    }

    public void setPickupPosX(double pickupPosX) {
        this.pickupPosX = pickupPosX;
    }

    public double getPickupPosY() {
        return pickupPosY;
    }

    public void setPickupPosY(double pickupPosY) {
        this.pickupPosY = pickupPosY;
    }

    public double getEndPosX() {
        return endPosX;
    }

    public void setEndPosX(double endPosX) {
        this.endPosX = endPosX;
    }

    public double getEndPosY() {
        return endPosY;
    }

    public void setEndPosY(double endPosY) {
        this.endPosY = endPosY;
    }
    @Override
    public String toString() {
        if(containerId != -1) return craneId + ";" + containerId + ";" + pickupTime + ";" + endTime + ";" + ";" + pickupPosX + ";" + pickupPosY + ";" + endPosX + ";" + endPosY + ";";
        else return craneId + ";" + ";" + pickupTime + ";" + endTime + ";" + ";" + pickupPosX + ";" + pickupPosY + ";" + endPosX + ";" + endPosY + ";";

    }
}