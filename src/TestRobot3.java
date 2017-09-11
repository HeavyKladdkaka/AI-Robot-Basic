import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public class TestRobot3
{
    private double robotHeading;
    private RobotCommunication robotcomm;  // communication drivers
    private Position[] path;
    private Position robotPosition;

    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot3(String host, int port)
    {
        robotcomm = new RobotCommunication(host, port);
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     * @param args         not used
     * @throws Exception   not caught
     */
    public static void main(String[] args) throws Exception
    {
        System.out.println("Creating Robot3");
        TestRobot3 robot = new TestRobot3("http://127.0.0.1", 50000);
        robot.run();
    }


    public void run() throws Exception
    {
        path = SetRobotPath("./input/Path-around-table.json");

        while(true){
            double[] coordinates = getPosition(new LocalizationResponse());
            robotHeading = getHeadingAngle(new LocalizationResponse());
            robotPosition = new Position(coordinates[0], coordinates[2]);

            System.out.println("Position: "+ robotPosition);
            System.out.println("Heading: "+ robotHeading);
        }
    }

    Position[] SetRobotPath(String filename){
        File pathFile = new File(filename);
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pathFile)));

            ObjectMapper mapper = new ObjectMapper();

            // read the path from the file
            Collection<Map<String, Object>> data = (Collection<Map<String, Object>>) mapper.readValue(in, Collection.class);
            int nPoints = data.size();
            path = new Position[nPoints];
            int index = 0;
            for (Map<String, Object> point : data)
            {
                Map<String, Object> pose = (Map<String, Object>)point.get("Pose");
                Map<String, Object> aPosition = (Map<String, Object>)pose.get("Position");
                double x = (Double)aPosition.get("X");
                double y = (Double)aPosition.get("Y");
                path[index] = new Position(x, y);
                index++;
            }
            System.out.println("Found path. First position is: ");
            System.out.println(path[0].getX() + ":" + path[0].getY());
            return path;
        } catch(FileNotFoundException e){
            System.out.println("File not found. ");
        } catch(IOException e){
            System.out.println("IOException. ");
        }
        System.out.println("Null path");
        return null;
    }

    /**
     * Extract the robot heading from the response
     * @param lr
     * @return angle in degrees
     */
    double getHeadingAngle(LocalizationResponse lr)
    {
        double e[] = lr.getOrientation();

        double angle = 2 * Math.atan2(e[3], e[0]);
        return angle * 180 / Math.PI;
    }

    /**
     * Extract the position
     * @param lr
     * @return coordinates
     */
    double[] getPosition(LocalizationResponse lr)
    {
        return lr.getPosition();
    }

}

