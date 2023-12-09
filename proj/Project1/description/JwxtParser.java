import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.List;
// TODO: import the json library of your choice

public class JwxtParser {

    private static final String pathToJSONFile = "/path/to/your/json/file";

    public static void main(String[] args) throws IOException {
        String content = Files.readString(Path.of(pathToJSONFile));

        // Gson is an example
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<List<Course>>(){}.getType();
        List<Course> courses=gson.fromJson(content,  type);

        System.out.println(gson.toJson(courses));

        // or ...
        // ObjectMapper mapper = new ObjectMapper();
    }
}

class Course {
    // TODO:
    private int totalCapacity;
    private String courseId;
    private String prerequisite;
    private String teacher;
    private ClassList[] classList;
}

class ClassList {
    // TODO: define data-class as the json structure

    private int[] weekList;
    private String location;
    private String classTime;

}
