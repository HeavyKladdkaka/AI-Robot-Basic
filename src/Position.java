
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Iterator;

public class Position {

    double x, y;
    public double getX() { return x; }
    public double getY() { return y; }

    public Position(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public static void main(String[] args) {
        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader(
                    "./input/Path-around-table.json"));

            JSONObject jsonObject = (JSONObject) obj;

            JSONValue pose = (JSONValue) jsonObject.get("Pose");
            String author = (String) jsonObject.get("Author");
            JSONArray companyList = (JSONArray) jsonObject.get("Company List");

            System.out.println("Pose: " + pose);
            System.out.println("Author: " + author);
            System.out.println("\nCompany List:");
            Iterator<String> iterator = companyList.iterator();
            while (iterator.hasNext()) {
                System.out.println(iterator.next());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getDistanceTo(Position p)
    {
        return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y));
    }

    // Bearing to another position, realtive to 'North'
    // Bearing have several meanings, in this case the angle between
    // north and the position p.
    public double getBearingTo(Position p)
    {
        return Math.atan2(p.y - y, p.x - x);
    }
}
