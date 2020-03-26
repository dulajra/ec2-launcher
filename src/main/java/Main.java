package main.java;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            if ("w".equals(args[0])) {
                System.out.println("Start app in worker mode");
                new Worker().run();
            } else if ("a".equals(args[0])) {
                System.out.println("Start app in auto scalar mode");
                new AutoScalar().run();
            } else {
                System.out.println("Unrecognized option");
            }
        }
    }
}
