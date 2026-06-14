package demo;

import java.util.Scanner;

import CausalMulticast.*;

public class Client implements ICausalMulticast {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: ./gradlew :app:run --args=\"<ip> <porta>\"");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        System.err.println("IP: " + ip);
        System.err.println("Port: " + port);

        CausalMulticast causalMulticast = new CausalMulticast(ip, port, new Client());

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("exit")) {
                break;
            }

            causalMulticast.mcsend(message, new Client());
        }

        System.out.println("Hello, World!");
    }

    public void deliver(String message) {
        System.out.println("Received message: " + message);
    }
}