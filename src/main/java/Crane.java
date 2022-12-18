import com.google.gson.annotations.SerializedName;

import java.util.*;

public class Crane {
    @SerializedName("id")
    private int id;
    private boolean inUse = false;
    @SerializedName("x")
    private double x;
    @SerializedName("y")
    private double y;
    @SerializedName("ymin")
    private double ymin;
    @SerializedName("ymax")
    private double ymax;
    @SerializedName("xmax")
    private double xmax;
    @SerializedName("xmin")
    private double xmin;
    @SerializedName("xspeed")
    private double Vx; // Velocity in X-direction
    @SerializedName("yspeed")
    private double Vy; // Velocity in Y-direction
    List<CraneMovement> trajectory;

    public List<CraneMovement> getTrajectory() {
        return trajectory;
    }

    public int getId() {
        return id;
    }

    public boolean isInUse() {
        return inUse;
    }

    public Coordinate getLocation() {
        return new Coordinate(x, y);
    }
    public double getVx() {
        return Vx;
    }

    public double getVy() {
        return Vy;
    }
    public void setInUse() {
        assert !inUse : "Crane was already in use";
        inUse = true;
    }
    public void setNotInUse() {
        assert inUse : "Crane not in use";
        inUse = false;
    }
    public double travelTime(Coordinate destination) {
        Coordinate startPoint = getLocation();
        double x_time_component = startPoint.getXdistance(destination)/Vx;
        double y_time_component = startPoint.getYdistance(destination)/Vy;
        return Math.max(y_time_component, x_time_component);
    }
    public Coordinate calculateIntermediatePoint(Coordinate end) {
        Coordinate start = new Coordinate(x, y);

        double x_time_component = start.getXdistance(end)/Vx;
        double y_time_component = start.getYdistance(end)/Vy;
        // Put a critical point in the trajectory
        if(x_time_component>y_time_component) return (new Coordinate((start.getX() + Vx*y_time_component) ,end.getY()));
        // else if(x_time_component<=y_time_component) ....
        else return (new Coordinate(end.getX(), (start.getY() + Vy*x_time_component)));
    }

    public Coordinate calculateIntermediatePoint(Coordinate start, Coordinate end) {
        double x_time_component = start.getXdistance(end)/Vx;
        double y_time_component = start.getYdistance(end)/Vy;
        // Put a critical point in the trajectory
        if(x_time_component>y_time_component) return (new Coordinate((start.getX() + Vx*y_time_component) ,end.getY()));
        // else if(x_time_component<=y_time_component) ....
        else return (new Coordinate(end.getX(), (start.getY() + Vy*x_time_component)));
    }
    public void addToTrajectory(FullMovement movement) {
        CraneMovement craneMovement = new CraneMovement(this, movement.getStartPoint(), movement.getEndPoint(), movement.getPickupTime());
        trajectory.add(craneMovement);
    }

    public boolean inRange(Coordinate start) {
        return xmin <= start.getX() && start.getX() <= xmax && ymin <= start.getY() && start.getY() <= ymax;
    }

    public void updateLocation(Coordinate containerLocation) {
        x = containerLocation.getX();
        y = containerLocation.getY();
    }

    public FullMovement moveToSaveDistance(double timer, CraneMovement move1, CraneMovement move2) {
        double maxMovement = Math.max(Math.max(move1.getStartPoint().getX(), move2.getStartPoint().getX()), Math.max(move1.getEndPoint().getX(), move2.getEndPoint().getX()));
        double minMovement = Math.min(Math.max(move1.getStartPoint().getX(), move2.getStartPoint().getX()), Math.max(move1.getEndPoint().getX(), move2.getEndPoint().getX()));
        if (xmax >= maxMovement + 1) {
            Coordinate destination = new Coordinate(maxMovement+1, y);
            double endTime = timer + travelTime(destination);
            return new FullMovement(id, -1, timer, endTime, new Coordinate(x,y), destination);
        }
        else if (xmin + 1 <= minMovement) {
            Coordinate destination = new Coordinate(minMovement-1, y);
            double endTime = timer + travelTime(destination);
            return new FullMovement(id, -1, timer, endTime, new Coordinate(x,y), destination);
        }
        else {
            System.out.println("Crane "+ id + " cannot move out of the way");
            return null;
        }
    }
/*    public boolean SafetyDistances(Crane crane2, int safeDistance) {
        List<Integer> times1 = new ArrayList(trajectory.keySet());
        Collections.sort(times1);
        for(int i=0; i<times1.size(); i++) {
            int t1 = times1.get(i);
            int t2 = times1.get(i+1);

            List<Integer> times2 = new ArrayList(crane2.getTrajectory().keySet());
            Collections.sort(times2);
            for(int j = 0; j <times2.size(); j++) {
                int t3 = times2.get(j);
                int t4 = times2.get(j);

                if(t1 < t2 && t2 < t3) {
                    Coordinate coordinate1 = trajectory.get(t1);
                    Coordinate coordinate2 = trajectory.get(t2);

                    Coordinate coordinate3 = crane2.getTrajectory().get(t3);
                    Coordinate coordinate4 = crane2.getTrajectory().get(t4);

                    double x1 = Math.min(coordinate1.getX(), coordinate2.getX());
                    double x2 = Math.max(coordinate1.getX(), coordinate2.getX());

                    double x3 = Math.min(coordinate4.getX(), coordinate3.getX());
                    double x4 = Math.max(coordinate4.getX(), coordinate3.getX());
                    ;
                    return !(x2+safeDistance >= x3 && x1+safeDistance <= x4);
                }
            }
        }
        return true;
    }*/
}
