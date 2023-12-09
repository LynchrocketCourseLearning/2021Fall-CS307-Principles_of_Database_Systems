package test;

import cn.edu.sustech.cs307.dto.prerequisite.*;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.util.DataImporter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;

import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class test {
    public static void main(String[] args) throws IOException {
        Set<Short> weekList = new HashSet<>();
        for (short c=1;c<16;c++){
            weekList.add(c);
        }

        String[] weeks = new String[weekList.size()];
        int counter = 0;
        for (short t:weekList){
            weeks[counter++] = Short.toString(t);
        }
        String array = "{";
        StringBuilder sb = new StringBuilder(array);
        for (int c=0;c<counter-1;c++){
            sb.append(weeks[c]);
            sb.append(",");
        }
        sb.append(weeks[weeks.length-1]);
        sb.append("}");

        System.out.println(sb);
    }
}
