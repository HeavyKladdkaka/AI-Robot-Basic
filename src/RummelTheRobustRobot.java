import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TestRobotinterfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 ->
 * Lokarria(Robulab) -> Core -> MRDS4
 *
 * @author Aron Nisbel, id15anl
 * @author Jesper Blomqvist,
 */
public class RummelTheRobustRobot {

    private RobotCommunication robotcomm;  // communication drivers
    private double robotAngle;
    private double nextPositionAngle;
    private String host;
    private int port;
    private LinkedList<Position> pathQueue;

    /**
     * Create a RummelRobot connected to host "host" at port "port" and
     * initialize a LinkedList later to be used in run()-method and
     * setRobotPath()-method.
     *
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public RummelTheRobustRobot(String host, int port) {

        this.host = host;
        this.port = port;
        this.pathQueue = new LinkedList<>();
    }

    /**
     * This simple main program creates a RummelRobot, the fastest robot-type
     * there is. Then it calls the run-method.
     * @param args not used
     * @throws Exception not caught
     */
    public static void main(String[] args) throws Exception {

        System.out.println("Creating RummelRobot");
        RummelTheRobustRobot robot = new RummelTheRobustRobot("http://127.0.0.1", 50000);
        robot.run();
    }

    /**
     *  This run-method creates the communication with the server and reads
     *  from a path file which will be the path for the RummelRobot to follow.
     *  It creates the necessary positions, sets a start time, a
     *  lookAheadDistance and start a do-while-loop.
     *
     *  The loop gets the RummelRobot's angle and the angle to the next
     *  position in the path from a queue. It then compares the distance from
     *  the RummelRobot to the next position and checks with the hardcoded
     *  lookAheadDistance and sees if it's more or less. If it's less than
     *  the lookAheadDistance then it loops and does the same thing until it
     *  finds a position that is further away than the lookAheadDistance.
     *
     *  Then it moves towards that position by using moveRobot() and keeps
     *  searching for positions further away than the lookAheadDistance and
     *  keeps updating the directions.
     * @throws Exception DifferentialDriveRequest-Exception
     */
    private void run() throws Exception {

        double[] position;
        robotcomm = new RobotCommunication(host, port);
        pathQueue = SetRobotPath("D:\\exam2017.json");
        LocalizationResponse lr = new LocalizationResponse();

        Position goToPosition = pathQueue.peekFirst();
        Position robotPosition;
        double lookAheadDistance = 1;

        Date startTime = new Date();

        do {

            robotcomm.getResponse(lr);
            robotAngle = lr.getHeadingAngle();
            position = lr.getPosition();

            robotPosition = new Position(position);

            if (robotPosition.getDistanceTo(goToPosition) <
                    lookAheadDistance) {

                goToPosition = pathQueue.poll();

            } else if (robotPosition.getDistanceTo(goToPosition)
                    > lookAheadDistance) {

                nextPositionAngle = robotPosition.getBearingTo(goToPosition);
                moveRobot();
            }

        } while (!pathQueue.isEmpty() && pathQueue.peekLast().getDistanceTo
                (robotPosition) > 0.1);

        setWheelSpeed(0, 0);
        Date stopTime = new Date();

        System.out.println("Robot is within 1 meter from the last point in" +
                " the path. Seconds passed: " + ((stopTime.getTime()
                -startTime.getTime())/1000));
    }

    /**
     * Sets the wheel speed of the RummelRobot and makes a request to the
     * server.
     * @param angularSpeed the turning speed of the RummelRobot.
     * @param linearSpeed the speed straight forward of the RummelRobot.
     */
    private void setWheelSpeed(double angularSpeed,double linearSpeed) {

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        dr.setAngularSpeed(angularSpeed);
        dr.setLinearSpeed(linearSpeed);

        try {
            robotcomm.putRequest(dr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Measures the angle between the robot and the position and moves
     * accordingly by setting the angularspeed and linearspeed. Using the
     * method setWheelSpeed() it calculates the radians with 2 times pi to
     * account for the times that the angle might be small to the left/right of
     * the robot but the robot wants to turn the other way and then makes sure
     * that doesn't happen. All the hardcoded values here can be optimized
     * for each specific path. Especially the linearspeed in the last
     * else-statement.
     */
    private void moveRobot() {

        double robotToPositionAngle;

        robotToPositionAngle = nextPositionAngle - robotAngle;

        if (robotToPositionAngle > Math.PI) {

            setWheelSpeed(robotToPositionAngle - Math.PI * 2, 1);

        } else if (robotToPositionAngle < -Math.PI) {

            setWheelSpeed(robotToPositionAngle + Math.PI * 2,1);

        } else if (robotToPositionAngle == 0) {

            setWheelSpeed(0,1);

        } else {

            setWheelSpeed(robotToPositionAngle,0.5);
        }
    }

    /**
     * This method extracts each position in the given path file and puts
     * them in a linkedlist ordered like a queue.
     * @param filename the path file.
     * @return a linkedlist as a queue with all the positions of the path
     * file in order first to last.
     */
    private LinkedList SetRobotPath(String filename) {

        Position[] path;
        File pathFile = new File(filename);

        try {

            BufferedReader in = new BufferedReader(new InputStreamReader
                    (new FileInputStream(pathFile)));

            ObjectMapper mapper = new ObjectMapper();

            Collection<Map<String, Object>> data =
                    (Collection<Map<String, Object>>) mapper.readValue
                            (in, Collection.class);

            int nPoints = data.size();
            path = new Position[nPoints];
            int index = 0;

            for (Map<String, Object> point : data) {

                Map<String, Object> pose =
                        (Map<String, Object>) point.get("Pose");
                Map<String, Object> aPosition =
                        (Map<String, Object>) pose.get("Position");

                double x = (Double) aPosition.get("X");
                double y = (Double) aPosition.get("Y");

                path[index] = new Position(x, y);
                pathQueue.add(new Position(x, y));

                index++;
            }

            System.out.println("Found path. First position is: ");
            System.out.println(path[0].getX() + ":" + path[0].getY());

            return pathQueue;

        } catch (FileNotFoundException e) {

            System.out.println("File not found. ");

        } catch (IOException e) {

            System.out.println("IOException. ");

        }

        System.out.println("Null path");

        return null;
    }
}

