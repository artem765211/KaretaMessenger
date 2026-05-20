import ru.gr0946x.net.Server;

public class Main {
    public static void main(String[] args) throws Exception {
        org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
        new Server(9460);
    }
}