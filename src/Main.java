import server.Server;
import java.io.IOException;

import static java.lang.System.exit;

public class Main {

    public static void main(String[] args) {

        if (args.length == 3 || args.length == 5) {
            String name = args[0];
            Integer lossPercentage = null;
            String parentIp = null;
            Integer parentPort = null;
            Integer selfPort = null;
            try {
                selfPort = Integer.valueOf(args[2]);
                lossPercentage = Integer.valueOf(args[1]);
                if (args.length == 5) {
                    parentIp = args[3];
                    parentPort = Integer.valueOf(args[4]);
                }
            } catch (NumberFormatException ex) {
                handleError();
            }

            // if all args are ok
            Server server = null;
            try {
                server = (parentIp != null && parentPort != null)
                        ? new Server(name, selfPort, lossPercentage, parentIp, parentPort)
                        : new Server(name, selfPort, lossPercentage);
            } catch (IOException ex) {
                ex.printStackTrace();
                handleError();
            }
            if (server != null) {
                new Thread(server).start();
            }

        } else {
            handleError();
        }
    }

    private static void handleError() {
        System.out.println("usage: .jar name lossPercent port [parentIP parentPort]");
        exit(0);
    }

}
