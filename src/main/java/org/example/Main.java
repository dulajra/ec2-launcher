package org.example;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            if ("w".equals(args[0])) {
                System.out.println("\n*************************");
                System.out.println("Start app in worker mode");
                System.out.println("*************************\n");
                new Worker().run();
            } else if ("a".equals(args[0])) {
                System.out.println("\n*************************");
                System.out.println("Start app in auto scalar mode");
                System.out.println("*************************\n");
                new AutoScalar().run();
            } else {
                System.out.println("Unrecognized option");
            }
        } else {
            System.out.println("No arguments supplied");
        }
    }
}
