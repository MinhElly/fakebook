import tools.jackson.databind.JsonNode;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) throws Exception {
        for (Method m : JsonNode.class.getMethods()) {
            if (m.getName().toLowerCase().contains("text") || m.getName().toLowerCase().contains("string")) {
                System.out.println(m.getName());
            }
        }
    }
}
